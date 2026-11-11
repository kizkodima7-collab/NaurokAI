package com.example.naurokai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Bg = Color(0xFF0B1020)
private val Panel = Color(0xFF151D32)
private val Accent = Color(0xFF7C8CFF)
private val Muted = Color(0xFFAAB4CC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NaurokApp() }
    }
}

@Composable
fun NaurokApp() {
    var apiKey by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("https://api.openai.com/v1/chat/completions") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = Bg, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("NAUROK AI", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Помощник для розуміння навчальних завдань", color = Muted)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Налаштування AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(
                            value = apiKey, onValueChange = { apiKey = it },
                            label = { Text("API-ключ") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endpoint, onValueChange = { endpoint = it },
                            label = { Text("API endpoint") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = model, onValueChange = { model = it },
                            label = { Text("Модель") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Ключ використовується лише під час поточного запуску й не зберігається застосунком.", color = Muted, fontSize = 12.sp)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Навчальне завдання", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(
                            value = question, onValueChange = { question = it },
                            label = { Text("Встав запитання або умову") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                            minLines = 4
                        )
                        Button(
                            onClick = {
                                error = ""; answer = ""
                                scope.launch {
                                    busy = true
                                    try {
                                        answer = askAi(endpoint.trim(), apiKey.trim(), model.trim(), question.trim())
                                    } catch (e: Exception) {
                                        error = e.message ?: "Не вдалося отримати відповідь"
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && apiKey.isNotBlank() && question.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (busy) "Аналізую…" else "Пояснити завдання")
                        }
                    }
                }
                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF8A8A))
                }
                if (answer.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Пояснення", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(answer, color = Color.White)
                        }
                    }
                }
                Text("Порада: перевіряй пояснення та використовуй його для навчання. Автоматичне проходження тестів і натискання відповідей не передбачене.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

private suspend fun askAi(endpoint: String, key: String, model: String, question: String): String =
    withContext(Dispatchers.IO) {
        if (!endpoint.startsWith("https://")) throw IOException("Endpoint має починатися з https://")
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put(
                "content",
                "Ти навчальний помічник. Поясни запитання українською, покажи хід міркування коротко, наведи правильний принцип або спосіб розв'язання. Не проходь тест замість учня."
            ))
            .put(JSONObject().put("role", "user").put("content", question))
        val body = JSONObject().put("model", model).put("messages", messages).put("temperature", 0.2)
            .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(endpoint).header("Authorization", "Bearer $key")
            .post(body).build()
        OkHttpClient().newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Помилка API ${response.code}: ${raw.take(500)}")
            val json = JSONObject(raw)
            json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }
