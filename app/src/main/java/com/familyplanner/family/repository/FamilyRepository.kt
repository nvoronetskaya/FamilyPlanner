package com.familyplanner.family.repository

import com.familyplanner.common.data.User
import com.familyplanner.common.schema.ApplicationDbSchema
import com.familyplanner.common.schema.CompletionDbSchema
import com.familyplanner.common.schema.FamilyDbSchema
import com.familyplanner.common.schema.ObserverDbSchema
import com.familyplanner.common.schema.TaskDbSchema
import com.familyplanner.common.schema.UserDbSchema
import com.familyplanner.events.repository.EventRepository
import com.familyplanner.family.data.Application
import com.familyplanner.family.data.ApplicationStatus
import com.familyplanner.family.data.CompletionDto
import com.familyplanner.family.data.Family
import com.familyplanner.lists.repository.GroceryListRepository
import com.familyplanner.tasks.repository.TaskRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FamilyRepository {
    private val eventRepo = EventRepository()
    private val listRepo = GroceryListRepository()
    private val taskRepo = TaskRepository()
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getFamilyById(familyId: String): Flow<Family> =
        firestore.collection(FamilyDbSchema.FAMILY_TABLE)
            .whereEqualTo(FieldPath.documentId(), familyId).snapshots()
            .map {
                val doc = it.documents[0]
                Family(
                    doc.id,
                    doc[FamilyDbSchema.NAME].toString(),
                    doc[FamilyDbSchema.CREATED_BY].toString()
                )
            }

    suspend fun getFamilyByIdOnce(familyId: String): Family? =
        firestore.collection(FamilyDbSchema.FAMILY_TABLE)
            .whereEqualTo(FieldPath.documentId(), familyId).get()
            .await().documents.map {
                Family(
                    it.id,
                    it[FamilyDbSchema.NAME].toString(),
                    it[FamilyDbSchema.CREATED_BY].toString()
                )
            }.firstOrNull()

    suspend fun updateFamily(familyId: String, newName: String) {
        firestore.collection(FamilyDbSchema.FAMILY_TABLE).document(familyId).get()
            .await().reference.update(mapOf(FamilyDbSchema.NAME to newName))
    }

    fun getFamilyMembers(familyId: String): Flow<List<User>> =
        firestore.collection(UserDbSchema.USER_TABLE).whereEqualTo(UserDbSchema.FAMILY_ID, familyId)
            .snapshots().map {
                val users = mutableListOf<User>()
                for (doc in it.documents) {
                    val user = User(
                        doc.id,
                        doc[UserDbSchema.NAME].toString(),
                        doc[UserDbSchema.BIRTHDAY].toString(),
                        doc[UserDbSchema.FAMILY_ID].toString(),
                        doc[UserDbSchema.EMAIL].toString(),
                        doc.getGeoPoint(UserDbSchema.LOCATION)
                    )
                    users.add(user)
                }
                users
            }

    suspend fun getFamilyMembersOnce(familyId: String): List<User> =
        firestore.collection(UserDbSchema.USER_TABLE).whereEqualTo(UserDbSchema.FAMILY_ID, familyId)
            .get().await().map {
                User(
                    it.id,
                    it[UserDbSchema.NAME].toString(),
                    it[UserDbSchema.BIRTHDAY].toString(),
                    it[UserDbSchema.FAMILY_ID].toString(),
                    it[UserDbSchema.EMAIL].toString()
                )
            }

    fun getApplicationsToFamily(familyId: String): Flow<List<User>> {
        val applications = MutableSharedFlow<List<User>>()
        scope.launch {
            firestore.collection(ApplicationDbSchema.APPLICATION_TABLE)
                .whereEqualTo(ApplicationDbSchema.FAMILY_ID, familyId).snapshots()
                .collect {
                    val newApplications =
                        it.documents.filter { it[ApplicationDbSchema.STATUS].toString() == ApplicationStatus.NEW.name }
                            .map { it[ApplicationDbSchema.USER_ID].toString() }
                    if (newApplications.isEmpty()) {
                        return@collect
                    }
                    firestore.collection(UserDbSchema.USER_TABLE)
                        .whereIn(FieldPath.documentId(), newApplications).get().continueWith {
                            launch {
                                val users = it.result.documents.map {
                                    User(
                                        it.id,
                                        it[UserDbSchema.NAME].toString(),
                                        it[UserDbSchema.BIRTHDAY].toString(),
                                        it[UserDbSchema.FAMILY_ID].toString(),
                                        it[UserDbSchema.EMAIL].toString()
                                    )
                                }
                                applications.emit(users)
                            }
                        }
                }
        }
        return applications
    }

    fun getApplicants(ids: List<String>): Flow<List<User>> {
        val newIds = mutableListOf("1")
        newIds.addAll(ids)
        return firestore.collection(UserDbSchema.USER_TABLE).whereIn(FieldPath.documentId(), newIds)
            .snapshots()
            .map {
                val users = mutableListOf<User>()
                for (doc in it.documents) {
                    val user = User(
                        doc.id,
                        doc[UserDbSchema.NAME].toString(),
                        doc[UserDbSchema.BIRTHDAY].toString(),
                        doc[UserDbSchema.FAMILY_ID].toString(),
                        doc[UserDbSchema.EMAIL].toString()
                    )
                    users.add(user)
                }
                users
            }
    }

    fun approveApplication(userId: String, familyId: String) {
        firestore.collection(ApplicationDbSchema.APPLICATION_TABLE)
            .whereEqualTo(ApplicationDbSchema.USER_ID, userId)
            .whereEqualTo(ApplicationDbSchema.FAMILY_ID, familyId).get()
            .continueWith {
                if (it.result.documents.isNotEmpty()) {
                    for (doc in it.result.documents) {
                        doc.reference.update(ApplicationDbSchema.STATUS, ApplicationStatus.APPROVED)
                    }
                }

                firestore.collection(UserDbSchema.USER_TABLE)
                    .whereEqualTo(FieldPath.documentId(), userId).get()
                    .continueWith {
                        if (!it.result.isEmpty) {
                            for (doc in it.result.documents) {
                                if (doc[UserDbSchema.FAMILY_ID].toString().isEmpty()) {
                                    doc.reference.update(UserDbSchema.FAMILY_ID, familyId)
                                }
                            }
                        }
                    }
            }


    }

    fun rejectApplication(userId: String, familyId: String) {
        firestore.collection(ApplicationDbSchema.APPLICATION_TABLE)
            .whereEqualTo(ApplicationDbSchema.USER_ID, userId)
            .whereEqualTo(ApplicationDbSchema.FAMILY_ID, familyId).get().addOnCompleteListener {
                if (!it.result.isEmpty) {
                    for (doc in it.result) {
                        doc.reference.update(ApplicationDbSchema.STATUS, ApplicationStatus.REJECTED)
                    }
                }
            }
    }

    fun removeMember(userId: String) {
        firestore.collection(UserDbSchema.USER_TABLE).whereEqualTo(FieldPath.documentId(), userId)
            .get()
            .continueWith {
                if (!it.result.isEmpty) {
                    for (doc in it.result.documents) {
                        doc.reference.update(
                            mapOf(
                                UserDbSchema.FAMILY_ID to ""
                            )
                        )
                    }
                }
            }.continueWith {
                eventRepo.removeEventsForUser(userId)
                listRepo.removeListsForUser(userId)
                taskRepo.removeTasksForUser(userId)
            }
    }

    fun getUserById(userId: String): Flow<User> =
        firestore.collection(UserDbSchema.USER_TABLE).whereEqualTo(FieldPath.documentId(), userId)
            .snapshots().map {
                val doc = it.documents[0]
                User(
                    doc.id,
                    doc[UserDbSchema.NAME].toString(),
                    doc[UserDbSchema.BIRTHDAY].toString(),
                    doc[UserDbSchema.FAMILY_ID].toString(),
                    doc[UserDbSchema.EMAIL].toString()
                )
            }

    fun applyToFamily(userId: String, familyId: String) {
        val data = HashMap<String, Any>()
        data[ApplicationDbSchema.USER_ID] = userId
        data[ApplicationDbSchema.FAMILY_ID] = familyId
        data[ApplicationDbSchema.STATUS] = ApplicationStatus.NEW
        firestore.collection(ApplicationDbSchema.APPLICATION_TABLE).add(data)
    }

    fun createFamily(name: String, userId: String): Task<DocumentReference> {
        val data = HashMap<String, Any>()
        data[FamilyDbSchema.NAME] = name
        data[FamilyDbSchema.CREATED_BY] = userId
        return firestore.collection(FamilyDbSchema.FAMILY_TABLE).add(data)
    }

    fun deleteFamily(familyId: String): Task<Void> {
        val task = firestore.collection(FamilyDbSchema.FAMILY_TABLE).document(familyId).delete()
        task.continueWith {
            firestore.collection(UserDbSchema.USER_TABLE)
                .whereEqualTo(UserDbSchema.FAMILY_ID, familyId).get()
                .continueWith {
                    if (!it.result.isEmpty) {
                        for (doc in it.result.documents) {
                            doc.reference.update(
                                mapOf(
                                    UserDbSchema.FAMILY_ID to ""
                                )
                            )
                        }
                    }
                }
            firestore.collection(ApplicationDbSchema.APPLICATION_TABLE)
                .whereEqualTo(ApplicationDbSchema.FAMILY_ID, familyId).get()
                .continueWith {
                    if (!it.result.isEmpty) {
                        for (doc in it.result.documents) {
                            doc.reference.delete()
                        }
                    }
                }
            eventRepo.removeEventsForFamily(familyId)
            listRepo.removeListsForFamily(familyId)
            taskRepo.removeTasksForFamily(familyId)
        }

        return task
    }

    fun getCompletionHistory(userId: String, start: Long, finish: Long): Flow<List<CompletionDto>> {
        val history = MutableSharedFlow<List<CompletionDto>>()
        scope.launch {
            firestore.collection(ObserverDbSchema.OBSERVER_TABLE)
                .whereEqualTo(ObserverDbSchema.USER_ID, userId).snapshots().collect {
                    val observerTaskIds =
                        it.documents.map { observer -> observer[ObserverDbSchema.TASK_ID].toString() }
                    if (observerTaskIds.isEmpty()) {
                        history.emit(listOf())
                    } else {
                        launch {
                            firestore.collection(CompletionDbSchema.COMPLETION_TABLE)
                                .whereGreaterThanOrEqualTo(
                                    CompletionDbSchema.COMPLETION_DATE,
                                    start
                                ).snapshots().collect {
                                    val documents =
                                        it.documents.filter {
                                            it.getLong(
                                                CompletionDbSchema.COMPLETION_DATE
                                            )!! <= finish && observerTaskIds.contains(it[CompletionDbSchema.TASK_ID].toString())
                                        }
                                    val result = documents.map {
                                        val userName =
                                            firestore.collection(UserDbSchema.USER_TABLE)
                                                .document(it[CompletionDbSchema.USER_ID].toString())
                                                .get()
                                                .await()[UserDbSchema.NAME].toString()
                                        CompletionDto(
                                            it[CompletionDbSchema.TASK_ID].toString(),
                                            it[CompletionDbSchema.TASK_NAME].toString(),
                                            it[CompletionDbSchema.USER_ID].toString(),
                                            userName,
                                            it.getLong(CompletionDbSchema.COMPLETION_DATE)!!
                                        )
                                    }
                                    history.emit(result)
                                }
                        }
                    }
                }
        }
        return history
    }
}