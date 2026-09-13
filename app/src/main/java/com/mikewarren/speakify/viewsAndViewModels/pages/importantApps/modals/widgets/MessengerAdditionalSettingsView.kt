package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.Composable
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent

@AdditionalSettingsComponent(listName = "FacebookMessengerAppList")
@Composable
fun MessengerAdditionalSettingsView(viewModel: MessengerAdditionalSettingsViewModel) {
    MessagingAppAdditionalSettingsView(viewModel)
}
