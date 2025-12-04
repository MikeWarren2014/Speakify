package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.mikewarren.speakify.data.uiStates.SignInUiState
import com.mikewarren.speakify.viewsAndViewModels.widgets.PasswordField


@Composable
fun ForgotPasswordView(reason: String) {
    val viewModel : ForgotPasswordViewModel = viewModel()

    var subtext = ""
    if (reason == SignInUiState.ResetPassword.PwnedCredentials) {
        subtext = "Your password has been compromised."
    }

    val focusManager = LocalFocusManager.current
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Log.d("ForgotPasswordView", "state == ${state}")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            ForgotPasswordViewModel.UiState.Complete -> {
                Text("Active session: ${Clerk.session?.id}")
            }

            ForgotPasswordViewModel.UiState.NeedsFirstFactor -> {
                InputContent(
                    header = "Verify Email",
                    subtext = "Check your email inbox for the verification code.",
                    placeholder = "Enter your code",
                    keyboardType = KeyboardType.NumberPassword,
                    buttonText = "Verify",
                    focusManager = focusManager,
                    onDone = viewModel::verify,
                )
            }
            ForgotPasswordViewModel.UiState.NeedsNewPassword -> {
                InputContent(
                    header = "New Password",
                    subtext = "Enter your new secure password below.",
                    placeholder = "Enter your new password",
                    buttonText = "Set new password",
                    onDone = viewModel::setNewPassword,
                    focusManager = focusManager,
                    keyboardType = KeyboardType.Password,
                )
            }
            ForgotPasswordViewModel.UiState.NeedsSecondFactor -> {
                Text("2FA is required but this UI does not handle that")
            }
            ForgotPasswordViewModel.UiState.SignedOut -> {
                InputContent(
                    header = "Forgot Password",
                    subtext = subtext,
                    placeholder = "Enter your email address",
                    buttonText = "Send Reset Code",
                    onDone = viewModel::createSignIn,
                    keyboardType = KeyboardType.Email,
                    focusManager = focusManager,
                )
            }

            ForgotPasswordViewModel.UiState.Loading -> CircularProgressIndicator()
        }
    }

}

@Composable
fun InputContent(
    header: String? = null,
    subtext: String? = null,
    placeholder: String,
    buttonText: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusManager: FocusManager,
    onDone: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        // Display Header if provided
        if (header != null) {
            Text(
                text = header,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }

        // Display Subtext if provided
        if (subtext != null) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (keyboardType == KeyboardType.Password) {
            PasswordField(
                value = value,
                onValueChange = { value = it },
                placeholderText = placeholder,
                onDone = { onDone(value) },
                focusManager = focusManager,
            )
        } else {
            TextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onDone(value)
                    }
                ),
                placeholder = { Text(placeholder) },
            )
        }
        Button(onClick = { onDone(value) }) { Text(buttonText) }
    }
}