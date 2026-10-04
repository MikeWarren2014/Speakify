package com.mikewarren.speakify.strategies

import android.app.Notification
import android.app.Person
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.DbProviderMocker
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.utils.log.LogUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmailNotificationStrategyTest() : BaseNotificationStrategyTest(),
    DbProviderMocker {

    override val context = createStubContext()
    override lateinit var db: AppDatabase

    val gmailPackageName = "com.google.android.gm"

    @Before
    override fun setUp() {
        super.setUp()
        setUpDatabaseDoubles()

        mockkObject(LogUtils)
        every { LogUtils.LogNonFatalError(any(), any(), any()) } returns Unit
    }

    @After
    override fun tearDown() {
        super.tearDown()
        tearDownDatabaseDoubles()
        unmockkObject(LogUtils)
    }

    @Test
    fun `notificationSources being empty should NOT filter out any emails`() {
        val sbn = createIncomingEmailNotification("Uber Eats", "uber@uber.com", "Get 25% off your next convenience store order")

        val context = createStubContext()
        val appSettingsModel = AppSettingsModel(
            gmailPackageName,
            Constants.DefaultTTSVoice,
        )

        val strategy = EmailNotificationStrategy(sbn, appSettingsModel, context, ttsManager)
        assert(strategy.shouldSpeakify())
    }

    @Test
    fun `incoming emails should have the type NewMessage`() {
        val sbn = createIncomingEmailNotification("Uber Eats", "uber@uber.com", "Get 25% off your next convenience store order")

        val context = createStubContext()
        val appSettingsModel = AppSettingsModel(
            gmailPackageName,
            Constants.DefaultTTSVoice,
        )

        val strategy = EmailNotificationStrategy(sbn, appSettingsModel, context, ttsManager)
        assertEquals(EmailNotificationStrategy.NotificationTypes.NewMessage, strategy.getNotificationType())
    }

    override fun createStubContext(): Context {
        val context = super.createStubContext()
        val resources = context.resources

        every { resources.getStringArray(R.array.action_mark_read) } returns arrayOf("Mark as read", "Mark read")

        return context
    }

    private fun createIncomingEmailNotification(recipientName: String, recipientEmail: String, messageSubject: String): StatusBarNotification =
        createIncomingEmailNotification(recipientName, recipientEmail, messageSubject, "Dummy message text")

    private fun createIncomingEmailNotification(recipientName: String, recipientEmail: String, messageSubject: String, messageText: String): StatusBarNotification {
        val sbn = mockk<StatusBarNotification>(relaxed = true)

        val person = Person.Builder()
            .setUri("mailto:$recipientEmail")
            .build()

        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, recipientName)
            putCharSequence(Notification.EXTRA_TEXT, messageText)
            putParcelableArrayList(Notification.EXTRA_PEOPLE_LIST, arrayListOf(person))
        }

        val notification = Notification().apply {
            this.extras = extras
            this.actions = listOf("Reply", "Mark as read", "Archive")
                .map { Notification.Action.Builder(null, it, null).build() }
                .toTypedArray()

            this.extras.putString(Notification.EXTRA_BIG_TEXT, "$messageSubject\n$messageText")
        }

        every { sbn.notification } returns notification
        every { sbn.packageName } returns gmailPackageName

        return sbn
    }
}
