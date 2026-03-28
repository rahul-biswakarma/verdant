package com.verdant.core.model.repository

import com.verdant.core.model.Label
import kotlinx.coroutines.flow.Flow

interface LabelRepository {
    fun observeAll(): Flow<List<Label>>
    suspend fun getById(id: String): Label?
    suspend fun getAllLabels(): List<Label>
    suspend fun insert(label: Label)
    suspend fun update(label: Label)
    suspend fun delete(label: Label)
    suspend fun deleteAllLabels()
}
