package com.mikewarren.speakify.data

import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.FeedbackModel
import com.mikewarren.speakify.data.models.OnboardingCategorySelection
import com.mikewarren.speakify.data.models.OnboardingModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val userSettingsDataStore: DataStore<UserSettingsModel>,
) : OnboardingRepository {
    override val onboardingModel: Flow<OnboardingModel> = userSettingsDataStore.data
        .map { it.onboardingModel }

    override val appOpenCount: Flow<Int> = onboardingModel
        .map { model -> model.appOpenCount }

    override val speakificationCount: Flow<Int> = onboardingModel
        .map { model -> model.speakificationCount }

    override val onboardingStep: Flow<OnboardingUiState> = onboardingModel
        .map { model -> model.onboardingStep }

    override val feedback: Flow<FeedbackModel?> = onboardingModel
        .map { model -> model.feedback }

    override val primaryGoal: Flow<String?> = onboardingModel
        .map { model -> model.primaryGoal }

    override val importantAppCategories: Flow<List<OnboardingCategorySelection>> = onboardingModel
        .map { model -> model.importantAppCategories }

    override val hasShownRatingsPrompt: Flow<Boolean> = onboardingModel
        .map { model -> model.hasShownRatingsPrompt }

    override val hasShownTrialConversionPrompt: Flow<Boolean> = onboardingModel
        .map { model -> model.hasShownTrialConversionPrompt }


    override suspend fun incrementAppOpenCount() {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    appOpenCount = model.onboardingModel.appOpenCount + 1
                )
            )
        }
    }

    override suspend fun incrementSpeakificationCount() {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    speakificationCount = model.onboardingModel.speakificationCount + 1
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

    override suspend fun saveFeedback(feedback: FeedbackModel) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    feedback = feedback
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

    override suspend fun saveImportantAppCategories(categories: List<String>) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    importantAppCategories = categories.mapNotNull { categoryName ->
                        AppCategory.entries.find { it.categoryName == categoryName }?.let {
                            OnboardingCategorySelection(it)
                        }
                    }
                )
            )
        }
    }

    override suspend fun satisfyCategory(category: AppCategory) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    importantAppCategories = model.onboardingModel.importantAppCategories.map {
                        if (it.category == category) it.copy(isSatisfied = true) else it
                    }
                )
            )
        }
    }

    override suspend fun setHasShownRatingsPrompt(shown: Boolean) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    hasShownRatingsPrompt = shown
                )
            )
        }
    }

    override suspend fun setHasShownTrialConversionPrompt(shown: Boolean) {
        userSettingsDataStore.updateData { model ->
            model.copy(
                onboardingModel = model.onboardingModel.copy(
                    hasShownTrialConversionPrompt = shown
                )
            )
        }
    }

    override suspend fun restoreOnboardingModel(model: OnboardingModel) {
        userSettingsDataStore.updateData { it.copy(onboardingModel = model) }
    }
}
