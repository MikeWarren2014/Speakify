package com.mikewarren.speakify.viewsAndViewModels.pages.auth


import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.data.SessionRepository
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.data.uiStates.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = sessionRepository.uiState
    val accountDeletionUiState: StateFlow<AccountDeletionUiState> = sessionRepository.accountDeletionUiState
    val isDeletionInProgress: Flow<Boolean> = sessionRepository.accountDeletionUiState
        .map { it !is AccountDeletionUiState.NotRequested }


    fun markAccountForDeletion() {
        sessionRepository.markAccountForDeletion()
    }

    fun markAccountVerified() {
        sessionRepository.markAccountVerified()
    }

    fun cancelAccountDeletion() {
        sessionRepository.cancelAccountDeletion()
    }

    fun signOut() {
        sessionRepository.signOut()
    }
}


