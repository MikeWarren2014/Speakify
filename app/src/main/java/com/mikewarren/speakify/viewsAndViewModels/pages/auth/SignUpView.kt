package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.data.uiStates.SignUpUiState

@Composable
fun SignUpView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {

    val state by viewModel.uiState.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)

        SignUpScreenView(viewModel, state, onDone)
    }
}

@Composable
fun SignUpScreenView(viewModel: SignUpViewModel = viewModel(), state: SignUpUiState, onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
    if (state is SignUpUiState.NeedsVerification) {
        VerificationView(viewModel, onDone)
        return
    }
    SignUpFormView(viewModel, onDone)
}

@Composable
fun VerificationView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
    var code by remember { mutableStateOf("") }

    Text(
        "Check your email inbox for the verification code and enter it here. " +
                "Please note, it could take a couple minutes to arrive and could be in the spam folder.",
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )


    TextField(value = code, onValueChange = { code = it })

    Button(onClick = { viewModel.checkVerification(code, onDone) }) { Text("Verify") }
}

@Composable
fun SignUpFormView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
    var passwordVisible by remember { mutableStateOf(false) }

    TextField(value = viewModel.model.firstName,
        onValueChange = { firstName: String -> viewModel.onModelChange(viewModel.model.copy(
            firstName = firstName
        ))},
        placeholder = { Text("First Name *") },
        isError = viewModel.errorsDict.containsKey("firstName"),
        supportingText = {
            viewModel.errorsDict["firstName"]?.let { Text(it) }
        },
    )

    TextField(value = viewModel.model.lastName,
        onValueChange = { lastName: String -> viewModel.onModelChange(viewModel.model.copy(
            lastName = lastName
        ))},
        placeholder = { Text("Last Name") })


    TextField(value = viewModel.model.email,
        onValueChange = { email: String -> viewModel.onModelChange(viewModel.model.copy(
            email = email
         )) },
        placeholder = { Text("Email *") },
        isError = viewModel.errorsDict.containsKey("email"),
        supportingText = {
            viewModel.errorsDict["email"]?.let { Text(it) }
        }
    )

    TextField(
        value = viewModel.model.password,
        placeholder = { Text("Password *") },
        onValueChange = { password : String -> viewModel.onModelChange(viewModel.model.copy(
            password = password
        )) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = viewModel.errorsDict.containsKey("password"),
        supportingText = {
            viewModel.errorsDict["password"]?.let { Text(it) }
        },
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff

            // Please provide localized description for accessibility services
            val description = if (passwordVisible) "Hide password" else "Show password"

            IconButton(onClick = {passwordVisible = !passwordVisible}){
                Icon(imageVector  = image, description)
            }
        }
    )
    Button(onClick = { viewModel.signUp( onDone) }) { Text("Sign Up") }

}
