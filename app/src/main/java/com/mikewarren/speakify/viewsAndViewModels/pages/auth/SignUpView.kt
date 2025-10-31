package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun SignUpView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean) -> Unit) {

    val state by viewModel.uiState.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text("Sign Up")

        if (state is SignUpViewModel.SignUpUiState.NeedsVerification) {
            var code by remember { mutableStateOf("") }

            TextField(value = code, onValueChange = { code = it })

            Button(onClick = { viewModel.verify(code, onDone) }) { Text("Verify") }
        } else {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            TextField(value = email, onValueChange = { email = it }, placeholder = { Text("Email") })

            TextField(
                value = password,
                placeholder = { Text("Password") },
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
            )

            Button(onClick = { viewModel.signUp(email, password, onDone) }) { Text("Sign Up") }
        }
    }
}