package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R

@Composable
fun MessagingAppAdditionalSettingsView(viewModel: BaseMessagingAppAdditionalSettingsViewModel,
                                       MoreSettings: @Composable (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.read_messages),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = viewModel.readMessages,
                onCheckedChange = { viewModel.readMessages = it }
            )


        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.ignore_single_word_messages),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = viewModel.ignoreSingleWordMessages,
                onCheckedChange = { viewModel.ignoreSingleWordMessages = it }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.ignore_reactions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = viewModel.ignoreReactions,
                onCheckedChange = { viewModel.ignoreReactions = it }
            )
        }

        if (MoreSettings != null)
            MoreSettings()
    }
}
