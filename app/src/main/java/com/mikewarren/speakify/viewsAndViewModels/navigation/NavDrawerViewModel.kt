package com.mikewarren.speakify.viewsAndViewModels.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class NavDrawerViewModel : ViewModel() {

    lateinit var navController: NavController

    fun goBack() {
        navController.popBackStack()
    }

    fun navigate(route: String) {
        navController.navigate(route)
    }

    fun navigateAndPopUpTo(route: String) {
        navController.navigate(route, {
            popUpTo(navController.graph.startDestinationId)
            launchSingleTop = true
        })
    }

    fun clearNavigationHistory() {
        navController.popBackStack(navController.graph.startDestinationId, false)

    }
}