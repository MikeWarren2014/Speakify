package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R

@Composable
fun <T : Any?> NotificationSourceListView(
    viewModel: INotificationSourceListViewModel<T>,
    creationWidget: @Composable (viewModel: INotificationSourceListViewModel<T>) -> Unit
) {
    val notificationSources by viewModel.notificationSourcesFlow.collectAsState()
    val notificationSourcesName = viewModel.getNotificationSourcesNameText()
        .asString()

    val dataStream = remember(viewModel) { viewModel.getMainDataStream() }
    val allData by (dataStream?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    Text(text = stringResource(R.string.alert_me_to_notifications_from),
        style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier
        .heightIn(max = 200.dp)
        .border(3.dp, MaterialTheme.colorScheme.primaryContainer)
    ) { // Limit height for scrollability
        if (dataStream != null) {
            if (allData.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var messageText = stringResource(R.string.loading)
                        if (!viewModel.isLoading())
                            messageText = stringResource(R.string.autocomplete_no_choices_available_yet,
                                notificationSourcesName)

                        Text(
                            text = messageText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // A bit lighter
                        )
                    }
                }

                return@LazyColumn
            }
        }


        if (notificationSources.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = stringResource(R.string.no_notification_sources_yet,
                            notificationSourcesName),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // A bit lighter
                    )
                    Text(
                        text = stringResource(R.string.no_notification_sources_instruction,
                            notificationSourcesName),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            return@LazyColumn
        }

        items(viewModel.getAddedSourceModels()) { sourceModel ->
            NotificationSourceItemView(viewModel.toViewString(sourceModel)) {
                viewModel.removeNotificationSource(sourceModel)
            }
        }
    }

    creationWidget(viewModel)
}

@Composable
fun NotificationSourceItemView(source: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f), // Ensures text takes only available space and wraps if needed
            text = source,
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
