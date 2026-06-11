package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Territory
import kotlinx.coroutines.flow.Flow

@Dao
interface TerritoryDao {
    @Query("SELECT * FROM territories ORDER BY createdAt DESC")
    fun getAllTerritories(): Flow<List<Territory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritory(territory: Territory): Long

    @Update
    suspend fun updateTerritory(territory: Territory)

    @Delete
    suspend fun deleteTerritory(territory: Territory)

    @Query("SELECT * FROM territories WHERE id = :id")
    suspend fun getTerritoryById(id: Long): Territory?
}
