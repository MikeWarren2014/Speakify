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
            text = "Legal",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
            // TODO: Replace this URL with your actual Privacy Policy link
            val privacyPolicyUrl = "https://doc-hosting.flycricket.io/speakify-privacy-policy/b5748f69-011e-427e-9114-071d734c1d6e/privacy"
            val intent = Intent(Intent.ACTION_VIEW, privacyPolicyUrl.toUri())
            context.startActivity(intent)
        }

        ActionCard(
            title = "Terms of Service",
            description = "Read our terms and conditions for using Speakify.",
            buttonText = "View Terms",
            icon = Icons.Default.Description
        ) {
            // TODO: Replace this URL with your actual Terms of Service link
            val termsUrl = "https://doc-hosting.flycricket.io/speakify-terms-of-use/2124491a-1cd9-4df5-b9a8-ab4e487f504a/terms"
            val intent = Intent(Intent.ACTION_VIEW, termsUrl.toUri())
            context.startActivity(intent)
        }
    }
}
