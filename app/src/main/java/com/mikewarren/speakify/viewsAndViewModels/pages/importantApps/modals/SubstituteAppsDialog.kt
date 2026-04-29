package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.SubstituteAppCandidate

@Composable
fun SubstituteAppsDialog(
    candidate: SubstituteAppCandidate,
    onSubstitute: (UserAppModel) -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSubstitute by remember { mutableStateOf<UserAppModel?>(candidate.substitutes.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.substitute_missing_app)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.app_is_not_installed,
                        candidate.missingApp.appName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(candidate.substitutes) { substitute ->
                        val icon = remember(substitute.packageName) {
                            try {
                                context.packageManager.getApplicationIcon(substitute.packageName)
                            } catch (_: Exception) {
                                null
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubstitute = substitute }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSubstitute == substitute,
                                onClick = { selectedSubstitute = substitute }
                            )
                            AsyncImage(
                                model = icon,
                                contentDescription = "${substitute.appName} icon",
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = substitute.appName,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedSubstitute?.let { onSubstitute(it) }
                },
                enabled = selectedSubstitute != null
            ) {
                Text(stringResource(R.string.confirm_substitute))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onIgnore) {
                    Text(stringResource(R.string.dont_ask_again))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.later))
                }
            }
        }
    )
}

private fun Modifier.heightIn(max: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.height(max) // simplified for now or use foundation version if available
)
