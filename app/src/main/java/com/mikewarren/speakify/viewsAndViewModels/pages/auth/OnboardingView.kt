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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            OnboardingUiState.PreferenceGathering -> PreferenceGathering(
                onComplete = { goal ->
                    viewModel.savePrimaryGoal(goal)
                    viewModel.updateOnboardingStep(OnboardingUiState.AppUsageInsight)
                }
            )
            OnboardingUiState.AppUsageInsight -> AppUsageInsightView(
                onComplete = { categories ->
                    viewModel.saveImportantAppCategories(categories)
                    viewModel.updateOnboardingStep(OnboardingUiState.ValueDiscovery)
                }
            )
            OnboardingUiState.ValueDiscovery -> ValueDiscoveryView(
                viewModel = viewModel,
                onComplete = {
                    viewModel.updateOnboardingStep(OnboardingUiState.Completed)
                }
            )
            OnboardingUiState.Completed -> { /* Should not be reached */ }
        }
    }
}

@Composable
fun PreferenceGathering(onComplete: (String) -> Unit) {
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
            Triple("Accessibility", stringResource(R.string.preference_gathering_option_accessibility), stringResource(R.string.preference_gathering_option_accessibility_desc)),
            Triple("Productivity", stringResource(R.string.preference_gathering_option_productivity), stringResource(R.string.preference_gathering_option_productivity_desc)),
            Triple("Hands-free Use", stringResource(R.string.preference_gathering_option_hands_free_use), stringResource(R.string.preference_gathering_option_hands_free_use_desc)),
            Triple("Other", stringResource(R.string.preference_gathering_option_other), ""),
        )
        var selectedValue by remember { mutableStateOf(goals[0].first) }

        Column(Modifier.selectableGroup()) {
            goals.forEach { (value, title, description) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = value == selectedValue,
                            onClick = { selectedValue = value },
                            role = Role.RadioButton
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (value == selectedValue),
                        onClick = null
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (description.isNotEmpty()) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
    onComplete: (List<String>) -> Unit
) {
    val categories = listOf(
        Triple("Communication", stringResource(R.string.via_category_communication), stringResource(R.string.via_category_communication_desc)),
        Triple("Business/Productivity", stringResource(R.string.via_category_business), stringResource(R.string.via_category_business_desc)),
        Triple("Shopping", stringResource(R.string.via_category_shopping), stringResource(R.string.via_category_shopping_desc)),
        Triple("Other", stringResource(R.string.preference_gathering_option_other), ""),
    )
    val selectedCategories = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.via_category_header),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.via_category_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            categories.forEach { (id, title, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedCategories[id] == true,
                            onClick = { selectedCategories[id] = !(selectedCategories[id] ?: false) },
                            role = Role.Checkbox
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedCategories[id] == true,
                        onCheckedChange = null
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (desc.isNotEmpty()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.via_category_footer),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                onComplete(selectedCategories.filter { it.value }.keys.toList())
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
fun ValueDiscoveryView(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val goal by viewModel.primaryGoal.collectAsState()

    val sampleText = when (goal) {
        "Accessibility" -> stringResource(R.string.value_discovery_sample_text_accessibility)
        "Productivity" -> stringResource(R.string.value_discovery_sample_text_productivity)
        "Hands-free Use" -> stringResource(R.string.value_discovery_sample_text_hands_free)
        else -> stringResource(R.string.value_discovery_sample_text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.value_discovery_header),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.value_discovery_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.speakSample(sampleText) },
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = stringResource(R.string.value_discovery_button_speak),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next))
        }
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
