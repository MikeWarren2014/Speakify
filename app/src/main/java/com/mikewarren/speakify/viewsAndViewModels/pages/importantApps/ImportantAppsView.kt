package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikewarren.speakify.data.UserAppModel

@Composable
fun ImportantAppsView() {
    val viewModel: ImportantAppsViewModel = hiltViewModel()
    val appVMs by viewModel.filteredApps.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedApps = remember { mutableStateListOf<UserAppModel>() }
    var isAddMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(appVMs) { appVM ->
                AppListItemView(
                    viewModel = appVM as ConfigurableAppListItemViewModel,
                    onConfigClick = { /* TODO: Navigate to config screen for this app */ },
                )
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
        if (isAddMenuExpanded) {
            AddAppMenuView(
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