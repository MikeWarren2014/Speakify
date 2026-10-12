package com.mikewarren.speakify.viewsAndViewModels.pages.trialOnboarding

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mikewarren.speakify.data.constants.OnboardingConstants
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
                viewModel = viewModel,
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
            Triple(OnboardingConstants.PrimaryGoal.Accessibility, stringResource(R.string.preference_gathering_option_accessibility), stringResource(R.string.preference_gathering_option_accessibility_desc)),
            Triple(OnboardingConstants.PrimaryGoal.Productivity, stringResource(R.string.preference_gathering_option_productivity), stringResource(R.string.preference_gathering_option_productivity_desc)),
            Triple(OnboardingConstants.PrimaryGoal.HandsFreeUse, stringResource(R.string.preference_gathering_option_hands_free_use), stringResource(R.string.preference_gathering_option_hands_free_use_desc)),
            Triple(OnboardingConstants.PrimaryGoal.Other, stringResource(R.string.preference_gathering_option_other), ""),
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
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: (List<String>) -> Unit
) {
    val primaryGoal by viewModel.primaryGoal.collectAsState()
    if (primaryGoal == null)
        return
    val categories = listOf(
        Triple(OnboardingConstants.AppCategory.Communication, stringResource(R.string.via_category_communication), stringResource(R.string.via_category_communication_desc)),
        Triple(OnboardingConstants.AppCategory.BusinessProductivity, stringResource(R.string.via_category_business), stringResource(R.string.via_category_business_desc)),
        Triple(OnboardingConstants.AppCategory.Shopping, stringResource(R.string.via_category_shopping), stringResource(R.string.via_category_shopping_desc)),
        Triple(OnboardingConstants.AppCategory.Other, stringResource(R.string.preference_gathering_option_other), ""),
    )
    
    val selectedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val suggestedAppCategories = viewModel.getSuggestedAppCategories(primaryGoal!!)

    val infiniteTransition = rememberInfiniteTransition(label = "pulsingBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

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
                val isSuggested = id in suggestedAppCategories && selectedCategories[id] != true

                var itemModifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedCategories[id] == true,
                        onClick = { selectedCategories[id] = !(selectedCategories[id] ?: false) },
                        role = Role.Checkbox
                    )
                    .padding(16.dp)
                if (isSuggested) {
                    itemModifier = itemModifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                            shape = RoundedCornerShape(12.dp)
                        )
                }

                Row(
                    modifier = itemModifier,
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

        val isSelectedCategoriesEmpty = selectedCategories.values.none { it }
        var buttonText = stringResource(R.string.next)
        if (isSelectedCategoriesEmpty)
            buttonText = stringResource(R.string.via_category_error_message)

        Button(
            enabled = !isSelectedCategoriesEmpty,
            onClick = {
                if (isSelectedCategoriesEmpty)
                    return@Button

                onComplete(selectedCategories.filter { it.value }.keys.toList())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonText)
        }
    }
}

@Composable
fun ValueDiscoveryView(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val goal by viewModel.primaryGoal.collectAsState()

    val sampleText = when (goal) {
        OnboardingConstants.PrimaryGoal.Accessibility -> stringResource(R.string.value_discovery_sample_text_accessibility)
        OnboardingConstants.PrimaryGoal.Productivity -> stringResource(R.string.value_discovery_sample_text_productivity)
        OnboardingConstants.PrimaryGoal.HandsFreeUse -> stringResource(R.string.value_discovery_sample_text_hands_free)
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
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Text(stringResource(R.string.conversion_ready_button_continue))
        }
    }
}

