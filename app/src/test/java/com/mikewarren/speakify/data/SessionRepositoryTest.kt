package com.mikewarren.speakify.data

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.clerk.api.user.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.mikewarren.speakify.data.db.firestore.AccountDeletionFirestoreRepository
import com.mikewarren.speakify.data.db.firestore.FirestoreSyncRepository
import com.mikewarren.speakify.data.models.TrialModel
import com.mikewarren.speakify.data.uiStates.MainUiState
import com.mikewarren.speakify.data.uiStates.OnboardingUiState
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var firestoreSyncRepository: FirestoreSyncRepository
    private lateinit var accountDeletionFirestoreRepository: AccountDeletionFirestoreRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var trialRepository: TrialRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var firebaseAuth: FirebaseAuth

    private val isInitializedFlow = MutableStateFlow(false)
    private val userFlow = MutableStateFlow<User?>(null)
    private val sessionFlow = MutableStateFlow<com.clerk.api.session.Session?>(mockk(relaxed = true))
    private val trialModelFlow = MutableStateFlow(TrialModel())
    private val isNewDirectSignUpFlow = MutableStateFlow(false)
    private val appOpenCountFlow = MutableStateFlow(1)
    private val onboardingStepFlow = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Completed)
    private val speakificationCountFlow = MutableStateFlow(0)
    private val hasShownRatingsPromptFlow = MutableStateFlow(false)
    private val hasShownTrialConversionPromptFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        firestoreSyncRepository = mockk(relaxed = true)
        accountDeletionFirestoreRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        trialRepository = mockk(relaxed = true)
        onboardingRepository = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)

        mockkObject(Clerk)

        every { Clerk.isInitialized } returns isInitializedFlow
        every { Clerk.userFlow } returns userFlow
        every { Clerk.sessionFlow } returns sessionFlow

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth

        every { trialRepository.trialModelFlow } returns trialModelFlow
        every { trialRepository.isNewDirectSignUp } returns isNewDirectSignUpFlow
        every { onboardingRepository.appOpenCount } returns appOpenCountFlow
        every { onboardingRepository.onboardingStep } returns onboardingStepFlow
        every { onboardingRepository.speakificationCount } returns speakificationCountFlow
        every { onboardingRepository.hasShownRatingsPrompt } returns hasShownRatingsPromptFlow
        every { onboardingRepository.hasShownTrialConversionPrompt } returns hasShownTrialConversionPromptFlow
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
    fun `Signed out state when trial is not needed and user is null`() = runTest {
        val repository = createRepository()

        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotNeeded)
        userFlow.value = null

        advanceUntilIdle()

        assertEquals(MainUiState.SignedOut, repository.uiState.value)
    }

    @Test
    fun `Signed out state when trial is not started and user is null`() = runTest {
        val repository = createRepository()

        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotStarted)
        userFlow.value = null

        advanceUntilIdle()

        assertEquals(MainUiState.SignedOut, repository.uiState.value)
        assertEquals(TrialStatus.NotStarted, trialRepository.trialModelFlow.first().status)
    }

    @Test
    fun `TrialActive state when trial is active and user is null`() = runTest {
        val repository = createRepository()

        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.Active(7))
        userFlow.value = null

        advanceUntilIdle()

        assertEquals(MainUiState.TrialActive, repository.uiState.value)
    }

    @Test
    fun `proceedToTrialSession updates state correctly`() = runTest {
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
    fun `TrialEnded state when trial is expired and user is null`() = runTest {
        val repository = createRepository()

        isInitializedFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.Expired)
        userFlow.value = null

        advanceUntilIdle()

        assertEquals(MainUiState.TrialEnded, repository.uiState.value)
    }

    @Test
    fun `Scenario 1 - New user signs up results in Onboarding state`() = runTest {
        val repository = createRepository()

        applyNewUserState()

        advanceUntilIdle()

        assertEquals(MainUiState.Onboarding(OnboardingUiState.NotStarted), repository.uiState.value)
    }

    @Test
    fun `Scenario 2 - New user makes it through onboarding results in SignedIn state`() = runTest {
        val repository = createRepository()

        applyNewUserState()
        advanceUntilIdle()

        // Complete onboarding
        onboardingStepFlow.value = OnboardingUiState.Completed
        advanceUntilIdle()

        // Verify resetNewDirectSignUp was called
        io.mockk.verify { trialRepository.resetNewDirectSignUp() }

        // Simulate the flow update from resetNewDirectSignUp
        isNewDirectSignUpFlow.value = false
        advanceUntilIdle()

        assertEquals(MainUiState.SignedIn, repository.uiState.value)
    }

    @Test
    fun `Scenario 3 - User signs out results in SignedOut state`() = runTest {
        val repository = createRepository()

        trialModelFlow.value = TrialModel(status = TrialStatus.NotNeeded)

        // Start in SignedIn state
        isInitializedFlow.value = true
        userFlow.value = createMockUser("test@example.com")
        isNewDirectSignUpFlow.value = false
        advanceUntilIdle()
        assertEquals(MainUiState.SignedIn, repository.uiState.value)

        // Sign out
        userFlow.value = null
        advanceUntilIdle()

        assertEquals(MainUiState.SignedOut, repository.uiState.value)
        io.mockk.verify { firebaseAuth.signOut() }
        io.mockk.coVerify { settingsRepository.clearAllData() }
    }

    @Test
    fun `Scenario 4 - Existing user signs in results in SignedIn state`() = runTest {
        val repository = createRepository()

        isInitializedFlow.value = true
        userFlow.value = createMockUser("test@example.com")
        isNewDirectSignUpFlow.value = false

        advanceUntilIdle()

        assertEquals(MainUiState.SignedIn, repository.uiState.value)
    }

    @Test
    fun `Scenario 5 - Newly signed up user who completed onboarding sees RatingsPrompt after first speakification`() = runTest {
        val repository = createRepository()

        // GIVEN: user is newly signed up user, who just completed the onboarding steps
        isInitializedFlow.value = true
        userFlow.value = createMockUser("test@example.com")
        isNewDirectSignUpFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotNeeded)
        onboardingStepFlow.value = OnboardingUiState.Completed
        speakificationCountFlow.value = 0
        hasShownRatingsPromptFlow.value = false

        advanceUntilIdle()

        // At this point, since speakificationCount is 0, isHandlingTrialEngagement(TrialBypass)
        // returns false. Then it should have called trialRepository.resetNewDirectSignUp().
        io.mockk.verify { trialRepository.resetNewDirectSignUp() }

        // Simulate the flow update from resetNewDirectSignUp
        isNewDirectSignUpFlow.value = false
        advanceUntilIdle()
        assertEquals(MainUiState.SignedIn, repository.uiState.value)

        // WHEN: user has a notification speakify'd
        speakificationCountFlow.value = 1
        advanceUntilIdle()

        // THEN:
        assertEquals(MainUiState.RatingsPrompt, repository.uiState.value)
    }

    @Test
    fun `Scenario 6 - lastDataBundle maintains progress after logout`() = runTest {
        val repository = createRepository()

        // GIVEN: user logs in, completes onboarding, and has seen ratings prompt
        isInitializedFlow.value = true
        userFlow.value = createMockUser("user_123")
        isNewDirectSignUpFlow.value = false
        onboardingStepFlow.value = OnboardingUiState.Completed
        hasShownRatingsPromptFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotNeeded)

        advanceUntilIdle()

        assertEquals(OnboardingUiState.Completed, repository.lastDataBundle?.onboardingStep)
        assertEquals(true, repository.lastDataBundle?.hasShownRatingsPrompt)

        // WHEN: user logs out
        userFlow.value = null
        advanceUntilIdle()

        // Simulate repositories being cleared after logout
        onboardingStepFlow.value = OnboardingUiState.NotStarted
        hasShownRatingsPromptFlow.value = false
        advanceUntilIdle()

        // THEN: lastDataBundle should maintain their onboarding/ratings progress
        assertEquals(null, repository.lastDataBundle?.user)
        assertEquals(OnboardingUiState.Completed, repository.lastDataBundle?.onboardingStep)
        assertEquals(true, repository.lastDataBundle?.hasShownRatingsPrompt)
    }

