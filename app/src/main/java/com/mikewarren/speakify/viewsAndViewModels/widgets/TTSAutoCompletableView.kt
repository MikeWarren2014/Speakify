package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun TTSAutoCompletableView(viewModel: BaseTTSAutoCompletableViewModel,
                           onHandleSelection: (BaseTTSAutoCompletableViewModel, String) -> Any) {

    ModelAutoCompletableView(
        viewModel,
        onGetDefaultValues = { viewModel ->
            (viewModel as BaseTTSAutoCompletableViewModel).ttsManager
                .getRecommendedDefaultVoiceModels()
        },
        onHandleSelection = { viewModel, selection: String ->
            onHandleSelection((viewModel as BaseTTSAutoCompletableViewModel), selection)
        },
        onGetAnnotatedString = { voiceInfoModel ->
            val displayText = viewModel.toViewString(voiceInfoModel)

            buildAnnotatedString {
                // Main display text
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(displayText)
                }
                // Secondary, smaller text for the raw name
                withStyle(style = SpanStyle(color = Color.Gray, fontSize = 12.sp)) {
                    append("\n${voiceInfoModel.name}")
                }
                // The annotation now stores the raw voice name for selection
                addStringAnnotation(tag = "Clickable", annotation = voiceInfoModel.name, start = 0, end = displayText.length)
            }
        },
        itemLineHeight = 18.sp,
    )

}