package com.example.data.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiService {
    suspend fun generateSalesPitch(
        address: String,
        homeownerName: String,
        status: String,
        notes: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Demo mode active (Configure GEMINI_API_KEY in Secrets for live pitches!). Here is a quick pitch suggestion:\n\n'Hi Jane, I'm stopping by because we're doing installations down the street. It looks like you've got great direct sunlight on your roof, are you interested in lowering your bills?'"
        }

        val prompt = """
            You are an expert door-to-door sales pitch generator.
            Generate a concise, engaging, and personalized 3-sentence door opener pitch for a lead with:
            Name: $homeownerName
            Address: $address
            Visit Status: $status (e.g. WARM_LEAD, NOT_HOME)
            Key Notes: $notes
            
            Focus on creating immediate rapport, mentioning a benefit relevant to homeowners (such as solar potential, exterior enhancements, pest barriers, or local neighborhood specials), and ending with a low-pressure open-ended question.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        return try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No pitch text could be generated. Try introducing solar benefits or exterior enhancements directly!"
        } catch (e: Exception) {
            "Pitch Generator is offline: ${e.localizedMessage}. Sample opener: 'Hi $homeownerName, I'm with solar services, we are setting up energy assessments for homeowners on $address today. Would you like a quick 2-minute estimate?'"
        }
    }
}
