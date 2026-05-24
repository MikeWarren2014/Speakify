package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.SessionRepository
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import com.mikewarren.speakify.services.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val _importantApps = MutableStateFlow<List<UserAppModel>>(emptyList())
    val importantApps: StateFlow<List<UserAppModel>> = _importantApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val primaryGoal: StateFlow<String?> = sessionRepository.onboardingRepository
        .primaryGoal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    fun updateOnboardingStep(step: OnboardingUiState) {
        sessionRepository.updateOnboardingStep(step)
    }

    fun saveSurveyResult(result: String) {
        sessionRepository.saveSurveyResult(result)
    }

    fun savePrimaryGoal(goal: String) {
        sessionRepository.savePrimaryGoal(goal)
    }

    fun saveImportantAppCategories(categories: List<String>) {
        sessionRepository.saveImportantAppCategories(categories)
    }

    fun startTrialConversion() {
        sessionRepository.startTrialConversion()
    }

    fun proceedToTrialSession() {
        sessionRepository.proceedToTrialSession()
    }

    fun speakSample(text: String) {
        viewModelScope.launch {
            ttsManager.speak(text)
        }
    }
}