package com.mikewarren.speakify.data // Or a 'repositories' sub-package

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
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

    init {
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            isInitialized to user
        }
            .distinctUntilChanged()
            .onEach { (isInitialized, user) ->
                _uiState.value = when {
                    !isInitialized -> MainUiState.Loading
                    user != null -> {
                        // Trigger a download when the user signs in
                        scope.launch(Dispatchers.IO) {
                            firestoreSyncRepository.downloadAndRestoreData()
                        }
                        MainUiState.SignedIn
                    }
                    else -> MainUiState.SignedOut
                }
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
        CoroutineScope(Dispatchers.IO).launch { // Use a dedicated scope
            Clerk.signOut()
                .onSuccess {
                    _uiState.value = MainUiState.SignedOut
                }
                .onFailure {
                    Log.e("SessionRepository", it.longErrorMessageOrNull, it.throwable)
                }
        }
    }
}
