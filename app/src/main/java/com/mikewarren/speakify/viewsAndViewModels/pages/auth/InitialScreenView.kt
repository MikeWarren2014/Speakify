package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.TrialStatus
import com.mikewarren.speakify.data.uiStates.InitialScreenUiState

@Composable
fun InitialScreenView(viewModel: InitialScreenViewModel = hiltViewModel()) {
    var initialScreenUiState by remember { mutableStateOf<InitialScreenUiState>(InitialScreenUiState.Title) }


    if (initialScreenUiState is InitialScreenUiState.Title) {
         TitleView(
            viewModel,
            onSignInClicked = { initialScreenUiState = InitialScreenUiState.SignIn },
            onSignUpClicked = { initialScreenUiState = InitialScreenUiState.SignUp },
        )
        return
    }
    SignInOrUpView(initialScreenUiState)
}

@Composable
fun TitleView(
    viewModel: InitialScreenViewModel = hiltViewModel(),
    onSignInClicked: () -> Unit,
    onSignUpClicked: () -> Unit,
) {
    val trialStatus by viewModel.trialStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(128.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Speakify",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Text(
            text = stringResource(R.string.slogan),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSignUpClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sign_up))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onSignInClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sign_in))
        }

        if (trialStatus is TrialStatus.NotStarted) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.not_sure_yet),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.startTrial() },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.try_free,
                    Constants.TrialNumberOfDays))
            }
        } else if (trialStatus is TrialStatus.Expired) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.trial_expired),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
