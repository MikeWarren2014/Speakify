package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.viewsAndViewModels.widgets.TTSAutoCompletableView
import kotlin.math.roundToInt

@Composable
fun SettingsView() {
    val viewModel: SettingsViewModel = hiltViewModel() // Use hiltViewModel()
    val isDarkThemePreferred by viewModel.useDarkTheme.collectAsState(initial = isSystemInDarkTheme())
    val shouldMaximizeVolumeOnScreenOff by viewModel.maximizeVolumeOnScreenOff.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    val minVolume by viewModel.minVolume.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 18.dp),
        )

        // Refactored Theme Toggle Card
        SettingsToggleCard(
            title = "Dark Theme",
            description = "Enable or disable dark mode for the app.",
            isChecked = isDarkThemePreferred ?: false,
            onCheckedChange = { viewModel.updateUseDarkTheme(it) },
        )

        // Refactored TTS Voice Selection Card
        SettingsItemCard(
            title = "TTS Voice",
            description = "Select the voice for spoken notifications.",
        ) {
            TTSAutoCompletableView(
                viewModel,
                onHandleSelection = { vm, selectedVoice ->
                    vm.onSelectedVoice(selectedVoice)
                },
            )
        }

        MinVolumeSettingCard(
            title = "Minimum Volume",
            description = "The lowest volume level the app will use when speaking notifications.",
            currentValue = minVolume,
            onValueChange = { viewModel.setMinVolume(it) }
        )

        SettingsToggleCard(
            title = "Maximize volume on screen off",
            description = "Boosts notification volume to maximum when the screen is locked to ensure you hear it",
            isChecked = shouldMaximizeVolumeOnScreenOff,
            onCheckedChange = { viewModel.setMaximizeVolumeOnScreenOff(it) },
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.childMainVM.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
fun MinVolumeSettingCard(
    title: String,
    description: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Title and Description
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
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
        }
    }
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