package com.mikewarren.speakify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.events.ContactEvent
import com.mikewarren.speakify.data.events.ContactEventBus
import com.mikewarren.speakify.databinding.ContactsFetcherActivityBinding
import com.mikewarren.speakify.ui.theme.MyApplicationTheme
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.contactsFetcher.ContactsFetcherView
import com.mikewarren.speakify.viewsAndViewModels.pages.contactsFetcher.ContactsFetcherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsFetcherActivity : AppCompatActivity() {
    private val eventBus = ContactEventBus.GetInstance()
    private lateinit var binding: ContactsFetcherActivityBinding

    private val viewModel: ContactsFetcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ContactsFetcherActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestContactsPermission()
    }

    private fun requestContactsPermission() {
        if (!hasContactsPermission()) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_REQUEST_CODE
            )
        } else {
            fetchContacts()
        }

        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactsFetcherView(viewModel)
                }
            }
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                fetchContacts()
            } else {
                eventBus.post(ContactEvent.PermissionDenied)
                finish()
            }
        }
    }

    private fun fetchContacts() {
        lifecycleScope.launch {
            try {
                viewModel.setIsLoading(true)
                val contacts = fetchContactsFromSystem()
                eventBus.post(ContactEvent.ContactsFetched(contacts))
            } catch (e: Exception) {
                eventBus.post(ContactEvent.FetchFailed(e.message ?: "Unknown error"))
            } finally {
                finish()
                viewModel.setIsLoading(false)
            }
        }
    }

    private suspend fun fetchContactsFromSystem(): List<ContactModel> {
        val resultSetCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        ) ?: return emptyList()

        return resultSetCursor.use { cursor ->
            val contacts = mutableListOf<ContactModel>()
            withContext(Dispatchers.IO) {
                while (cursor.moveToNext()) {
                    var id: Long = 0
                    var name: String = ""
                    var phoneNumber: String = ""

                    val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
                    val phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (idIdx > -1)
                        id = cursor.getLong(idIdx)

                    if (nameIdx > -1)
                        name = cursor.getString(nameIdx)

                    if (phoneNumberIdx > -1)
                        phoneNumber = cursor.getString(phoneNumberIdx)

                    contacts.add(
                        ContactModel(
                            id,
                            name,
                            phoneNumber,
                        )
                    )
                }
            }
            contacts
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }

}