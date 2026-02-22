package com.mikewarren.speakify.viewsAndViewModels

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mikewarren.speakify.activities.LoginActivity
import com.mikewarren.speakify.data.constants.ActionConstants
import com.mikewarren.speakify.viewsAndViewModels.navigation.NavigationItem
import com.mikewarren.speakify.viewsAndViewModels.navigation.Routes
import com.mikewarren.speakify.viewsAndViewModels.navigation.navItems
import com.mikewarren.speakify.viewsAndViewModels.pages.AboutView
import com.mikewarren.speakify.viewsAndViewModels.pages.DefaultView
import com.mikewarren.speakify.viewsAndViewModels.pages.LegalView
import com.mikewarren.speakify.viewsAndViewModels.pages.SettingsView
import com.mikewarren.speakify.viewsAndViewModels.pages.SupportView
import com.mikewarren.speakify.viewsAndViewModels.pages.auth.accountDeletion.AccountDeletionView
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.ImportantAppsView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppView(navController: NavHostController = rememberNavController(),
            viewModel: AppViewModel = viewModel()) {
    viewModel.childNavDrawerViewModel.navController = navController

    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity

    LaunchedEffect(Unit) {
        val postLoginAction = activity.intent.getStringExtra(ActionConstants.PostLoginActionKey)
        if (postLoginAction == ActionConstants.ActionDeleteAccount) {
            viewModel.childNavDrawerViewModel.navigate(Routes.AccountDeletion)
            activity.intent.removeExtra(ActionConstants.PostLoginActionKey)
        }
    }

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
                    val title = stringResource(item.titleResId)
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                viewModel.childNavDrawerViewModel.navigateAndPopUpTo(item.route)
                            }
                        }
                    )
                }
            }
        }
    ) {
        NavHost(navController, startDestination = Routes.ImportantApps) {
            navItems.forEach { navItem : NavigationItem ->
                composable(navItem.route) { 
                    ScreenContent(viewModel, navItem.titleResId, navItem.route, drawerState, scope) 
                }
            }

            composable(Routes.AccountDeletion) {
                val context = LocalContext.current

                AccountDeletionView(
                    onCancel = {
                        viewModel.childNavDrawerViewModel.goBack()
                    },
                    onDeleted = {
                        viewModel.childNavDrawerViewModel.clearNavigationHistory()
                        context.startActivity(Intent(context, LoginActivity::class.java)
                            .apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContent(viewModel: AppViewModel, titleResId: Int, route: String, drawerState: DrawerState, scope: CoroutineScope) {
    val title = stringResource(titleResId)
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
                ChildView(viewModel, route, paddingValues)
            })
    })
}

@Composable
fun ChildView(viewModel: AppViewModel, route: String, paddingValues: PaddingValues) {
    when (route) {
        Routes.ImportantApps -> ImportantAppsView()
        Routes.About -> AboutView()
        Routes.Support -> SupportView()
        Routes.Settings -> SettingsView({
            viewModel.childNavDrawerViewModel.navigate(Routes.AccountDeletion)
        })
        Routes.Legal -> LegalView()
        else -> DefaultView(route, paddingValues)
    }
}
