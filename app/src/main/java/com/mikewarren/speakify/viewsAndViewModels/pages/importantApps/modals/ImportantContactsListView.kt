package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ImportantContactsListView(viewModel: BaseImportantContactsListViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchContacts()
    }

    NotificationSourceListView(viewModel)
}