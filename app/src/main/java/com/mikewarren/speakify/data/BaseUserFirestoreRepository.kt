package com.mikewarren.speakify.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.mikewarren.speakify.data.db.firestore.BaseFirestoreRepository

open class BaseUserFirestoreRepository: BaseFirestoreRepository() {
    protected val firebaseAuth = FirebaseAuth.getInstance()

    protected val userDoc: DocumentReference
        get() = firestore.collection("users")
            .document(userId)

    protected val userId: String
        get() = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
}