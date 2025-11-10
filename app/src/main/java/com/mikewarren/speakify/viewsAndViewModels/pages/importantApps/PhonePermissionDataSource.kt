package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.semantics.error
import com.mikewarren.speakify.activities.PhonePermissionsActivity
import com.mikewarren.speakify.data.events.PhonePermissionEvent
import com.mikewarren.speakify.data.events.PhonePermissionEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Making this a Singleton makes it easy to inject and use anywhere
@Singleton

class PhonePermissionDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val eventBus = PhonePermissionEventBus.GetInstance()

    init {
        scope.launch {
            eventBus.events().collect { event: PhonePermissionEvent ->
                when (event) {
                    is PhonePermissionEvent.RequestPermission -> {
                        Log.d(
                            "PhonePermissionDS",
                            "RequestData event received. Launching PhonePermissionsActivity."
                        )
                        onRequestData()
                    }

                    is PhonePermissionEvent.PermissionGranted -> {
                        Log.d("PhonePermissionDS", "PermissionGranted event received.")
                        // Can add logic here if something needs to happen app-wide on success
                    }

                    is PhonePermissionEvent.PermissionDenied -> {
                        Log.w("PhonePermissionDS", "PermissionDenied event received.")
                        // Can handle failure/denial globally here if needed
                    }

                    is PhonePermissionEvent.Failure -> {
                        Log.e("PhonePermissionDS", "FetchFailed event received: ${event.message}")
                    }
                }
            }
        }
    }

    /**
     * Launches the activity responsible for asking for phone-related permissions.
     */
    private fun onRequestData() {
        val intent = Intent(context, PhonePermissionsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Public method to allow other parts of the app (like ViewModels) to easily
     * trigger the permission request flow.
     */
    fun requestPermissions() {
        scope.launch {
            eventBus.post(PhonePermissionEvent.RequestPermission)
        }
    }
}