package com.fitplan.app.data.backup

import com.fitplan.app.data.FitPlanDatabase
import com.fitplan.app.data.entity.BodyRecord
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.entity.ScheduledExercise
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** 一次完整导出的内容 */
data class ExportBundle(
    val exercises: List<Exercise> = emptyList(),
    val scheduled: List<ScheduledExercise> = emptyList(),
    val body: List<BodyRecord> = emptyList()
)

object BackupManager {

    private const val APP_TAG = "fitplan"
    private const val VERSION = 1

    // ---------- 导出 JSON ----------

    suspend fun exportJson(db: FitPlanDatabase): String {
        val ex = db.exerciseDao().all()
        val sc = db.scheduledExerciseDao().all()
        val bo = db.bodyRecordDao().all()
        return buildJson(ExportBundle(ex, sc, bo)).toString(2)
    }

    suspend fun exportBodyCsv(db: FitPlanDatabase): String {
        val records = db.bodyRecordDao().all()
        val sb = StringBuilder()
        sb.append("timestamp,date,weight_kg,body_fat_pct,muscle_kg,bone_kg,water_pct,bmi,bmr_kcal,visceral_fat,waist_cm\n")
        records.forEach { r ->
            sb.append(r.timestamp).append(',')
                .append(java.time.Instant.ofEpochMilli(r.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()).append(',')
                .append(num(r.weightKg)).append(',')
                .append(num(r.bodyFatPct)).append(',')
                .append(num(r.muscleKg)).append(',')
                .append(num(r.boneKg)).append(',')
                .append(num(r.waterPct)).append(',')
                .append(num(r.bmi)).append(',')
                .append(num(r.bmrKcal)).append(',')
                .append(num(r.visceralFat)).append(',')
                .append(num(r.waistCm)).append('\n')
        }
        return sb.toString()
    }

    // ---------- 导入 JSON（整库替换，需在空表上执行以保持自增主键一致） ----------

    suspend fun importJson(db: FitPlanDatabase, json: String) {
        val bundle = parseJson(json)
        db.withTransaction {
            db.exerciseDao().clearAll()
            db.scheduledExerciseDao().clearAll()
            db.bodyRecordDao().clearAll()
            db.exerciseDao().insertAll(bundle.exercises)
            db.scheduledExerciseDao().insertAll(bundle.scheduled)
            db.bodyRecordDao().insertAll(bundle.body)
        }
    }

    // ---------- 序列化实现 ----------

    private fun buildJson(b: ExportBundle): JSONObject {
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("version", VERSION)

        val exArr = JSONArray()
        b.exercises.forEach { e ->
            exArr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("muscleGroup", e.muscleGroup)
                    .put("equipment", e.equipment)
                    .putOpt("note", e.note)
                    .put("isCustom", e.isCustom)
                    .put("kind", e.kind)
            )
        }
        root.put("exercises", exArr)

        val scArr = JSONArray()
        b.scheduled.forEach { s ->
            scArr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("dateEpochDay", s.dateEpochDay)
                    .put("exerciseId", s.exerciseId)
                    .put("targetSets", s.targetSets)
                    .put("targetReps", s.targetReps)
                    .putOpt("weight", s.weight)
                    .put("restSeconds", s.restSeconds)
                    .put("sortOrder", s.sortOrder)
                    .put("isCompleted", s.isCompleted)
                    .putOpt("actualNote", s.actualNote)
                    .put("createdAt", s.createdAt)
            )
        }
        root.put("scheduled", scArr)

        val boArr = JSONArray()
        b.body.forEach { r ->
            boArr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("timestamp", r.timestamp)
                    .putOpt("weightKg", r.weightKg)
                    .putOpt("bodyFatPct", r.bodyFatPct)
                    .putOpt("muscleKg", r.muscleKg)
                    .putOpt("boneKg", r.boneKg)
                    .putOpt("waterPct", r.waterPct)
                    .putOpt("bmi", r.bmi)
                    .putOpt("bmrKcal", r.bmrKcal)
                    .putOpt("visceralFat", r.visceralFat)
                    .putOpt("waistCm", r.waistCm)
                    .putOpt("note", r.note)
            )
        }
        root.put("body", boArr)
        return root
    }

    private fun parseJson(json: String): ExportBundle {
        val root = JSONObject(json)
        val exercises = root.optJSONArray("exercises").toList<Exercise> { o ->
            Exercise(
                id = o.optLong("id"),
                name = o.optString("name"),
                muscleGroup = o.optString("muscleGroup"),
                equipment = o.optString("equipment"),
                note = if (o.isNull("note")) null else o.optString("note"),
                isCustom = o.optBoolean("isCustom"),
                kind = o.optString("kind", "STRENGTH")
            )
        }
        val scheduled = root.optJSONArray("scheduled").toList<ScheduledExercise> { o ->
            ScheduledExercise(
                id = o.optLong("id"),
                dateEpochDay = o.optLong("dateEpochDay"),
                exerciseId = o.optLong("exerciseId"),
                targetSets = o.optInt("targetSets"),
                targetReps = o.optInt("targetReps"),
                weight = if (o.isNull("weight")) null else o.optDouble("weight"),
                restSeconds = o.optInt("restSeconds", 90),
                sortOrder = o.optInt("sortOrder"),
                isCompleted = o.optBoolean("isCompleted"),
                actualNote = if (o.isNull("actualNote")) null else o.optString("actualNote"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }
        val body = root.optJSONArray("body").toList<BodyRecord> { o ->
            BodyRecord(
                id = o.optLong("id"),
                timestamp = o.optLong("timestamp"),
                weightKg = optDouble(o, "weightKg"),
                bodyFatPct = optDouble(o, "bodyFatPct"),
                muscleKg = optDouble(o, "muscleKg"),
                boneKg = optDouble(o, "boneKg"),
                waterPct = optDouble(o, "waterPct"),
                bmi = optDouble(o, "bmi"),
                bmrKcal = optDouble(o, "bmrKcal"),
                visceralFat = optDouble(o, "visceralFat"),
                waistCm = optDouble(o, "waistCm"),
                note = if (o.isNull("note")) null else o.optString("note")
            )
        }
        return ExportBundle(exercises, scheduled, body)
    }

    private fun optDouble(o: JSONObject, key: String): Double? =
        if (o.has(key) && !o.isNull(key)) o.optDouble(key) else null

    private fun num(v: Double?): String = if (v == null) "" else v.toString()

    private inline fun <T> JSONArray?.toList(crossinline map: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            out.add(map(getJSONObject(i)))
        }
        return out
    }
}
