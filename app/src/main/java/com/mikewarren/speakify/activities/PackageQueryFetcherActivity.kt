package com.mikewarren.speakify.activities

import android.Manifest
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.mikewarren.speakify.data.constants.PermissionCodes
import com.mikewarren.speakify.data.events.PackageQueryEvent
import com.mikewarren.speakify.data.events.PackageQueryEventBus
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.AppFetcherViewModel
import com.mikewarren.speakify.utils.NotificationPermissionHelper

@RequiresApi(Build.VERSION_CODES.R)
class PackageQueryFetcherActivity: BaseFetcherActivity<ApplicationInfo, PackageQueryEvent>(
    eventBus = PackageQueryEventBus.GetInstance(),
    permission = Manifest.permission.QUERY_ALL_PACKAGES,
    permissionRequestCode = PermissionCodes.PackageQueryFetcher,
) {
    override val viewModel: AppFetcherViewModel by viewModels()


    override fun getPermissionDeniedEvent(): PackageQueryEvent {
        return PackageQueryEvent.PermissionDenied
    }

    override fun getFetchFailedEvent(message: String): PackageQueryEvent {
        return PackageQueryEvent.FetchFailed(message)
    }

    override fun getDataFetchedEvent(data: List<ApplicationInfo>): PackageQueryEvent {
        return PackageQueryEvent.DataFetched(data)
    }

    override suspend fun fetchDataFromSystem(): List<ApplicationInfo> {
        return NotificationPermissionHelper(this).getAppsWithNotificationPermission()
    }
}