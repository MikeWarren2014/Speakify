package com.mikewarren.speakify.data.delegates

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk

class AnonymousUserFirebaseAuthDelegate: IFirebaseAuth {
    override var firebaseAuth: FirebaseAuth = mockk<FirebaseAuth>(relaxed = true)

    override var firebaseUser: FirebaseUser? = mockk<FirebaseUser>(relaxed = true)

    override fun setUpFirebaseAuth() {
        super.setUpFirebaseAuth()

        every { firebaseUser!!.isAnonymous } returns true
        every { firebaseUser!!.uid } returns "anonymous_user"
        every { firebaseAuth.currentUser } returns firebaseUser
    }
}