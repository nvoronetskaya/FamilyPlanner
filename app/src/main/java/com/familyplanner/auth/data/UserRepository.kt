package com.familyplanner.auth.data

import com.familyplanner.common.User
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow

class UserRepository {
    private val firestore = Firebase.firestore

    fun addUser(name: String, birthday: String, email: String) {
        val data = HashMap<String, Any>()
        data["name"] = name
        data["birthday"] = birthday
        data["email"] = email
        data["familyId"] = ""
        data["hasFamily"] = false
        firestore.collection("users").add(data)
    }

    fun getUserByEmail(email: String): Flow<List<User>> = firestore.collection("users").whereEqualTo("email", email).dataObjects()

    fun getUserById(userId: String): Flow<List<User>> = firestore.collection("users").whereEqualTo("id", userId).dataObjects()

    fun updateUser(id: String, name: String, birthday: String) {
        firestore.collection("users").whereEqualTo("id", id).get().addOnCompleteListener {
            if (!it.result.isEmpty) {
                for (doc in it.result) {
                    doc.reference.update(mapOf("name" to name, "birthday" to birthday))
                }
            }
        }
    }
}