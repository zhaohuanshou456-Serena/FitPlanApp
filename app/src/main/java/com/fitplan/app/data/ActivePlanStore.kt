package com.fitplan.app.data

import android.content.Context

/**
 * 记录当前选中的“训练方案”(ProgramId)，供今日轮换建议使用。
 * 用 SharedPreferences 存一个 long，简单稳定，不引入新表。
 */
class ActivePlanStore(context: Context) {

    private val prefs = context.getSharedPreferences("fitplan_app_prefs", Context.MODE_PRIVATE)

    fun get(): Long? {
        val v = prefs.getLong(KEY, -1L)
        return if (v > 0) v else null
    }

    fun set(id: Long) {
        prefs.edit().putLong(KEY, id).apply()
    }

    companion object {
        private const val KEY = "active_program_id"
    }
}
