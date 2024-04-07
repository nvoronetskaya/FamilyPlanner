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
import kotlinx.coroutines.tasks.await

class GroceryListRepository {
    private val firestore = Firebase.firestore

    suspend fun getListsForUser(userId: String): Flow<List<GroceryList>> {
        val listIds =
            firestore.collection("usersLists").whereEqualTo("userId", userId).get().await()
                .map { it["listId"].toString() }
        return firestore.collection("lists").whereIn(FieldPath.documentId(), listIds).snapshots()
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
                queryLists
            }
    }

    fun getListById(listId: String): Flow<GroceryList?> {
        return firestore.collection("lists").whereEqualTo(FieldPath.documentId(), listId)
            .snapshots().map {
                if (it.isEmpty) {
                    null
                } else {
                    val document = it.documents[0]
                    val list = GroceryList(
                        listId,
                        document["name"].toString(),
                        document.getBoolean("isCompleted") ?: false,
                        document["createdBy"].toString()
                    )
                    list
                }
            }
    }

    fun getProductsForList(listId: String): Flow<List<Product>> {
        return firestore.collection("products").whereEqualTo("listId", listId).snapshots().map {
            val dbProducts = it.documents.map { product ->
                Product(
                    product.id,
                    product["name"].toString(),
                    product.getBoolean("isPurchased") ?: false
                )
            }
            dbProducts
        }
    }

    suspend fun getObserversForList(listId: String): Flow<List<ListObserver>> {
        val usersId =
            firestore.collection("usersLists").whereEqualTo("listId", listId).get().await()
                .map { it["userId"].toString() }
        return firestore.collection("users").whereIn(FieldPath.documentId(), usersId)
            .snapshots().map { users ->
                val dbObservers = mutableListOf<ListObserver>()
                for (user in users.documents) {
                    dbObservers.add(ListObserver(user.id, user["name"].toString()))
                }
                dbObservers
            }
    }

    suspend fun getNonObservers(listId: String, familyId: String): Flow<List<NonObserver>> {
        val observersId =
            firestore.collection("usersLists").whereEqualTo("listId", listId).get().await()
                .map { it.id }
        return firestore.collection("users").whereEqualTo("familyId", familyId)
            .whereNotIn(FieldPath.documentId(), observersId).snapshots().map {
                val nonObservers = it.documents.map { doc ->
                    NonObserver(doc.id, doc["name"].toString(), false)
                }
                nonObservers
            }
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

    fun addList(name: String, createdBy: String) {
        val listData =
            mapOf<String, Any>("name" to name, "createdBy" to createdBy, "isCompleted" to false)
        firestore.collection("lists").add(listData).addOnSuccessListener {
            firestore.collection("usersLists").add(mapOf("userId" to createdBy, "listId" to it.id))
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