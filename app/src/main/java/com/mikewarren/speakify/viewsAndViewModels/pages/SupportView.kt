package com.mikewarren.speakify.viewsAndViewModels.pages

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.card.ActionCard
import androidx.core.net.toUri

@Composable
fun SupportView() {
    val context = LocalContext.current

    val userEmail = Clerk.user?.emailAddresses?.first()?.emailAddress

    val emailSubject = stringResource(R.string.feedback_email_subject)
    val emailBodyTemplate = stringResource(R.string.feedback_email_body)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.support_instruction_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.support_instruction_description),
            style = MaterialTheme.typography.bodyMedium
        )

        ActionCard(
            title = stringResource(R.string.support_feedback_card_title),
            description = stringResource(R.string.support_feedback_card_description),
            buttonText = stringResource(R.string.support_feedback_card_button),
            icon = Icons.Default.Email
        ) {
            val emailBody = String.format(
                emailBodyTemplate,
                "${Build.MANUFACTURER} ${Build.MODEL}",
                Build.VERSION.RELEASE,
                userEmail,
            ).replace("\n", "\n\n")

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@speakify.it"))
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }
            context.startActivity(intent)
        }
    }
}
