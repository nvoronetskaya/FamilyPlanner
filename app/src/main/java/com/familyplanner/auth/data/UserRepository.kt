package com.familyplanner.auth.data

import com.familyplanner.common.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = Firebase.firestore

    fun addUser(name: String, birthday: String, email: String, uid: String) {
        val data = HashMap<String, Any>()
        data["name"] = name
        data["birthday"] = birthday
        data["email"] = email
        data["familyId"] = ""
        data["hasFamily"] = false
        data["fcmToken"] = ""
        firestore.collection("users").document(uid).set(data)
    }

    fun getUserByEmail(email: String): Flow<User> =
        firestore.collection("users").whereEqualTo("email", email)
            .snapshots().map {
                val doc = it.documents[0]
                User(
                    doc.id,
                    doc["name"].toString(),
                    doc["birthday"].toString(),
                    doc["hasFamily"] as Boolean,
                    doc["familyId"].toString(),
                    doc["email"].toString(),
                    doc.getGeoPoint("location")
                )
            }

    fun getUserById(userId: String): Flow<User> = firestore.collection("users").whereEqualTo(
        FieldPath.documentId(), userId
    ).snapshots().map {
        val doc = it.documents[0]
        User(
            doc.id,
            doc["name"].toString(),
            doc["birthday"].toString(),
            doc["hasFamily"] as Boolean,
            doc["familyId"].toString(),
            doc["email"].toString(),
            doc.getGeoPoint("location")
        )
    }

    suspend fun getUserByIdOnce(userId: String): User {
        val doc = firestore.collection("users").document(userId).get().await()
        return User(
            doc.id,
            doc["name"].toString(),
            doc["birthday"].toString(),
            doc["hasFamily"] as Boolean,
            doc["familyId"].toString(),
            doc["email"].toString(),
            doc.getGeoPoint("location")
        )
    }

    fun updateUser(id: String, name: String, birthday: String) {
        firestore.collection("users").whereEqualTo(FieldPath.documentId(), id).get()
            .addOnCompleteListener {
                if (!it.result.isEmpty) {
                    for (doc in it.result) {
                        doc.reference.update(mapOf("name" to name, "birthday" to birthday))
                    }
                }
            }
    }

    fun setFcmToken(userId: String, token: String) {
        firestore.collection("users").document(userId).update("fcmToken", token)
    }
}