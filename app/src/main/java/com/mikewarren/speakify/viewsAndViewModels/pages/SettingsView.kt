package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.TTSAutoCompletableView
import kotlin.math.roundToInt

@Composable
fun SettingsView(onNavigateToDeleteAccount: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val isDarkThemePreferred by viewModel.useDarkTheme.collectAsState(initial = isSystemInDarkTheme())
    val shouldMaximizeVolumeOnScreenOff by viewModel.maximizeVolumeOnScreenOff.collectAsState()
    val isCrashlyticsEnabled by viewModel.isCrashlyticsEnabled.collectAsStateWithLifecycle()

    val minVolume by viewModel.minVolume.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // --- Backup Launchers ---

    // 1. Launcher for EXPORT (Creating a file)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    // 2. Launcher for IMPORT (Opening a file)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection(stringResource(R.string.settings_section_general)) {
                SettingsToggleCard(
                    title = stringResource(R.string.settings_dark_theme_title),
                    description = stringResource(R.string.settings_dark_theme_description),
                    isChecked = isDarkThemePreferred ?: false,
                    onCheckedChange = { viewModel.updateUseDarkTheme(it) },
                )
                SingleColumnSettingsItemCard(
                    title = stringResource(R.string.settings_tts_voice_title),
                    description = stringResource(R.string.settings_tts_voice_description),
                ) {
                    TTSAutoCompletableView(
                        viewModel,
                        onHandleSelection = { vm, selectedVoice ->
                            vm.onSelectedVoice(selectedVoice)
                        },
                    )
                }
                MinVolumeSettingCard(
                    title = stringResource(R.string.settings_min_volume_title),
                    description = stringResource(R.string.settings_min_volume_description),
                    currentValue = minVolume,
                    onValueChange = { viewModel.setMinVolume(it) }
                )
                SettingsToggleCard(
                    title = stringResource(R.string.settings_maximize_volume_title),
                    description = stringResource(R.string.settings_maximize_volume_description),
                    isChecked = shouldMaximizeVolumeOnScreenOff,
                    onCheckedChange = { viewModel.setMaximizeVolumeOnScreenOff(it) },
                )
            }

            SettingsSection(stringResource(R.string.settings_section_privacy)) {
                SettingsToggleCard(
                    title = stringResource(R.string.settings_crashlytics_title),
                    description = stringResource(R.string.settings_crashlytics_description),
                    isChecked = isCrashlyticsEnabled,
                    onCheckedChange = { viewModel.setCrashlyticsEnabled(it) },
                )
            }

            SettingsSection(stringResource(R.string.settings_section_data)) {
                SettingsItemCard(
                    title = stringResource(R.string.settings_backup_title),
                    description = stringResource(R.string.settings_backup_description)
                ) {
                    Button(onClick = {
                        exportLauncher.launch("speakify_backup_${System.currentTimeMillis()}.json")
                    }) {
                        Text(stringResource(R.string.settings_export))
                    }
                }
                SettingsItemCard(
                    title = stringResource(R.string.settings_restore_title),
                    description = stringResource(R.string.settings_restore_description)
                ) {
                    Button(onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    }) {
                        Text(stringResource(R.string.settings_import))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SettingsSection(stringResource(R.string.settings_section_danger)) {

                Button(
                    onClick = { viewModel.childMainVM.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_sign_out))
                }

                Button(
                    onClick = {
                        viewModel.childMainVM.markAccountForDeletion()
                        onNavigateToDeleteAccount()
                  },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.warning),
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.settings_delete_account))
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun MinVolumeSettingCard(
    title: String,
    description: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {

    SingleColumnSettingsItemCard(title,
        description,
        {
            Spacer(modifier = Modifier.height(8.dp))
            // Slider and Value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Slider(
                    value = currentValue.toFloat(),
                    onValueChange = { onValueChange(it.roundToInt()) },
                    valueRange = 0f..15f, // Standard Android media volume range is 0-15
                    steps = 14, // 14 steps create 15 distinct values (0 to 15)
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = currentValue.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        })
}

@Composable
fun SettingsToggleCard(title: String,
                       description: String,
                       isChecked: Boolean,
                       onCheckedChange: (Boolean) -> Unit) {
   SettingsItemCard(title,
       description,
       {
           Switch(
               checked = isChecked,
               onCheckedChange = onCheckedChange,
               modifier = Modifier.padding(start = 16.dp)
           )
       })
}

@Composable
fun SettingsItemCard(title: String,
                     description: String,
                     content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
fun SingleColumnSettingsItemCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title and Description
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}
