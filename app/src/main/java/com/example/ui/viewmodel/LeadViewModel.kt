package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Lead
import com.example.data.model.SyncLog
import com.example.data.model.Territory
import com.example.data.repository.LeadRepository
import com.example.data.repository.SyncLogRepository
import com.example.data.repository.TerritoryRepository
import com.example.data.api.SyncService
import com.example.data.api.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class LeadViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val leadRepository = LeadRepository(database.leadDao())
    private val territoryRepository = TerritoryRepository(database.territoryDao())
    private val syncLogRepository = SyncLogRepository(database.syncLogDao())
    
    private val syncService = SyncService()
    private val geminiService = GeminiService()

    // Protected reactive states
    val territories: StateFlow<List<Territory>> = territoryRepository.allTerritories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = syncLogRepository.allSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLeads: StateFlow<List<Lead>> = leadRepository.allLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTerritoryId = MutableStateFlow<Long?>(null)

    val filteredLeads: StateFlow<List<Lead>> = combine(allLeads, selectedTerritoryId) { leads, territoryId ->
        if (territoryId == null) {
            leads
        } else {
            leads.filter { it.territoryId == territoryId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI state tracking
    val generatedPitch = MutableStateFlow<String?>(null)
    val isGeneratingPitch = MutableStateFlow(false)

    // Syncing state tracking
    val isSyncing = MutableStateFlow(false)

    init {
        // Pre-populate some territories & door pins on first load so the map works immediately without manual typings
        viewModelScope.launch {
            territories.take(1).collect { existingTerritories ->
                if (existingTerritories.isEmpty()) {
                    createDefaultTerritoriesAndLeads()
                }
            }
        }
    }

    private suspend fun createDefaultTerritoriesAndLeads() {
        val t1Id = territoryRepository.insert(
            Territory(
                name = "Miami Coastline",
                description = "Coastal dynamic solar sector, highly qualified multi-family/single homes.",
                colorHex = "#2E7D32" // Forest Green
            )
        )
        val t2Id = territoryRepository.insert(
            Territory(
                name = "Tampa Bay Area",
                description = "D2D target zone near Tampa Bay beach parcels.",
                colorHex = "#1565C0" // Blue
            )
        )

        // Pre-populate realistic Florida lead pin details with real-world lat/long coordinates (translated to Canvas)
        leadRepository.insert(
            Lead(
                name = "Jane & Mark Cooper",
                address = "342 Ocean Drive, Miami FL",
                status = "WARM_LEAD",
                latitude = 25.7711,
                longitude = -80.1303,
                notes = "Interested in solar panel estimate. High electric bills last winter.",
                territoryId = t1Id,
                isSynced = true
            )
        )
        leadRepository.insert(
            Lead(
                name = "Michael Vance",
                address = "1412 Sun & Beach Ave, Tampa FL",
                status = "GO_BACK",
                latitude = 27.9506,
                longitude = -82.4572,
                notes = "Requested callback on Tuesday evening around 7 PM.",
                territoryId = t2Id,
                isSynced = false
            )
        )
        leadRepository.insert(
            Lead(
                name = "Homeowner Unidentified",
                address = "705 Citrus Grove Way, Orlando FL",
                status = "NOT_HOME",
                latitude = 28.5383,
                longitude = -81.3792,
                notes = "Left pamphlet with solar incentives on front porch gate.",
                territoryId = t1Id,
                isSynced = false
            )
        )
        leadRepository.insert(
            Lead(
                name = "Arthur Pendelton",
                address = "890 Palm Blvd, Key West FL",
                status = "CUSTOMER",
                latitude = 24.5551,
                longitude = -81.7800,
                notes = "Contract successfully signed. Installation scheduled on June 28th.",
                territoryId = t1Id,
                isSynced = true
            )
        )
        leadRepository.insert(
            Lead(
                name = "Richard Smith",
                address = "1024 Broward Blvd, Fort Lauderdale FL",
                status = "REFUSED",
                latitude = 26.1224,
                longitude = -80.1373,
                notes = "Refused proposal. Mentioned having home solar warranty issues previously.",
                territoryId = t2Id,
                isSynced = true
            )
        )
    }

    // Lead Operations
    fun addLead(
        name: String,
        address: String,
        status: String,
        latitude: Double,
        longitude: Double,
        notes: String,
        phone: String = "",
        territoryId: Long?
    ) {
        viewModelScope.launch {
            val lead = Lead(
                name = name.ifBlank { "Unidentified Owner" },
                address = address.ifBlank { "Custom Location Pin" },
                status = status,
                latitude = latitude,
                longitude = longitude,
                notes = notes,
                phone = phone,
                territoryId = territoryId,
                isSynced = false
            )
            leadRepository.insert(lead)
        }
    }

    fun updateLead(lead: Lead) {
        viewModelScope.launch {
            val updated = lead.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            leadRepository.update(updated)
        }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            leadRepository.delete(lead)
        }
    }

    // Territory Operations
    fun addTerritory(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            val territory = Territory(
                name = name.ifBlank { "New Territory" },
                description = description.ifBlank { "Custom Territory Description" },
                colorHex = colorHex
            )
            territoryRepository.insert(territory)
        }
    }

    fun deleteTerritory(territory: Territory) {
        viewModelScope.launch {
            territoryRepository.delete(territory)
            // Dissociate leads linked to this deleted territory
            allLeads.value.forEach { lead ->
                if (lead.territoryId == territory.id) {
                    leadRepository.update(lead.copy(territoryId = null, isSynced = false))
                }
            }
            if (selectedTerritoryId.value == territory.id) {
                selectedTerritoryId.value = null
            }
        }
    }

    // Sync Action
    fun syncWithBackend() {
        if (isSyncing.value) return
        isSyncing.value = true

        viewModelScope.launch {
            try {
                val unsyncedList = leadRepository.getUnsyncedLeads()
                if (unsyncedList.isEmpty()) {
                    syncLogRepository.insert(
                        SyncLog(
                            status = "SUCCESS",
                            recordsSynced = 0,
                            message = "Everything is up-to-date. No offline edits pending sync."
                        )
                    )
                    isSyncing.value = false
                    return@launch
                }

                val hasUploaded = syncService.uploadLeads(unsyncedList)
                if (hasUploaded) {
                    val syncedIds = unsyncedList.map { it.id }
                    leadRepository.markAsSynced(syncedIds)
                    
                    syncLogRepository.insert(
                        SyncLog(
                            status = "SUCCESS",
                            recordsSynced = unsyncedList.size,
                            message = "Successfully synced ${unsyncedList.size} changes to lead catalog dashboard."
                        )
                    )
                } else {
                    syncLogRepository.insert(
                        SyncLog(
                            status = "FAILED",
                            recordsSynced = 0,
                            message = "Connection timeout. Storing ${unsyncedList.size} leads in database sync queue."
                        )
                    )
                }
            } catch (e: Exception) {
                syncLogRepository.insert(
                    SyncLog(
                        status = "FAILED",
                        recordsSynced = 0,
                        message = "Exception during upload: ${e.localizedMessage}. Saved records locally."
                    )
                )
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            syncLogRepository.clearAll()
        }
    }

    // AI Pitch Retrieval
    fun generatePitch(address: String, name: String, status: String, notes: String) {
        generatedPitch.value = null
        isGeneratingPitch.value = true
        viewModelScope.launch {
            val responseText = geminiService.generateSalesPitch(address, name, status, notes)
            generatedPitch.value = responseText
            isGeneratingPitch.value = false
        }
    }

    fun clearGeneratedPitch() {
        generatedPitch.value = null
    }
}
