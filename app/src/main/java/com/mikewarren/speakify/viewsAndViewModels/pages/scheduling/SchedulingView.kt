package com.mikewarren.speakify.viewsAndViewModels.pages.scheduling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.data.models.DayScheduleState
import com.mikewarren.speakify.data.models.DayScheduleType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingView(viewModel: SchedulingViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- SECTION: STATUS ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusText = if (viewModel.isAppOn) "ON" else "OFF"
                    Text(
                        "App is currently $statusText!",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    IconButton(onClick = { viewModel.toggleAppStatus() }) {
                        Icon(
                            imageVector = if (viewModel.isAppOn) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Toggle Status",
                            modifier = Modifier.size(48.dp),
                            tint = if (viewModel.isAppOn) Color.Red else Color.Green
                        )
                    }
                }

                if (!viewModel.isAppOn) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Turn on after:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.pauseHours,
                            onValueChange = { viewModel.pauseHours = it },
                            label = { Text("Hrs") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.pauseMinutes,
                            onValueChange = { viewModel.pauseMinutes = it },
                            label = { Text("Mins") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }

        // --- SECTION: SCHEDULE ---
        Text(
            text = "Weekly Schedule",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        viewModel.weeklySchedule.forEach { dayState ->
            DayScheduleRow(dayState) { newType ->
                viewModel.updateDayType(dayState.dayName, newType)
            }
        }
    }
}

@Composable
fun DayScheduleRow(
    state: DayScheduleState,
    onTypeChange: (DayScheduleType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.dayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Dropdown Selector
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(state.type.label)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DayScheduleType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    onTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Time Pickers (Enabled only if "Select Times" is chosen)
            val isEnabled = state.type == DayScheduleType.SELECT_TIMES
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "from",
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                // In a real app, you'd use showTimePicker() on click here
                OutlinedButton(
                    onClick = { /* Open Time Picker */ },
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(state.fromTime)
                }
                Text(
                    text = "to",
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                OutlinedButton(
                    onClick = { /* Open Time Picker */ },
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(state.toTime)
                }
            }
        }
    }
}