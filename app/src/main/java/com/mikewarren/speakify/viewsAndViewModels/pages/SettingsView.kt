package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Theme Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Dark Theme")
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = isDarkThemePreferred ?: false,
                onCheckedChange = { viewModel.updateUseDarkTheme(it) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "TTS Voice")
            Spacer(modifier = Modifier.width(16.dp))
            TTSAutoCompletableView(
                viewModel,
                onHandleSelection = { viewModel, selectedVoice: String ->
                    viewModel.onSelectedVoice(selectedVoice)
                },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "Minimum Volume", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "The lowest volume level the app will use when speaking notifications.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = minVolume.toFloat(),
                    onValueChange = { viewModel.setMinVolume(it.roundToInt()) },
                    valueRange = 0f..15f, // Standard Android media volume range is 0-15
                    steps = 14, // 14 steps create 15 distinct values (0 to 15)
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = minVolume.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Maximize volume on screen off",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Boosts notification volume to maximum when the screen is locked to ensure you hear it.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = shouldMaximizeVolumeOnScreenOff,
                    onCheckedChange = { viewModel.setMaximizeVolumeOnScreenOff(it) },
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.childMainVM.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}