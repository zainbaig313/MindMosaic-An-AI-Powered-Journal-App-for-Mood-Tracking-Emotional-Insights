package com.example.mindmosaic

// Request classes
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 100,
    val temperature: Double = 0.3
)

data class Message(
    val role: String,
    val content: String
)

// Response classes
data class OpenRouterResponse(
    val id: String?,
    val choices: List<Choice>?,
    val usage: Usage?,
    val error: ApiError?
)

data class Choice(
    val message: MessageResponse?,
    val finish_reason: String?
)

data class MessageResponse(
    val role: String?,
    val content: String?
)

data class Usage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

data class ApiError(
    val message: String?,
    val type: String?,
    val code: String?
)

// New comprehensive journal analysis data class
data class JournalAnalysis(
    val summary: String,
    val emotionalTone: String,
    val motivationalQuote: String,
    val relaxationActivity: String
)

// Keep MotivationalContent for backward compatibility (can be removed later if not needed elsewhere)
data class MotivationalContent(
    val quote: String,
    val activity: String,
    val emotion: String
)