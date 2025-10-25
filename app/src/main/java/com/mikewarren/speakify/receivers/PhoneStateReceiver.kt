package com.mikewarren.speakify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.services.PhoneCallAnnouncer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhoneStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var announcer: PhoneCallAnnouncer

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        TODO("we need to get the AppSettingsModel , also we should NOT be running this if they DID NOT have Phone as important app!")


        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.d("PhoneStateReceiver", "Phone is RINGING. Incoming number: $incomingNumber")
                if (!incomingNumber.isNullOrEmpty()) {
                    TODO("we should check if the phone number is in our app settings model's notificationSources, before tryna speakify")
                    // HERE IS WHERE YOU TRIGGER THE SPEAKIFY LOGIC
                    // We need a way to pass this info to your service or a TTS handler.
                    // For now, let's just log it to confirm it works.
                    // In the next step, we'll build a proper handler.
                    announcer.announceCall(incomingNumber)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d("PhoneStateReceiver", "Phone is OFFHOOK (call answered or dialing out).")
                // Here you would stop any TTS announcement.
                announcer.stopAnnouncing()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d("PhoneStateReceiver", "Phone is IDLE (call ended or hung up).")
                // Here you would also stop any TTS announcement.
                announcer.stopAnnouncing()
            }
        }
    }
}
