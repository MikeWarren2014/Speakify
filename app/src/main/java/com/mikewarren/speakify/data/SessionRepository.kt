package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.mikewarren.speakify.data.db.firestore.AccountDeletionFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FeedbackFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import com.mikewarren.speakify.utils.AnalyticsHelper
import com.mikewarren.speakify.utils.log.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val feedbackFirestoreRepository: FeedbackFirestoreRepository,
    private val accountDeletionFirestoreRepository: AccountDeletionFirestoreRepository,
    private val settingsRepository: SettingsRepository,
    val trialRepository: TrialRepository,
    val onboardingRepository: OnboardingRepository,
    private val analyticsHelper: AnalyticsHelper
) {
    private val _accountDeletionUiState = MutableStateFlow<AccountDeletionUiState>(
        AccountDeletionUiState.NotRequested)
    val accountDeletionUiState = _accountDeletionUiState.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Flags to track background operations and avoid redundant/looping calls
    private var isTrialAuthorized = false

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
            onboardingRepository.hasShownTrialConversionPrompt
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
                hasShownTrialConversionPrompt = args[8] as Boolean
            )
        }
            .distinctUntilChanged()
            .onEach { dataBundle: DataBundle ->
                Log.d("SessionRepository", "New DataBundle: init=${dataBundle.isInitialized}, user=${dataBundle.user?.id}, trialStatus=${dataBundle.trialModel.status}")
                reactToSessionState(dataBundle)
            }
            .launchIn(scope)
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
            hasShownTrialConversionPrompt) = dataBundle

        if (!isInitialized) {
            _uiState.value = MainUiState.Loading
            return
        }

        val trialStatus = trialModel.status

        val engagementContext = if ((trialStatus == TrialStatus.NotNeeded) && (!isNewDirectSignUp)) {
            null
        } else {
            TrialEngagementContext.from(
                trialModel,
                onboardingStep,
                speakificationCount,
                openCount,
                hasShownRatingsPrompt,
                hasShownTrialConversionPrompt
            )
        }

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

        signIntoAndSyncWithFirebase(user)

        if (isNewDirectSignUp) {
            if (isHandlingTrialEngagement(engagementContext))
                return

            if (onboardingStep == OnboardingUiState.Completed) {
                trialRepository.resetNewDirectSignUp()
                return
            }
            setOnboardingState(OnboardingUiState.NotStarted)
            return
        }

        _uiState.value = MainUiState.SignedIn
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
                        _uiState.value = MainUiState.RatingsPrompt
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

    private fun signIntoAndSyncWithFirebase(user: com.clerk.api.user.User) {
        signInToFirebase(user, { result ->
            if (result.isSuccess) {
                Log.d("SessionRepo", "Successfully signed into Firebase")

                assert(firebaseAuth.currentUser != null)

                scope.launch(Dispatchers.IO) {
                    firestoreSyncRepository.downloadAndRestoreData()
                    feedbackFirestoreRepository.syncFeedback()
                }

                return@signInToFirebase
            }

            Log.e("SessionRepo", "Failed to sign into Firebase", result.exceptionOrNull())
            signOut()
        })
    }

    private fun signInToFirebase(user: com.clerk.api.user.User, onDone: (result: Result<Unit>) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentUser = firebaseAuth.currentUser
                val clerkEmail = user.emailAddresses.firstOrNull()?.emailAddress

                // If we are already signed in with the correct user, skip.
                if (currentUser != null && clerkEmail != null && currentUser.email == clerkEmail) {
                    Log.d("SessionRepo", "Firebase already signed in as $clerkEmail")
                    onDone(Result.success(Unit))
                    return@launch
                }

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
                                var result = Result.success(Unit)
                                if (!task.isSuccessful) {
                                    val exception = task.exception
                                    if (exception?.message?.contains("PROVIDER_ALREADY_LINKED") != true)
                                        result = Result.failure(exception ?: Exception("Unknown error"))
                                }

                                onDone(result)
                            }
                    }
                    .onFailure { failure ->
                        Log.e("SessionRepo", "Failed to fetch Clerk token for Firebase: ${failure.longErrorMessageOrNull}")
                        failure.throwable?.let { onDone(Result.failure(it)) }
                    }

            } catch (e: Exception) {
                Log.e("SessionRepo", "Failed to bridge Clerk to Firebase", e)
                onDone(Result.failure(e))
            }
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
        }
    }

    fun markRatingsPromptShown() {
        scope.launch {
            onboardingRepository.setHasShownRatingsPrompt(true)
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
        }
    }

    fun endTrial() {
        _uiState.value = MainUiState.TrialEnded
    }

    fun updateOnboardingStep(step: OnboardingUiState) {
        scope.launch {
            onboardingRepository.updateOnboardingStep(step)
            analyticsHelper.logOnboardingStep(step.toString())
            if (Clerk.user != null) {
                feedbackFirestoreRepository.syncFeedback()
            }
        }
    }

    fun saveSurveyResult(result: String) {
        scope.launch {
            onboardingRepository.saveSurveyResult(result)
            analyticsHelper.logSurveyResult(result)
            if (Clerk.user != null) {
                feedbackFirestoreRepository.syncFeedback()
            }
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
        if (_uiState.value == MainUiState.SignedOut || _uiState.value == MainUiState.TrialEnded) {
            return
        }

        settingsRepository.clearAllData()
        if (trialStatus is TrialStatus.Expired) {
            _uiState.value = MainUiState.TrialEnded
            return
        }
        firebaseAuth.signOut()
        _uiState.value = MainUiState.SignedOut
    }

    suspend fun deleteUserData(): Result<Unit> {
        return accountDeletionFirestoreRepository.deleteUserData()
    }

    private data class DataBundle(
        val isInitialized: Boolean,
        val user: com.clerk.api.user.User?,
        val trialModel: TrialModel,
        val isNewDirectSignUp: Boolean,
        val openCount: Int,
        val onboardingStep: OnboardingUiState,
        val speakificationCount: Int,
        val hasShownRatingsPrompt: Boolean,
        val hasShownTrialConversionPrompt: Boolean
    )

}
