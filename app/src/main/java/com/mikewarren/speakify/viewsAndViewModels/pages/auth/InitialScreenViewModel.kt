package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.TrialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitialScreenViewModel @Inject constructor(
    private val trialRepository: TrialRepository
) : ViewModel() {

    val trialStatus: StateFlow<TrialStatus> = trialRepository.trialStatus as StateFlow<TrialStatus>

    init {
        viewModelScope.launch {
            trialRepository.refreshTrialStatus()
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            trialRepository.startTrial()
        }
    }
}
