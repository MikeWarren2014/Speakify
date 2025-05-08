package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

class ContactFetcherViewModel: BaseFetcherViewModel() {
    override fun getDataName(): String {
        return "contacts"
    }
}