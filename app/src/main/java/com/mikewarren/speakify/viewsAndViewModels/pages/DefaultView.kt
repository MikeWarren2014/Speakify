package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DefaultView(title: String, paddingValues: PaddingValues) {
    return Text(text = "$title Content", modifier = Modifier.padding(paddingValues))
}
