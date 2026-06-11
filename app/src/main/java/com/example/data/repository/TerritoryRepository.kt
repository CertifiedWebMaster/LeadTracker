package com.example.data.repository

import com.example.data.local.TerritoryDao
import com.example.data.model.Territory
import kotlinx.coroutines.flow.Flow

class TerritoryRepository(private val territoryDao: TerritoryDao) {
    val allTerritories: Flow<List<Territory>> = territoryDao.getAllTerritories()

    suspend fun insert(territory: Territory): Long = territoryDao.insertTerritory(territory)

    suspend fun update(territory: Territory) = territoryDao.updateTerritory(territory)

    suspend fun delete(territory: Territory) = territoryDao.deleteTerritory(territory)

    suspend fun getTerritoryById(id: Long): Territory? = territoryDao.getTerritoryById(id)
}
