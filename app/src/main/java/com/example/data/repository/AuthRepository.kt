package com.example.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: return Result.failure(Exception("Authentication returned no user."))
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmail failed", e)
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: return Result.failure(Exception("Account creation returned no user."))
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signUpWithEmail failed", e)
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendPasswordReset failed", e)
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun mapAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters."
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists. Try signing in instead."
            is FirebaseAuthInvalidUserException ->
                "No account found for this email address. Tap Create Account to sign up."
            is FirebaseAuthInvalidCredentialsException ->
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                        "Incorrect email or password. Please check and try again."
                    else -> e.localizedMessage ?: "Invalid email or password."
                }
            is FirebaseTooManyRequestsException ->
                "Too many attempts. Please wait a few minutes and try again."
            is FirebaseNetworkException ->
                "Network error. Check your internet connection and try again."
            is FirebaseAuthException ->
                when (e.errorCode) {
                    "ERROR_OPERATION_NOT_ALLOWED" ->
                        "Email/Password sign-in is disabled in Firebase Console. Enable it under Authentication -> Sign-in method -> Email/Password."
                    else -> e.localizedMessage ?: "Authentication failed. Please try again."
                }
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
