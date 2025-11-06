package com.mikewarren.speakify.data

import com.mikewarren.speakify.viewsAndViewModels.pages.auth.SignUpViewModel

sealed interface SignUpUiState {
    data object Success : SignUpUiState
    data object SignedOut : SignUpUiState
    data object NeedsVerification : SignUpUiState
}