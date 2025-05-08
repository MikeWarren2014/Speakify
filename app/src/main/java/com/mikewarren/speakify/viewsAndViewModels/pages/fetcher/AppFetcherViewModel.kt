package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

class AppFetcherViewModel: BaseFetcherViewModel() {
    override fun getDataName(): String {
        return "apps that can notify you"
    }
}