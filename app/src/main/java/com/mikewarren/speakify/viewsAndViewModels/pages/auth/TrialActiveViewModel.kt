package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SessionRepository
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.TrialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrialActiveViewModel @Inject constructor(
    private val trialRepository: TrialRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val trialStatus: StateFlow<TrialStatus> = trialRepository.trialModelFlow
        .map { it.status }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrialStatus.NotStarted
        )

    fun proceedToTrialSession() {
        viewModelScope.launch {
            sessionRepository.proceedToTrialSession()
        }
    }

    fun goToSignUp() {
        sessionRepository.startTrialConversion()
    }

    fun endTrial() {
        viewModelScope.launch {
            trialRepository.endTrial()
            sessionRepository.endTrial()
        }
    }
}
