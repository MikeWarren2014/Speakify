package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import android.annotation.SuppressLint
import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.card.ToggleCard

@SuppressLint("InlinedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionsView(
    viewModel: NotificationPermissionsViewModel = hiltViewModel(),
    permissions: Array<String>,
    onRequestPermission: (String, (Boolean) -> Unit) -> Unit,
    onDone: (Boolean) -> Unit,
) {

    val permissionStates by viewModel.permissionStates.collectAsState()
    var showRationaleDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(permissions)
    }

    val isReadNotificationsGranted =
        permissionStates[Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE] ?: false

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
                title = { Text(stringResource(R.string.notification_permissions_title)) }
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
                ToggleCard(
                    stringResource(R.string.read_notifications),
                    stringResource(R.string.notification_access_required_message),
                    isReadNotificationsGranted,
                    isReadNotificationsGranted,
                ) { _ ->
                    onRequestPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) { success ->
                        if (success) {
                            viewModel.grantPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                        }
                    }
                }
                if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                    val isPostNotificationsGranted =
                        permissionStates[Manifest.permission.POST_NOTIFICATIONS] ?: false

                    ToggleCard(
                        stringResource(R.string.post_notifications),
                        stringResource(R.string.post_notifications),
                        isPostNotificationsGranted,
                        isPostNotificationsGranted
                    ) { _ ->
                        onRequestPermission(Manifest.permission.POST_NOTIFICATIONS) { success ->
                            if (success) {
                                viewModel.grantPermission(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val ungranted = permissions.filter { permissionStates[it] != true }
                        if (ungranted.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                            showRationaleDialog = Manifest.permission.POST_NOTIFICATIONS
                        } else {
                            onDone(ungranted.isEmpty())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isReadNotificationsGranted
                ) {
                    Text(stringResource(R.string.next))
                }
            }

            if (showRationaleDialog != null) {
                val permission = showRationaleDialog!!
                AlertDialog(
                    onDismissRequest = { showRationaleDialog = null },
                    title = { Text(stringResource(R.string.permissions_required_title)) },
                    text = { Text(stringResource(R.string.post_notifications)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showRationaleDialog = null
                            onRequestPermission(permission) { success ->
                                if (success) {
                                    viewModel.checkPermissions(permissions)
                                }
                                // After requesting, check again if everything is granted
                                val stillUngranted = permissions.filter { p ->
                                    if (p == permission) !success
                                    else permissionStates[p] != true
                                }
                                onDone(stillUngranted.isEmpty())
                            }
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRationaleDialog = null
                            onDone(false)
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}