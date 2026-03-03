package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.uiStates.EmailVerificationUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailVerificationViewModel : ViewModel() {
    var code by mutableStateOf("")

    var secondsTilRequestCode by mutableLongStateOf(0L)
        private set

    var secondsTilExpiredCode by mutableLongStateOf(0L)

    var failedAttempts by mutableStateOf(0)
        private set

    private val _uiState = MutableStateFlow<EmailVerificationUiState>(EmailVerificationUiState.Initial)
    val uiState = _uiState.asStateFlow()

    init {
        startResendTimer()
        startExpirationTimer()
    }

    fun startResendTimer() {
        _uiState.value = EmailVerificationUiState.Initial
        startTimer(30L,
            {
                secondsTilRequestCode = it
            },
            EmailVerificationUiState.RequestCodeReady)
    }

    fun startExpirationTimer() {
        startTimer(60L,
            {
                secondsTilExpiredCode = it
            },
            EmailVerificationUiState.ExpiredCode)

    }

    fun startTimer(durationInSeconds: Long,
                   onUpdateTimerState: (timer: Long) -> Unit,
                   endState: EmailVerificationUiState) {
        var seconds = durationInSeconds
        onUpdateTimerState(durationInSeconds)
        viewModelScope.launch {
            while (seconds > 0) {
                delay(Constants.OneSecond)
                onUpdateTimerState(--seconds)
            }
            _uiState.value = endState
        }
    }

    fun onCodeSubmitted(onSubmit: (String) -> Unit) {
        if (code.isBlank()) return
        
        // Notify the parent to do the heavy lifting (API call)
        onSubmit(code)
    }

    fun onVerificationFailure() {
        failedAttempts++
        if (failedAttempts >= 3) {
            _uiState.value = EmailVerificationUiState.TooManyAttempts
        }
    }
}
