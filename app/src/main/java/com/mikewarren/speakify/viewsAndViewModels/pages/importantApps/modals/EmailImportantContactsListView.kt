package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@NotificationListComponent(appTypes = ["email"])
@Composable
fun EmailImportantContactsListView(viewModel: EmailImportantContactsListViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchRecentContacts()
    }

    AutoCompletableNotificationSourceListView(viewModel)
}
