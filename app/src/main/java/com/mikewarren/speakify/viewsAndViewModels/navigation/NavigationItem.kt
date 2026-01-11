package com.mikewarren.speakify.viewsAndViewModels.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(val title: String, val icon: ImageVector, val route: String)

object Titles {
    const val About = "About"
    const val ImportantApps = "Important Apps"
    const val Settings = "Settings"
    const val Support = "Support & Feedback"
    const val Legal = "Legal"
}

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
        Titles.ImportantApps,
        Icons.AutoMirrored.Filled.List,
        Routes.ImportantApps,
    ),
    NavigationItem(
        Titles.Settings,
        Icons.Filled.Settings,
        Routes.Settings,
    ),
    NavigationItem(
        Titles.Support,
        Icons.Default.Email, // or Icons.Default.Feedback
        Routes.Support,
    ),
    NavigationItem(
        Titles.Legal,
        Icons.Default.PrivacyTip,
        Routes.Legal,
    ),
    NavigationItem(
        Titles.About,
        Icons.Filled.Info,
        Routes.About,
    ),
)