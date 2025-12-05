package com.mikewarren.speakify.viewsAndViewModels.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(val title: String, val icon: ImageVector, val route: String)

object Titles {
    const val About = "About"
    const val ImportantApps = "Important Apps"
    const val Settings = "Settings"
    const val Support = "Support & Feedback"
}

val navItems = listOf(
    NavigationItem(Titles.ImportantApps, Icons.AutoMirrored.Filled.List, "important_apps"),
    NavigationItem(Titles.Settings, Icons.Filled.Settings, "settings"),
    NavigationItem(
        title = Titles.Support,
        icon = Icons.Default.Email, // or Icons.Default.Feedback
        route = "support"
    ),
    NavigationItem(Titles.About, Icons.Filled.Info, "about"),
)