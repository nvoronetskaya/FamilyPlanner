package com.familyplanner.lists.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.events.data.Invitation
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.lists.repository.GroceryListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewListViewModel : ViewModel() {
    private val familyRepo = FamilyRepository()
    private val listRepo = GroceryListRepository()
    private val members = mutableListOf<Invitation>()
    private val userId = FamilyPlanner.userId
    private var familyId: String = ""

    suspend fun getFamilyMembers(familyId: String): List<Invitation> {
        if (members.isEmpty() || this.familyId != familyId) {
            members.clear()
            members.addAll(
                familyRepo.getFamilyMembersOnce(familyId)
                    .map { Invitation(it.id, it.name, it.birthday, true) })
        }
        this.familyId = familyId
        return members
    }

    fun createList(name: String, invitations: List<Invitation>) {
        viewModelScope.launch(Dispatchers.IO) {
            listRepo.addList(name, userId, familyId, invitations)
        }
    }
}