package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.uiStates.InitialScreenUiState
import com.mikewarren.speakify.data.uiStates.SignUpUiState

@Composable
fun SignInOrUpView(initialScreenUiState: InitialScreenUiState) {
    var isSignUp by remember { mutableStateOf(initialScreenUiState is InitialScreenUiState.SignUp) }
    var signUpUiState by remember { mutableStateOf<SignUpUiState>(SignUpUiState.SignedOut) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        if (isSignUp) {
            SignUpView(
                onDone = { success, state ->
                    signUpUiState = state
                    if ((success) && (state is SignUpUiState.Success)) {
                        isSignUp = false
                    }
                })
        } else {
            SignInView()
        }

        if (signUpUiState is SignUpUiState.SignedOut) {
            Button(onClick = { isSignUp = !isSignUp }) {
                if (isSignUp) {
                    Text(stringResource(R.string.sign_up_switch_to_sign_in))
                    return@Button
                }
                Text(stringResource(R.string.sign_in_switch_to_sign_up))
            }
        }
    }
}
