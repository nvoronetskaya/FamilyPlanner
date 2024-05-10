package com.familyplanner.location.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.FamilyPlanner
import com.familyplanner.auth.repository.UserRepository
import com.familyplanner.family.repository.FamilyRepository
import com.familyplanner.location.data.UserLocationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FamilyLocationViewModel : ViewModel() {
    private val userId = FamilyPlanner.userId
    private val userRepo = UserRepository()
    private val familyRepo = FamilyRepository()
    private val users = MutableSharedFlow<List<UserLocationDto>>(replay = 1)

    init {
        setLocationUpdates()
    }

    private fun setLocationUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            val familyId = userRepo.getUserByIdOnce(userId).familyId
            familyRepo.getFamilyMembers(familyId ?: "").collect {
                users.emit(it.filter { it.location != null }.map { user ->
                    UserLocationDto(
                        user.id,
                        user.name,
                        user.location!!.latitude,
                        user.location.longitude
                    )
                })
            }
        }
    }

    fun getFamilyMembers(): Flow<List<UserLocationDto>> = users
}