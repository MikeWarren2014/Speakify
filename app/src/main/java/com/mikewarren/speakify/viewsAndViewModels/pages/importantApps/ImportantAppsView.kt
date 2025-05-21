package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AddAppMenuView
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.DeleteConfirmationDialog

import androidx.compose.runtime.LaunchedEffect

@Composable
fun ImportantAppsView() {
    val viewModel: ImportantAppsViewModel = hiltViewModel()
    val appVMs by viewModel.filteredApps.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedApps = remember { mutableStateListOf<UserAppModel>() }
    var isAddMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchApps()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Search Bar
        OutlinedTextField(
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Filter Apps")
            },
            value = searchText,
            onValueChange = { viewModel.onSearchTextChange(it) },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 24.dp),
            placeholder = { Text("Search Apps") }
        )
        Spacer(Modifier.height(16.dp))

        // App List
        if (appVMs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take available space
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Center vertically as well
            ) {
                Text(
                    text = "No apps added yet!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Click the Add App button below to add some apps to be alerted when they send you notifications.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(appVMs) { appVM ->
                    AppListItemView(
                        viewModel = appVM as ConfigurableAppListItemViewModel,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Add/Delete Button
        val selectedCount = viewModel.getSelectedApps().count()

        var buttonText: String = "Add App"
        var colors = ButtonDefaults.buttonColors()
        if (selectedCount > 0) {
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White,
            )
            buttonText = "Delete Apps ($selectedCount)"
        }

        Button(
            onClick = {
                if (selectedCount == 0) {
                    isAddMenuExpanded = true
                } else {
                    showDeleteDialog = true
                }
            },
            modifier = Modifier.align(Alignment.End),
            colors = colors,
        ) {
            Text(buttonText)
        }

        // Add App Menu
        if ((isAddMenuExpanded) && (viewModel.childAddAppMenuViewModel != null)) {
            AddAppMenuView(
                viewModel = viewModel.childAddAppMenuViewModel!!,
                onDismissRequest = { isAddMenuExpanded = false },
                onAppSelected = { model : UserAppModel ->
                    viewModel.addApp(model)
                    isAddMenuExpanded = false
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deleteSelectedApps()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}