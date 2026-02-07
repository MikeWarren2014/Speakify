package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString

@Composable
fun SimpleAutoCompletableView(
    viewModel: BaseSimpleAutoCompletableViewModel,
    onGetDefaultValues: (BaseAutoCompletableViewModel<String>) -> List<String>,
    onHandleSelection: (BaseAutoCompletableViewModel<String>, String) -> Any,
    onGetAnnotatedString: @Composable (String) -> AnnotatedString,
) { 
    AutoCompletableView(
        viewModel,
        onGetDefaultValues,
        onHandleSelection,
        onGetAnnotatedString,
        { newValue: String, filteredChoices: List<String> ->
            newValue in filteredChoices
        }
    )
}