package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.uiStates.OnboardingUiState

@Composable
fun OnboardingView(
    step: OnboardingUiState,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (step) {
            OnboardingUiState.NotStarted,
            OnboardingUiState.SatisfactionSurvey -> SatisfactionSurvey(
                onResult = { result ->
                    viewModel.saveSurveyResult(result)
                    viewModel.updateOnboardingStep(OnboardingUiState.PreferenceGathering)
                }
            )
            OnboardingUiState.PreferenceGathering -> PreferenceGathering(
                onComplete = { goal ->
                    viewModel.savePrimaryGoal(goal)
                    viewModel.updateOnboardingStep(OnboardingUiState.AppUsageInsight)
                }
            )
            OnboardingUiState.AppUsageInsight -> AppUsageInsightView(
                viewModel = viewModel,
                onComplete = { vias ->
                    viewModel.saveVeryImportantApps(vias)
                    viewModel.updateOnboardingStep(OnboardingUiState.ConversionReady)
                }
            )
            OnboardingUiState.ConversionReady -> ConversionReady(
                onSignUp = { viewModel.startTrialConversion() },
                onLater = { viewModel.proceedToTrialSession() }
            )
            OnboardingUiState.Completed -> { /* Should not be reached */ }
        }
    }
}

@Composable
fun SatisfactionSurvey(onResult: (String) -> Unit) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    val options = listOf(
        Triple("Very Dissatisfied",
            stringResource(R.string.satisfaction_survey_very_dissatisfied_header),
            Icons.Default.SentimentVeryDissatisfied),
        Triple("Dissatisfied",
            stringResource(R.string.satisfaction_survey_dissatisfied_header),
            Icons.Default.SentimentDissatisfied),
        Triple("Neutral",
            stringResource(R.string.satisfaction_survey_neutral_header),
            Icons.Default.SentimentNeutral),
        Triple("Satisfied",
            stringResource(R.string.satisfaction_survey_satisfied_header),
            Icons.Default.SentimentSatisfied),
        Triple("Very Satisfied",
            stringResource(R.string.satisfaction_survey_very_satisfied_header),
            Icons.Default.SentimentVerySatisfied),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.satisfaction_survey_header),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEachIndexed { index, (_, label, icon) ->
                SurveyIcon(
                    icon = icon,
                    label = label,
                    isSelected = selectedOption == index,
                    onClick = { selectedOption = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { selectedOption?.let { onResult(options[it].first) } },
            enabled = selectedOption != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next))
        }
    }
}

@Composable
fun SurveyIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.selectable(
            selected = isSelected,
            onClick = onClick,
            role = Role.RadioButton
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(48.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PreferenceGathering(onComplete: (String) -> Unit) {
    // Placeholder for Preference Gathering
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.what_is_your_primary_goal),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        val goals = listOf(
            "Accessibility" to stringResource(R.string.preference_gathering_option_accessibility),
            "Productivity" to stringResource(R.string.preference_gathering_option_productivity),
            "Hands-free Use" to stringResource(R.string.preference_gathering_option_hands_free_use),
            "Other" to stringResource(R.string.preference_gathering_option_other),
        )
        var selectedValue by remember { mutableStateOf(goals[0].first) }

        Column(Modifier.selectableGroup()) {
            goals.forEach { (value, text) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = value == selectedValue,
                            onClick = { selectedValue = value },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (value == selectedValue),
                        onClick = null // null recommended for accessibility with screen readers
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onComplete(selectedValue) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next))
        }
    }
}

@Composable
fun AppUsageInsightView(
    viewModel: OnboardingViewModel,
    onComplete: (List<String>) -> Unit
) {
    val importantApps by viewModel.importantApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedAppsInsight by viewModel.selectedAppsInsight.collectAsState()

    val selectedApps = remember { mutableStateMapOf<String, Boolean>() }
    var searchText by remember { mutableStateOf("") }

    // Fetch apps when this view is shown
    LaunchedEffect(Unit) {
        viewModel.fetchApps()
    }

    // Initialize selectedApps from viewModel's state
    LaunchedEffect(selectedAppsInsight) {
        selectedAppsInsight.forEach { appName ->
            selectedApps[appName] = true
        }
    }

    val filteredApps = if (searchText.isBlank()) {
        importantApps
    } else {
        importantApps.filter { it.appName.contains(searchText, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.via_header),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val description = stringResource(R.string.via_description)
        val annotatedDescription = buildAnnotatedString {
            val parts = description.split("**")
            if (parts.size == 3) {
                append(parts[0])
                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                    append(parts[1])
                }
                append(parts[2])
            } else {
                append(description)
            }
        }

        Text(
            text = annotatedDescription,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredApps) { app ->
                    AppUsageListItem(
                        app = app,
                        isSelected = selectedApps[app.appName] == true,
                        onSelectedChange = { isSelected ->
                            selectedApps[app.appName] = isSelected
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val selectedList = selectedApps.filter { it.value }
                    .keys
                    .toList()
                onComplete(selectedList)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next))
        }
    }
}

@Composable
fun AppUsageListItem(
    app: UserAppModel,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = isSelected,
                onClick = { onSelectedChange(!isSelected) },
                role = Role.Checkbox
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        AsyncImage(
            model = icon,
            contentDescription = "${app.appName} icon",
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ConversionReady(onSignUp: () -> Unit, onLater: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.conversion_ready_header),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.conversion_ready_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.conversion_ready_button_sign_up))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLater,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
        ) {
            Text(stringResource(R.string.conversion_ready_button_continue))
        }
    }
}
