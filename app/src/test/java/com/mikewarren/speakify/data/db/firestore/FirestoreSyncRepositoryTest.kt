package com.mikewarren.speakify.data.db.firestore

import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.BaseDbTest
import com.mikewarren.speakify.data.MessengerContactsRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.SchedulingRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.TrialRepository
import com.mikewarren.speakify.data.TrialStatus
import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.delegates.AnonymousUserFirebaseAuthDelegate
import com.mikewarren.speakify.data.delegates.IFirebaseAuth
import com.mikewarren.speakify.data.delegates.ISettingsTest
import com.mikewarren.speakify.data.delegates.SettingsTestDelegate
import com.mikewarren.speakify.data.models.OnboardingModel
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.models.scheduling.SchedulingModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestoreSyncRepositoryTest : BaseDbTest(),
    IFirebaseAuth by AnonymousUserFirebaseAuthDelegate(),
    ISettingsTest by SettingsTestDelegate() {

    private lateinit var appsRepository: AppsRepository
    private lateinit var schedulingRepository: SchedulingRepository
    private lateinit var messengerContactsRepository: MessengerContactsRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var trialRepository: TrialRepository
    private lateinit var uploadRepository: UploadRepository
    private lateinit var downloadRepository: DownloadRepository

    private lateinit var syncRepository: FirestoreSyncRepository

    private val importantAppsFlow = MutableStateFlow(emptyList<UserAppModel>())
    private val schedulingFlow = MutableStateFlow(SchedulingModel())
    private val trialModelFlow = MutableStateFlow(TrialModel())
    private val onboardingModelFlow = MutableStateFlow(OnboardingModel())
    private val messengerContactsFlow = MutableStateFlow(emptyList<RecentMessengerContactModel>())

    @Before
    override fun setUp() {
        super.setUp()
        setUpFirebaseAuth()
        setUpSettings(context)

        appsRepository = mockk(relaxed = true)
        schedulingRepository = mockk(relaxed = true)
        messengerContactsRepository = mockk(relaxed = true)
        onboardingRepository = mockk(relaxed = true)
        trialRepository = mockk(relaxed = true)
        uploadRepository = mockk(relaxed = true)
        downloadRepository = mockk(relaxed = true)

        every { appsRepository.importantApps } returns importantAppsFlow
        every { schedulingRepository.scheduling } returns schedulingFlow
        every { messengerContactsRepository.recentContacts } returns messengerContactsFlow
        every { onboardingRepository.onboardingModel } returns onboardingModelFlow
        every { trialRepository.trialModelFlow } returns trialModelFlow

        coEvery { uploadRepository.doAllFirestoreTransactions() } returns Result.success(Unit)

        // Initialize the repository. This starts the collector.
        syncRepository = FirestoreSyncRepository(
            settingsRepository,
            appsRepository,
            schedulingRepository,
            messengerContactsRepository,
            onboardingRepository,
            trialRepository,
            uploadRepository,
            downloadRepository
        )

        // Prime the flows to clear the drop(1)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `changes trigger upload for anonymous users after debounce`() = runTest {
        // GIVEN: User is anonymous with active trial
        trialModelFlow.value = TrialModel(uid = "anon", status = TrialStatus.Active(7))
        runCurrent()

        // WHEN: Important apps change
        importantAppsFlow.value = listOf(mockk(relaxed = true))
        
        // Wait for debounce (2000ms)
        advanceTimeBy(2100.milliseconds)
        testDispatcher.scheduler.runCurrent()

        // THEN: Upload should be triggered
        unmockkStatic(Dispatchers::class)
        coVerify(exactly = 1) { uploadRepository.doAllFirestoreTransactions() }
    }

    @Test
    fun `changes do NOT trigger upload if trial is expired`() = runTest {
        // GIVEN: User is anonymous but trial expired
        trialModelFlow.value = TrialModel(uid = "anon", status = TrialStatus.Expired)
        runCurrent()

        // WHEN: Changes occur
        importantAppsFlow.value = listOf(mockk(relaxed = true))
        advanceTimeBy(2100.milliseconds)
        testDispatcher.scheduler.runCurrent()

        // THEN: No upload
        unmockkStatic(Dispatchers::class)
        coVerify(exactly = 0) { uploadRepository.doAllFirestoreTransactions() }
    }
}
