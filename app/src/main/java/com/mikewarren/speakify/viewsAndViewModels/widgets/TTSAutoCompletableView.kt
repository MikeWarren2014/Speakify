package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.runtime.Composable
import com.mikewarren.speakify.utils.TTSUtils

@Composable
fun TTSAutoCompletableView(viewModel: BaseTTSAutoCompletableViewModel,
                           onHandleSelection: (BaseTTSAutoCompletableViewModel, String) -> Any) {
    AutoCompletableView(
        viewModel,
        onGetDefaultValues = { viewModel ->
            (viewModel as BaseTTSAutoCompletableViewModel).ttsManager
                .getRecommendedDefaultVoiceNames()
        },
        onHandleSelection = { viewModel, selection: String ->
            onHandleSelection((viewModel as BaseTTSAutoCompletableViewModel), selection)
        },
    )
    
}