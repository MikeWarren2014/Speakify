package com.mikewarren.speakify.data.uiStates

sealed interface AccountDeletionUiState {
    object RequestMade: AccountDeletionUiState
    object RequestConfirmed: AccountDeletionUiState
    object Verified: AccountDeletionUiState
    object Deleted: AccountDeletionUiState

}