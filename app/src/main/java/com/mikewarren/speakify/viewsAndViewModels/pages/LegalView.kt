package com.mikewarren.speakify.viewsAndViewModels.pages

import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
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
            text = stringResource(R.string.legal_instruction),
            style = MaterialTheme.typography.bodyMedium
        )

        ActionCard(
            title = stringResource(R.string.legal_privacy_policy_title),
            description = stringResource(R.string.legal_privacy_policy_description),
            buttonText = stringResource(R.string.legal_privacy_policy_button),
            icon = Icons.Default.PrivacyTip
        ) {
            context.startActivity(Intent(
                Intent.ACTION_VIEW,
                DocumentURLs.PrivacyPolicy.toUri()))
        }

        ActionCard(
            title = stringResource(R.string.legal_terms_of_service_title),
            description = stringResource(R.string.legal_terms_of_service_description),
            buttonText = stringResource(R.string.legal_terms_of_service_button),
            icon = Icons.Default.Description
        ) {
            context.startActivity(Intent(
                Intent.ACTION_VIEW,
                DocumentURLs.TermsOfService.toUri()))
        }
    }
}
