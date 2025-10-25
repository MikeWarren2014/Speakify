package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.AppListItemViewModel

@Composable
fun AddAppMenuView(
    viewModel: AddAppMenuViewModel,
    onDismissRequest: () -> Unit,
    onAppSelected: (UserAppModel) -> Unit,
) {
    val searchText by viewModel.searchText.collectAsState()
    val appVMs by viewModel.filteredApps.collectAsState()

    var searchTextState by remember { mutableStateOf(TextFieldValue(searchText)) }

    val context = LocalContext.current

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

                        val icon = remember(model.packageName) {
                            try {
                                context.packageManager.getApplicationIcon(model.packageName)
                            } catch (e: Exception) {
                                null // Handle cases where the app might be uninstalled
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onAppSelected(model, onAppSelected)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Use Coil's AsyncImage to display the derived icon
                            AsyncImage(
                                model = icon,
                                contentDescription = "${model.appName} icon",
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(Modifier.width(16.dp))

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