package com.maya.android.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeMillis: Long,
    val isCompleted: Boolean = false,
    val requestCode: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
    val createdAt: Long = System.currentTimeMillis()
)
