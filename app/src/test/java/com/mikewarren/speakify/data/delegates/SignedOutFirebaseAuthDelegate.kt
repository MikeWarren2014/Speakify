package com.mikewarren.speakify.data.delegates

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk

class SignedOutFirebaseAuthDelegate: IFirebaseAuth {
    override var firebaseAuth: FirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    override var firebaseUser: FirebaseUser? = null

    override fun setUpFirebaseAuth() {
        super.setUpFirebaseAuth()
        every { firebaseAuth.currentUser } returns null
    }
}