package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FetcherView(viewModel: BaseFetcherViewModel) {
    if (viewModel.isLoading)
        LoadingScreen(viewModel);
}

@Composable
private fun LoadingScreen(viewModel: BaseFetcherViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(text = "Fetching ${viewModel.getDataName()}...", modifier = Modifier.padding(top = 8.dp))
        }
    }
}