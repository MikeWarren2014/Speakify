package com.mikewarren.speakify.data.db.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.seconds

abstract class BaseFirestoreRepository : ITaggable {

    protected val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    protected val firebaseAuth: FirebaseAuth get() = FirebaseAuth.getInstance()

    companion object {
        private val authMutex = Mutex()
    }

    /**
     * Executes a Firestore call with retries if the client is offline, unavailable,
     * or if permissions are temporarily denied (often due to auth propagation delay).
     */
    protected suspend fun <T> safeFirestoreCall(call: suspend () -> T): T {
        var retries = 5
        while (true) {
            // Proactive check: If no user is present, try to sign in anonymously before even attempting the call.
            if (firebaseAuth.currentUser == null) {
                try {
                    authMutex.withLock {
                        if (firebaseAuth.currentUser == null) {
                            Log.d(TAG, "No Firebase user found, attempting proactive anonymous sign-in")
                            firebaseAuth.signInAnonymously().await()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Proactive anonymous sign-in failed", e)
                }
            }

            try {
                return call()
            } catch (e: Exception) {
                val firestoreEx = (e as? FirebaseFirestoreException) ?: (e.cause as? FirebaseFirestoreException)
                val code = firestoreEx?.code
                val message = e.message ?: ""

                val isOffline = message.contains("offline", ignoreCase = true)
                val isPermissionDenied = code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                val isUnavailable = code == FirebaseFirestoreException.Code.UNAVAILABLE
                
                // If it's a permission denied error and there's no current user, it's likely an auth issue.
                // Also check for our custom "User not logged in" IllegalStateException.
                val isAuthMissing = (e is IllegalStateException && message.contains("User not logged in")) ||
                                     (isPermissionDenied && firebaseAuth.currentUser == null)

                val isRetryable = isPermissionDenied || isUnavailable || isOffline || isAuthMissing

                if (isRetryable && retries > 0) {
                    retries--
                    
                    // Reduce log level for the first few PERMISSION_DENIED if we're trying to fix auth
                    if (isPermissionDenied && firebaseAuth.currentUser == null) {
                        Log.d(TAG, "Firestore PERMISSION_DENIED (no auth), retrying... ($retries left)")
                    } else {
                        Log.w(TAG, "Firestore call failed (code: $code, isAuthMissing: $isAuthMissing, isOffline: $isOffline), retrying in 2s... ($retries left)", e)
                    }

                    if (isAuthMissing) {
                        try {
                            authMutex.withLock {
                                if (firebaseAuth.currentUser == null) {
                                    firebaseAuth.signInAnonymously().await()
                                    Log.d(TAG, "Successfully signed in anonymously after auth failure")
                                }
                            }
                        } catch (signInEx: Exception) {
                            Log.e(TAG, "Failed to sign in anonymously during retry", signInEx)
                        }
                    }

                    if (isUnavailable || isOffline) {
                        try { firestore.enableNetwork().await() } catch (_: Exception) {}
                    }

                    delay(2.seconds)
                    continue
                }
                throw e
            }
        }
    }

    protected suspend fun writeTransaction(document: DocumentReference, data: Any) : Result<Unit> {
        return try {
            safeFirestoreCall {
                document
                    .set(data)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
