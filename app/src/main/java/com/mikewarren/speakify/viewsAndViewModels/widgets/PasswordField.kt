package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun PasswordField(placeholderText: String? = null,
                  value: String,
                  onDone: () -> Unit,
                  onValueChange: (String) -> Unit,
                  isError: Boolean = false,
                  supportingText: String? = null,
                  focusManager: FocusManager,
                  ) {
    var passwordVisible by remember { mutableStateOf(false) }

    var placeholder: @Composable (() -> Unit)? = null
    if (placeholderText != null && placeholderText.isNotEmpty()) {
        placeholder =  { Text(placeholderText) }
    }

    var visualTransformation : VisualTransformation = PasswordVisualTransformation()
    if (passwordVisible) {
        visualTransformation = VisualTransformation.None
    }

    var supportingTextComposable: @Composable (() -> Unit)? = null
    if (supportingText != null && supportingText.isNotEmpty()) {
        supportingTextComposable = { Text(supportingText) }
    }


    TextField(
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingTextComposable,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus() // Hide the keyboard
                onDone()
            }
        ),
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff

            // Please provide localized description for accessibility services
            val description = if (passwordVisible) "Hide password" else "Show password"

            IconButton(onClick = {passwordVisible = !passwordVisible}) {
                Icon(imageVector  = image, contentDescription = description)
            }
        }
    )
}
