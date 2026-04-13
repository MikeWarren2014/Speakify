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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.models.scheduling.DayScheduleModel
import com.mikewarren.speakify.data.models.scheduling.DayScheduleType
import com.mikewarren.speakify.data.models.scheduling.StatusSectionViewModel
import com.mikewarren.speakify.data.models.scheduling.WeeklyScheduleViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingView(viewModel: SchedulingViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StatusSectionView(viewModel.childStatusSectionViewModel)

        WeeklyScheduleSection(viewModel.childWeeklyScheduleViewModel)
    }
}

@Composable
fun StatusSectionView(viewModel: StatusSectionViewModel?) {
    if (viewModel == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.scheduling_loading_status), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.scheduling_status_header),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusResource = if (viewModel.isAppOn) R.string.scheduling_status_on else R.string.scheduling_status_off
                val statusText = stringResource(statusResource)
                Text(
                    stringResource(R.string.scheduling_status_text,
                        statusText),
                    style = MaterialTheme.typography.bodyLarge
                )

                IconButton(onClick = {
                    viewModel.toggleAppStatus()
                }) {
                    Icon(
                        imageVector = if (viewModel.isAppOn) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Toggle Status",
                        modifier = Modifier.size(48.dp),
                        tint = if (viewModel.isAppOn) Color.Red else Color.Green
                    )
                }
            }

            if (!viewModel.isAppOn) {
                Text(
                    text = stringResource(R.string.scheduling_status_off_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }

    if (viewModel.isOpen) {
        PauseDurationDialog(
            viewModel,
            onConfirm = {
                viewModel.save()
            },
            onDismiss = {
                viewModel.cancel()
            }
        )
    }
}

@Composable
fun PauseDurationDialog(
    viewModel: StatusSectionViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.scheduling_pause_duration),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    stringResource(R.string.scheduling_pause_duration_text),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val context = LocalContext.current

                    val hoursError = viewModel.errorsDict[StatusSectionViewModel.HoursField]
                    OutlinedTextField(
                        value = viewModel.pauseHours,
                        onValueChange = { viewModel.updatePauseHours(it) },
                        label = { Text(stringResource(R.string.scheduling_pause_duration_hours)) },
                        isError = hoursError != null,
                        supportingText = hoursError?.let { listOfErrors ->
                            { Text(listOfErrors.joinToString(
                                transform = { errorUiText ->
                                    errorUiText.asString(context)
                                },
                                separator="\n"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    val minutesError = viewModel.errorsDict[StatusSectionViewModel.MinutesField]
                    OutlinedTextField(
                        value = viewModel.pauseMinutes,
                        onValueChange = { viewModel.updatePauseMinutes(it) },
                        label = { Text(stringResource(R.string.scheduling_pause_duration_minutes)) },
                        isError = minutesError != null,
                        supportingText = minutesError?.let { listOfErrors ->
                            { Text(listOfErrors.joinToString(
                                transform = { errorUiText ->
                                    errorUiText.asString(context)
                                },
                                separator="\n"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                val globalError = viewModel.errorsDict[StatusSectionViewModel.BothFields]
                if (globalError != null) {
                    Text(
                        text = globalError.joinToString("\n"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(onClick = onConfirm) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyScheduleSection(viewModel: WeeklyScheduleViewModel?) {
    if (viewModel == null) {
        Text(stringResource(R.string.scheduling_schedule_loading), style = MaterialTheme.typography.bodyMedium)
        return
    }

    Text(
        text = stringResource(R.string.scheduling_weekly_schedule_header),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )

    val weeklySchedule by viewModel.weeklyScheduleFlow
        .collectAsStateWithLifecycle()

    for ((dayOfWeek, dayScheduleModel) in weeklySchedule) {
        DayScheduleRow(
            state = dayScheduleModel,
            onTypeChange = { newType -> viewModel.updateDayType(dayOfWeek, newType) },
            onFromTimeChange = { newTime -> viewModel.updateDayFromTime(dayOfWeek, newTime) },
            onToTimeChange = { newTime -> viewModel.updateDayToTime(dayOfWeek, newTime) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScheduleRow(
    state: DayScheduleModel,
    onTypeChange: (DayScheduleType) -> Unit,
    onFromTimeChange: (String) -> Unit,
    onToTimeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showFromTimePicker by remember { mutableStateOf(false) }
    var showToTimePicker by remember { mutableStateOf(false) }

    val displayFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(state.type.labelUiText.asString())
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DayScheduleType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.labelUiText.asString()) },
                                onClick = {
                                    onTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

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
                
                OutlinedButton(
                    onClick = { showFromTimePicker = true },
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    val time = try { LocalTime.parse(state.fromTime) } catch (e: Exception) { LocalTime.MIDNIGHT }
                    Text(time.format(displayFormatter))
                }

                Text(
                    text = "to",
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                
                OutlinedButton(
                    onClick = { showToTimePicker = true },
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    val time = try { LocalTime.parse(state.toTime) } catch (e: Exception) { LocalTime.MIDNIGHT }
                    Text(time.format(displayFormatter))
                }
            }
        }
    }

    if (showFromTimePicker) {
        TimePickerDialog(
            initialTime = try { LocalTime.parse(state.fromTime) } catch (e: Exception) { LocalTime.MIDNIGHT },
            onConfirm = { selectedTime ->
                onFromTimeChange(selectedTime.toString())
                showFromTimePicker = false
            },
            onDismiss = { showFromTimePicker = false }
        )
    }

    if (showToTimePicker) {
        TimePickerDialog(
            initialTime = try { LocalTime.parse(state.toTime) } catch (e: Exception) { LocalTime.MIDNIGHT },
            onConfirm = { selectedTime ->
                onToTimeChange(selectedTime.toString())
                showToTimePicker = false
            },
            onDismiss = { showToTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = stringResource(R.string.timepicker_select_time),
                    style = MaterialTheme.typography.labelLarge
                )
                
                TimePicker(
                    state = timePickerState
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(onClick = {
                        onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
