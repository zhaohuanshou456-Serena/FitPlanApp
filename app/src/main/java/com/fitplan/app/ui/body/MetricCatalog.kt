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
        MetricDef("height", "身高", "cm") { it.heightCm },
        MetricDef("weight", "体重", "kg") { it.weightKg },
        MetricDef("bmi", "BMI", "") { it.bmi },
        MetricDef("bodyFat", "体脂率", "%") { it.bodyFatPct },
        MetricDef("visceralFat", "内脏脂肪", "级") { it.visceralFat },
        MetricDef("subcutaneous", "皮下脂肪", "%") { it.subcutaneousPct },
        MetricDef("muscle", "肌肉", "%") { it.muscleKg },
        MetricDef("bone", "骨量", "kg") { it.boneKg },
        MetricDef("water", "体水分", "%") { it.waterPct },
        MetricDef("protein", "蛋白质", "%") { it.proteinPct },
        MetricDef("bmr", "基础代谢", "kcal") { it.bmrKcal },
        MetricDef("waist", "腰围", "cm") { it.waistCm }
    )

    fun byKey(key: String): MetricDef = metrics.firstOrNull { it.key == key } ?: metrics.first()

    /** 读入 BodyRecord 时为某指标填值（copy 出新对象） */
    fun withValue(record: BodyRecord, key: String, value: Double?): BodyRecord = when (key) {
        "height" -> record.copy(heightCm = value)
        "weight" -> record.copy(weightKg = value)
        "bmi" -> record.copy(bmi = value)
        "bodyFat" -> record.copy(bodyFatPct = value)
        "visceralFat" -> record.copy(visceralFat = value)
        "subcutaneous" -> record.copy(subcutaneousPct = value)
        "muscle" -> record.copy(muscleKg = value)
        "bone" -> record.copy(boneKg = value)
        "water" -> record.copy(waterPct = value)
        "protein" -> record.copy(proteinPct = value)
        "bmr" -> record.copy(bmrKcal = value)
        "waist" -> record.copy(waistCm = value)
        else -> record
    }
}
