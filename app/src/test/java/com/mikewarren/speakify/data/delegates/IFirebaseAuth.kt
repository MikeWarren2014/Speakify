package com.mikewarren.speakify.data.delegates

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockkStatic

interface IFirebaseAuth {
    var firebaseAuth: FirebaseAuth
    var firebaseUser: FirebaseUser?

    fun setUpFirebaseAuth() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
    }
}
