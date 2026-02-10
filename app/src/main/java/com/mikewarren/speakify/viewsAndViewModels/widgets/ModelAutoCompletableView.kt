package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit

@Composable
fun <T> ModelAutoCompletableView(
    viewModel: BaseModelAutoCompletableViewModel<T>,
    onGetDefaultValues: (BaseAutoCompletableViewModel<T>) -> List<T>,
    onHandleSelection: (BaseAutoCompletableViewModel<T>, String) -> Any,
    onGetAnnotatedString: @Composable (T) -> AnnotatedString,
    itemLineHeight: TextUnit = TextUnit.Unspecified,
    supportingText: @Composable (() -> Unit)? = null,
) {
    AutoCompletableView(
        viewModel = viewModel,
        onGetDefaultValues = onGetDefaultValues,
        onHandleSelection = onHandleSelection,
        onGetAnnotatedString = onGetAnnotatedString,
        onCheckSearchValue = { newValue, filteredChoices ->
            filteredChoices.any { viewModel.toSourceString(it) == newValue }
        },
        itemLineHeight = itemLineHeight,
        supportingText = supportingText,
    )
}
