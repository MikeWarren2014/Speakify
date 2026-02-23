package com.mikewarren.speakify.viewsAndViewModels.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.mikewarren.speakify.R

data class NavigationItem(@StringRes val titleResId: Int, val icon: ImageVector, val route: String)

object Routes {
    const val About = "about"
    const val ImportantApps = "important_apps"
    const val Settings = "settings"
    const val Support = "support"
    const val Legal = "legal"
    const val AccountDeletion = "account_deletion"
}

val navItems = listOf(
    NavigationItem(
        R.string.title_important_apps,
        Icons.AutoMirrored.Filled.List,
        Routes.ImportantApps,
    ),
    NavigationItem(
        R.string.title_settings,
        Icons.Filled.Settings,
        Routes.Settings,
    ),
    NavigationItem(
        R.string.title_support,
        Icons.Default.Email,
        Routes.Support,
    ),
    NavigationItem(
        R.string.title_legal,
        Icons.Default.PrivacyTip,
        Routes.Legal,
    ),
    NavigationItem(
        R.string.title_about,
        Icons.Filled.Info,
        Routes.About,
    ),
)
