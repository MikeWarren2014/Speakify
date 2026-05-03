package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.card.ToggleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonePermissionsView(
    viewModel: PhonePermissionsViewModel = hiltViewModel(),
    permissions: Array<String>,
    onRequestPermission: (String, (Boolean) -> Unit) -> Unit,
    onDone: (Boolean) -> Unit,
) {
    val permissionStates by viewModel.permissionStates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(permissions)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(permissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.phone_permissions_title)) }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Read Phone State
                val isPhoneStateGranted = permissionStates[Manifest.permission.READ_PHONE_STATE] ?: false
                ToggleCard(
                    title = stringResource(R.string.read_phone_state),
                    description = stringResource(R.string.read_phone_state_description),
                    isChecked = isPhoneStateGranted,
                    isDisabled = isPhoneStateGranted,
                    onCheckedChange = {
                        onRequestPermission(Manifest.permission.READ_PHONE_STATE) { success ->
                            if (success) viewModel.grantPermission(Manifest.permission.READ_PHONE_STATE)
                        }
                    }
                )

                // Read Contacts
                val isContactsGranted = permissionStates[Manifest.permission.READ_CONTACTS] ?: false
                ToggleCard(
                    title = stringResource(R.string.read_contacts),
                    description = stringResource(R.string.read_contacts_description),
                    isChecked = isContactsGranted,
                    isDisabled = isContactsGranted,
                    onCheckedChange = {
                        onRequestPermission(Manifest.permission.READ_CONTACTS) { success ->
                            if (success) viewModel.grantPermission(Manifest.permission.READ_CONTACTS)
                        }
                    }
                )

                // Call Screening Role
                val isCallScreeningGranted = permissionStates[Manifest.permission.BIND_SCREENING_SERVICE] ?: false
                ToggleCard(
                    title = stringResource(R.string.call_screening),
                    description = stringResource(R.string.call_screening_description),
                    isChecked = isCallScreeningGranted,
                    isDisabled = isCallScreeningGranted,
                    onCheckedChange = {
                        onRequestPermission(Manifest.permission.BIND_SCREENING_SERVICE) { success ->
                            if (success) viewModel.grantPermission(Manifest.permission.BIND_SCREENING_SERVICE)
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onDone(permissionStates[Manifest.permission.READ_PHONE_STATE] == true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isPhoneStateGranted
                ) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}
