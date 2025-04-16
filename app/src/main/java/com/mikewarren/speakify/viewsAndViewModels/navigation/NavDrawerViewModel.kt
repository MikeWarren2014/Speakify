package com.mikewarren.speakify.viewsAndViewModels.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class NavDrawerViewModel : ViewModel() {

    fun navigate(navController: NavController, route: String) {
        navController.navigate(route, {
            popUpTo(navController.graph.startDestinationId)
            launchSingleTop = true
        })
    }
}