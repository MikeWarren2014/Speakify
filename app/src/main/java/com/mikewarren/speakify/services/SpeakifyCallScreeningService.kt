package com.mikewarren.speakify.services

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.di.ApplicationScope
import com.mikewarren.speakify.utils.PackageHelper
import com.mikewarren.speakify.utils.SearchUtils
import com.mikewarren.speakify.utils.log.ITaggable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpeakifyCallScreeningService : CallScreeningService(), ITaggable {

    @Inject
    lateinit var gatekeeper: SpeakifyEngineGatekeeper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userAppsDao: UserAppsDao

    @Inject
    lateinit var appSettingsDao: AppSettingsDao

    @Inject
    lateinit var announcer: PhoneCallAnnouncer

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Below API 29, we handle things via BroadcastReceiver
            return
        }
        
        processCall(callDetails)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun processCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        // We only care about incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, response)
            return
        }

        val handle = callDetails.handle
        val incomingNumber = handle?.schemeSpecificPart

        if (incomingNumber.isNullOrEmpty()) {
            Log.d(TAG, "Incoming number is null or empty in CallScreeningService.")
            respondToCall(callDetails, response)
            return
        }

        Log.d(TAG, "Screening incoming call from: $incomingNumber")

        applicationScope.launch {
            try {
                if (!gatekeeper.canSpeakNow()) {
                    return@launch
                }

                val importantApps = userAppsDao.getAll().map { it.packageName }
                if (importantApps.none { PackageNames.PhoneAppList.contains(it) }) {
                    Log.d(TAG, "No configured phone app found in user's important apps.")
                    return@launch
                }

                val defaultDialerPackage = PackageHelper.GetDefaultDialerApp(this@SpeakifyCallScreeningService)
                
                val appSettingsModel = defaultDialerPackage?.let {
                    appSettingsDao.getByPackageName(it)?.let { dbModel -> AppSettingsModel.FromDbModel(dbModel) } 
                }
                
                if (appSettingsModel != null && appSettingsModel.notificationSources.isNotEmpty() &&
                    !SearchUtils.IsInPhoneNumberList(appSettingsModel.notificationSources.map { it.value }, incomingNumber)) {
                    Log.d(TAG, "Number $incomingNumber is not in the allowed list.")
                    return@launch
                }

                announcer.announceCall(incomingNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Error in CallScreeningService: ${e.message}", e)
            }
        }

        // Always allow the call to proceed
        respondToCall(callDetails, response)
    }
}
