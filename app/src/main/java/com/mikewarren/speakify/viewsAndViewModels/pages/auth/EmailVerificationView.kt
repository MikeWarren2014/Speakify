package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.uiStates.EmailVerificationUiState
import com.mikewarren.speakify.utils.SearchUtils


@Composable
fun EmailVerificationView(
    headerText: String = stringResource(R.string.email_verification_header),
    mainText: String,
    isLoading: Boolean = false,
    buttonText: String = stringResource(R.string.email_verification_default_button_text),
    onRequestCode: () -> Unit,
    onSubmitCode: (str: String) -> Unit,
    viewModel: EmailVerificationViewModel = viewModel()
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = headerText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ScreenMainSection(viewModel,
                mainText,
                isLoading,
                buttonText,
                onSubmitCode)

            RequestCodeButtonSection(viewModel, onRequestCode)
        }
    }

}

@Composable
fun ScreenMainSection(
    viewModel: EmailVerificationViewModel,
    mainText: String,
    isLoading: Boolean = false,
    buttonText: String = stringResource(R.string.email_verification_default_button_text),
    onSubmitCode: (str: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()

    var text = mainText
    if (uiState is EmailVerificationUiState.TooManyAttempts) {
        text = stringResource(R.string.email_verification_too_many_attempts)
    }
    if (uiState is EmailVerificationUiState.ExpiredCode) {
        text = stringResource(R.string.email_verification_expired_code)
    }
    if (uiState is EmailVerificationUiState.Success) {
        text = stringResource(R.string.email_verification_success)
    }

    Text(
        text,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )

//    if ((uiState is EmailVerificationUiState.TooManyAttempts) || (uiState is EmailVerificationUiState.ExpiredCode))
    if (SearchUtils.IsAnyOf(uiState, listOf(EmailVerificationUiState.TooManyAttempts::class,
            EmailVerificationUiState.ExpiredCode::class,
            EmailVerificationUiState.Success::class,
        )))
        return

    TextField(
        value = viewModel.code,
        onValueChange = { viewModel.code = it },
        isError = viewModel.failedAttempts > 0,
        supportingText = {
            if (viewModel.failedAttempts > 0) {
                Text(stringResource(R.string.email_verification_incorrect_code, 3 - viewModel.failedAttempts))
            }
        },
        placeholder = { Text(stringResource(R.string.email_verification_code_placeholder)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                viewModel.onCodeSubmitted(onSubmitCode)
            }
        )
    )

    Button(
        onClick = { viewModel.onCodeSubmitted(onSubmitCode) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            return@Button
        }
        Text(buttonText)
    }
}

@Composable
fun RequestCodeButtonSection(viewModel: EmailVerificationViewModel, onRequestCode: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    var enabled = true
    var text = stringResource(R.string.email_verification_request_new_code)
    if (uiState is EmailVerificationUiState.Initial) {
        enabled = false
        text = stringResource(R.string.email_verification_request_in, viewModel.secondsTilRequestCode)
    }

    Button(
        onClick = { 
            onRequestCode() 
            viewModel.startResendTimer()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    ) {

        Text(text)
    }
}
