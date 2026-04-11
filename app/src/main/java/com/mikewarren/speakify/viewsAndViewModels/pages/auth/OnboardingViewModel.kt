package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.SessionRepository
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
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
    private val appsRepository: AppsRepository
) : ViewModel() {

    private val _importantApps = MutableStateFlow<List<UserAppModel>>(emptyList())
    val importantApps: StateFlow<List<UserAppModel>> = _importantApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val selectedAppsInsight: StateFlow<List<String>> = sessionRepository.onboardingRepository
        .veryImportantApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    fun saveVeryImportantApps(vias: List<String>) {
        sessionRepository.saveVeryImportantApps(vias)
    }

    fun startTrialConversion() {
        sessionRepository.startTrialConversion()
    }

    fun proceedToTrialSession() {
        sessionRepository.proceedToTrialSession()
    }

    fun fetchApps() {
        viewModelScope.launch {
            _isLoading.value = true
            appsRepository.importantApps.collect { apps ->
                _importantApps.value = apps
                _isLoading.value = false
            }
        }
    }
}