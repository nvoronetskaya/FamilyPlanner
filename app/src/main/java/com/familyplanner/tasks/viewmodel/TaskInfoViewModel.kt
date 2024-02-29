package com.familyplanner.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyplanner.tasks.model.Comment
import com.familyplanner.tasks.model.Observer
import com.familyplanner.tasks.model.Task
import com.familyplanner.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TaskInfoViewModel : ViewModel() {
    private var task: MutableSharedFlow<Task> = MutableSharedFlow(replay = 1)
    private var comments: MutableSharedFlow<List<Comment>> = MutableSharedFlow(replay = 1)
    private var observers: MutableSharedFlow<List<Observer>> = MutableSharedFlow(replay = 1)
    private var executors: MutableSharedFlow<List<Observer>> = MutableSharedFlow(replay = 1)
    private val repo = TaskRepository()
    private var subTasks: MutableSharedFlow<List<Task>> = MutableSharedFlow(replay = 1)

    fun setTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getTaskById(taskId).collect {
                task.emit(it)
            }

            repo.getTaskComments(taskId).collect {
                comments.emit(it)
            }

            repo.getTaskObservers(taskId).collect {
                TODO()
            }

            repo.getSubtasks(taskId).collect {

            }
        }
    }

    fun getTask(): Flow<Task> = task

    fun getComments(): Flow<List<Comment>> = comments

    fun getObservers(): Flow<List<Observer>> = observers

    fun getSubtasks(): Flow<List<Task>> = subTasks
}