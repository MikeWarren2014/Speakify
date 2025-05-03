package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mikewarren.speakify.data.Constants

@Composable
fun AppSettingsView(
    viewModel: AppSettingsViewModel,
) {
    if (!viewModel.isOpen)
        return

    val onClose = { viewModel.isOpen = false }

    Dialog(onDismissRequest = onClose,
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
                if (viewModel.childAnnouncerVoiceSectionViewModel != null)
                    AnnouncerVoiceSectionView(viewModel.childAnnouncerVoiceSectionViewModel!!)

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.childNotificationListViewModel != null)
                    GetChildListView(viewModel)

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End){
                    OutlinedButton(onClick = {
                        viewModel.cancel()
                        onClose()
                    }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        viewModel.save()
                        onClose()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun GetChildListView(viewModel: AppSettingsViewModel) {
    if ((viewModel.getPackageName() in Constants.PhoneAppPackageNames) ||
        (viewModel.getPackageName() in Constants.MessagingAppPackageNames)) {
        return ImportantContactsListView(viewModel.childNotificationListViewModel as BaseImportantContactsListViewModel)
    }

    if (viewModel.childNotificationListViewModel == null)
        return NotSupportedView(viewModel.appModel.appName)

    return NotificationSourceListView(viewModel.childNotificationListViewModel!!)
}

@Composable
fun NotSupportedView(appName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Notifications not yet supported",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = "Currently, we do not support important notification sources for $appName.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}