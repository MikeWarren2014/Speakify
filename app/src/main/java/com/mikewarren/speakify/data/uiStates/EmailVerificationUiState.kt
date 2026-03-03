package com.mikewarren.speakify.data.uiStates

sealed interface EmailVerificationUiState {
    data object Initial: EmailVerificationUiState
    data object TooManyAttempts: EmailVerificationUiState
    data object RequestCodeReady: EmailVerificationUiState
    data object ExpiredCode: EmailVerificationUiState
    data object Success: EmailVerificationUiState
}