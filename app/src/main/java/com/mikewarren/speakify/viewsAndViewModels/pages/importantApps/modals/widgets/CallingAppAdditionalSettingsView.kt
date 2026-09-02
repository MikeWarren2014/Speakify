package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.CustomSwitch

@Composable
fun CallingAppAdditionalSettingsView(viewModel: CallingAppAdditionalSettingsViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text= stringResource(R.string.announce_anonymous_calls),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            CustomSwitch(
                checked = viewModel.announceAnonymousCalls,
                onCheckedChange = { viewModel.announceAnonymousCalls = it }
            )
        }
    }
}