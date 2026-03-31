package com.mikewarren.speakify.data.uiStates

sealed interface MainUiState {
    data object Loading : MainUiState
    data object SignedIn : MainUiState
    data object TrialActive : MainUiState

    data object TrialUsage: MainUiState
    data object TrialEnded: MainUiState
    data object SignedOut : MainUiState
    data object TrialConversion: MainUiState
}