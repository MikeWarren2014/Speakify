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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.viewsAndViewModels.widgets.AutoCompletableView

@Composable
fun <T : Any?> NotificationSourceListView(
    viewModel: BaseNotificationSourceListViewModel<T>,
) {
    val notificationSources by viewModel.notificationSources.collectAsState()
    val allData by viewModel.allData.collectAsState()

    Text(text = "Alert me to Notifications From", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier
        .heightIn(max = 200.dp)
        .border(3.dp, MaterialTheme.colorScheme.primaryContainer)
    ) { // Limit height for scrollability
        if (allData.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading....",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // A bit lighter
                    )
                }
            }

            return@LazyColumn
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
                        text = "No ${viewModel.getNotificationSourcesName()} yet.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // A bit lighter
                    )
                    Text(
                        text = "Add some ${viewModel.getNotificationSourcesName()} to be notified about them.",
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

    AutoCompletableView(
        viewModel,
        onGetDefaultValues = { viewModel ->
            val vm = viewModel as BaseNotificationSourceListViewModel<T>
            if (vm.isLoading) {
                return@AutoCompletableView emptyList()
            }
            vm.allAddableSourceModels
                .value
                .map { model -> viewModel.toViewString(model) }
        },
        onHandleSelection = { viewModel, selection -> (viewModel as BaseNotificationSourceListViewModel<T>).addNotificationSource(selection) }
    )
}

@Composable
fun NotificationSourceItemView(source: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.padding(horizontal = 16.dp),
            text = source)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove")
        }
    }
}