//    @Test
//    fun `Scenario 7 - lastDataBundle doesn\'t reset after signing back in`() = runTest {
//        val repository = createRepository(this)
//
//        // simulate the last data bundle state of someone who last signed in and completed all onboarding steps including ratings screen
//        isInitializedFlow.value = true
//        userFlow.value = createMockUser("user_123")
//        isNewDirectSignUpFlow.value = false
//        onboardingStepFlow.value = OnboardingUiState.Completed
//        hasShownRatingsPromptFlow.value = true
//
//    }

    private fun createRepository() = SessionRepository(
        firestoreSyncRepository,
        accountDeletionFirestoreRepository,
        settingsRepository,
        trialRepository,
        onboardingRepository,
        mockk(relaxed = true)
    )

    private fun applyNewUserState() {
        isInitializedFlow.value = true
        userFlow.value = createMockUser("test@example.com")
        isNewDirectSignUpFlow.value = true
        trialModelFlow.value = TrialModel(status = TrialStatus.NotNeeded)
        onboardingStepFlow.value = OnboardingUiState.NotStarted
    }

    private fun createMockUser(id: String, email: String = "test@example.com"): User {
        val mockUser = mockk<User>(relaxed = true)
        every { mockUser.id } returns id
        val emailAddress = mockk<EmailAddress>(relaxed = true)
        every { emailAddress.emailAddress } returns email
        every { mockUser.emailAddresses } returns listOf(emailAddress)

        val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true)
        every { mockFirebaseUser.email } returns email
        every { firebaseAuth.currentUser } returns mockFirebaseUser

        return mockUser
    }
}
