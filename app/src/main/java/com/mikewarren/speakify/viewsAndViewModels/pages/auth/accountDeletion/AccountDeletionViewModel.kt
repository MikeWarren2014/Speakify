package com.mikewarren.speakify.viewsAndViewModels.pages.auth.accountDeletion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.attemptVerification
import com.clerk.api.emailaddress.prepareVerification
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.delete
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountDeletionViewModel : ViewModel(), ITaggable {
    private val _uiState = MutableStateFlow<AccountDeletionUiState>(AccountDeletionUiState.RequestMade)
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val userEmailAddress = Clerk.user!!
        .emailAddresses
        .first()


    fun startDeletionProcess(onFailure: () -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {
            userEmailAddress
                .prepareVerification(EmailAddress.PrepareVerificationParams.EmailCode())
                .onSuccess {
                    _uiState.value = AccountDeletionUiState.RequestConfirmed
                }
                .onFailure {
                    LogUtils.LogWarning(TAG, "Error preparing email verification")
                    onFailure()
                }
        }
    }

    fun verify(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            userEmailAddress
                .attemptVerification(code)
                .onSuccess {
                    _uiState.value = AccountDeletionUiState.Verified
                    _isLoading.value = false
                }
                .onFailure {
                    // FIXME: somehow, we get an "already verified" error
                    LogUtils.LogWarning(TAG, "Error verifying email: ${it.longErrorMessageOrNull}")
                    _isLoading.value = false
                    // TODO: should probably re-attempt this, somehow, and possibly do a shake event
                }

        }

    }

    fun deleteUser() {
        viewModelScope.launch(Dispatchers.IO) {
            Clerk.user!!
                .delete()
                .onSuccess {
                    _uiState.value = AccountDeletionUiState.Deleted
                }
                .onFailure {
                    LogUtils.LogWarning(TAG, "Error deleting user: ${it.longErrorMessageOrNull}")
                }

        }
    }

}