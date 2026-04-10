package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.user.User
import com.google.firebase.auth.FirebaseAuth
import com.mikewarren.speakify.data.db.firestore.AccountDeletionFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FeedbackFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var firestoreSyncRepository: FirestoreSyncRepository
    private lateinit var feedbackFirestoreRepository: FeedbackFirestoreRepository
    private lateinit var accountDeletionFirestoreRepository: AccountDeletionFirestoreRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var trialRepository: TrialRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var firebaseAuth: FirebaseAuth

    private val isInitializedFlow = MutableStateFlow(false)
    private val userFlow = MutableStateFlow<User?>(null)
    private val trialModelFlow = MutableStateFlow(TrialModel())
    private val appOpenCountFlow = MutableStateFlow(1)
    private val onboardingStepFlow = MutableStateFlow(OnboardingUiState.Completed)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        firestoreSyncRepository = mockk(relaxed = true)
        feedbackFirestoreRepository = mockk(relaxed = true)
        accountDeletionFirestoreRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        trialRepository = mockk(relaxed = true)
        onboardingRepository = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)

        mockkObject(Clerk)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { Clerk.isInitialized } returns isInitializedFlow
        every { Clerk.userFlow } returns userFlow
        
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth

        every { trialRepository.trialModelFlow } returns trialModelFlow
        every { onboardingRepository.appOpenCount } returns appOpenCountFlow
        every { onboardingRepository.onboardingStep } returns onboardingStepFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        val repository = createRepository()
        assertEquals(MainUiState.Loading, repository.uiState.value)
    }

    @Test
    fun `Scenario 1 - NotNeeded trial status results in SignedOut`() = runTest {
        val repository = createRepository()
        
        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotNeeded)
        userFlow.value = null
        
        advanceUntilIdle()
        
        assertEquals(MainUiState.SignedOut, repository.uiState.value)
    }

    @Test
    fun `Scenario 2 - NotStarted trial status results in SignedOut`() = runTest {
        val repository = createRepository()
        
        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotStarted)
        userFlow.value = null
        
        advanceUntilIdle()
        
        assertEquals(MainUiState.SignedOut, repository.uiState.value)
        assertEquals(TrialStatus.NotStarted, trialRepository.trialModelFlow.first().status)
    }

    @Test
    fun `Scenario 3 - Active trial status results in TrialActive`() = runTest {
        val repository = createRepository()
        
        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.Active(7))
        userFlow.value = null
        
        advanceUntilIdle()
        
        assertEquals(MainUiState.TrialActive, repository.uiState.value)
    }

    @Test
    fun `Scenario 4 - proceedToTrialSession changes state and resets on re-init`() = runTest {
        var repository = createRepository()
        
        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.Active(7))
        userFlow.value = null
        advanceUntilIdle()
        
        assertEquals(MainUiState.TrialActive, repository.uiState.value)
        
        repository.proceedToTrialSession()
        assertEquals(MainUiState.TrialUsage, repository.uiState.value)
        
        // Simulate app re-open by creating a new repository instance
        repository = createRepository()
        advanceUntilIdle()
        
        assertEquals(MainUiState.TrialActive, repository.uiState.value)
    }

    @Test
    fun `Scenario 5 - Expired trial status results in TrialEnded`() = runTest {
        val repository = createRepository()
        
        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.Expired)
        userFlow.value = null
        
        advanceUntilIdle()
        
        assertEquals(MainUiState.TrialEnded, repository.uiState.value)
    }

    private fun createRepository() = SessionRepository(
        firestoreSyncRepository,
        feedbackFirestoreRepository,
        accountDeletionFirestoreRepository,
        settingsRepository,
        trialRepository,
        onboardingRepository,
        mockk(relaxed = true)
    )
}
