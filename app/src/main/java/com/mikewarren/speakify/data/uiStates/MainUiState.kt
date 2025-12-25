package com.mikewarren.speakify.data.uiStates

sealed interface MainUiState {
    data object Loading : MainUiState
    data object SignedIn : MainUiState
    data object SignedOut : MainUiState
    data object AccountDeleted : MainUiState
}