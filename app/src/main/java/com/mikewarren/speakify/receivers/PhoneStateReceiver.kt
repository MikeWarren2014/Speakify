package com.mikewarren.speakify.receivers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.clerk.api.Clerk
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.services.PhoneCallAnnouncer
import com.mikewarren.speakify.utils.PackageHelper
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.ITaggable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneStateReceiver : BroadcastReceiver(), ITaggable {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userAppsDao: UserAppsDao

    @Inject
    lateinit var appSettingsDao: AppSettingsDao

    @Inject
    lateinit var notificationSourcesDao: NotificationSourcesDao

    @Inject
    lateinit var announcer: PhoneCallAnnouncer

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    companion object {
        private var lastProcessedState: String? = null
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        if (Clerk.user == null) {
            Log.w("PhoneStateReceiver", "User is not logged in. Skipping call processing.")
            return
        }

        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        if (incomingNumber.isNullOrEmpty()) {
            Log.d(TAG, "Incoming number is null or empty. Cannot announce.")
            return
        }

        if (state == lastProcessedState) {
            Log.d(TAG, "State is the same as last time. Skipping.")
            return
        }
        lastProcessedState = state

        if (state != TelephonyManager.EXTRA_STATE_RINGING) {
            Log.d(TAG, "Phone state is '$state'. Stopping any active announcement.")
            applicationScope.launch {
                announcer.stopAnnouncing()
            }
            return
        }

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val liveState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tm.callStateForSubscription else tm.callState

                if (liveState != TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(TAG, "State was RINGING, but is now '$liveState'. Stale event, aborting.")
                    announcer.stopAnnouncing()
                    return@launch
                }

                Log.d(TAG, "Phone is ringing. Processing announcement.")
                val importantApps = userAppsDao.getAll().map { it.packageName }
                if (importantApps.none { PackageNames.PhoneAppList.contains(it) }) {
                    Log.d(TAG, "No configured phone app found in user's important apps.")
                    return@launch
                }

                val defaultVoice = settingsRepository.selectedTTSVoice.first() ?: Constants.DefaultTTSVoice
                val packageName = PackageHelper.GetDefaultDialerApp(context)
                var appSettingsModel = appSettingsDao.getByPackageName(packageName!!)?.let { AppSettingsModel.FromDbModel(it) }
                if (appSettingsModel == null) {
                    appSettingsModel = AppSettingsModel(packageName, defaultVoice)
                }

                if (appSettingsModel.notificationSources.isNotEmpty() && !SearchUtils.IsInPhoneNumberList(appSettingsModel.notificationSources, incomingNumber)) {
                    Log.d(TAG, "Number $incomingNumber is not in the allowed list.")
                    return@launch
                }

                announcer.announceCall(incomingNumber)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
