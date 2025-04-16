package com.mikewarren.speakify.viewsAndViewModels.pages

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@Composable
fun SettingsView() {
    val viewModel: SettingsViewModel = hiltViewModel() // Use hiltViewModel()
    val isDarkThemePreferred by viewModel.useDarkTheme.collectAsState(initial = null)
    val selectedVoice by viewModel.selectedTTSVoice.collectAsState(initial = null)
    var expanded by remember { mutableStateOf(false) }
    val voices = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewModel.tts?.voices?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
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
        // TTS Voice Selection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "TTS Voice")
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = selectedVoice ?: "Default", color = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice.name) },
                        onClick = {
                            viewModel.saveSelectedVoice(voice.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}