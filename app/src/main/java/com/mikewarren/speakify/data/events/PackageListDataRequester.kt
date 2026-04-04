package com.mikewarren.speakify.data.events

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import com.mikewarren.speakify.activities.PackageQueryFetcherActivity
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.launch

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
                    PackageQueryEvent.RequestData -> onRequestData()
                }
            }
        }
    }

    override fun getRequestEvent(): PackageQueryEvent {
        return PackageQueryEvent.RequestData
    }

    override fun onRequestData() {
        context.startActivity(
            Intent(context, PackageQueryFetcherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            null)
    }
}
