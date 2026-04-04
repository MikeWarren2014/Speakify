package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mikewarren.speakify.R
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
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val filteredChoices : List<T> = remember(viewModel.searchText) {
        (if (viewModel.searchText.isBlank()) {
            onGetDefaultValues(viewModel)
        } else {
            viewModel.filterChoices(viewModel.searchText)
        })!!
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
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
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState: FocusState ->
                    viewModel.setAutocompleteDropdownState(focusState.isFocused)
                },
            label = {
                var labelStringResource = R.string.search_for_entities
                if (viewModel.isDisabled)
                    labelStringResource = R.string.autocomplete_no_choices_available_yet

                Text(stringResource(labelStringResource,
                viewModel.getLabelText().asString()))
            },
            enabled = !viewModel.isDisabled,
            interactionSource = interactionSource,
            supportingText = supportingText,
            leadingIcon = leadingIcon,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (filteredChoices.size == 1) {
                        onHandleSelection(viewModel, viewModel.toViewString(filteredChoices[0]))
                    }
                    viewModel.setAutocompleteDropdownState(false)
                    focusManager.clearFocus()
                }
            )
        )

        if (filteredChoices.isNotEmpty() && viewModel.isAutocompleteDropdownOpen) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(filteredChoices.take(Constants.AutoCompleteListSizeLimit)) { choice ->
                        val annotatedString = onGetAnnotatedString(choice)

                        Text(
                            text = annotatedString,
                            modifier = Modifier
                                .fillMaxWidth()
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
}
