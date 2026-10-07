package com.example.feladatok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.feladatok.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).taskDao()

    val sortMode = MutableStateFlow(SortMode.BY_DATE)
    val filterMode = MutableStateFlow(FilterMode.ALL)

    val tasks: StateFlow<List<Task>> = combine(
        dao.getAllTasks(),
        sortMode,
        filterMode
    ) { rawTasks, sort, filter ->
        rawTasks
            .filter { task ->
                when (filter) {
                    FilterMode.ALL -> true
                    FilterMode.PENDING -> !task.isCompleted
                    FilterMode.COMPLETED -> task.isCompleted
                }
            }
            .sortedWith { t1, t2 ->
                when (sort) {
                    SortMode.BY_DATE -> t1.deadlineMillis.compareTo(t2.deadlineMillis)
                    SortMode.BY_PRIORITY_THEN_DATE -> {
                        val priorityComp = t1.priority.orderValue.compareTo(t2.priority.orderValue)
                        if (priorityComp != 0) priorityComp else t1.deadlineMillis.compareTo(t2.deadlineMillis)
                    }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTask(title: String, deadlineMillis: Long, priority: Priority) {
        viewModelScope.launch {
            dao.insertTask(Task(title = title, deadlineMillis = deadlineMillis, priority = priority))
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }
}
