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
import com.mikewarren.speakify.activities.AccountDeletedActivity
import com.mikewarren.speakify.activities.ActivityProvider
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.uiStates.AccountDeletionUiState
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountDeletionViewModel : ViewModel(), ITaggable {

    val childMainVM = MainViewModel()

    private val _uiState = MutableStateFlow<AccountDeletionUiState>(AccountDeletionUiState.RequestMade)
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val userEmailAddress = Clerk.user!!
        .emailAddresses
        .first()


    fun shouldReverify() : Boolean {
        return Clerk.session!!.createdAt - System.currentTimeMillis() >= 10 * 60 * Constants.OneSecond
    }

    fun startDeletionProcess(onFailure: () -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {
            userEmailAddress
                .prepareVerification(EmailAddress.PrepareVerificationParams.EmailCode())
                .onSuccess {
                    if (shouldReverify()) {
                        ActivityProvider.GetInstance().setActivityClass(AccountDeletedActivity::class.java,
                            {
                                _uiState.value = AccountDeletionUiState.Verified
                                ActivityProvider.GetInstance().resetActivityClass()
                            })

                        childMainVM.signOut()
                        return@onSuccess
                    }
                    _uiState.value = AccountDeletionUiState.Verified
                }
                .onFailure {
                    LogUtils.LogWarning(TAG, "Error preparing email verification")
                    onFailure()
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