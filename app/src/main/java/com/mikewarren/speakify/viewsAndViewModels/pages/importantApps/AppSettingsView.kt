package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AppSettingsView(
    viewModel: AppSettingsViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    if (!viewModel.isOpen)
        return

    val appSettings by viewModel.settings.collectAsState()
//    val suggestionQuery by viewModel.suggestionQuery.collectAsState()
//    val suggestions = remember(suggestionQuery) { viewModel.getSuggestions() }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()) {
                // Title
                Text(text = "${viewModel.appModel.appName} Settings", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Announcer Voice Section (needs Autocomplete)
                AnnouncerVoiceSectionView(viewModel.childAnnouncerVoiceSectionViewModel)

                Spacer(modifier = Modifier.height(16.dp))

                // Important Notifications List
                Text(text = "Alert me to Notifications From", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) { // Limit height for scrollability
                    items(appSettings.notificationSources) { source ->
                        ImportantNotificationsListItemView(source) {
                            viewModel.removeNotificationSource(source)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End){

                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = onSave) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncerVoiceSection() {
    // Implement similar logic to your SettingsView announcer voice section here
    // You might reuse components or extract common composables.
    Text(text = "Announcer Voice (Placeholder)")
}

@Composable
fun ImportantNotificationsListItemView(source: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = source)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove")
        }
    }
}