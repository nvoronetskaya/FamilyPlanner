package com.familyplanner.lists.repository

import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.model.ListObserver
import com.familyplanner.lists.model.Product
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

class GroceryListRepository {
    private val firestore = Firebase.firestore

    fun getListsForUser(userId: String): Flow<List<GroceryList>> {
        val lists = MutableSharedFlow<List<GroceryList>>(replay = 1)
        firestore.collection("usersLists").whereEqualTo("userId", userId).snapshots().map {
            var counter = 0
            val listIds = it.map { list -> list.id }
            firestore.collection("lists").whereIn(FieldPath.documentId(), listIds).snapshots()
                .map { result ->
                    val queryLists = mutableListOf<GroceryList>()
                    for (doc in result.documents) {
                        val list = GroceryList(
                            doc.id,
                            doc["name"].toString(),
                            doc.getBoolean("isCompleted") ?: false,
                            doc["createdBy"].toString()
                        )
                        queryLists.add(list)
                    }
                    lists.emit(queryLists)
                }
        }
        return lists
    }

    fun getListById(listId: String): Flow<GroceryList?> {
        val result = MutableSharedFlow<GroceryList?>()
        firestore.collection("lists").whereEqualTo(FieldPath.documentId(), listId).snapshots().map {
            if (it.isEmpty) {
                result.emit(null)
            } else {
                val document = it.documents[0]
                val list = GroceryList(
                    listId,
                    document["name"].toString(),
                    document.getBoolean("isCompleted") ?: false,
                    document["createdBy"].toString()
                )
                result.emit(list)
            }
        }
        return result
    }

    fun getProductsForList(listId: String): Flow<List<Product>> {
        val products = MutableSharedFlow<List<Product>>()
        firestore.collection("products").whereEqualTo("listId", listId).snapshots().map {
            val dbProducts = it.documents.map { product ->
                Product(
                    product.id,
                    product["name"].toString(),
                    product.getBoolean("isPurchased") ?: false
                )
            }
            products.emit(dbProducts)
        }
        return products
    }

    fun getObserversForList(listId: String): Flow<List<ListObserver>> {
        val observers = MutableSharedFlow<List<ListObserver>>(replay = 1)
        firestore.collection("usersLists").whereEqualTo("listId", listId).snapshots().map {
            val usersId = it.documents.map { user -> user["userId"].toString() }
            firestore.collection("users").whereIn(FieldPath.documentId(), usersId)
                .snapshots().map { users ->
                    val dbObservers = mutableListOf<ListObserver>()
                    for (user in users.documents) {
                        dbObservers.add(ListObserver(user.id, user["name"].toString()))
                    }
                    observers.emit(dbObservers)
                }
        }
        return observers
    }
}