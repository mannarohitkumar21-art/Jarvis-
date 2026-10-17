package com.example.ai

import android.content.Context
import com.example.BuildConfig
import com.example.audit.AuditLogger
import com.example.model.AiConfig
import com.example.model.AiProviderType
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.model.PlanStep
import com.example.model.RiskLevel
import com.example.model.StepStatus
import com.example.model.TaskPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiProviderManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun loadConfig(): AiConfig {
        val providerStr = prefs.getString("provider", AiProviderType.GEMINI.name) ?: AiProviderType.GEMINI.name
        val provider = try {
            AiProviderType.valueOf(providerStr)
        } catch (_: Exception) {
            AiProviderType.GEMINI
        }
        val model = prefs.getString("model", provider.defaultModel) ?: provider.defaultModel
        val key = prefs.getString("api_key", "") ?: ""
        val endpoint = prefs.getString("custom_endpoint", "") ?: ""
        return AiConfig(
            selectedProvider = provider,
            selectedModel = model,
            customApiKey = key,
            customEndpoint = endpoint
        )
    }

    fun saveConfig(config: AiConfig) {
        prefs.edit()
            .putString("provider", config.selectedProvider.name)
            .putString("model", config.selectedModel)
            .putString("api_key", config.customApiKey)
            .putString("custom_endpoint", config.customEndpoint)
            .apply()

        AuditLogger.log(
            category = AuditCategory.SYSTEM,
            severity = AuditSeverity.INFO,
            action = "AI Provider Config Updated",
            details = "Provider: ${config.selectedProvider.displayName}, Model: ${config.selectedModel}",
            outcome = "SAVED"
        )
    }

    suspend fun query(prompt: String): String = withContext(Dispatchers.IO) {
        val config = loadConfig()
        val rawKey = config.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
        val key = if (rawKey == "MY_GEMINI_API_KEY") "" else rawKey

        AuditLogger.log(
            category = AuditCategory.AI_QUERY,
            severity = AuditSeverity.INFO,
            action = "AI Query Dispatched",
            details = "Prompt: '$prompt' via ${config.selectedProvider.displayName}",
            outcome = "DISPATCHED"
        )

        if (config.selectedProvider == AiProviderType.OFFLINE_CORE || key.isBlank()) {
            return@withContext queryOfflineCore(prompt)
        }

        try {
            when (config.selectedProvider) {
                AiProviderType.GEMINI -> queryGemini(prompt, config.selectedModel, key)
                AiProviderType.OPENAI -> queryOpenAiCompatible(
                    prompt = prompt,
                    model = config.selectedModel,
                    apiKey = key,
                    baseUrl = config.customEndpoint.ifBlank { "https://api.openai.com/v1" }
                )
                AiProviderType.GROQ -> queryOpenAiCompatible(
                    prompt = prompt,
                    model = config.selectedModel,
                    apiKey = key,
                    baseUrl = "https://api.groq.com/openai/v1"
                )
                AiProviderType.OPENROUTER -> queryOpenAiCompatible(
                    prompt = prompt,
                    model = config.selectedModel,
                    apiKey = key,
                    baseUrl = "https://openrouter.ai/api/v1"
                )
                AiProviderType.OFFLINE_CORE -> queryOfflineCore(prompt)
            }
        } catch (e: Exception) {
            AuditLogger.log(
                category = AuditCategory.AI_QUERY,
                severity = AuditSeverity.WARNING,
                action = "AI Query Failed",
                details = e.message ?: "Network error",
                outcome = "FALLBACK_TO_OFFLINE"
            )
            // Graceful fallback to offline core
            val fallback = queryOfflineCore(prompt)
            "[$fallback (Cloud query failed: ${e.message})]"
        }
    }

    private fun queryGemini(prompt: String, model: String, apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val systemInstruction = "You are JARVIS AI, an autonomous personal Android assistant. You are concise, precise, helpful, and multilingual (English, Bengali, Hindi). If the user asks for a device action, explain clearly what to do or propose steps."

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API HTTP ${response.code}: $body")
        }

        val root = JSONObject(body)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        return parts?.optJSONObject(0)?.optString("text") ?: "No response from Gemini."
    }

    private fun queryOpenAiCompatible(prompt: String, model: String, apiKey: String, baseUrl: String): String {
        val cleanUrl = if (baseUrl.endsWith("/chat/completions")) baseUrl else "${baseUrl.trimEnd('/')}/chat/completions"

        val json = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are JARVIS AI, an autonomous personal Android assistant. Concise and multilingual.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.4)
        }

        val request = Request.Builder()
            .url(cleanUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            throw IllegalStateException("OpenAI API HTTP ${response.code}: $body")
        }

        val root = JSONObject(body)
        val choices = root.optJSONArray("choices")
        val msg = choices?.optJSONObject(0)?.optJSONObject("message")
        return msg?.optString("content") ?: "No response received."
    }

    /**
     * Autonomous Offline Core Engine
     * Generates rich responses, device awareness, and language-tailored greetings without external dependencies.
     */
    fun queryOfflineCore(prompt: String): String {
        val p = prompt.lowercase().trim()
        return when {
            p.contains("hello") || p.contains("hi") || p.contains("hey jarvis") ->
                "Greetings. JARVIS autonomous systems are online and standing by. How may I assist you with your Android device or tasks today?"
            p.contains("নমস্কার") || p.contains("কেমন আছো") || p.contains("হ্যালো") ->
                "নমস্কার! আমি জারভিস (JARVIS)। আপনার অ্যান্ড্রয়েড ডিভাইসের সমস্ত কাজ এবং পরিকল্পনা পরিচালনা করার জন্য প্রস্তুত।"
            p.contains("नमस्ते") || p.contains("कैसे हो") || p.contains("जार्विस") ->
                "नमस्ते! मैं जार्विस (JARVIS) हूँ। आपकी आज्ञा अनुसार कार्य करने और डिवाइस नियंत्रण के लिए तैयार हूँ।"
            p.contains("who are you") || p.contains("what can you do") ->
                "I am JARVIS AI, an autonomous personal Android assistant. I can plan complex tasks, execute terminal commands, control hardware (flashlight, battery, alarms, apps, location), run automatic diagnostics, and self-repair system issues."
            p.contains("battery") ->
                "Battery telemetry is available. You can execute 'battery' in the terminal or view the live gauge in the header."
            p.contains("torch") || p.contains("flashlight") ->
                "Flashlight hardware can be toggled using the terminal command 'torch' or the quick button on the Integrations tab."
            p.contains("diagnose") || p.contains("health") ->
                "Running full diagnostics will check voice, camera, battery, network, storage, notifications, and location."
            p.contains("repair") ->
                "The controlled self-repair engine can resolve configuration errors, restart audio pipelines, and request needed permissions."
            else ->
                "Understood. Instruction registered: \"$prompt\". I can formulate an autonomous plan or execute device commands for this. Open the Planner tab to generate structured steps."
        }
    }
}
