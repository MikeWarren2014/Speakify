package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
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