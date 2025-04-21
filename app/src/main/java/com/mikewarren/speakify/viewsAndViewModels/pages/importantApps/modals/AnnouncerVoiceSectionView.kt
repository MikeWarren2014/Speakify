package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikewarren.speakify.viewsAndViewModels.widgets.TTSAutoCompletableView

@Composable
fun AnnouncerVoiceSectionView(
    viewModel: AnnouncerVoiceSectionViewModel,
) {
    Row {
        Text(text = "Announcer Voice")
        Spacer(modifier = Modifier.width(16.dp))
        TTSAutoCompletableView(viewModel = viewModel,
            onHandleSelection = { viewModel, selection: String ->
                (viewModel as AnnouncerVoiceSectionViewModel).saveSelectedVoice(selection)
            })
    }
}