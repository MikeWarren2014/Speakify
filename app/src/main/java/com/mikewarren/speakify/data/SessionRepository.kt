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
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.db.firestore.AccountDeletionFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FeedbackFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.utils.AnalyticsHelper
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    // Flag to track if the user has dismissed the TrialActiveView in the current "session"
    private var isTrialAuthorized = false

    init {
        combine(
            Clerk.isInitialized,
            Clerk.userFlow,
            trialRepository.trialModelFlow,
            onboardingRepository.appOpenCount,
            onboardingRepository.onboardingStep
        ) { isInitialized, user, trialModel, openCount, onboardingStep ->
            DataBundle(isInitialized, user, trialModel.status, openCount, onboardingStep)
        }
            .distinctUntilChanged()
            .onEach { (isInitialized, user, trialStatus, openCount, onboardingStep) ->
                if (!isInitialized) {
                    _uiState.value = MainUiState.Loading
                    return@onEach
                }

                if (user == null) {
                    // If we are currently in the TrialEnded state (showing thank you message), 
                    // we don't want the automated logic to jump immediately to SignedOut.
                    if (_uiState.value == MainUiState.TrialEnded) return@onEach

                    if (isHandlingSpecialTrialStatus(trialStatus, openCount, onboardingStep)) {
                        return@onEach
                    }
                    
                    // If trial status is NotNeeded but user is null, we are likely in the middle 
                    // of a sign-up/sign-in transition. We should NOT sign out and clear data yet.
                    if (trialStatus == TrialStatus.NotNeeded) {
                        Log.d("SessionRepository", "_uiState.value == ${_uiState.value}")
                        if (_uiState.value == MainUiState.Loading)
                            _uiState.value = MainUiState.SignedOut
                        return@onEach
                    }

                    onSuccessfulSessionEnd(trialStatus)
                    return@onEach
                }

                // User is logged in
                isTrialAuthorized = false // Reset trial flag if they log in

                // 1. Sign into Firebase using Clerk's OIDC JWT
                scope.launch(Dispatchers.IO) {
                    try {
                        // Check if already signed in to the correct user to avoid redundant calls
                        if (firebaseAuth.currentUser?.email == user.emailAddresses.firstOrNull()?.emailAddress) {
                            return@launch
                        }
                        // Fetch the token using the 'firebase' template we created in Clerk
                        Clerk.session?.fetchToken(GetTokenOptions("firebase"))
                            ?.onSuccess { tokenResource ->
                                val clerkToken = tokenResource.jwt
                                // Create a credential for the OIDC provider we set up in Firebase
                                val credential = OAuthProvider.newCredentialBuilder("oidc.clerk")
                                    .setIdToken(clerkToken)
                                    .build()

                                try {
                                    firebaseAuth.signInWithCredential(credential).await()
                                    Log.d("SessionRepo", "Successfully bridged Clerk to Firebase via OIDC")

                                    // Give Firebase a brief moment to initialize its connection/state
                                    delay(500)

                                    // 2. Trigger sync
                                    val result = firestoreSyncRepository.downloadAndRestoreData()
                                    if (result.isFailure) {
                                        Log.e("SessionRepo", "Failed to sync Firestore data after login", result.exceptionOrNull())
                                    }
                                } catch (e: Exception) {
                                    Log.e("SessionRepo", "Failed to sign into Firebase with credential", e)
                                }
                            }
                            ?.onFailure {
                                Log.e("SessionRepo", "Failed to fetch Clerk token for Firebase: ${it.longErrorMessageOrNull}")
                            }
                    } catch (e: Exception) {
                        Log.e("SessionRepo", "Failed to bridge Clerk to Firebase", e)
                    }
                }
                _uiState.value = MainUiState.SignedIn
                return@onEach

            }
            .launchIn(scope)
    }

    private fun isHandlingSpecialTrialStatus(
        trialStatus: TrialStatus,
        openCount: Int,
        onboardingStep: OnboardingUiState
    ): Boolean {
        if (trialStatus is TrialStatus.Active) {
            // Trigger onboarding after 2 opens if not already completed
            if (openCount >= 2 && onboardingStep != OnboardingUiState.Completed) {
                _uiState.value = MainUiState.Onboarding(onboardingStep)
                analyticsHelper.logOnboardingStep(onboardingStep.toString())
                return true
            }

            _uiState.value = if (isTrialAuthorized) MainUiState.TrialUsage else MainUiState.TrialActive
            return true
        }
        // TODO: are we *ever* using this status anywhere?
        if (trialStatus is TrialStatus.Loading) {
            // Check current state to avoid infinite loop if it's already MainUiState.Loading
            if (_uiState.value != MainUiState.Loading) {
                _uiState.value = MainUiState.Loading
            }
            // Launch on IO to avoid blocking main thread
            scope.launch(Dispatchers.IO) {
                try {
                    trialRepository.refreshTrialStatus()
                } catch (e: Exception) {
                    Log.e("SessionRepository", "Failed to refresh trial status", e)
                }
            }
            return true
        }
        return false
    }

    fun proceedToTrialSession() {
        isTrialAuthorized = true
        _uiState.value = MainUiState.TrialUsage
        analyticsHelper.logTrialContinued()
    }

    fun startTrialConversion() {
        _uiState.value = MainUiState.TrialConversion
        analyticsHelper.logTrialConversionStarted()
    }

    fun resetTrialAuthorized() {
        isTrialAuthorized = false
        // Reset the UI state to Loading so we don't show a stale state on re-entry
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

    fun saveVeryImportantApps(vias: List<UserAppModel>) {
        scope.launch {
            onboardingRepository.saveVeryImportantApps(vias)
            analyticsHelper.logVIAs(vias)
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
        val trialStatus: TrialStatus,
        val openCount: Int,
        val onboardingStep: OnboardingUiState
    )
}
