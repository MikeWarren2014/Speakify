package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.mikewarren.speakify.data.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlin.onFailure
import kotlin.onSuccess

class MainViewModel: ViewModel() {
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        // TODO: we should move uiState to a SessionRepository, or something similar
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            _uiState.value = when {
                !isInitialized -> MainUiState.Loading
                user != null -> MainUiState.SignedIn
                else -> MainUiState.SignedOut
            }
        }
            .launchIn(viewModelScope)
    }

    fun signOut() {
        viewModelScope.launch() {
            Clerk.signOut()
                .onSuccess { _uiState.value = MainUiState.SignedOut }
                .onFailure {
                    // See https://clerk.com/docs/guides/development/custom-flows/error-handling
                    // for more info on error handling
                    Log.e("MainViewModel", it.longErrorMessageOrNull, it.throwable)
                }
        }
    }
}

