package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.content.res.Resources
import android.service.notification.StatusBarNotification
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.NotificationExtractionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before

open class BaseNotificationStrategyTest {

    lateinit var ttsManager: TTSManager

    @Before
    open fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        mockkObject(NotificationExtractionUtils)

        ttsManager = mockk<TTSManager>(relaxed = true)
    }

    @After
    open fun tearDown() {
        unmockkObject(NotificationExtractionUtils)
        unmockkStatic(FirebaseCrashlytics::class)

        unmockkObject(ttsManager)
    }

    open fun createStubContext(): Context {
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        every { context.resources } returns resources

        return context
    }
}