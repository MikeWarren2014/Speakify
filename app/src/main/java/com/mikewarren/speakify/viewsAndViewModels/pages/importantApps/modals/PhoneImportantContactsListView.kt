package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.NotificationSource
import kotlinx.coroutines.flow.collectLatest

/**
 * Custom contract to pick one or more phone numbers.
 * This allows us to get the name and number without READ_CONTACTS permission.
 */
class PickPhoneNumbers : ActivityResultContract<Unit?, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI).apply {
            // Standard flag for multiple selection
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            // Extra flag supported by some devices (Samsung/Google)
            putExtra("com.android.contacts.extra.MULTI_SELECT", true)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()

        val uris = mutableListOf<Uri>()
        // Handle single selection
        intent.data?.let { uris.add(it) }
        // Handle multiple selection via ClipData
        intent.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        }
        return uris.distinct()
    }
}

@Composable
fun PhoneImportantContactsListView(viewModel: PhoneImportantContactsListViewModel) {
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = PickPhoneNumbers()
    ) { uris ->
        uris.forEach { uri ->
            // This URI points directly to a Phone record, so we can query it without permission
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                    viewModel.addNotificationSource(NotificationSource(number, name))
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.launchContactPickerEvent.collectLatest {
            contactPickerLauncher.launch(null)
        }
    }

    NotificationSourceListView(viewModel) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.onAddContactClicked() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = stringResource(R.string.pick_contact))
        }
    }
}
