package com.familyplanner.auth.data

import com.familyplanner.common.User
import com.familyplanner.common.schema.UserDbSchema
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    fun addUser(name: String, birthday: String, email: String, uid: String) {
        val data = HashMap<String, Any>()
        data[UserDbSchema.NAME] = name
        data[UserDbSchema.BIRTHDAY] = birthday
        data[UserDbSchema.EMAIL] = email
        data[UserDbSchema.FAMILY_ID] = ""
        data[UserDbSchema.FCM_TOKEN] = ""
        firestore.collection(UserDbSchema.USER_TABLE).document(uid).set(data)
    }

    fun getUserById(userId: String): Flow<User> = firestore.collection(UserDbSchema.USER_TABLE).whereEqualTo(
        FieldPath.documentId(), userId
    ).snapshots().map {
        val doc = it.documents[0]
        User(
            doc.id,
            doc[UserDbSchema.NAME].toString(),
            doc[UserDbSchema.BIRTHDAY].toString(),
            doc[UserDbSchema.FAMILY_ID].toString(),
            auth.currentUser?.email ?: "",
            doc.getGeoPoint(UserDbSchema.LOCATION)
        )
    }

    suspend fun getUserByIdOnce(userId: String): User {
        val doc = firestore.collection(UserDbSchema.USER_TABLE).document(userId).get().await()
        return User(
            doc.id,
            doc[UserDbSchema.NAME].toString(),
            doc[UserDbSchema.BIRTHDAY].toString(),
            doc[UserDbSchema.FAMILY_ID].toString(),
            auth.currentUser?.email ?: "",
            doc.getGeoPoint(UserDbSchema.LOCATION)
        )
    }

    fun updateUser(id: String, name: String, birthday: String) {
        firestore.collection(UserDbSchema.USER_TABLE).whereEqualTo(FieldPath.documentId(), id).get()
            .addOnCompleteListener {
                if (!it.result.isEmpty) {
                    for (doc in it.result) {
                        doc.reference.update(mapOf(UserDbSchema.NAME to name, UserDbSchema.BIRTHDAY to birthday))
                    }
                }
            }
    }

    fun setFcmToken(userId: String, token: String) {
        firestore.collection(UserDbSchema.USER_TABLE).document(userId).update(UserDbSchema.FCM_TOKEN, token)
    }

    fun checkPassword(password: String): Task<Void>? {
        val user = auth.currentUser ?: return null
        val credentials = EmailAuthProvider.getCredential(user.email!!, password)
        return user.reauthenticate(credentials)
    }

    suspend fun changeEmail(password: String, newEmail: String): Task<Void>? {
        val credentials = EmailAuthProvider.getCredential(auth.currentUser!!.email!!, password)
        val task = auth.currentUser?.reauthenticate(credentials) ?: return null
        task.await()
        if (task.isSuccessful) {
            return auth.currentUser?.updateEmail(newEmail)
        }
        return null
    }

    suspend fun hasAccount(email: String): Boolean {
        return auth.fetchSignInMethodsForEmail(email).await().signInMethods?.isNotEmpty() ?: false
    }

    fun removeFcmToken(userId: String) {
        firestore.collection(UserDbSchema.USER_TABLE).document(userId).update(UserDbSchema.FCM_TOKEN, "")
    }
}