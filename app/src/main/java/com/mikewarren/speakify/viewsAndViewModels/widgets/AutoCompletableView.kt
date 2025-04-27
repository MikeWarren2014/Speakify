package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikewarren.speakify.data.Constants

@Composable
fun AutoCompletableView(
    viewModel: BaseAutoCompletableViewModel,
    onGetDefaultValues: (BaseAutoCompletableViewModel) -> List<String>,
    onHandleSelection: (BaseAutoCompletableViewModel, String) -> Any,
) {
    val filteredChoices : List<String> = remember(viewModel.searchText) {
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
                    if ((filteredChoices.size == 1) && (newValue in filteredChoices)) {
                        onHandleSelection(viewModel, newValue)
                    }
                })
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState: FocusState ->
                    viewModel.setAutocompleteDropdownState(focusState.isFocused)
                },
            label = { Text("Search for ${viewModel.getLabel()}") }
        )

        if (filteredChoices.isNotEmpty() && viewModel.isAutocompleteDropdownOpen) {
            LazyColumn {
                items(filteredChoices.take(Constants.AutoCompleteListSizeLimit)) { choice ->
                    val annotatedString = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                // Add more style attributes as needed
                            )
                        ) {
                            append(choice)
                        }
                        addStringAnnotation(
                            tag = "Clickable",
                            annotation = choice, // Store choice as annotation
                            start = 0,
                            end = choice.length
                        )
                    }

                    Text(
                        // Use Text Composable
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
                    )
                }
            }
        }
    }
}