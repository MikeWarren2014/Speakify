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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AppSettingsView

@Composable
fun AppListItemView(
    viewModel: ConfigurableAppListItemViewModel,
) {
    LaunchedEffect(viewModel.isSelected) {
        Log.d("AppListItemView", "App: ${viewModel.model.appName}, Package:${viewModel.model.packageName}, isSelected: ${viewModel.isSelected}")
    }

    val context = LocalContext.current

    val icon = remember(viewModel.model.packageName) {
        try {
            context.packageManager.getApplicationIcon(viewModel.model.packageName)
        } catch (e: Exception) {
            null // Handle cases where the app might be uninstalled
        }
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
        AsyncImage(
            model = icon,
            contentDescription = "${viewModel.model.appName} icon",
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(viewModel.model.appName)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            viewModel.childViewModel.open();
        }) {
            Icon(Icons.Filled.Settings, contentDescription = "Configure")
        }
    }

    AppSettingsView(
        viewModel = viewModel.childViewModel,
    )
}