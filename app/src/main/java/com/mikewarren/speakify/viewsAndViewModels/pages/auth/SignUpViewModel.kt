package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.prepareVerification
import com.mikewarren.speakify.data.SignUpUiState
import com.mikewarren.speakify.data.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.SignedOut)
    val uiState = _uiState.asStateFlow()

    var model by mutableStateOf(UserModel())
        private set

    var errorsDict by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    fun onModelChange(updatedModel: UserModel) {
        model = updatedModel
        validate()
    }

    private fun validate(): Boolean {
        val newErrors = mutableMapOf<String, String>()
        if (model.firstName.isBlank()) {
            newErrors["firstName"] = "First Name is required."
        }
        if (model.email.isBlank()) {
            newErrors["email"] = "Email is required."
        } else if (!Patterns.EMAIL_ADDRESS.matcher(model.email).matches()) {
            newErrors["email"] = "Invalid email format."
        }
        if (model.password.isBlank()) {
            newErrors["password"] = "Password is required."
        } else if (model.password.length < 8) {
            newErrors["password"] = "Password must be at least 8 characters."
        }
        errorsDict = newErrors
        return newErrors.isEmpty()
    }


    fun signUp(onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
        if (!validate()) {
            onDone(false, SignUpUiState.SignedOut)
            return
        }
        viewModelScope.launch {
            SignUp.create(SignUp.CreateParams.Standard(emailAddress = model.email,
                password = model.password,
                firstName = model.firstName,
                lastName = model.lastName,
                ))
                .onSuccess {
                    if (it.status == SignUp.Status.COMPLETE) {
                        _uiState.value = SignUpUiState.Success
                        onDone(true, SignUpUiState.Success)
                    } else {
                        _uiState.value = SignUpUiState.NeedsVerification
                        it.prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailCode())
                        onDone(true, SignUpUiState.NeedsVerification)
                    }
                }
                .onFailure {
                    val newErrorsDict = errorsDict.toMutableMap()
                    // See https://clerk.com/docs/guides/development/custom-flows/error-handling
                    // for more info on error handling
                    it.error?.errors?.forEach { error ->
                        // TODO: concatenate each error message onto its respective map entry in errors
                        arrayOf("email", "password")
                            .forEach { key ->
                                if (!error.message.lowercase().contains(key)) return@forEach
                                if (errorsDict[key].isNullOrBlank()) {
                                    newErrorsDict[key] = error.message
                                    return@forEach
                                }
                                newErrorsDict[key] = "${errorsDict[key]}\n${error.message}"
                            }
                    }
                    errorsDict = newErrorsDict

                    Log.e("SignUpViewModel", it.longErrorMessageOrNull, it.throwable)
                    onDone(false, SignUpUiState.SignedOut)
                }
        }
    }

    fun checkVerification(code: String, onDone: (success: Boolean, signUpUiState: SignUpUiState) -> Unit) {
        val inProgressSignUp = Clerk.signUp ?: return
        viewModelScope.launch {
            inProgressSignUp.attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
                .onSuccess {
                    _uiState.value = SignUpUiState.Success
                    onDone(true, SignUpUiState.Success)
                }
                .onFailure {
                    // See https://clerk.com/docs/guides/development/custom-flows/error-handling
                    // for more info on error handling
                    Log.e("SignUpViewModel", it.longErrorMessageOrNull, it.throwable)
                    onDone(false, SignUpUiState.NeedsVerification)
                }
        }
    }


}