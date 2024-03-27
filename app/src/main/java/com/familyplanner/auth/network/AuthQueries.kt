package com.familyplanner.auth.network

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthQueries {
    private val auth = Firebase.auth

    fun signUp(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(
            email, password
        )
    }

    fun signIn(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(
            email, password
        )
    }

    fun resetPassword(email: String) {
        Firebase.auth.sendPasswordResetEmail(email)
    }
}