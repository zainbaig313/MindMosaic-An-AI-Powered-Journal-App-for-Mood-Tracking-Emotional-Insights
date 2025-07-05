package com.example.mindmosaic

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class EmotionRepository {

    // 🔑 Replace this with your actual OpenRouter API key
    private val apiKey = "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"

    private val apiService: OpenRouterApiService

    init {
        // Create logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Create OkHttpClient with timeout and logging
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(OpenRouterApiService::class.java)
    }

    suspend fun analyzeJournal(text: String): Result<JournalAnalysis> {
        return try {
            // Create the comprehensive prompt for journal analysis
            val prompt = """
                Analyze the following journal entry and provide a comprehensive response in JSON format with exactly these fields:
                {
                  "summary": "Brief 1-2 sentence summary of the journal entry",
                  "emotionalTone": "One of: Happy, Sad, Angry, Anxious, Excited, Neutral",
                  "motivationalQuote": "An appropriate motivational quote with attribution",
                  "relaxationActivity": "A specific relaxation or wellness activity suggestion"
                }                
                Journal Entry: $text               
                Respond ONLY with the JSON object, no other text.
            """.trimIndent()

            val request = OpenRouterRequest(
                model = "deepseek/deepseek-chat-v3-0324:free",
                messages = listOf(
                    Message(
                        role = "user",
                        content = prompt
                    )
                ),
                max_tokens = 1024,
                temperature = 0.3
            )

            Log.d("EmotionRepository", "Sending journal analysis request to OpenRouter...")

            val response = apiService.analyzeEmotion(
                authorization = "Bearer $apiKey",
                referer = "https://mindmosaic.app", // Your app identifier
                title = "MindMosaic Journal Analysis",
                request = request
            )

            if (response.isSuccessful) {
                val responseBody = response.body()

                if (responseBody?.error != null) {
                    Log.e("EmotionRepository", "API Error: ${responseBody.error.message}")
                    return Result.failure(Exception("API Error: ${responseBody.error.message}"))
                }

                val analysisText = responseBody?.choices?.firstOrNull()?.message?.content?.trim()
                Log.d("EmotionRepository", "Analysis response from API: $analysisText")

                if (analysisText != null) {
                    val analysis = parseAnalysisResponse(analysisText)
                    if (analysis != null) {
                        Log.d("EmotionRepository", "Successfully parsed journal analysis")
                        Result.success(analysis)
                    } else {
                        Log.e("EmotionRepository", "Failed to parse analysis response")
                        Result.failure(Exception("Failed to parse analysis response"))
                    }
                } else {
                    Log.e("EmotionRepository", "No analysis content received")
                    Result.failure(Exception("No analysis content received"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("EmotionRepository", "HTTP Error: ${response.code()} - ${response.message()}")
                Log.e("EmotionRepository", "Error body: $errorBody")
                Result.failure(Exception("HTTP Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("EmotionRepository", "Exception during analysis: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseAnalysisResponse(jsonString: String): JournalAnalysis? {
        return try {

            val cleanedJson = jsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonObject = JSONObject(cleanedJson)

            val summary = jsonObject.optString("summary", "Journal entry recorded")
            val emotionalTone = jsonObject.optString("emotionalTone", "Neutral")
            val motivationalQuote = jsonObject.optString("motivationalQuote", "Every day is a fresh start.")
            val relaxationActivity = jsonObject.optString("relaxationActivity", "Take 5 minutes to practice deep breathing")

            // Validate emotional tone is one of our expected categories
            val validEmotions = listOf("Happy", "Sad", "Angry", "Anxious", "Excited", "Neutral")
            val normalizedEmotion = if (validEmotions.contains(emotionalTone)) {
                emotionalTone
            } else {
                "Neutral" // Default fallback
            }

            JournalAnalysis(
                summary = summary,
                emotionalTone = normalizedEmotion,
                motivationalQuote = motivationalQuote,
                relaxationActivity = relaxationActivity
            )
        } catch (e: Exception) {
            Log.e("EmotionRepository", "JSON parsing error: ${e.message}", e)
            null
        }
    }
}