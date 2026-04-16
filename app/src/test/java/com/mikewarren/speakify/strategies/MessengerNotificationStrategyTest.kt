package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.services.TTSManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessengerNotificationStrategyTest {

    @Before
    fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
    }

    @Test
    fun testIsFromSentMessage_YourPhotoWasSent() {
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        val notification = mockk<Notification>(relaxed = true)
        
        // Use a SpannableString-like behavior by putting a CharSequence in the bundle
        val extras = Bundle()
        extras.putCharSequence(Notification.EXTRA_TITLE, "Your photo was sent")
        extras.putCharSequence(Notification.EXTRA_TEXT, "")

        every { sbn.notification } returns notification
        notification.extras = extras

        assertEquals("", NotificationExtractionUtils.ExtractText(sbn))

        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        every { context.resources } returns resources
        every { context.getString(R.string.action_reply) } returns "reply"
        every { resources.getStringArray(R.array.message_sent_titles) } returns arrayOf("is sending", "was sent")
        every { resources.getStringArray(R.array.action_mark_read) } returns arrayOf("mark as read", "mark read")
        every { resources.getStringArray(R.array.action_outgoing_call) } returns arrayOf("end call", "hang up", "speaker")
        every { resources.getStringArray(R.array.action_incoming_call_list) } returns arrayOf("answer", "decline")
        every { resources.getStringArray(R.array.messenger_message_request_keywords) } returns arrayOf("Message Request", "wants to connect", "requested to message")

        val ttsManager = mockk<TTSManager>(relaxed = true)

        val strategy = MessengerNotificationStrategy(sbn, null, context, ttsManager)

        // Verify notification type is OutgoingMessage
        assertEquals(MessengerNotificationStrategy.MessengerNotificationTypes.OutgoingMessage, strategy.getNotificationType())
        
        // Verify shouldSpeakify is false for outgoing messages
        assertFalse("Should not speakify outgoing messages", strategy.shouldSpeakify())
    }

    @Test
    fun testGetNotificationType_YourPhotoWasSent() {
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        val notification = mockk<Notification>(relaxed = true)

        val extras = Bundle()
        extras.putCharSequence(Notification.EXTRA_TITLE, "Your photo was sent")
        extras.putCharSequence(Notification.EXTRA_TEXT, "")

        every { sbn.notification } returns notification
        notification.extras = extras

        assertEquals("", NotificationExtractionUtils.ExtractText(sbn))

        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        every { context.resources } returns resources
        every { context.getString(R.string.action_reply) } returns "reply"
        every { resources.getStringArray(R.array.message_sent_titles) } returns arrayOf("is sending", "was sent")
        every { resources.getStringArray(R.array.action_mark_read) } returns arrayOf("mark as read", "mark read")
        every { resources.getStringArray(R.array.action_outgoing_call) } returns arrayOf("end call", "hang up", "speaker")
        every { resources.getStringArray(R.array.action_incoming_call_list) } returns arrayOf("answer", "decline")
        every { resources.getStringArray(R.array.messenger_message_request_keywords) } returns arrayOf("Message Request", "wants to connect", "requested to message")

        val ttsManager = mockk<TTSManager>(relaxed = true)

        val strategy = MessengerNotificationStrategy(sbn, null, context, ttsManager)

        // Verify notification type is OutgoingMessage
        assertEquals(MessengerNotificationStrategy.MessengerNotificationTypes.OutgoingMessage, strategy.getNotificationType())
        assertEquals(false, strategy.shouldSpeakify())
    }
}
