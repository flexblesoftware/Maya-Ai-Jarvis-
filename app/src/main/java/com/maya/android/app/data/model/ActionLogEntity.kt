package com.maya.android.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolName: String,
    val parametersJson: String,
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)
