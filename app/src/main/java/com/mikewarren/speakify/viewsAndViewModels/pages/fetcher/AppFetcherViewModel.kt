package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

class AppFetcherViewModel: BaseFetcherViewModel() {
    override fun getDataNameText(): UiText {
        return UiText.StringResource(R.string.apps_data_name)

    }
}