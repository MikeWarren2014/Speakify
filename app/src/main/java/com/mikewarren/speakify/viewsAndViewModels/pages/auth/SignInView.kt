package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.data.uiStates.SignInUiState
import com.mikewarren.speakify.viewsAndViewModels.widgets.PasswordField
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@Composable
fun SignInView(viewModel: SignInViewModel = viewModel()) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is SignInUiState.ResetPassword) {
        ForgotPasswordView((uiState as SignInUiState.ResetPassword).reason)
        return
    }

    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            if (event is SignInViewModel.Event.Shake) {
                shake.animateTo(0f) // Reset before starting
                for (i in 0..5) {
                    when (i % 2) {
                        0 -> shake.animateTo(10f, tween(50))
                        1 -> shake.animateTo(-10f, tween(50))
                    }
                }
                shake.animateTo(0f, tween(50))
            }
        }
    }

    val onSignInAction = {
        viewModel.signIn(email, password)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value }
    ) {
        Text("Sign In", style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next // Go to next field on "Enter"
            ),
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            placeholderText = "password",
            onDone = { onSignInAction() },
            focusManager = LocalFocusManager.current,
        )
        TextButton(
            onClick = { viewModel.onClickForgotPassword() },
            modifier = Modifier.align(Alignment.End) // Align to the right
        ) {
            Text("Forgot password?")
        }
        Button(onClick = { onSignInAction() }) { Text("Sign In") }
    }
}
