package com.servicesphere.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val signedInEmail: String? get() = auth.currentUser?.email
    val isSignedIn: Boolean get() = auth.currentUser != null
    fun signInIntent(context: Context): Intent = GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(com.servicesphere.R.string.firebase_google_web_client_id).also { require(it.isNotBlank()) { "Google sign-in is not configured for this build." } }).requestEmail().build()).signInIntent

    suspend fun completeGoogleSignIn(data: Intent?): Result<Unit> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).await()
    }
    fun signOut() { GoogleSignIn.getClient(com.servicesphere.data.ServiceLocator.appContext, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut(); auth.signOut() }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it, onCancellation = {}) }
    addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
}
