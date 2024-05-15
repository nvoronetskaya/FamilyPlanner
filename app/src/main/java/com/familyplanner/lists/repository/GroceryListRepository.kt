package com.familyplanner.lists.repository

import com.familyplanner.common.schema.ListDbSchema
import com.familyplanner.common.schema.ProductDbSchema
import com.familyplanner.common.schema.SpendingDbSchema
import com.familyplanner.common.schema.UserDbSchema
import com.familyplanner.common.schema.UserListDbSchema
import com.familyplanner.events.data.Invitation
import com.familyplanner.lists.data.BudgetDto
import com.familyplanner.lists.data.GroceryList
import com.familyplanner.lists.data.ListObserver
import com.familyplanner.lists.data.NonObserver
import com.familyplanner.lists.data.Product
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class GroceryListRepository {
    private val firestore = Firebase.firestore
    private val userLists = MutableSharedFlow<List<GroceryList>>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getListsForUser(userId: String): Flow<List<GroceryList>> {
        scope.launch {
            firestore.collection(UserListDbSchema.USER_LIST_TABLE)
                .whereEqualTo(UserListDbSchema.USER_ID, userId).snapshots().collect {
                    val listIds =
                        it.documents.map { it[UserListDbSchema.LIST_ID].toString() }.toMutableList()
                    if (listIds.isEmpty()) {
                        userLists.emit(listOf())
                        return@collect
                    }
                    launch {
                        firestore.collection(ListDbSchema.LIST_TABLE)
                            .whereIn(FieldPath.documentId(), listIds)
                            .snapshots()
                            .collect { result ->
                                val lists = mutableListOf<GroceryList>()
                                for (doc in result.documents) {
                                    val list = GroceryList(
                                        doc.id,
                                        doc[ListDbSchema.NAME].toString(),
                                        doc.getBoolean(ListDbSchema.IS_COMPLETED) ?: false,
                                        doc[ListDbSchema.CREATED_BY].toString()
                                    )
                                    lists.add(list)
                                }
                                userLists.emit(lists)
                            }
                    }
                }
        }
        return userLists
    }

    fun getNotCompletedListsForUser(userId: String): Flow<List<GroceryList>> {
        scope.launch {
            firestore.collection(UserListDbSchema.USER_LIST_TABLE)
                .whereEqualTo(UserListDbSchema.USER_ID, userId).snapshots().collect {
                    val listIds =
                        it.documents.map { it[UserListDbSchema.LIST_ID].toString() }.toMutableList()
                    if (listIds.isEmpty()) {
                        userLists.emit(listOf())
                        return@collect
                    }
                    launch {
                        firestore.collection(ListDbSchema.LIST_TABLE)
                            .whereIn(FieldPath.documentId(), listIds)
                            .snapshots()
                            .collect { result ->
                                val lists = mutableListOf<GroceryList>()
                                val filteredDocs =
                                    result.documents.filter { it.getBoolean(ListDbSchema.IS_COMPLETED) == false }
                                for (doc in filteredDocs) {
                                    val list = GroceryList(
                                        doc.id,
                                        doc[ListDbSchema.NAME].toString(),
                                        false,
                                        doc[ListDbSchema.CREATED_BY].toString()
                                    )
                                    lists.add(list)
                                }
                                userLists.emit(lists)
                            }
                    }
                }
        }
        return userLists
    }

    fun getListById(listId: String): Flow<GroceryList?> {
        return firestore.collection(ListDbSchema.LIST_TABLE)
            .whereEqualTo(FieldPath.documentId(), listId)
            .snapshots().map {
                if (it.isEmpty) {
                    null
                } else {
                    val document = it.documents[0]
                    val list = GroceryList(
                        listId,
                        document[ListDbSchema.NAME].toString(),
                        document.getBoolean(ListDbSchema.IS_COMPLETED) ?: false,
                        document[ListDbSchema.CREATED_BY].toString()
                    )
                    list
                }
            }
    }

    fun getProductsForList(listId: String): Flow<List<Product>> {
        return firestore.collection(ProductDbSchema.PRODUCT_TABLE)
            .whereEqualTo(ProductDbSchema.LIST_ID, listId).snapshots().map {
                val dbProducts = it.documents.map { product ->
                    Product(
                        product.id,
                        product[ProductDbSchema.NAME].toString(),
                        product.getBoolean(ProductDbSchema.IS_PURCHASED) ?: false
                    )
                }
                dbProducts
            }
    }

    suspend fun getObserversForList(listId: String): Flow<List<ListObserver>> {
        val usersId =
            firestore.collection(UserListDbSchema.USER_LIST_TABLE)
                .whereEqualTo(UserListDbSchema.LIST_ID, listId).get().await()
                .map { it[UserListDbSchema.USER_ID].toString() }.toMutableList()
        usersId.add("1")
        return firestore.collection(UserDbSchema.USER_TABLE)
            .whereIn(FieldPath.documentId(), usersId)
            .snapshots().map { users ->
                val dbObservers = mutableListOf<ListObserver>()
                for (user in users.documents) {
                    dbObservers.add(ListObserver(user.id, user[UserDbSchema.NAME].toString()))
                }
                dbObservers
            }
    }

    suspend fun getNonObservers(listId: String, familyId: String): Flow<List<NonObserver>> {
        val observersId =
            firestore.collection(UserListDbSchema.USER_LIST_TABLE)
                .whereEqualTo(UserListDbSchema.LIST_ID, listId).get().await()
                .map { it.id }
        return firestore.collection(UserDbSchema.USER_TABLE)
            .whereEqualTo(UserDbSchema.FAMILY_ID, familyId)
            .whereNotIn(FieldPath.documentId(), observersId).snapshots().map {
                val nonObservers = it.documents.map { doc ->
                    NonObserver(doc.id, doc[UserDbSchema.NAME].toString(), false)
                }
                nonObservers
            }
    }

    suspend fun addProduct(productName: String, listId: String) {
        val product =
            mapOf<String, Any>(
                ProductDbSchema.NAME to productName,
                ProductDbSchema.LIST_ID to listId,
                ProductDbSchema.IS_PURCHASED to false
            )
        firestore.collection(ProductDbSchema.PRODUCT_TABLE).add(product).await()
        changeListCompleted(listId)
    }

    suspend fun changeProductPurchased(productId: String, isPurchased: Boolean, listId: String) {
        firestore.collection(ProductDbSchema.PRODUCT_TABLE).document(productId)
            .update(ProductDbSchema.IS_PURCHASED, isPurchased)
            .await()
        changeListCompleted(listId)
    }

    suspend fun deleteProduct(id: String, listId: String) {
        firestore.collection(ProductDbSchema.PRODUCT_TABLE).document(id).delete().await()
        changeListCompleted(listId)
    }

    fun addObservers(observersId: List<String>, listId: String) {
        for (id in observersId) {
            val data = mapOf(UserListDbSchema.USER_ID to id, UserListDbSchema.LIST_ID to listId)
            firestore.collection(UserListDbSchema.USER_LIST_TABLE).add(data)
        }
    }

    fun deleteObserver(observerId: String, listId: String) {
        firestore.collection(UserListDbSchema.USER_LIST_TABLE)
            .whereEqualTo(UserListDbSchema.LIST_ID, listId)
            .whereEqualTo(UserListDbSchema.USER_ID, observerId).get().continueWith {
                for (doc in it.result.documents) {
                    doc.reference.delete()
                }
            }
    }

    fun addList(name: String, createdBy: String, familyId: String, members: List<Invitation>) {
        val listData =
            mapOf<String, Any>(
                ListDbSchema.NAME to name,
                ListDbSchema.CREATED_BY to createdBy,
                ListDbSchema.IS_COMPLETED to false,
                ListDbSchema.FAMILY_ID to familyId
            )
        val listId = UUID.randomUUID().toString()
        firestore.collection(ListDbSchema.LIST_TABLE).document(listId).set(listData)
        members.forEach {
            firestore.collection(UserListDbSchema.USER_LIST_TABLE)
                .add(
                    mapOf(
                        UserListDbSchema.USER_ID to it.userId,
                        UserListDbSchema.LIST_ID to listId
                    )
                )
        }
    }

    private suspend fun changeListCompleted(listId: String) {
        val products = firestore.collection(ProductDbSchema.PRODUCT_TABLE)
            .whereEqualTo(ProductDbSchema.LIST_ID, listId).get().await()
        val isCompleted =
            products.documents.all { it.getBoolean(ProductDbSchema.IS_PURCHASED) ?: true }
        changeListCompleted(listId, isCompleted)
    }

    fun changeListCompleted(listId: String, isCompleted: Boolean) {
        firestore.collection(ListDbSchema.LIST_TABLE).document(listId)
            .update(ListDbSchema.IS_COMPLETED, isCompleted)
        if (isCompleted) {
            firestore.collection(ProductDbSchema.PRODUCT_TABLE)
                .whereEqualTo(ProductDbSchema.LIST_ID, listId).get()
                .continueWith {
                    for (doc in it.result.documents) {
                        doc.reference.update(ProductDbSchema.IS_PURCHASED, true)
                    }
                }
        }
    }

    fun deleteList(listId: String) {
        firestore.collection(ListDbSchema.LIST_TABLE).document(listId).delete().continueWith {
            firestore.collection(ProductDbSchema.PRODUCT_TABLE)
                .whereEqualTo(ProductDbSchema.LIST_ID, listId).get()
                .continueWith {
                    for (doc in it.result.documents) {
                        doc.reference.delete()
                    }
                }
            firestore.collection(UserListDbSchema.USER_LIST_TABLE)
                .whereEqualTo(UserListDbSchema.LIST_ID, listId).get().continueWith {
                    it.result.documents.forEach { it.reference.delete() }
                }
            firestore.collection(SpendingDbSchema.SPENDING_TABLE)
                .whereEqualTo(SpendingDbSchema.LIST_ID, listId).get().continueWith {
                    it.result.documents.forEach { it.reference.delete() }
                }
        }
    }

    fun changeListName(listId: String, newName: String) {
        firestore.collection(ListDbSchema.LIST_TABLE).document(listId)
            .update(ListDbSchema.NAME, newName)
    }

    fun addSpending(userId: String, listId: String, value: Double) {
        val addedAt =
            LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
                .toInstant().epochSecond
        firestore.collection(SpendingDbSchema.SPENDING_TABLE).add(
            mapOf(
                SpendingDbSchema.USER_ID to userId,
                SpendingDbSchema.LIST_ID to listId,
                SpendingDbSchema.SUM_SPENT to value,
                SpendingDbSchema.ADDED_AT to addedAt
            )
        )
    }

    fun getSpendingUpdates(familyId: String, start: Long, finish: Long): Flow<List<BudgetDto>> {
        val result = MutableSharedFlow<List<BudgetDto>>()
        scope.launch {
            firestore.collection(ListDbSchema.LIST_TABLE)
                .whereEqualTo(ListDbSchema.FAMILY_ID, familyId).snapshots().collect {
                    val listIds = it.documents.map { doc -> doc.id }
                    if (listIds.isEmpty()) {
                        result.emit(listOf())
                    } else {
                        launch {
                            firestore.collection(SpendingDbSchema.SPENDING_TABLE)
                                .whereIn(SpendingDbSchema.LIST_ID, listIds).snapshots().collect {
                                    val budgetObjects = mutableListOf<BudgetDto>()
                                    for (spending in it.documents) {
                                        val userId = spending[SpendingDbSchema.USER_ID].toString()
                                        val addedAt = LocalDateTime.ofInstant(
                                            Instant.ofEpochSecond(
                                                spending.getLong(SpendingDbSchema.ADDED_AT) ?: 0
                                            ),
                                            ZoneId.systemDefault()
                                        )
                                        if (!(addedAt.toLocalDate()
                                                .toEpochDay() in start..finish)
                                        ) {
                                            continue
                                        }
                                        val userName =
                                            firestore.collection(UserDbSchema.USER_TABLE)
                                                .document(userId).get().await()
                                                .get(UserDbSchema.NAME)?.toString()
                                                ?: ""
                                        val listName =
                                            firestore.collection(ListDbSchema.LIST_TABLE)
                                                .document(spending[SpendingDbSchema.LIST_ID].toString())
                                                .get()
                                                .await()[ListDbSchema.NAME].toString()
                                        budgetObjects.add(
                                            BudgetDto(
                                                addedAt,
                                                listName,
                                                spending[SpendingDbSchema.LIST_ID].toString(),
                                                userName,
                                                spending.getDouble(SpendingDbSchema.SUM_SPENT)
                                                    ?: 0.0
                                            )
                                        )
                                    }
                                    result.emit(budgetObjects)
                                }
                        }
                    }
                }
        }
        return result
    }

    suspend fun getSpendingOnce(familyId: String): List<BudgetDto> {
        val listIds = firestore.collection(ListDbSchema.LIST_TABLE)
            .whereEqualTo(ListDbSchema.FAMILY_ID, familyId).get().await()
            .map { it.id }
        if (listIds.isEmpty()) {
            return listOf()
        }
        val result = mutableListOf<BudgetDto>()
        val documents = firestore.collection(SpendingDbSchema.SPENDING_TABLE)
            .whereIn(SpendingDbSchema.LIST_ID, listIds).get().await()
        for (spending in documents) {
            val userId = spending[SpendingDbSchema.USER_ID].toString()
            val userName =
                firestore.collection(UserDbSchema.USER_TABLE).document(userId).get().await()
                    .get(UserDbSchema.NAME)?.toString()
                    ?: ""
            val addedAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(spending.getLong(SpendingDbSchema.ADDED_AT) ?: 0),
                ZoneId.systemDefault()
            )
            val listName =
                firestore.collection(ListDbSchema.LIST_TABLE)
                    .document(spending[SpendingDbSchema.LIST_ID].toString()).get()
                    .await()[ListDbSchema.NAME].toString()
            result.add(
                BudgetDto(
                    addedAt,
                    listName,
                    spending[SpendingDbSchema.LIST_ID].toString(),
                    userName,
                    spending.getDouble(SpendingDbSchema.SUM_SPENT) ?: 0.0
                )
            )
        }
        return result
    }

    suspend fun getListSpendingUpdates(
        listId: String,
        start: Long,
        finish: Long
    ): Flow<List<BudgetDto>> {
        val result = MutableSharedFlow<List<BudgetDto>>()
        scope.launch {
            firestore.collection(SpendingDbSchema.SPENDING_TABLE)
                .whereEqualTo(SpendingDbSchema.LIST_ID, listId).snapshots().collect {
                    val spendings = mutableListOf<BudgetDto>()
                    for (spending in it.documents) {
                        val userId = spending[SpendingDbSchema.USER_ID].toString()
                        val userName =
                            firestore.collection(UserDbSchema.USER_TABLE).document(userId).get()
                                .await()
                                .get(UserDbSchema.NAME)?.toString()
                                ?: ""
                        val addedAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(
                                spending.getLong(SpendingDbSchema.ADDED_AT) ?: 0
                            ),
                            ZoneId.systemDefault()
                        )
                        if (!(addedAt.toLocalDate()
                                .toEpochDay() in start..finish)
                        ) {
                            continue
                        }
                        spendings.add(
                            BudgetDto(
                                addedAt,
                                null,
                                listId,
                                userName,
                                spending.getDouble(SpendingDbSchema.SUM_SPENT) ?: 0.0
                            )
                        )
                    }
                    result.emit(spendings)
                }
        }
        return result
    }

    fun removeListsForUser(userId: String) {
        firestore.collection(UserListDbSchema.USER_LIST_TABLE)
            .whereEqualTo(UserListDbSchema.USER_ID, userId).get().continueWith {
                it.result.documents.forEach { it.reference.delete() }
            }
        firestore.collection(SpendingDbSchema.SPENDING_TABLE)
            .whereEqualTo(SpendingDbSchema.USER_ID, userId).get().continueWith {
                it.result.documents.forEach { it.reference.delete() }
            }
    }

    fun removeListsForFamily(familyId: String) {
        firestore.collection(ListDbSchema.LIST_TABLE)
            .whereEqualTo(ListDbSchema.FAMILY_ID, familyId).get().continueWith {
                it.result.documents.map { it.id }.forEach { deleteList(it) }
            }
    }
}