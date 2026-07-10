package com.mikewarren.speakify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.db.firestore.DownloadRepository
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.data.db.firestore.UploadRepository
import com.mikewarren.speakify.data.fakes.FakeFirestore
import com.mikewarren.speakify.utils.AnalyticsHelper
import com.mikewarren.speakify.utils.DeviceIdProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignInHistoryWipeTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var db: AppDatabase
    private lateinit var fakeFirestore: FakeFirestore
    private lateinit var userSettingsDataStore: DataStore<UserSettingsModel>

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appsRepository: AppsRepository
    private lateinit var messengerContactsRepository: MessengerContactsRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var trialRepository: TrialRepository
    private lateinit var analyticsHelper: AnalyticsHelper

    private lateinit var downloadRepository: DownloadRepository
    private lateinit var uploadRepository: UploadRepository
    private lateinit var firestoreSyncRepository: FirestoreSyncRepository
    private lateinit var sessionRepository: SessionRepository

    private val clerkUserFlow = MutableStateFlow<User?>(null)
    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val firebaseUser = mockk<FirebaseUser>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeFirestore = FakeFirestore()
        
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns fakeFirestore.mock

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
        every { firebaseAuth.currentUser } returns null // Start logged out

        mockkObject(Clerk)
        every { Clerk.userFlow } returns clerkUserFlow
        every { Clerk.isInitialized } returns MutableStateFlow(true)
        every { Clerk.user } answers { clerkUserFlow.value }
        every { Clerk.sessionFlow } returns MutableStateFlow(mockk(relaxed = true))

        userSettingsDataStore = mockk(relaxed = true)
        val userSettingsFlow = MutableStateFlow(UserSettingsModel())
        every { userSettingsDataStore.data } returns userSettingsFlow
        coEvery { userSettingsDataStore.updateData(any()) } answers {
            val transform = firstArg<suspend (UserSettingsModel) -> UserSettingsModel>()
            val newValue = kotlinx.coroutines.runBlocking { transform(userSettingsFlow.value) }
            userSettingsFlow.value = newValue
            newValue
        }

        mockkObject(com.mikewarren.speakify.data.db.DbProvider)
        every { com.mikewarren.speakify.data.db.DbProvider.GetDb(any()) } returns db

        onboardingRepository = OnboardingRepositoryImpl(userSettingsDataStore)
        val appCategoryRepository = mockk<AppCategoryRepository>(relaxed = true)
        appsRepository = AppsRepositoryImpl(context, onboardingRepository, appCategoryRepository)
        settingsRepository = SettingsRepositoryImpl(context, userSettingsDataStore)
        messengerContactsRepository = MessengerContactsRepositoryImpl(context)
        
        val deviceIdProvider = mockk<DeviceIdProvider>(relaxed = true)
        every { deviceIdProvider.deviceId } returns "fake-device-id"
        trialRepository = TrialRepositoryImpl(settingsRepository, deviceIdProvider, userSettingsDataStore)
        
        analyticsHelper = mockk(relaxed = true)

        downloadRepository = DownloadRepository(settingsRepository, appsRepository, messengerContactsRepository, onboardingRepository)
        uploadRepository = UploadRepository(settingsRepository, appsRepository, messengerContactsRepository, onboardingRepository)
        
        firestoreSyncRepository = FirestoreSyncRepository(settingsRepository, appsRepository, messengerContactsRepository, onboardingRepository, uploadRepository, downloadRepository)
        
        sessionRepository = SessionRepository(
            firestoreSyncRepository,
            mockk(relaxed = true),
            settingsRepository,
            trialRepository,
            onboardingRepository,
            analyticsHelper,
            mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `reproduce history wipe - Firestore data should NOT be wiped when user signs in with empty local state`() = runTest {
        val testUserId = "user_123"
        val testEmail = "test@example.com"

        // 1. GIVEN: Firestore has existing data for this user
        val userDocPath = "users/$testUserId"
        val app1 = UserAppModel(packageName = "com.example.app1", appName = "App 1")
        fakeFirestore.setData("$userDocPath/important_apps/com.example.app1", app1)
        
        val appSettings = mapOf("packageName" to "com.example.app1", "announcerVoice" to "Voice 1")
        fakeFirestore.setData("$userDocPath/app_settings/com.example.app1", appSettings)

        // 2. GIVEN: Local database is empty
        assertTrue(db.userAppsDao().getAll().isEmpty())

        // 3. WHEN: User signs in
        val mockUser = mockk<User>(relaxed = true)
        every { mockUser.id } returns "clerk_user_123"
        val emailAddress = mockk<com.clerk.api.emailaddress.EmailAddress>(relaxed = true)
        every { emailAddress.emailAddress } returns testEmail
        every { mockUser.emailAddresses } returns listOf(emailAddress)

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns testUserId
        every { firebaseUser.email } returns testEmail

        clerkUserFlow.value = mockUser
        
        // Ensure initial sync runs
        firestoreSyncRepository.downloadAndRestoreData()
        advanceUntilIdle()

        // 4. THEN: Firestore data should still exist
        assertNotNull("App 1 should still exist in Firestore", fakeFirestore.getData("$userDocPath/important_apps/com.example.app1"))
        assertNotNull("App settings should still exist in Firestore", fakeFirestore.getData("$userDocPath/app_settings/com.example.app1"))
        
        // 5. THEN: Local database should be restored
        val localApps = db.userAppsDao().getAll()
        assertTrue("Local apps should be restored from Firestore", localApps.isNotEmpty())
    }
}
