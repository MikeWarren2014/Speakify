package com.mikewarren.speakify.data.db.firestore

import com.clerk.api.Clerk
import com.google.firebase.firestore.DocumentReference

open class BaseUserFirestoreRepository: BaseFirestoreRepository() {

    protected val userDoc: DocumentReference
        get() = firestore.collection("users")
            .document(userId)

    protected val userId: String
        get() = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    suspend fun writeClerkUserData(): Result<Unit> {
        val clerkUserId = Clerk.user?.id

        if (clerkUserId == null)
            return Result.failure(IllegalStateException("Clerk user ID is null. Is user even logged in?"))

        return writeTransaction(userDoc,
            hashMapOf(
                "clerkUserId" to clerkUserId,
                "userEmail" to Clerk.user?.emailAddresses!!.first().emailAddress,
            ))


    }
}