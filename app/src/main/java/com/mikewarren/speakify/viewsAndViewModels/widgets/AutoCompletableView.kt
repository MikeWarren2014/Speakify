package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikewarren.speakify.data.Constants

@Composable
fun <T>AutoCompletableView(
    viewModel: BaseAutoCompletableViewModel<T>,
    onGetDefaultValues: (BaseAutoCompletableViewModel<T>) -> List<T>,
    onHandleSelection: (BaseAutoCompletableViewModel<T>, String) -> Any,
    onGetAnnotatedString: @Composable (T) -> AnnotatedString,
    onCheckSearchValue: (String, List<T>) -> Boolean,
    itemLineHeight: TextUnit = TextUnit.Unspecified,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val filteredChoices : List<T> = remember(viewModel.searchText) {
        (if (viewModel.searchText.isBlank()) {
            onGetDefaultValues(viewModel)
        } else {
            viewModel.filterChoices(viewModel.searchText)
        })!!
    }

    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isTextFieldFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isTextFieldFocused) { // React to focus changes
        viewModel.setAutocompleteDropdownState(isTextFieldFocused)
    }

    Column {
        TextField(
            value = viewModel.searchText,
            onValueChange = { text: String ->
                viewModel.setAutocompleteDropdownState(true)
                viewModel.onSearchTextChanged(text, { newValue:String ->
                    if ((filteredChoices.size == 1) && (onCheckSearchValue(newValue, filteredChoices))) {
                        onHandleSelection(viewModel, newValue)
                    }
                })
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState: FocusState ->
                    viewModel.setAutocompleteDropdownState(focusState.isFocused)
                },
            label = { Text("Search for ${viewModel.getLabel()}") },
            supportingText = supportingText
        )

        if (filteredChoices.isNotEmpty() && viewModel.isAutocompleteDropdownOpen) {
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(filteredChoices.take(Constants.AutoCompleteListSizeLimit)) { choice ->
                    val annotatedString = onGetAnnotatedString(choice)

                    Text(
                        text = annotatedString,
                        modifier = Modifier
                            .clickable {
                                annotatedString
                                    .getStringAnnotations("Clickable", 0, 0)
                                    .firstOrNull()?.let { annotation ->
                                        onHandleSelection(viewModel, annotation.item)
                                        viewModel.setAutocompleteDropdownState(false)
                                    }
                            }
                            .padding(16.dp),
                        style = LocalTextStyle.current,
                        lineHeight = itemLineHeight,
                    )
                }
            }
        }
    }
}
