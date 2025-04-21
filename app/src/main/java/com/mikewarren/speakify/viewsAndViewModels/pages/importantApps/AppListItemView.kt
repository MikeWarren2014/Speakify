package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.LaunchedEffect
import android.util.Log

@Composable
fun AppListItemView(
    viewModel: ConfigurableAppListItemViewModel,
) {
    LaunchedEffect(viewModel.isSelected) {
        Log.d("AppListItemView", "App: ${viewModel.model.appName}, Package:${viewModel.model.packageName}, isSelected: ${viewModel.isSelected}")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.toggleSelected() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = viewModel.isSelected, onCheckedChange = { _ ->
            viewModel.toggleSelected()
        })
        Spacer(Modifier.width(8.dp))
        Text(viewModel.model.appName)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            viewModel.childViewModel.isOpen = true;
        }) {
            Icon(Icons.Filled.Settings, contentDescription = "Configure")
        }
    }

    AppSettingsView(
        viewModel = viewModel.childViewModel,
        onDismiss = {
            viewModel.childViewModel.isOpen = false;
        },
        onSave = {
            viewModel.childViewModel.isOpen = false;
        },
    )
}