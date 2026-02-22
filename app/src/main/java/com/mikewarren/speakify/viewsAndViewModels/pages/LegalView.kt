package com.mikewarren.speakify.viewsAndViewModels.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.viewsAndViewModels.widgets.card.ActionCard
import androidx.core.net.toUri
import com.mikewarren.speakify.data.constants.DocumentURLs

@Composable
fun LegalView() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Review our policies and terms of service.",
            style = MaterialTheme.typography.bodyMedium
        )

        ActionCard(
            title = "Privacy Policy",
            description = "Read about how we handle and protect your data.",
            buttonText = "View Policy",
            icon = Icons.Default.PrivacyTip
        ) {
            context.startActivity(Intent(
                Intent.ACTION_VIEW,
                DocumentURLs.PrivacyPolicy.toUri()))
        }

        ActionCard(
            title = "Terms of Service",
            description = "Read our terms and conditions for using Speakify.",
            buttonText = "View Terms",
            icon = Icons.Default.Description
        ) {
            context.startActivity(Intent(
                Intent.ACTION_VIEW,
                DocumentURLs.TermsOfService.toUri()))
        }
    }
}
