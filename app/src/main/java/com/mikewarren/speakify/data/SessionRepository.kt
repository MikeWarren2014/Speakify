package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.fold
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.clerk.api.user.delete
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.mikewarren.speakify.data.db.firestore.AccountDeletionFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.data.models.FeedbackModel
import com.mikewarren.speakify.data.models.RatingsPromptModel
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.utils.AnalyticsHelper
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SessionRepository @Inject constructor(
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val accountDeletionFirestoreRepository: AccountDeletionFirestoreRepository,
    private val settingsRepository: SettingsRepository,
    val trialRepository: TrialRepository,
    val onboardingRepository: OnboardingRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val authMessageRepository: AuthMessageRepository
): ITaggable {
    private val _accountDeletionUiState = MutableStateFlow<AccountDeletionUiState>(
        AccountDeletionUiState.NotRequested)
    val accountDeletionUiState = _accountDeletionUiState.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Flags to track background operations and avoid redundant/looping calls
    private var isTrialAuthorized = false
    internal var lastDataBundle: DataBundle? = null
    private var isSyncing = false
    private var syncedUserId: String? = null

    init {
        Log.d("SessionRepository", "Initializing SessionRepository")
        combine(
            Clerk.isInitialized,
            Clerk.userFlow,
            trialRepository.trialModelFlow,
            trialRepository.isNewDirectSignUp,
            onboardingRepository.appOpenCount,
            onboardingRepository.onboardingStep,
            onboardingRepository.speakificationCount,
            onboardingRepository.hasShownRatingsPrompt,
            onboardingRepository.hasShownTrialConversionPrompt,
            onboardingRepository.onboardingModel.map { it.ratingsPrompt },
            onboardingRepository.feedback
        ) { args ->
            DataBundle(
                isInitialized = args[0] as Boolean,
                user = args[1] as com.clerk.api.user.User?,
                trialModel = args[2] as TrialModel,
                isNewDirectSignUp = args[3] as Boolean,
                openCount = args[4] as Int,
                onboardingStep = args[5] as OnboardingUiState,
                speakificationCount = args[6] as Int,
                hasShownRatingsPrompt = args[7] as Boolean,
                hasShownTrialConversionPrompt = args[8] as Boolean,
                ratingsPrompt = args[9] as RatingsPromptModel,
                feedback = args[10] as FeedbackModel?
            )
        }
            .distinctUntilChanged()
            .onEach { incomingBundle: DataBundle ->
                val mergedBundle = lastDataBundle?.let { last ->
                    mergeDataBundles(last, incomingBundle)
                } ?: incomingBundle

                lastDataBundle = mergedBundle

                Log.d("SessionRepository", "Reacting to DataBundle: init=${mergedBundle.isInitialized}, user=${mergedBundle.user?.id}, trialStatus=${mergedBundle.trialModel.status}, openCount=${mergedBundle.openCount}, step=${mergedBundle.onboardingStep}")
                scope.launch {
                    reactToSessionState(mergedBundle)
                }
            }
            .launchIn(scope)
    }

    private fun mergeDataBundles(old: DataBundle, new: DataBundle): DataBundle {
        // If we are in the same identity (both guest or same user), prevent regressions in progress
        return if (old.user?.id == new.user?.id) {
            new.copy(
                openCount = maxOf(old.openCount, new.openCount),
                speakificationCount = maxOf(old.speakificationCount, new.speakificationCount),
                onboardingStep = if (old.onboardingStep == OnboardingUiState.Completed) OnboardingUiState.Completed else new.onboardingStep,
                hasShownRatingsPrompt = old.hasShownRatingsPrompt || new.hasShownRatingsPrompt,
                hasShownTrialConversionPrompt = old.hasShownTrialConversionPrompt || new.hasShownTrialConversionPrompt,
                ratingsPrompt = if (new.ratingsPrompt.numberOfReviewAsks >= old.ratingsPrompt.numberOfReviewAsks) new.ratingsPrompt else old.ratingsPrompt,
                feedback = new.feedback ?: old.feedback
            )
        } else {
            // Transition between guest/signed-in or between different users.
            new
        }
    }

    private suspend fun reactToSessionState(dataBundle: DataBundle) {
        val (isInitialized,
            user,
            trialModel,
            isNewDirectSignUp,
            openCount,
            onboardingStep,
            speakificationCount,
            hasShownRatingsPrompt,
            hasShownTrialConversionPrompt,
            ratingsPrompt,
            feedback) = dataBundle

        if (!isInitialized) {
            _uiState.value = MainUiState.Loading
            return
        }

        val trialStatus = trialModel.status

        val engagementContext = createTrialEngagementContext(dataBundle)

        if (user == null) {
            if (_uiState.value == MainUiState.TrialEnded) return

            if (isHandlingTrialEngagement(engagementContext)) {
                return
            }

            onSuccessfulSessionEnd(trialStatus)
            return
        }

        // User is logged in
        isTrialAuthorized = false

        if (isSyncing) {
            _uiState.value = MainUiState.Loading
            return
        }

        val syncResult = signIntoAndSyncWithFirebase(user)
        if (syncResult.isFailure) {
            Log.e("SessionRepository", "Failed to bridge Clerk to Firebase", syncResult.exceptionOrNull())
            signOut()
            authMessageRepository.postMessage("Failed to synchronize account data. Please try again.")
            return
        }

        // If a sync actually occurred, the 'finally' block in signIntoAndSyncWithFirebase
        // has already triggered a fresh reactToSessionState call with the latest data.
        // We MUST return here to avoid proceeding with stale dataBundle/engagementContext.
        if (syncResult.getOrDefault(false)) return

        Log.d("SessionRepository", "engagementContext == $engagementContext")
        if (isHandlingTrialEngagement(engagementContext))
            return

        if (isNewDirectSignUp) {
            if (onboardingStep == OnboardingUiState.Completed) {
                trialRepository.resetNewDirectSignUp()
                return
            }
            setOnboardingState(OnboardingUiState.NotStarted)
            return
        }

        _uiState.value = MainUiState.SignedIn
    }

    private fun createTrialEngagementContext(dataBundle: DataBundle): TrialEngagementContext {
        val (isInitialized,
            user,
            trialModel,
            isNewDirectSignUp,
            openCount,
            onboardingStep,
            speakificationCount,
            hasShownRatingsPrompt,
            hasShownTrialConversionPrompt,
            ratingsPrompt,
            feedback,
            ) = dataBundle

        if ((trialModel.status in listOf(TrialStatus.NotStarted, TrialStatus.NotNeeded)) &&
            (user == null)) {
            return TrialEngagementContext.Other(
                trialModel,
                onboardingStep,
                speakificationCount,
                openCount,
                hasShownRatingsPrompt,
                hasShownTrialConversionPrompt,
                ratingsPrompt,
                feedback
            )
        }

        return TrialEngagementContext.from(
            trialModel,
            onboardingStep,
            speakificationCount,
            openCount,
            hasShownRatingsPrompt,
            hasShownTrialConversionPrompt,
            ratingsPrompt,
            feedback
        )
    }

    private fun isHandlingTrialEngagement(context: TrialEngagementContext?): Boolean {
        if (context == null) return false

        return when (context) {
            is TrialEngagementContext.TrialBypass,
            is TrialEngagementContext.Active -> {
                if (context.shouldShowOnboarding()) {
                    setOnboardingState(context.onboardingStep)
                    return true
                }

                if (!isTrialAuthorized) {
                    if (context.shouldShowTrialConversionPrompt()) {
                        _uiState.value = MainUiState.TrialConversionPrompt
                        return true
                    }
                    if (context.shouldShowRatingsPrompt()) {
                        _uiState.value = MainUiState.RatingsPrompt(context.feedback)
                        return true
                    }
                }

                if (context is TrialEngagementContext.TrialBypass) return false

                _uiState.value = if (isTrialAuthorized) MainUiState.TrialUsage else MainUiState.TrialActive
                true
            }

            is TrialEngagementContext.Loading -> {
                if (_uiState.value != MainUiState.Loading) {
                    _uiState.value = MainUiState.Loading
                }
                scope.launch(Dispatchers.IO) {
                    try {
                        trialRepository.refreshTrialStatus()
                    } catch (e: Exception) {
                        Log.e("SessionRepository", "Failed to refresh trial status", e)
                    }
                }
                true
            }

            is TrialEngagementContext.Other -> false
        }
    }

    private suspend fun signIntoAndSyncWithFirebase(user: com.clerk.api.user.User): Result<Boolean> {
        if (isSyncing || syncedUserId == user.id) return Result.success(false)

        isSyncing = true
        return try {
            val signInResult = signInToFirebase(user)
            if (signInResult.isFailure) {
                return Result.failure(signInResult.exceptionOrNull() ?: Exception("Unknown sign-in error"))
            }

            withContext(Dispatchers.IO) {
                Log.d("SessionRepo", "Downloading the user data from Firebase")
                firestoreSyncRepository.downloadAndRestoreData()
            }
            syncedUserId = user.id
            Log.d("SessionRepo", "Successfully synced data for user ${user.id}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SessionRepo", "Failed to sync data", e)
            Result.failure(e)
        } finally {
            isSyncing = false
            // Now that we're done (success or fail), we trigger a re-evaluation
            // without being blocked by the 'isSyncing' flag.
            lastDataBundle?.let { reactToSessionState(it) }
        }
    }

    private suspend fun signInToFirebase(user: com.clerk.api.user.User): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            val currentUser = firebaseAuth.currentUser
            val clerkEmail = user.emailAddresses.firstOrNull()?.emailAddress

            if (currentUser != null && clerkEmail != null && currentUser.email == clerkEmail) {
                Log.d("SessionRepo", "Firebase already signed in as $clerkEmail")
                continuation.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }

            scope.launch {
                val activeSession = Clerk.sessionFlow
                    .filterNotNull()
                    .first()

                activeSession.fetchToken(GetTokenOptions("firebase"))
                    .onSuccess { tokenResource ->
                        val clerkToken = tokenResource.jwt
                        val credential = OAuthProvider.newCredentialBuilder("oidc.clerk")
                            .setIdToken(clerkToken)
                            .build()
                        firebaseAuth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                Log.d("SessionRepo", "Firebase auth result: ${task.isSuccessful}")

                                var result = Result.success(Unit)
                                if (!task.isSuccessful) {
                                    Log.d("SessionRepo", "Firebase auth failed with message: ${task.exception?.message}")

                                    val exception = task.exception
                                    if (exception?.message?.contains("PROVIDER_ALREADY_LINKED") != true)
                                        result = Result.failure(exception ?: Exception("Unknown error"))
                                }
                                continuation.resume(result)
                            }
                    }
                    .onFailure { failure ->
                        Log.e("SessionRepo", "Failed to fetch Clerk token for Firebase: ${failure.longErrorMessageOrNull}")
                        continuation.resume(Result.failure(failure.throwable ?: Exception(failure.longErrorMessageOrNull)))
                    }
            }
        } catch (e: Exception) {
            Log.e("SessionRepo", "Failed to bridge Clerk to Firebase", e)
            continuation.resume(Result.failure(e))
        }
    }

    private fun setOnboardingState(onboardingStep: OnboardingUiState) {
        _uiState.value = MainUiState.Onboarding(onboardingStep)
        analyticsHelper.logOnboardingStep(onboardingStep.toString())
    }

    fun proceedToTrialSession() {
        if (_uiState.value == MainUiState.TrialConversionPrompt) {
            markTrialConversionShown()
        }
        isTrialAuthorized = true
        _uiState.value = MainUiState.TrialUsage
        analyticsHelper.logTrialContinued()
    }

    fun startTrialConversion() {
        markTrialConversionShown()
        _uiState.value = MainUiState.TrialConversion
        analyticsHelper.logTrialConversionStarted()
    }

    fun markTrialConversionShown() {
        scope.launch {
            onboardingRepository.setHasShownTrialConversionPrompt(true)
            lastDataBundle = lastDataBundle?.copy(hasShownTrialConversionPrompt = true)
            lastDataBundle?.let { reactToSessionState(it) }
        }
    }

    fun markRatingsPromptShown() {
        scope.launch {
            onboardingRepository.setHasShownRatingsPrompt(true)
            lastDataBundle = lastDataBundle?.copy(hasShownRatingsPrompt = true)
            lastDataBundle?.let { reactToSessionState(it) }
        }
    }

    fun recordRatingsPromptAsk() {
        scope.launch {
            val currentModel = onboardingRepository.onboardingModel.first()
            val newCount = currentModel.ratingsPrompt.numberOfReviewAsks + 1
            val currentTime = System.currentTimeMillis()
            onboardingRepository.updateRatingsPrompt(currentTime, newCount)

            lastDataBundle = lastDataBundle?.copy(
                ratingsPrompt = RatingsPromptModel(currentTime, newCount)
            )
            lastDataBundle?.let { reactToSessionState(it) }
        }
    }

    fun resetTrialAuthorized() {
        isTrialAuthorized = false
        if (_uiState.value is MainUiState.TrialUsage) {
            _uiState.value = MainUiState.TrialActive
        }
    }

    fun incrementAppOpenCount() {
        scope.launch {
            onboardingRepository.incrementAppOpenCount()
            lastDataBundle = lastDataBundle?.copy(openCount = (lastDataBundle?.openCount ?: 0) + 1)
            lastDataBundle?.let { reactToSessionState(it) }
        }
    }

    fun endTrial() {
        _uiState.value = MainUiState.TrialEnded
    }

    fun updateOnboardingStep(step: OnboardingUiState) {
        scope.launch {
            onboardingRepository.updateOnboardingStep(step)
            analyticsHelper.logOnboardingStep(step.toString())
            lastDataBundle = lastDataBundle?.copy(onboardingStep = step)
            lastDataBundle?.let { reactToSessionState(it) }
        }
    }

    fun saveFeedback(feedback: FeedbackModel) {
        scope.launch {
            onboardingRepository.saveFeedback(feedback)
            analyticsHelper.logFeedback(feedback)
        }
    }

    fun savePrimaryGoal(goal: String) {
        scope.launch {
            onboardingRepository.savePrimaryGoal(goal)
            analyticsHelper.logPrimaryGoal(goal)
        }
    }

    fun saveImportantAppCategories(categories: List<String>) {
        scope.launch {
            onboardingRepository.saveImportantAppCategories(categories)
        }
    }

    fun setAccountDeletionUiState(state: AccountDeletionUiState) {
        _accountDeletionUiState.value = state
    }

    fun markAccountForDeletion() {
        CoroutineScope(Dispatchers.IO).launch {
            _accountDeletionUiState.value = AccountDeletionUiState.RequestMade
        }
    }

    fun cancelAccountDeletion() {
        CoroutineScope(Dispatchers.IO).launch {
            _accountDeletionUiState.value = AccountDeletionUiState.NotRequested
        }
    }

    fun markAccountVerified() {
        CoroutineScope(Dispatchers.IO).launch {
            _accountDeletionUiState.value = AccountDeletionUiState.Verified
        }
    }

    fun signOut() {
        scope.launch {
            Clerk.signOut()
                .onSuccess {
                    onSuccessfulSignOut()
                }
                .onFailure {
                    Log.e("SessionRepository", it.longErrorMessageOrNull, it.throwable)
                }
        }
    }

    private suspend fun onSuccessfulSignOut() {
        onSuccessfulSessionEnd(TrialStatus.NotNeeded)
    }

    private suspend fun onSuccessfulSessionEnd(trialStatus: TrialStatus){
        if (_uiState.value in listOf(MainUiState.SignedOut, MainUiState.TrialEnded)) {
            return
        }

        settingsRepository.clearAllData()
        if (trialStatus is TrialStatus.Expired) {
            _uiState.value = MainUiState.TrialEnded
            return
        }
        firebaseAuth.signOut()

        syncedUserId = null
        _uiState.value = MainUiState.SignedOut
    }

    suspend fun deleteUser(): Result<Unit> {
        val result = accountDeletionFirestoreRepository.deleteUserData()

        if (result.isFailure) {
            LogUtils.LogWarning(TAG, "Failed to delete Firestore data: ${result.exceptionOrNull()?.message}")
            // We might want to show an error to the user here, 
            // but proceeding with account deletion is often safer to ensure the account is gone.
        }

        return Clerk.user!!.delete().fold(
            onSuccess = {
                firebaseAuth.currentUser?.delete()
                setAccountDeletionUiState(AccountDeletionUiState.Deleted)
                Result.success(Unit)
            },
            onFailure = { failure ->
                LogUtils.LogWarning(TAG, "Error deleting user: ${failure.longErrorMessageOrNull}")
                Result.failure(failure.throwable ?: Exception("Unknown error"))
            }
        )
    }

    internal data class DataBundle(
        val isInitialized: Boolean,
        val user: com.clerk.api.user.User?,
        val trialModel: TrialModel,
        val isNewDirectSignUp: Boolean,
        val openCount: Int,
        val onboardingStep: OnboardingUiState,
        val speakificationCount: Int,
        val hasShownRatingsPrompt: Boolean,
        val hasShownTrialConversionPrompt: Boolean,
        val ratingsPrompt: RatingsPromptModel,
        val feedback: FeedbackModel?
    )

}
