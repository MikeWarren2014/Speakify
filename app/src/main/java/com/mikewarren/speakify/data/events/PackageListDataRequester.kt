package com.mikewarren.speakify.data.events

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PackageListDataRequester protected constructor(context: Context) : BaseDataRequester<ApplicationInfo, PackageQueryEvent>(context), ITaggable {
    companion object {
        private var _instance: PackageListDataRequester? = null
        fun GetInstance(context: Context) : PackageListDataRequester {
            if (_instance == null) {
                _instance = PackageListDataRequester(context.applicationContext)
            }

            return _instance!!
        }
    }

    override val eventBus = PackageQueryEventBus.GetInstance()

    init {
        scope.launch {
            eventBus.events().collect { event: PackageQueryEvent ->
                when (event) {
                    is PackageQueryEvent.DataFetched -> {
                        dataFlow.emit(event.data)
                        _isLoading.value = false
                    }
                    is PackageQueryEvent.FetchFailed -> {
                        Log.e(TAG, "Error when fetching the packages: ${event.message}")
                        _isLoading.value = false
                    }
                    PackageQueryEvent.PermissionDenied -> {
                        Log.e(TAG, "Permission denied for fetching the packages")
                        _isLoading.value = false
                    }
                    PackageQueryEvent.RequestData -> {
                        _isLoading.value = true
                        onRequestData()
                    }
                }
            }
        }
    }

    override fun getRequestEvent(): PackageQueryEvent {
        return PackageQueryEvent.RequestData
    }

    override fun onRequestData() {
        scope.launch {
            try {
                val data = NotificationPermissionHelper(context).getAppsWithNotificationPermission()
                eventBus.post(PackageQueryEvent.DataFetched(data))
            } catch (e: Exception) {
                eventBus.post(PackageQueryEvent.FetchFailed(e.message ?: "Unknown error"))
            }
        }
    }
}
