package com.mikewarren.speakify.data

import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) : OnboardingRepository {
    override val appOpenCount: Flow<Int> = userSettingsDataStore.data
        .map { model -> model.appOpenCount }

    override val onboardingStep: Flow<OnboardingUiState> = userSettingsDataStore.data
        .map { model -> model.onboardingStep }

    override val surveyResult: Flow<String?> = userSettingsDataStore.data
        .map { model -> model.surveyResult }

    override suspend fun incrementAppOpenCount() {
        userSettingsDataStore.updateData { model ->
            model.copy(appOpenCount = model.appOpenCount + 1)
        }
    }

    override suspend fun updateOnboardingStep(step: OnboardingUiState) {
        userSettingsDataStore.updateData { model ->
            model.copy(onboardingStep = step)
        }
    }

    override suspend fun saveSurveyResult(result: String) {
        userSettingsDataStore.updateData { model ->
            model.copy(surveyResult = result)
        }
    }

}