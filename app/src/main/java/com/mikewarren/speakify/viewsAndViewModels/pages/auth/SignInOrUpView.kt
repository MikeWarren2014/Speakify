package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SignInOrUpView() {
    var isSignUp by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        if (isSignUp) {
            SignUpView(
                onDone = { success ->
                    if (success) {
                        isSignUp = false
                    }
                })
        } else {
            SignInView()
        }

        Button(onClick = { isSignUp = !isSignUp }) {
            if (isSignUp) {
                Text("Already have an account? Sign in")
            } else {
                Text("Don't have an account? Sign up")
            }
        }
    }
}