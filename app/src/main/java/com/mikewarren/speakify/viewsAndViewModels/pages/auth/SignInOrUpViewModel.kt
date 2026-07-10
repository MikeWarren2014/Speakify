package com.mikewarren.speakify.viewsAndViewModels.pages.auth

import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.data.AuthMessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInOrUpViewModel @Inject constructor(
    val authMessageRepository: AuthMessageRepository
) : ViewModel()
