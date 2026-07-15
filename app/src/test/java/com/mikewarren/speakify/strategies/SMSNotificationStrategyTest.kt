package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SMSNotificationStrategyTest {

    @Before
    fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        mockkObject(NotificationExtractionUtils)
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationExtractionUtils)
    }

    @Test
    fun testTextToSpeakify_ContainsUrl_ReplacesWithLink() {
        mockkStatic(NotificationCompat.MessagingStyle::class)

        val sbn = mockk<StatusBarNotification>(relaxed = true)
        val notification = Notification()
        val extras = Bundle()
        notification.extras = extras
        
        val person = Person.Builder().setName("Mike").build()
        val messagingStyle = NotificationCompat.MessagingStyle(person)
        messagingStyle.addMessage("Check this out: https://www.google.com", System.currentTimeMillis(), person)
        
        every { sbn.notification } returns notification
        
        val markReadAction = Notification.Action.Builder(0, "mark read", null).build()
        notification.actions = arrayOf(markReadAction)

        every { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) } returns messagingStyle
        every { NotificationExtractionUtils.ExtractText(sbn) } returns "Check this out: https://www.google.com"
        every { NotificationExtractionUtils.ExtractContactModel(any(), any(), any(), any()) } returns ContactModel("Mike", "1234567890")

        val context = createStubContext()
        every { context.getString(R.string.sms_text_out_loud, "Mike", "Check this out: link") } returns "Mike says: Check this out: link"

        val appSettingsModel = mockk<AppSettingsModel>(relaxed = true)
        every { appSettingsModel.getBooleanSetting(any(), any()) } returns true

        val ttsManager = mockk<TTSManager>(relaxed = true)
        
        val strategy = SMSNotificationStrategy(sbn, appSettingsModel, context, ttsManager)

        val result = strategy.textToSpeakify()
        assertTrue("Result should contain 'link' instead of URL: $result", result.contains("link"))
        assertFalse("Result should not contain the actual URL: $result", result.contains("https://www.google.com"))
        assertEquals("Mike says: Check this out: link", result)
    }

    fun createStubContext(): Context {
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        every { context.resources } returns resources

        every { context.getString(R.string.contact_unknown) } returns "Unknown"
        every { context.getString(R.string.action_reply) } returns "reply"
        every { resources.getStringArray(R.array.action_mark_read) } returns arrayOf("mark as read", "mark read")

        return context
    }
}
