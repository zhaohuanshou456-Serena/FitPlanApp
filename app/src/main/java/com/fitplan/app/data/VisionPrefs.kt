package com.fitplan.app.data

import android.content.Context

/** 暂时存 AI 识别的 API Key 与模型名（SharedPreferences，仅本地） */
class VisionPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("fitplan_vision_prefs", Context.MODE_PRIVATE)

    fun apiKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
    fun setApiKey(key: String) = prefs.edit().putString(KEY_API, key).apply()

    fun model(): String = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    fun setModel(m: String) = prefs.edit().putString(KEY_MODEL, m).apply()

    companion object {
        const val DEFAULT_MODEL = "glm-4.6v-flashx"
        private const val KEY_API = "ai_api_key"
        private const val KEY_MODEL = "ai_model"
    }
}
