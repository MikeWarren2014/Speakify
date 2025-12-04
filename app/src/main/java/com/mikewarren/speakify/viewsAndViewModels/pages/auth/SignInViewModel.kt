package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.mikewarren.speakify.data.uiStates.SignInUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState = _uiState.asStateFlow()


    private val _eventFlow = MutableSharedFlow<Event>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class Event {
        object Shake : Event()
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { SignInUiState.Loading }
            SignIn.create(SignIn.CreateParams.Strategy.Password(identifier = email, password = password))
                .onSuccess { _uiState.value = SignInUiState.Success }
                .onFailure {
                    _uiState.value = SignInUiState.Error("Sign-in not complete. Details: ${it.longErrorMessageOrNull}")
                    _eventFlow.emit(Event.Shake)
                }
        }
    }

    fun onClickForgotPassword() {
        viewModelScope.launch {
            _uiState.update { SignInUiState.ResetPassword(SignInUiState.ResetPassword.ForgotPassword) }
        }
    }
}