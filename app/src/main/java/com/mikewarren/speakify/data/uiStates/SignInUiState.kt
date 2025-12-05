package com.mikewarren.speakify.data.uiStates

sealed interface SignInUiState {
    data object Idle : SignInUiState

    data object Loading: SignInUiState

    data class Error(val message: String) : SignInUiState

    data object Success : SignInUiState

    data class ResetPassword(val reason: String) : SignInUiState {
        companion object ResetPasswordReasons {
            val ForgotPassword = "Forgot Password"
            val PwnedCredentials = "Pwned Credentials"
        }
    }
}
