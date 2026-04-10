package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.services.TTSManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleCalendarNotificationStrategyTest {
    @Test
    fun extractRelativeTimeTest() {
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        val notification = mockk<Notification>(relaxed = true)
        val extras = Bundle()

        every { sbn.notification } returns notification
        notification.extras = extras

        val context = mockk<Context>(relaxed = true)
        val ttsManager = mockk<TTSManager>(relaxed = true)

        val strategy = GoogleCalendarNotificationStrategy(sbn, null, context, ttsManager)

        val text = "\u200E\u202A1:30 – 1:45 AM\u202C\u200E"
        val result = strategy.extractRelativeTime(text)

        assertNotNull("Relative time should not be null for text: $text", result)
        assertNotNull("Relative time should not be null for text: $text", strategy.parseNotificationText(text))

        val text2 = "\u200E\u202A2 – 10 AM\u202C\u200E"
        assertNotNull("Relative time should not be null for text: $text2", strategy.extractRelativeTime(text2))
        assertNotNull("Relative time should not be null for text: $text2", strategy.parseNotificationText(text2))
    }
}