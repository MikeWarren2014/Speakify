package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.mikewarren.speakify.data.UserAppModel

@Composable
fun AddAppMenuView(
    onDismissRequest: () -> Unit,
    onAppSelected: (UserAppModel) -> Unit,
) {
    val viewModel: AddAppMenuViewModel = hiltViewModel()
    val searchText by viewModel.searchText.collectAsState()
    val appVMs by viewModel.filteredApps.collectAsState()

    var searchTextState by remember { mutableStateOf(TextFieldValue(searchText)) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add App", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Filter Apps")
                    },
                    value = searchTextState,
                    onValueChange = { textFieldValue: TextFieldValue ->
                        searchTextState = textFieldValue
                        viewModel.onSearchTextChange(textFieldValue.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search Apps") }
                )
                Spacer(Modifier.height(8.dp))


                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(appVMs) { appVM : AppListItemViewModel ->
                        val model = appVM.model
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onAppSelected(model, onAppSelected)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(model.appName)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismissRequest, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddAppMenuPreview() {
    AddAppMenuView(onDismissRequest = {}, onAppSelected = { _ -> })
}