package com.familyplanner.family.data

import com.familyplanner.common.Application
import com.familyplanner.common.User
import com.familyplanner.family.model.Family
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow

class FamilyRepository {
    private val firestore = Firebase.firestore

    fun getFamilyById(familyId: String): Flow<List<Family>> =
        firestore.collection("families").whereEqualTo("id", familyId).dataObjects()

    fun updateFamily(familyId: String, newName: String) {
        firestore.collection("families").whereEqualTo("id", familyId).get().addOnCompleteListener {
            if (!it.result.isEmpty) {
                for (doc in it.result) {
                    doc.reference.update(mapOf("name" to newName))
                }
            }
        }
    }

    fun getFamilyMembers(familyId: String): Flow<List<User>> =
        firestore.collection("users").whereEqualTo("familyId", familyId).dataObjects()

    fun getApplicationsToFamily(familyId: String): Flow<List<Application>> =
        firestore.collection("applications").whereEqualTo("familyId", familyId)
            .whereEqualTo("status", ApplicationStatus.NEW).dataObjects()

    fun getApplicants(ids: List<String>): Flow<List<User>> =
        firestore.collection("users").whereIn("id", ids).dataObjects()

    fun approveApplication(userId: String, familyId: String) {
        firestore.collection("applications").whereEqualTo("userId", userId).get()
            .addOnCompleteListener {
                if (!it.result.isEmpty) {
                    for (doc in it.result) {
                        doc.reference.update(mapOf("status" to ApplicationStatus.APPROVED))
                    }
                }
            }

        firestore.collection("users").whereEqualTo("id", userId).get().addOnCompleteListener {
            if (!it.result.isEmpty) {
                for (doc in it.result) {
                    if (doc["hasFamily"] as Boolean)
                        doc.reference.update(mapOf("hasFamily" to true, "familyId" to ""))
                }
            }
        }
    }

    fun rejectApplication(userId: String, familyId: String) {
        firestore.collection("applications").whereEqualTo("userId", userId)
            .whereEqualTo("familyId", familyId).get().addOnCompleteListener {
            if (!it.result.isEmpty) {
                for (doc in it.result) {
                    doc.reference.update(mapOf("status" to ApplicationStatus.REJECTED))
                }
            }
        }
    }

    fun removeMember(userId: String) {
        firestore.collection("users").whereEqualTo("id", userId).get().addOnCompleteListener {
            if (!it.result.isEmpty) {
                for (doc in it.result) {
                    doc.reference.update(mapOf("hasFamily" to false, "familyId" to "", "isAdmin" to false))
                }
            }
        }
    }

    fun getUserById(userId: String): Flow<List<User>> =
        firestore.collection("users").whereEqualTo("id", userId).dataObjects()

    fun applyToFamily(userId: String, familyId: String) {
        val data = HashMap<String, Any>()
        data["userId"] = userId
        data["familyId"] = familyId
        data["status"] = ApplicationStatus.NEW
        firestore.collection("applications").add(data)
    }

    fun createFamily(name: String, userId: String): Task<DocumentReference> {
        val data = HashMap<String, Any>()
        data["name"] = name
        return firestore.collection("families").add(data)
    }

    fun setUserToAdmin(userId: String, familyId: String) {
        firestore.collection("users").document(userId).update(mapOf("hasFamily" to true, "familyId" to familyId, "isAdmin" to true))
    }
}