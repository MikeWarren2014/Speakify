package com.mikewarren.speakify.data

import android.util.Log
import androidx.datastore.core.DataStore
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.user.User
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.utils.DeviceIdProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TrialRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var deviceIdProvider: DeviceIdProvider
    private lateinit var userSettingsDataStore: DataStore<UserSettingsModel>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var directSignUpCollection: CollectionReference
    private lateinit var docRef: DocumentReference

    private lateinit var repository: TrialRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = mockk(relaxed = true)
        deviceIdProvider = mockk(relaxed = true)
        userSettingsDataStore = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        directSignUpCollection = mockk(relaxed = true)
        docRef = mockk(relaxed = true)

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
        mockkObject(Clerk)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { deviceIdProvider.deviceId } returns "test-device-id"
        every { firestore.collection("directSignUps") } returns directSignUpCollection
        every { directSignUpCollection.document(any()) } returns docRef


    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `recordSignUp calls recordDirectSignUp when no trial started`() = runTest {
        val trialModel = TrialModel(status = TrialStatus.NotStarted, startTimestamp = 0L)
        repository = createTrialRepository(trialModel)

        assertEquals(trialModel, repository.trialModelFlow.first())

        val user = mockk<User>(relaxed = true)
        every { Clerk.user } returns user
        val emailAddress = mockk<EmailAddress>(relaxed = true)
        every { user.emailAddresses } returns listOf(emailAddress)
        every { emailAddress.emailAddress } returns "test@example.com"

        val snapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns false
        every { docRef.get() } returns Tasks.forResult(snapshot)

        every { docRef.set(any()) } returns Tasks.forResult(null)

        repository.recordSignUp()

        io.mockk.coVerify { docRef.set(any()) }
    }

    @Test
    fun `refreshTrialStatus calls recordSignUp when user is logged in`() = runTest {
        // Given: start state
        val trialModel = TrialModel(status = TrialStatus.NotStarted, startTimestamp = 0L)
        repository = createTrialRepository(trialModel)

        // When: user is logged in
        val user = mockk<User>(relaxed = true)
        every { Clerk.user } returns user
        val emailAddress = mockk<EmailAddress>(relaxed = true)
        every { user.emailAddresses } returns listOf(emailAddress)
        every { emailAddress.emailAddress } returns "test@example.com"

        val snapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns false
        every { docRef.get() } returns Tasks.forResult(snapshot)
        every { docRef.set(any()) } returns Tasks.forResult(null)

        // Then: recordSignUp should be called
        repository.refreshTrialStatus()

        io.mockk.coVerify { docRef.set(any()) }
    }

    private fun createTrialRepository(trialModel: TrialModel): TrialRepositoryImpl {
        every { userSettingsDataStore.data } returns flowOf(UserSettingsModel().copy(trialModel = trialModel))

        return TrialRepositoryImpl(
            settingsRepository,
            deviceIdProvider,
            userSettingsDataStore
        )
    }

}
