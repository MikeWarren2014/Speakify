package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _permissionStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionStates: StateFlow<Map<String, Boolean>> = _permissionStates.asStateFlow()

    fun checkPermissions(permissions: Array<String>) {
        viewModelScope.launch {
            _permissionStates.update {
                permissions.associateWith { permission ->
                    PermissionUtils.isPermissionGranted(context, permission)
                }
            }
        }
    }


    fun grantPermission(permission: String) {
        updatePermissionState(permission, true)
    }

    fun updatePermissionState(permission: String, isGranted: Boolean) {
        _permissionStates.update { it + (permission to isGranted) }

    }
}