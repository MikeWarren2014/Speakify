package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CustomSwitch(checked: Boolean,
                 modifier: Modifier = Modifier,
                 onCheckedChange: (Boolean) -> Unit,
                 enabled: Boolean = true,
             ) {
    Switch(checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        thumbContent = {
            val imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    )
}