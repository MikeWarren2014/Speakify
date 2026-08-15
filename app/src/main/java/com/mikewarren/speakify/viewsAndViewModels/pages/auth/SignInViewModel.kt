package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptSecondFactor
import com.clerk.api.signin.prepareSecondFactor
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.uiStates.SignInUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val trialRepository: TrialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState = _uiState.asStateFlow()


    private val _eventFlow = MutableSharedFlow<Event>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class Event {
        object Shake : Event()
    }

    private var pendingSignIn: SignIn? = null

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { SignInUiState.Loading }
            SignIn.create(SignIn.CreateParams.Strategy.Password(identifier = email, password = password))
                .onSuccess { signIn ->
                    when (signIn.status) {
                        SignIn.Status.COMPLETE -> activate(signIn)

                        SignIn.Status.NEEDS_SECOND_FACTOR -> {
                            prepareSecondFactor(signIn)
                        }

                        else -> handleUnexpectedStatus(signIn.status)
                    }
                }
                .onFailure { failure: ClerkResult.Failure<ClerkErrorResponse> ->
                    if (failure.error?.errors?.firstOrNull()?.code == "form_password_pwned") {
                        _uiState.value = SignInUiState.ResetPassword(SignInUiState.ResetPassword.PwnedCredentials)
                        return@onFailure
                    }

                    handleFailure(failure)
                }
        }
    }

    private suspend fun activate(signIn: SignIn) {
        val sessionId = signIn.createdSessionId ?: return
        Clerk.setActive(sessionId)

        _uiState.value = SignInUiState.Success
        recordSignUp()
    }

    private fun recordSignUp() {
        viewModelScope.launch {
            trialRepository.recordSignUp()
        }
    }

    fun resendSecondFactorCode() {
        viewModelScope.launch {
            val signIn = pendingSignIn ?: return@launch
            prepareSecondFactor(signIn)
        }
    }

    private suspend fun prepareSecondFactor(signIn: SignIn) {
        signIn.prepareSecondFactor()
            .onSuccess { prepared ->
                pendingSignIn = prepared
                showEmailCodeEntry()
            }
            .onFailure { failure: ClerkResult.Failure<ClerkErrorResponse> -> handleFailure(failure)  }
    }

    private suspend fun handleFailure(failure: ClerkResult.Failure<ClerkErrorResponse>) {
        _uiState.value = SignInUiState.Error("Sign-in not complete. Details: ${failure.longErrorMessageOrNull}")
        _eventFlow.emit(Event.Shake)
    }

    private suspend fun handleUnexpectedStatus(status: SignIn.Status) {
        _uiState.value = SignInUiState.UnexpectedStatus(status.name)
    }

    fun onClickForgotPassword() {
        viewModelScope.launch {
            _uiState.update { SignInUiState.ResetPassword(SignInUiState.ResetPassword.ForgotPassword) }
        }
    }

    fun submitDeviceTrustCode(code: String, onFailure: (() -> Unit)? = null) {
        viewModelScope.launch {
            val signIn = pendingSignIn ?: return@launch
            _uiState.update { SignInUiState.EmailCodeEntry(isLoading = true) }

            signIn.attemptSecondFactor(
                SignIn.AttemptSecondFactorParams.EmailCode(code)
            )
            .onSuccess { completedSignIn ->
                if (completedSignIn.status != SignIn.Status.COMPLETE) {
                    handleUnexpectedStatus(completedSignIn.status)
                    return@onSuccess
                }
                activate(completedSignIn)
            }
            .onFailure { failure: ClerkResult.Failure<ClerkErrorResponse> ->
                _uiState.update { SignInUiState.EmailCodeEntry(isLoading = false) }
                handleFailure(failure)
                onFailure?.invoke()
            }
        }
    }

    private fun showEmailCodeEntry() {
        _uiState.update { SignInUiState.EmailCodeEntry() }
    }

    fun resetToIdle() {
        _uiState.update { SignInUiState.Idle }
    }
}
