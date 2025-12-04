package com.mikewarren.speakify.viewsAndViewModels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mikewarren.speakify.viewsAndViewModels.navigation.NavigationItem
import com.mikewarren.speakify.viewsAndViewModels.navigation.Titles
import com.mikewarren.speakify.viewsAndViewModels.navigation.navItems
import com.mikewarren.speakify.viewsAndViewModels.pages.AboutView
import com.mikewarren.speakify.viewsAndViewModels.pages.DefaultView
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsView
import com.mikewarren.speakify.viewsAndViewModels.pages.SupportView
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.ImportantAppsView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppView(navController: NavHostController = rememberNavController(),
            viewModel: AppViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.width(12.dp))
                navItems.forEach { item : NavigationItem ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            // Direct drawer closing within the Composable's coroutineScope:
                            scope.launch {
                                drawerState.close()
                                viewModel.childNavDrawerViewModel.navigate(navController, item.route) // Simpler ViewModel call
                            }
                        }
                    )
                }
            }
        }
    ) {
        NavHost(navController, startDestination = "important_apps") {
            navItems.forEach { navItem : NavigationItem ->
                composable(navItem.route) { ScreenContent(viewModel, navItem.title, drawerState, scope) }
            }
        }
    }
}

// Reusable screen content

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContent(viewModel: AppViewModel, title: String, drawerState: DrawerState, scope: CoroutineScope) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    , content = { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues),
            content = {
                ChildView(title, paddingValues)
            })
    })
}

@Composable
fun ChildView(title: String, paddingValues: PaddingValues) {
    if (title == Titles.ImportantApps)
        return ImportantAppsView()

    if (title == Titles.About)
        return AboutView()

    if (title == Titles.Support)
        return SupportView()

    if (title == Titles.Settings)
        return SettingsView()

    return DefaultView(title, paddingValues)
}

