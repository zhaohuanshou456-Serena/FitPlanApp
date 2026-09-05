package com.fitplan.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 智谱 GLM 视觉 API（OpenAI 兼容）。
 * 读取一张图片，返回识别出的 JSON 文本。
 */
object VisionApi {

    private const val ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    private const val PROMPT =
        "这是人体分析仪/体测仪的读数照片。请读出上面的各项数值，只输出一个 JSON 对象，" +
            "键名固定为 weight_kg, body_fat_pct, muscle_kg, bone_kg, water_pct, bmi, bmr_kcal, visceral, waist_cm；" +
            "没有读到的字段不要包含；值为数字。不要输出任何其他文字。"

    /** @param imageBase64DataUrl 形如 data:image/jpeg;base64,xxx */
    suspend fun recognize(apiKey: String, model: String, imageBase64DataUrl: String): String = withContext(Dispatchers.IO) {
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", PROMPT))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageBase64DataUrl)))
        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0.0)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))

        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw RuntimeException("HTTP $code: ${text.take(300)}")
            val root = JSONObject(text)
            root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } finally {
            conn.disconnect()
        }
    }

    /** 从模型返回文本里提取 JSON 对象（去掉可能的 ```json 代码块/前后缀） */
    fun extractJson(content: String): String {
        var c = content.trim()
        if (c.startsWith("```")) {
            c = c.removePrefix("```").removePrefix("json").removePrefix("Json").trim()
            if (c.endsWith("```")) c = c.dropLast(3).trim()
        }
        val start = c.indexOf('{')
        val end = c.lastIndexOf('}')
        return if (start >= 0 && end > start) c.substring(start, end + 1) else c
    }
}
