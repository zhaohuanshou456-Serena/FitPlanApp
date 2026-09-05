package com.fitplan.app.ui.body

import com.fitplan.app.data.entity.BodyRecord

/** 需要记录并可在趋势中展示的每一项身体指标 */
data class MetricDef(
    val key: String,
    val label: String,
    val unit: String,
    val extract: (BodyRecord) -> Double?
)

object MetricCatalog {

    val metrics: List<MetricDef> = listOf(
        MetricDef("weight", "体重", "kg") { it.weightKg },
        MetricDef("bodyFat", "体脂率", "%") { it.bodyFatPct },
        MetricDef("muscle", "肌肉量", "kg") { it.muscleKg },
        MetricDef("bone", "骨量", "kg") { it.boneKg },
        MetricDef("water", "体水分", "%") { it.waterPct },
        MetricDef("bmi", "BMI", "") { it.bmi },
        MetricDef("bmr", "基础代谢", "kcal") { it.bmrKcal },
        MetricDef("visceralFat", "内脏脂肪", "级") { it.visceralFat },
        MetricDef("waist", "腰围", "cm") { it.waistCm }
    )

    fun byKey(key: String): MetricDef = metrics.firstOrNull { it.key == key } ?: metrics.first()

    /** 读入 BodyRecord 时为某指标填值（copy 出新对象） */
    fun withValue(record: BodyRecord, key: String, value: Double?): BodyRecord = when (key) {
        "weight" -> record.copy(weightKg = value)
        "bodyFat" -> record.copy(bodyFatPct = value)
        "muscle" -> record.copy(muscleKg = value)
        "bone" -> record.copy(boneKg = value)
        "water" -> record.copy(waterPct = value)
        "bmi" -> record.copy(bmi = value)
        "bmr" -> record.copy(bmrKcal = value)
        "visceralFat" -> record.copy(visceralFat = value)
        "waist" -> record.copy(waistCm = value)
        else -> record
    }
}
