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
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
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
    private val accountDeletionFirestoreRepository: AccountDeletionFirestoreRepository,
    private val settingsRepository: SettingsRepository,
    private val trialRepository: TrialRepository
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
        combine(Clerk.isInitialized, Clerk.userFlow, trialRepository.trialStatus) { isInitialized, user, trialStatus ->
            Triple(isInitialized, user, trialStatus)
        }
            .distinctUntilChanged()
            .onEach { (isInitialized, user, trialStatus) ->
                if (!isInitialized) {
                    _uiState.value = MainUiState.Loading
                    return@onEach
                }

                if (user == null) {
                    // If we are currently in the TrialEnded state (showing thank you message), 
                    // we don't want the automated logic to jump immediately to SignedOut.
                    if (_uiState.value == MainUiState.TrialEnded) return@onEach

                    if (isHandlingSpecialTrialStatus(trialStatus)) {
                        return@onEach
                    }
                    onSuccessfulSignOut()
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

    private fun isHandlingSpecialTrialStatus(trialStatus: TrialStatus): Boolean {
        if (trialStatus is TrialStatus.Active) {
            _uiState.value = if (isTrialAuthorized) MainUiState.TrialUsage else MainUiState.TrialActive
            return true
        }
        if (trialStatus is TrialStatus.Loading) {
            // Check current state to avoid infinite loop if it's already MainUiState.Loading
            if (_uiState.value != MainUiState.Loading) {
                _uiState.value = MainUiState.Loading
            }
            // Launch on IO to avoid blocking main thread
            scope.launch(Dispatchers.IO) { trialRepository.refreshTrialStatus() }
            return true
        }
        return false
    }

    fun proceedToTrialSession() {
        isTrialAuthorized = true
        _uiState.value = MainUiState.TrialUsage
    }

    fun startTrialConversion() {
        _uiState.value = MainUiState.TrialConversion
    }

    fun endTrial() {
        _uiState.value = MainUiState.TrialEnded
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
        firebaseAuth.signOut()
        settingsRepository.clearAllData()
        _uiState.value = MainUiState.SignedOut
    }

    suspend fun deleteUserData(): Result<Unit> {
        return accountDeletionFirestoreRepository.deleteUserData()
    }
}
