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
        .map { model -> model.onboardingModel.appOpenCount }

    override val onboardingStep: Flow<OnboardingUiState> = userSettingsDataStore.data
        .map { model -> model.onboardingModel.onboardingStep }

    override val surveyResult: Flow<String?> = userSettingsDataStore.data
        .map { model -> model.onboardingModel.surveyResult }

    override val primaryGoal: Flow<String?> = userSettingsDataStore.data
        .map { model -> model.onboardingModel.primaryGoal }

    override val veryImportantApps: Flow<List<String>> = userSettingsDataStore.data
        .map { model -> model.onboardingModel.veryImportantApps }

    override suspend fun incrementAppOpenCount() {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    appOpenCount = model.onboardingModel.appOpenCount + 1
                )
            )
        }
    }

    override suspend fun updateOnboardingStep(step: OnboardingUiState) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    onboardingStep = step
                )
            )
        }
    }

    override suspend fun saveSurveyResult(result: String) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    surveyResult = result
                )
            )
        }
    }

    override suspend fun savePrimaryGoal(goal: String) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    primaryGoal = goal
                )
            )
        }
    }

    override suspend fun saveVeryImportantApps(vias: List<String>) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    veryImportantApps = vias
                )
            )
        }
    }

}