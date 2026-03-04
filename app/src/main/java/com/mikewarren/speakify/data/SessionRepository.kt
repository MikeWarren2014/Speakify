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
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val firestoreSyncRepository: FirestoreSyncRepository
) {
    private val _accountDeletionUiState = MutableStateFlow<AccountDeletionUiState>(
        AccountDeletionUiState.NotRequested)
    val accountDeletionUiState = _accountDeletionUiState.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private val firebaseAuth = FirebaseAuth.getInstance()

    init {
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            isInitialized to user
        }
            .distinctUntilChanged()
            .onEach { (isInitialized, user) ->
                if (!isInitialized) {
                    _uiState.value = MainUiState.Loading
                    return@onEach
                }

                if (user != null) {
                    // 1. Sign into Firebase using Clerk's OIDC JWT
                    scope.launch(Dispatchers.IO) {
                        try {
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
                                        // 2. Trigger sync
                                        firestoreSyncRepository.downloadAndRestoreData()
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
                firebaseAuth.signOut()
                _uiState.value = MainUiState.SignedOut
            }
            .launchIn(scope)
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
        CoroutineScope(Dispatchers.IO).launch {
            Clerk.signOut()
                .onSuccess {
                    firebaseAuth.signOut()
                    _uiState.value = MainUiState.SignedOut
                }
                .onFailure {
                    Log.e("SessionRepository", it.longErrorMessageOrNull, it.throwable)
                }
        }
    }
}
