package com.familyplanner.family.data

import com.familyplanner.common.Application
import com.familyplanner.common.User
import com.familyplanner.family.model.Family
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FamilyRepository {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getFamilyById(familyId: String): Flow<Family> =
        firestore.collection("families").whereEqualTo(FieldPath.documentId(), familyId).snapshots()
            .map {
                val doc = it.documents[0]
                Family(
                    doc.id,
                    doc["name"].toString(),
                    doc["code"].toString(),
                    doc["createdBy"].toString()
                )
            }

    suspend fun getFamilyByIdOnce(familyId: String): Family? =
        firestore.collection("families").whereEqualTo(FieldPath.documentId(), familyId).get()
            .await().documents.map {
                Family(
                    it.id,
                    it["name"].toString(),
                    it["code"].toString(),
                    it["createdBy"].toString()
                )
            }.firstOrNull()

    suspend fun updateFamily(familyId: String, newName: String) {
        firestore.collection("families").document(familyId).get()
            .await().reference.update(mapOf("name" to newName))
    }

    fun getFamilyMembers(familyId: String): Flow<List<User>> =
        firestore.collection("users").whereEqualTo("familyId", familyId).snapshots().map {
            val users = mutableListOf<User>()
            for (doc in it.documents) {
                val user = User(
                    doc.id,
                    doc["name"].toString(),
                    doc["birthday"].toString(),
                    doc["hasFamily"] as Boolean,
                    doc["familyId"].toString(),
                    doc["email"].toString(),
                    doc.getGeoPoint("location")
                )
                users.add(user)
            }
            users
        }

    suspend fun getFamilyMembersOnce(familyId: String): List<User> =
        firestore.collection("users").whereEqualTo("familyId", familyId).get().await().map {
            User(
                it.id,
                it["name"].toString(),
                it["birthday"].toString(),
                it["hasFamily"] as Boolean,
                it["familyId"].toString(),
                it["email"].toString()
            )
        }

    fun getApplicationsToFamily(familyId: String): Flow<List<Application>> =
        firestore.collection("applications").whereEqualTo("familyId", familyId)
            .whereEqualTo("status", ApplicationStatus.NEW.name).snapshots()
            .map {
                it.toObjects(Application::class.java)
            }

    fun getApplicants(ids: List<String>): Flow<List<User>> {
        val newIds = mutableListOf("1")
        newIds.addAll(ids)
        return firestore.collection("users").whereIn(FieldPath.documentId(), newIds).snapshots()
            .map {
                val users = mutableListOf<User>()
                for (doc in it.documents) {
                    val user = User(
                        doc.id,
                        doc["name"].toString(),
                        doc["birthday"].toString(),
                        doc["hasFamily"] as Boolean,
                        doc["familyId"].toString(),
                        doc["email"].toString()
                    )
                    users.add(user)
                }
                users
            }
    }

    fun approveApplication(userId: String, familyId: String) {
        firestore.collection("applications").whereEqualTo("userId", userId)
            .whereEqualTo("familyId", familyId).get()
            .addOnSuccessListener {
                if (it.documents.isNotEmpty()) {
                    for (doc in it.documents) {
                        doc.reference.update(mapOf("status" to ApplicationStatus.APPROVED))
                    }
                }

                firestore.collection("users").whereEqualTo(FieldPath.documentId(), userId).get()
                    .addOnCompleteListener {
                        if (!it.result.isEmpty) {
                            for (doc in it.result) {
                                if (doc["hasFamily"] as Boolean)
                                    doc.reference.update(
                                        mapOf(
                                            "hasFamily" to true,
                                            "familyId" to familyId
                                        )
                                    )
                            }
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
        firestore.collection("users").whereEqualTo(FieldPath.documentId(), userId).get()
            .addOnCompleteListener {
                if (!it.result.isEmpty) {
                    for (doc in it.result) {
                        doc.reference.update(
                            mapOf(
                                "hasFamily" to false,
                                "familyId" to "",
                                "isAdmin" to false
                            )
                        )
                    }
                }
            }
    }

    fun getUserById(userId: String): Flow<User> =
        firestore.collection("users").whereEqualTo(FieldPath.documentId(), userId).snapshots().map {
            val doc = it.documents[0]
            User(
                doc.id,
                doc["name"].toString(),
                doc["birthday"].toString(),
                doc["hasFamily"] as Boolean,
                doc["familyId"].toString(),
                doc["email"].toString()
            )
        }

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
        data["createdBy"] = userId
        return firestore.collection("families").add(data)
    }

    fun setUserToAdmin(userId: String, familyId: String) {
        firestore.collection("users").document(userId)
            .update(mapOf("hasFamily" to true, "familyId" to familyId, "isAdmin" to true))
    }

    fun deleteFamily(familyId: String): Task<Void> {
        val task = firestore.collection("families").document(familyId).delete()
        task.addOnSuccessListener {
            firestore.collection("users").whereEqualTo("familyId", familyId).get()
                .addOnCompleteListener {
                    if (!it.result.isEmpty) {
                        for (doc in it.result) {
                            doc.reference.update(
                                mapOf(
                                    "hasFamily" to false,
                                    "familyId" to "",
                                    "isAdmin" to false
                                )
                            )
                        }
                    }
                }

            firestore.collection("applications").whereEqualTo("familyId", familyId).get()
                .addOnCompleteListener {
                    if (!it.result.isEmpty) {
                        for (doc in it.result) {
                            doc.reference.delete()
                        }
                    }
                }
        }

        return task
    }

    fun getCompletionHistory(start: Long, finish: Long): Flow<List<CompletionDto>> {
        val history = MutableSharedFlow<List<CompletionDto>>()
        scope.launch {
            firestore.collection("taskCompletion")
                .whereGreaterThanOrEqualTo("completionDate", start).snapshots().collect {
                    val documents = it.documents.filter { it.getLong("competionDate")!! <= finish }
                    val result = documents.map {
                        val userName =
                            firestore.collection("users").document(it["userId"].toString()).get()
                                .await()["name"].toString()
                        CompletionDto(
                            it["taskId"].toString(),
                            it["taskName"].toString(),
                            it["userId"].toString(),
                            userName,
                            it.getLong("completionDate")!!
                        )
                    }
                    history.emit(result)
                }
        }
        return history
    }
}