package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class InitialScreenViewModel @Inject constructor(
    private val trialRepository: TrialRepository
) : ViewModel() {

    val trialStatus: StateFlow<TrialStatus> = trialRepository.trialModelFlow
        .map { it.status }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrialStatus.NotStarted
        )


    init {
        viewModelScope.launch {
            trialRepository.refreshTrialStatus()
            Log.d("InitialScreenViewModel", "Trial status: ${trialStatus.value}")
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            trialRepository.startTrial()
        }
    }
}
