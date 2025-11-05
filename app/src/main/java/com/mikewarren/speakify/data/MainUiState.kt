package com.mikewarren.speakify.data

sealed interface MainUiState {
    data object Loading : MainUiState
    data object SignedIn : MainUiState
    data object SignedOut : MainUiState
}