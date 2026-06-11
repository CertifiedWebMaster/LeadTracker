package com.example.data.repository

import com.example.data.local.SyncLogDao
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

class SyncLogRepository(private val syncLogDao: SyncLogDao) {
    val allSyncLogs: Flow<List<SyncLog>> = syncLogDao.getAllSyncLogs()

    suspend fun insert(log: SyncLog): Long = syncLogDao.insertSyncLog(log)

    suspend fun clearAll() = syncLogDao.clearLogs()
}
