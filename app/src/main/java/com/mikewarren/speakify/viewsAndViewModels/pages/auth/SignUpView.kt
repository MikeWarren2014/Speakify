package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.constants.DocumentURLs
import com.mikewarren.speakify.data.uiStates.SignUpUiState
import com.mikewarren.speakify.viewsAndViewModels.widgets.PasswordField

@Composable
fun SignUpView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {

    val state by viewModel.uiState.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text(stringResource(R.string.sign_up), 
            style = MaterialTheme.typography.headlineMedium)

        SignUpScreenView(viewModel, state, onDone)
    }
}

@Composable
fun SignUpScreenView(viewModel: SignUpViewModel = viewModel(), state: SignUpUiState, onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
    if (state is SignUpUiState.NeedsVerification) {
        VerificationView(viewModel, onDone)
        return
    }
    SignUpFormView(viewModel, onDone)
}

@Composable
fun VerificationView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
    EmailVerificationView(stringResource(R.string.sign_up_verification_message).replace("\n", "\n\n"),
        onDone = { code -> viewModel.checkVerification(code, onDone) })
}

@Composable
fun SignUpFormView(viewModel: SignUpViewModel = viewModel(), onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    TextField(
        value = viewModel.model.firstName,
        onValueChange = { firstName: String ->
            viewModel.onModelChange(
                viewModel.model.copy(
                    firstName = firstName
                )
            )
        },
        placeholder = { Text(stringResource(R.string.first_name_label)) },
        isError = viewModel.errorsDict.containsKey("firstName"),
        supportingText = {
            viewModel.errorsDict["firstName"]?.let { Text(it) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    TextField(value = viewModel.model.lastName,
        onValueChange = { lastName: String -> viewModel.onModelChange(viewModel.model.copy(
            lastName = lastName
        ))},
        placeholder = { Text(stringResource(R.string.last_name_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )


    TextField(value = viewModel.model.email,
        onValueChange = { email: String -> viewModel.onModelChange(viewModel.model.copy(
            email = email
         )) },
        placeholder = { Text(stringResource(R.string.email_label)) },
        isError = viewModel.errorsDict.containsKey("email"),
        supportingText = {
            viewModel.errorsDict["email"]?.let { Text(it) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    PasswordField(
        value = viewModel.model.password,
        placeholderText = stringResource(R.string.password_label),
        onValueChange = { password : String -> viewModel.onModelChange(viewModel.model.copy(
            password = password
        )) },
        isError = viewModel.errorsDict.containsKey("password"),
        supportingText = viewModel.errorsDict["password"],
        onDone = {
            viewModel.signUp(onDone)
        },
        focusManager = focusManager,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = viewModel.model.agreedToTerms,
            onCheckedChange = { viewModel.onModelChange(viewModel.model.copy(agreedToTerms = it)) }
        )
        LegalText(
            onTermsClick = { uriHandler.openUri(DocumentURLs.TermsOfService) },
            onPrivacyClick = { uriHandler.openUri(DocumentURLs.PrivacyPolicy) }
        )
    }
    if (viewModel.errorsDict.containsKey("agreedToTerms")) {
        Text(
            text = viewModel.errorsDict["agreedToTerms"]!!,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
    Button(onClick = { viewModel.signUp( onDone) }) { Text(stringResource(R.string.sign_up)) }

}


@Composable
private fun LegalText(onTermsClick: () -> Unit, onPrivacyClick: () -> Unit) {
    val termsOfService = stringResource(R.string.legal_terms_of_service)
    val privacyPolicy = stringResource(R.string.legal_privacy_policy)

    val annotatedString = buildAnnotatedString {
        append(stringResource(R.string.legal_agree_prefix))
        pushStringAnnotation(tag = "TERMS", annotation = termsOfService)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(termsOfService)
        }
        pop()
        append(stringResource(R.string.legal_and))
        pushStringAnnotation(tag = "PRIVACY", annotation = privacyPolicy)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(privacyPolicy)
        }
        pop()
        append(stringResource(R.string.legal_dot))
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                .firstOrNull()?.let { onTermsClick() }
            annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                .firstOrNull()?.let { onPrivacyClick() }
        }
    )
}
