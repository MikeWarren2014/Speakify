package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

class ContactFetcherViewModel: BaseFetcherViewModel() {
    override fun getDataNameText(): UiText {
        return UiText.StringResource(R.string.contacts_data_name)
    }
}