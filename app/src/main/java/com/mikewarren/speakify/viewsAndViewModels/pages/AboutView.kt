package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutView() {
    val context = LocalContext.current
    val viewModel = AboutViewModel(context)
    val aboutInfo by viewModel.aboutInfo.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        HeaderView("What is this app?")
        Text(text = "Feeling bombarded by notifications, even from the same app? This app could help, as it lets you pick the ones you care most about, and read them to you when they pop up!")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "This app will even let you prioritize what notifications from an app (e.g. what messages/calls/...) to report to you!")
        Spacer(modifier = Modifier.height(16.dp))
        HeaderView("App Info")
        aboutInfo?.let { info ->
            Text(text = "App: ${info.appName}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Version: ${info.version}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Author: ${info.author}")
        } ?: run {
            Text(text = "Loading app information...")
        }
    }
}

@Composable
fun HeaderView(headerText: String) {
    Text(
        text = headerText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
}