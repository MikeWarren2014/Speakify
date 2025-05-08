package com.mikewarren.speakify.data.events

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.core.content.ContextCompat
import com.mikewarren.speakify.PackageQueryFetcherActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PackageListDataSource(context: Context) : BaseDataSource<ApplicationInfo, PackageQueryEvent>(context) {
    override val eventBus = PackageQueryEventBus.GetInstance()

    init {
        scope.launch {
            eventBus.events().collect { event: PackageQueryEvent ->
                when (event) {
                    is PackageQueryEvent.DataFetched -> {
                        dataFlow.emit(event.data)
                    }
                    is PackageQueryEvent.FetchFailed -> TODO()
                    PackageQueryEvent.PermissionDenied -> TODO()
                    PackageQueryEvent.RequestData -> onRequestData()
                }
            }
        }
    }

    override fun getRequestEvent(): PackageQueryEvent {
        return PackageQueryEvent.RequestData
    }

    override fun onRequestData() {
        ContextCompat.startActivity(context,
            Intent(context, PackageQueryFetcherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            null)
    }
}