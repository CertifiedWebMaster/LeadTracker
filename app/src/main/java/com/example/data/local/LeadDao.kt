package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Lead
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY updatedAt DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE territoryId = :territoryId ORDER BY updatedAt DESC")
    fun getLeadsByTerritory(territoryId: Long): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun getLeadById(id: Long): Lead?

    @Query("SELECT * FROM leads WHERE isSynced = 0")
    suspend fun getUnsyncedLeads(): List<Lead>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    @Query("UPDATE leads SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markLeadsAsSynced(ids: List<Long>)
}
