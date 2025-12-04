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
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.card.ActionCard
import kotlin.text.format

@Composable
fun SupportView() {
    val context = LocalContext.current

    val userEmail = Clerk.user?.emailAddresses?.first()?.emailAddress

    val emailSubject = context.getString(R.string.feedback_email_subject)
    val emailBodyTemplate = context.getString(R.string.feedback_email_body)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "We'd love to hear from you!",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Since this is an early release, your feedback is crucial in shaping the future of Speakify.",
            style = MaterialTheme.typography.bodyMedium
        )

        ActionCard(
            title = "Send Feedback",
            description = "Have a feature request or found a bug? Email us directly.",
            buttonText = "Email Us",
            icon = Icons.Default.Email
        ) {
            val emailBody = String.format(
                emailBodyTemplate,
                "${Build.MANUFACTURER} ${Build.MODEL}",
                Build.VERSION.RELEASE,
                userEmail,
            ).replace("\n", "\n\n")

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("mwarren04011990@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }
            context.startActivity(intent)
        }
    }
}