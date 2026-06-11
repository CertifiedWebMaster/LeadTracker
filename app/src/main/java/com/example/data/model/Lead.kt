package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val status: String, // "NOT_HOME", "GO_BACK", "WARM_LEAD", "CUSTOMER", "REFUSED"
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val phone: String = "",
    val territoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
