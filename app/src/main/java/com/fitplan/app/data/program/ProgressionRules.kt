package com.fitplan.app.data.program

import kotlin.math.roundToInt

/**
 * 渐进加重建议（纯函数，UI 复用）。
 * 规则：做到目标次数上限→+2.5kg；只做到中段→保持；连下限都不到→-5%。
 */
object ProgressionRules {

    /** 按 0.5kg 归整（杠铃片常见最小步进） */
    private fun roundToPlate(v: Double): Double =
        (v * 2).roundToInt() / 2.0

    /**
     * @param bestWeightKg 该动作最近达到的重量（null=自重，不给建议）
     * @param bestReps     该重量下的最好次数
     */
    fun suggestNext(repMin: Int, repMax: Int, bestWeightKg: Double?, bestReps: Int?): Double? {
        if (bestWeightKg == null || bestReps == null || bestReps <= 0) return null
        return when {
            bestReps >= repMax -> roundToPlate(bestWeightKg + 2.5)
            bestReps <= repMin -> roundToPlate(bestWeightKg * 0.95)
            else -> bestWeightKg
        }
    }
}
