package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.db.firestore.AppUsageStatsRepository
import com.mikewarren.speakify.data.db.firestore.UploadRepository
import com.mikewarren.speakify.data.delegates.AnonymousUserFirebaseAuthDelegate
import com.mikewarren.speakify.data.delegates.IFirebaseAuth
import com.mikewarren.speakify.data.delegates.ISettingsTest
import com.mikewarren.speakify.data.delegates.SettingsTestDelegate
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.utils.DeviceIdProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UploadRepositoryTest: BaseDbTest(),
    ISettingsTest by SettingsTestDelegate(),
    IFirebaseAuth by AnonymousUserFirebaseAuthDelegate() {
    private lateinit var trialRepository: TrialRepository

    private lateinit var appsRepository: AppsRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var messengerContactsRepository: MessengerContactsRepository
    private lateinit var appUsageRepository: AppUsageStatsRepository
    private lateinit var uploadRepository: UploadRepository

    private val trialModelFlow = MutableStateFlow(TrialModel())

    private val newUserAppModel = UserAppModel(
        "com.sample.app",
        "Important App",
    )

    @Before
    override fun setUp() {
        super.setUp()
        setUpSettings(context)
        setUpFirebaseAuth()

        val deviceIdProvider = mockk<DeviceIdProvider>(relaxed = true)
        every { deviceIdProvider.deviceId } returns "fake-device-id"

        trialRepository = mockk(relaxed = true)
        every { trialRepository.trialModelFlow } returns trialModelFlow

        onboardingRepository = OnboardingRepositoryImpl(userSettingsDataStore)
        appsRepository = AppsRepositoryImpl(
            context,
            onboardingRepository,
            appCategoryRepository = mockk<AppCategoryRepository>(relaxed = true),
        )
        messengerContactsRepository = mockk(relaxed = true)
        appUsageRepository = AppUsageStatsRepository()

        uploadRepository = UploadRepository(
            settingsRepository,
            appsRepository,
            messengerContactsRepository,
            onboardingRepository,
            appUsageRepository
        )

    }

    @Test
    fun `new users adding documents should create record on the AppUsageRepository`() = runTest {
        // GIVEN: user has active trial
        trialModelFlow.value = TrialModel(
            uid = "anonymous_user",
            status = TrialStatus.Active(7),
            startTimestamp = System.currentTimeMillis(),
        )

        // WHEN: user adds an important app
        db.userAppsDao()
            .insert(newUserAppModel)
        uploadRepository.doAllFirestoreTransactions()


        // Wait for debounce (2000ms) and sync execution
        advanceUntilIdle()

        // THEN: there should exist a document in the app usage repository for that important app
        val appUsageDocPath = "app_usage_stats/${newUserAppModel.packageName}"

        if (!fakeFirestore.hasDocument(appUsageDocPath)) {
            fakeFirestore.dump()
        }

        assert(fakeFirestore.hasDocument(appUsageDocPath))
    }

    @After
    override fun tearDown() {
        super.tearDown()
        unmockkAll()
    }
}