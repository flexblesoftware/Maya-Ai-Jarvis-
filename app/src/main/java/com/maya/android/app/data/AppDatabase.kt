package com.maya.android.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.maya.android.app.data.dao.ActionLogDao
import com.maya.android.app.data.dao.MemoryDao
import com.maya.android.app.data.dao.ReminderDao
import com.maya.android.app.data.model.ActionLogEntity
import com.maya.android.app.data.model.MemoryEntity
import com.maya.android.app.data.model.ReminderEntity

@Database(
    entities = [
        MemoryEntity::class,
        ReminderEntity::class,
        ActionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun actionLogDao(): ActionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "maya_local_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
