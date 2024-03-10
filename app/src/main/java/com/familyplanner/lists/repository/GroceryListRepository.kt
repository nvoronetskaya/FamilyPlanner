package com.familyplanner.lists.repository

import com.familyplanner.lists.model.GroceryList
import com.familyplanner.lists.model.ListObserver
import com.familyplanner.lists.model.NonObserver
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

    fun getNonObservers(listId: String, familyId: String): Flow<List<NonObserver>> {
        val result = MutableSharedFlow<List<NonObserver>>()
        firestore.collection("usersLists").whereEqualTo("listId", listId).snapshots().map {
            val observersIds = it.documents.map { doc -> doc.id }
            firestore.collection("users").whereEqualTo("familyId", familyId)
                .whereNotIn(FieldPath.documentId(), observersIds).snapshots().map {
                    val nonObservers = it.documents.map { doc ->
                        NonObserver(doc.id, doc["name"].toString(), false)
                    }
                    result.emit(nonObservers)
                }
        }
        return result
    }

    fun addProduct(productName: String, listId: String) {
        val product =
            mapOf<String, Any>("name" to productName, "listId" to listId, "isPurchased" to false)
        firestore.collection("products").add(product)
    }

    fun changeProductPurchased(productId: String, isPurchased: Boolean) {
        firestore.collection("products").document(productId).update("isPurchased", isPurchased)
    }

    fun deleteProduct(id: String) {
        firestore.collection("products").document(id).delete()
    }

    fun addObservers(observersId: List<String>, listId: String) {
        for (id in observersId) {
            val data = mapOf("userId" to id, "listId" to listId)
            firestore.collection("usersLists").add(data)
        }
    }

    fun deleteObserver(observerId: String, listId: String) {
        firestore.collection("usersLists").whereEqualTo("listId", listId)
            .whereEqualTo("userId", observerId).get().addOnCompleteListener {
                for (doc in it.result.documents) {
                    doc.reference.delete()
                }
            }
    }

    fun addList(name: String, createdBy: String, observersId: List<String>) {
        val listData =
            mapOf<String, Any>("name" to name, "createdBy" to createdBy, "isCompleted" to false)
        firestore.collection("lists").add(listData).addOnCompleteListener {
            val listId = it.result.id
            for (id in observersId) {
                val observerData = mapOf("listId" to listId, "userId" to id)
                firestore.collection("usersLists").add(observerData)
            }
            firestore.collection("usersLists")
                .add(mapOf("listId" to listId, "userId" to createdBy))
        }
    }

    fun changeListCompleted(listId: String, isCompleted: Boolean) {
        firestore.collection("lists").document(listId).update("isCompleted", isCompleted)
        firestore.collection("products").whereEqualTo("listId", listId).get()
            .addOnCompleteListener {
                for (doc in it.result.documents) {
                    doc.reference.update("isPurchased", true)
                }
            }
    }

    fun deleteList(listId: String) {
        firestore.collection("products").whereEqualTo("listId", listId).get()
            .addOnCompleteListener {
                for (doc in it.result.documents) {
                    doc.reference.delete()
                }
            }
        firestore.collection("lists").document(listId).delete()
    }

    fun changeListName(listId: String, newName: String) {
        firestore.collection("lists").document(listId).update("name", newName)
    }
}