package com.mikewarren.speakify.viewsAndViewModels.pages.auth.accountDeletion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.delete
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SessionRepository
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel(), ITaggable {


    val uiState = sessionRepository.accountDeletionUiState


    fun shouldReverify() : Boolean {
        return System.currentTimeMillis() - Clerk.session!!.createdAt >= 10 * Constants.OneMinute
    }

    fun startDeletionProcess() {

        viewModelScope.launch(Dispatchers.IO) {
            if (shouldReverify()) {
                sessionRepository.setAccountDeletionUiState(AccountDeletionUiState.SigningOut)

                sessionRepository.signOut()
                return@launch
            }
            sessionRepository.setAccountDeletionUiState(AccountDeletionUiState.Verified)
        }
    }

    fun cancelAccountDeletion() {
        sessionRepository.cancelAccountDeletion()
    }


    fun deleteUser() {
        viewModelScope.launch(Dispatchers.IO) {

            Clerk.user!!
                .delete()
                .onSuccess {
                    sessionRepository.setAccountDeletionUiState(AccountDeletionUiState.Deleted)
                }
                .onFailure {
                    LogUtils.LogWarning(TAG, "Error deleting user: ${it.longErrorMessageOrNull}")
                }

        }
    }

}