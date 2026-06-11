package com.example.data.api

import com.example.data.model.Lead
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val deviceId: String = "rep-device-android",
    val syncTimestamp: Long = System.currentTimeMillis(),
    val leads: List<Lead>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val json: SyncPayload?,
    val url: String?
)

interface SyncApiService {
    @POST("post")
    suspend fun syncLeads(
        @Body payload: SyncPayload
    ): SyncResponse
}

object SyncClient {
    private const val BASE_URL = "https://httpbin.org/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: SyncApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(SyncApiService::class.java)
    }
}

class SyncService {
    suspend fun uploadLeads(leads: List<Lead>): Boolean {
        if (leads.isEmpty()) return true
        return try {
            val payload = SyncPayload(leads = leads)
            val response = SyncClient.service.syncLeads(payload)
            response.json?.leads?.isNotEmpty() == true
        } catch (e: Exception) {
            // Return false if offline or connection timeout
            false
        }
    }
}
