package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.services.TTSManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeohNotificationStrategyTest {

    @Test
    fun extractRelativeTimeTest() {
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        val notification = mockk<Notification>(relaxed = true)
        val extras = Bundle()
        
        every { sbn.notification } returns notification
        notification.extras = extras
        
        val context = mockk<Context>(relaxed = true)
        val ttsManager = mockk<TTSManager>(relaxed = true)
        
        val strategy = GeohNotificationStrategy(sbn, null, context, ttsManager)
        
        val text = "You have an upcoming session on Fri, Apr 3 2026, 6:00 PM EDT."
        val result = strategy.extractRelativeTime(text)
        
        assertNotNull("Relative time should not be null for text: $text", result)
    }
}
