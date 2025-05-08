package com.mikewarren.speakify.data.events

import android.content.pm.ApplicationInfo

sealed class PackageQueryEvent: Emittable<ApplicationInfo> {
    data class DataFetched(val data: List<ApplicationInfo>) : PackageQueryEvent()
    object PermissionDenied : PackageQueryEvent()
    data class FetchFailed(val message: String) : PackageQueryEvent()

    object RequestData : PackageQueryEvent()
}