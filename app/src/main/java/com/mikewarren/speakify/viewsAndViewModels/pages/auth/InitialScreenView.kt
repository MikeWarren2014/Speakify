package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.uiStates.InitialScreenUiState

@Composable
fun InitialScreenView() {
    var initialScreenUiState by remember { mutableStateOf<InitialScreenUiState>(InitialScreenUiState.Title) }

    if (initialScreenUiState is InitialScreenUiState.Title) {
         TitleView(
            onSignInClicked = { initialScreenUiState = InitialScreenUiState.SignIn },
            onSignUpClicked = { initialScreenUiState = InitialScreenUiState.SignUp }
        )
        return
    }
    SignInOrUpView(initialScreenUiState)
}

@Composable
fun TitleView(onSignInClicked: () -> Unit, onSignUpClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
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
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onSignUpClicked) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSignInClicked) {
            Text("Sign In")
        }
    }
}
