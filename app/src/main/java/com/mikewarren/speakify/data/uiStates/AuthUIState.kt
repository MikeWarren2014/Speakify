package com.mikewarren.speakify.data.uiStates

sealed interface AuthUiState {
    data object Success : AuthUiState
    data object SignedOut : AuthUiState
    data object NeedsVerification : AuthUiState
}