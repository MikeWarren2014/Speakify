package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@NotificationListComponent(listName = "FacebookMessengerAppList")
@Composable
fun MessengerImportantContactsListView(viewModel: MessengerImportantContactsListViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchRecentContacts()
    }

    AutoCompletableNotificationSourceListView(viewModel)
}
