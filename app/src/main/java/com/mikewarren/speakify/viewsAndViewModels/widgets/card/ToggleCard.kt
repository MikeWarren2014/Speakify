package com.mikewarren.speakify.viewsAndViewModels.widgets.card

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToggleCard(title: String,
               description: String,
               isChecked: Boolean,
               isDisabled: Boolean = false,
               onCheckedChange: (Boolean) -> Unit) {
    ItemCard(title, description) {
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = !isDisabled,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}