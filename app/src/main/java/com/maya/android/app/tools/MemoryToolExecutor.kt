package com.maya.android.app.tools

import com.maya.android.app.data.dao.MemoryDao
import com.maya.android.app.data.model.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryToolExecutor(private val memoryDao: MemoryDao) {

    suspend fun saveMemory(fact: String, category: String = "general"): String = withContext(Dispatchers.IO) {
        if (fact.isBlank()) return@withContext "Cannot save empty memory fact."
        val id = memoryDao.insertMemory(
            MemoryEntity(
                fact = fact.trim(),
                category = category.trim().ifBlank { "general" }
            )
        )
        "Saved to memory (ID: $id): \"$fact\""
    }

    suspend fun recallMemory(query: String): String = withContext(Dispatchers.IO) {
        val results = if (query.isBlank()) {
            memoryDao.getAllMemories()
        } else {
            memoryDao.searchMemories(query.trim())
        }
        if (results.isEmpty()) {
            "No matching memories found for \"$query\"."
        } else {
            val list = results.take(8).mapIndexed { i, m -> "${i + 1}. [${m.category}] ${m.fact} (ID: ${m.id})" }
            "Memories recalled:\n" + list.joinToString("\n")
        }
    }

    suspend fun forgetMemory(query: String): String = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext "Please specify what memory or ID to forget."
        val id = query.toLongOrNull()
        if (id != null) {
            memoryDao.deleteMemoryById(id)
            return@withContext "Memory ID $id forgotten."
        }
        val matches = memoryDao.searchMemories(query)
        if (matches.isEmpty()) {
            "No memory found matching \"$query\"."
        } else {
            for (m in matches) {
                memoryDao.deleteMemory(m)
            }
            "Removed ${matches.size} memories matching \"$query\"."
        }
    }
}
