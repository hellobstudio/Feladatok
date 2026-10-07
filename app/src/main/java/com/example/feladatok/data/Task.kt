package com.example.feladatok.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class Priority(val label: String, val orderValue: Int) {
    URGENT("Sürgős", 1),
    NORMAL("Normál", 2),
    LOW("Ráér", 3)
}

enum class SortMode {
    BY_DATE,
    BY_PRIORITY_THEN_DATE
}

enum class FilterMode {
    ALL,
    PENDING,
    COMPLETED
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val deadlineMillis: Long,
    val priority: Priority,
    val isCompleted: Boolean = false
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "feladatok_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
