package com.mikewarren.speakify.data.uiStates

sealed interface SignUpUiState {
    data object Success : SignUpUiState
    data object SignedOut : SignUpUiState
    data object NeedsVerification : SignUpUiState
}