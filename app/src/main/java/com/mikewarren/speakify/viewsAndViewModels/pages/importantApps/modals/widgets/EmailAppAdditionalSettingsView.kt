package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent
import com.mikewarren.speakify.viewsAndViewModels.widgets.CustomSwitch

@AdditionalSettingsComponent(appTypes = ["email"])
@Composable
fun EmailAppAdditionalSettingsView(viewModel: EmailAppAdditionalSettingsViewModel) {
    MessageReadingAdditionalSettingsView(viewModel, MoreSettings = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.announce_multiple_new_messages_setting),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                CustomSwitch(
                    checked = viewModel.announceMultipleMessages,
                    onCheckedChange = { viewModel.announceMultipleMessages = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.messageKeywords,
                onValueChange = { viewModel.messageKeywords = it },
                label = { Text(stringResource(R.string.email_message_keywords)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    })
}
