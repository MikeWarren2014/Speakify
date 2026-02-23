package com.mikewarren.speakify.utils

import android.content.Context
import android.content.pm.PackageManager
import com.mikewarren.speakify.viewsAndViewModels.pages.AboutInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAboutInfo(): AboutInfo {
        val author = "Mike Warren"
        
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val appName = packageInfo.applicationInfo?.loadLabel(packageManager).toString()
            val version = packageInfo.versionName

            AboutInfo(
                appName = appName,
                version = "$appName v$version",
                author
            )
        } catch (e: PackageManager.NameNotFoundException) {
            AboutInfo(
                appName = "Speakify",
                version = "Speakify v1.0 (fallback)",
                author
            )
        }
    }
}
