package com.mikewarren.speakify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.mikewarren.speakify.ApplicationScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.NotificationSourcesDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.services.PhoneCallAnnouncer
import com.mikewarren.speakify.utils.PackageHelper
import com.mikewarren.speakify.utils.SearchUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneStateReceiver : BroadcastReceiver() {
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

    // Inject the application-level CoroutineScope
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private lateinit var defaultVoice: String;


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }
        applicationScope.launch {
            settingsRepository.selectedTTSVoice.collect { selectedTTSVoice ->
                // Logic to process app settings and maybe update TTS instances
                defaultVoice = selectedTTSVoice ?: Constants.DefaultTTSVoice

                // This collector will run continuously in the background.
                Log.d("PhoneStateReceiver", "Ready to listen for calls!")
                val importantApps = userAppsDao.getAll()

                // TODO: we should consider when the user has designated some third-party App as a Phone app
                if (!SearchUtils.HasAnyOverlap(Constants.PhoneAppPackageNames, importantApps.map { it.packageName }))
                    return@collect

                process(context, intent)
            }
        }
    }

    suspend fun process(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        // TODO: get the dialer package. For now, let's assume default
        val packageName = PackageHelper.GetDefaultDialerApp(context)

        var appSettingsModel = AppSettingsModel.FromDbModel(appSettingsDao.getByPackageName(packageName!!))
        if (appSettingsModel == null) {
            appSettingsModel = AppSettingsModel(packageName, defaultVoice)
        }

        Log.d("PhoneStateReceiver", "State: $state, incoming number: $incomingNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.d("PhoneStateReceiver", "Phone is RINGING. Incoming number: $incomingNumber")
                if (incomingNumber.isNullOrEmpty())
                    return
                if ((appSettingsModel.notificationSources.isNotEmpty()) && (!SearchUtils.IsInPhoneNumberList(appSettingsModel.notificationSources, incomingNumber)))
                    return
                announcer.announceCall(incomingNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d("PhoneStateReceiver", "Phone is OFFHOOK (call answered or dialing out).")
                announcer.stopAnnouncing()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d("PhoneStateReceiver", "Phone is IDLE (call ended or hung up).")
                announcer.stopAnnouncing()
            }
        }
        
    }

}
