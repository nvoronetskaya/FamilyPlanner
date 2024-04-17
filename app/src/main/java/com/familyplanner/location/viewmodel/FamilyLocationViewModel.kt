package com.familyplanner.location.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.data.UserRepository
import com.familyplanner.common.User
import com.familyplanner.family.data.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FamilyLocationViewModel : ViewModel() {
    private val userId = FamilyPlanner.userId
    private val userRepo = UserRepository()
    private val familyRepo = FamilyRepository()
    private val users = MutableSharedFlow<List<User>>(replay = 1)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val familyId = userRepo.getUserByIdOnce(userId).familyId
            familyRepo.getFamilyMembers(familyId ?: "").collect {
                users.emit(it)
            }
        }
    }

    fun getFamilyMembers(): Flow<List<User>> = users
}