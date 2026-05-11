package com.mikewarren.speakify.data

import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central, thread-safe store for the current telephony state.
 * This allows various components (Receivers, Services, Audio Managers) to 
 * stay in sync without redundant system calls or static flags.
 */
@Singleton
class PhoneStateStore @Inject constructor() {
    private val _currentCallState = MutableStateFlow(TelephonyManager.EXTRA_STATE_IDLE)
    
    /**
     * Observable flow of the phone state. 
     * Values are one of: TelephonyManager.EXTRA_STATE_IDLE, 
     * TelephonyManager.EXTRA_STATE_RINGING, or TelephonyManager.EXTRA_STATE_OFFHOOK.
     */
    val currentCallState = _currentCallState.asStateFlow()

    fun updateState(state: String?) {
        state?.let {
            _currentCallState.value = it
        }
    }

    fun isRinging(): Boolean = _currentCallState.value == TelephonyManager.EXTRA_STATE_RINGING
    fun isOffHook(): Boolean = _currentCallState.value == TelephonyManager.EXTRA_STATE_OFFHOOK
    fun isIdle(): Boolean = _currentCallState.value == TelephonyManager.EXTRA_STATE_IDLE
}
