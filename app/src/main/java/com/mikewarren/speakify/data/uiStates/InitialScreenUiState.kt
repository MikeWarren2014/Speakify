package com.mikewarren.speakify.data.uiStates

sealed interface InitialScreenUiState {
    data object Title : InitialScreenUiState
    data object SignUp : InitialScreenUiState
    data object SignIn : InitialScreenUiState

}