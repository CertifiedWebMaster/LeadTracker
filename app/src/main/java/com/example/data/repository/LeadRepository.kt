package com.example.data.repository

import com.example.data.local.LeadDao
import com.example.data.model.Lead
import kotlinx.coroutines.flow.Flow

class LeadRepository(private val leadDao: LeadDao) {
    val allLeads: Flow<List<Lead>> = leadDao.getAllLeads()

    fun getLeadsByTerritory(territoryId: Long): Flow<List<Lead>> = leadDao.getLeadsByTerritory(territoryId)

    suspend fun getLeadById(id: Long): Lead? = leadDao.getLeadById(id)

    suspend fun getUnsyncedLeads(): List<Lead> = leadDao.getUnsyncedLeads()

    suspend fun insert(lead: Lead): Long = leadDao.insertLead(lead)

    suspend fun update(lead: Lead) = leadDao.updateLead(lead)

    suspend fun delete(lead: Lead) = leadDao.deleteLead(lead)

    suspend fun markAsSynced(ids: List<Long>) = leadDao.markLeadsAsSynced(ids)
}
