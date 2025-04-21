package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.runtime.Composable
import com.mikewarren.speakify.utils.TTSUtils
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel

@Composable
fun TTSAutoCompletableView(viewModel: BaseTTSAutoCompletableViewModel,
                           onHandleSelection: (BaseTTSAutoCompletableViewModel, String) -> Any) {
    AutoCompletableView(
        viewModel,
        onGetDefaultValues = { viewModel ->
            (viewModel as BaseTTSAutoCompletableViewModel).tts?.let {
                TTSUtils.GetRecommendedDefaultVoiceNames(it)
            }!!
        },
        onHandleSelection = { viewModel, selection: String ->
            onHandleSelection((viewModel as BaseTTSAutoCompletableViewModel), selection)
        },
    )
    
}