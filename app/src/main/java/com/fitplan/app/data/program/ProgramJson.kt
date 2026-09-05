package com.fitplan.app.data.program

import androidx.room.withTransaction
import com.fitplan.app.data.FitPlanDatabase
import com.fitplan.app.data.entity.Exercise
import org.json.JSONArray
import org.json.JSONObject

/**
 * 训练方案(Program 及其分节/动作)的 JSON 导入导出。
 * 用户可用它把自己的整周/整套方案搬进 App。
 */
object ProgramJson {

    private fun optS(o: JSONObject, key: String): String? =
        if (o.has(key) && !o.isNull(key)) o.optString(key) else null

    private fun optD(o: JSONObject, key: String): Double? =
        if (o.has(key) && !o.isNull(key)) o.optDouble(key) else null

    suspend fun export(db: FitPlanDatabase): String {
        val arr = JSONArray()
        for (p in db.programDao().allOrdered()) {
            val po = JSONObject()
                .put("name", p.name)
                .putOpt("goal", p.goal)
                .put("dayCount", p.dayCount)
                .putOpt("note", p.note)
            val sArr = JSONArray()
            for (s in db.programSessionDao().sessionsOf(p.id)) {
                val so = JSONObject().put("name", s.name)
                val iArr = JSONArray()
                for (it in db.programItemDao().itemsOf(s.id)) {
                    val ex = db.exerciseDao().byId(it.exerciseId)
                    iArr.put(
                        JSONObject()
                            .put("exercise", ex?.name ?: "")
                            .put("muscle", ex?.muscleGroup ?: "")
                            .put("equipment", ex?.equipment ?: "")
                            .put("sets", it.targetSets)
                            .put("repMin", it.repMin)
                            .put("repMax", it.repMax)
                            .putOpt("weightKg", it.weightKg)
                            .put("restSeconds", it.restSeconds)
                            .putOpt("cue", it.formCue)
                    )
                }
                so.put("items", iArr)
                sArr.put(so)
            }
            po.put("sessions", sArr)
            arr.put(po)
        }
        return JSONObject().put("version", 1).put("programs", arr).toString(2)
    }

    /** 返回成功导入的方案数 */
    suspend fun import(db: FitPlanDatabase, json: String): Int {
        val root = JSONObject(json)
        val arr = root.optJSONArray("programs")
            ?: JSONArray().put(root) // 兼容单个对象
        var count = 0
        db.withTransaction {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sArr = o.optJSONArray("sessions")
                val pid = db.programDao().insert(
                    Program(
                        name = o.optString("name", "我的方案"),
                        goal = optS(o, "goal"),
                        dayCount = o.optInt("dayCount", sArr?.length() ?: 4),
                        note = optS(o, "note")
                    )
                )
                var order = 1
                if (sArr != null) {
                    for (j in 0 until sArr.length()) {
                        val so = sArr.getJSONObject(j)
                        val sid = db.programSessionDao().insert(
                            ProgramSession(programId = pid, sessionOrder = order, name = so.optString("name", "节 $order"))
                        )
                        order++
                        val iArr = so.optJSONArray("items")
                        var io = 0
                        if (iArr != null) {
                            for (k in 0 until iArr.length()) {
                                val it = iArr.getJSONObject(k)
                                val exName = it.optString("exercise").trim()
                                if (exName.isEmpty()) continue
                                val exId = db.exerciseDao().idByName(exName)
                                    ?: db.exerciseDao().insert(
                                        Exercise(
                                            name = exName,
                                            muscleGroup = it.optString("muscle", "其他"),
                                            equipment = it.optString("equipment", "其他"),
                                            note = optS(it, "cue")?.takeIf { x -> x.isNotBlank() },
                                            isCustom = true
                                        )
                                    )
                                db.programItemDao().insert(
                                    ProgramItem(
                                        sessionId = sid,
                                        exerciseId = exId,
                                        itemOrder = io,
                                        targetSets = it.optInt("sets", 3),
                                        repMin = it.optInt("repMin", 8),
                                        repMax = it.optInt("repMax", 12),
                                        weightKg = optD(it, "weightKg"),
                                        restSeconds = it.optInt("restSeconds", 90),
                                        formCue = optS(it, "cue"),
                                        enableProgressive = true
                                    )
                                )
                                io++
                            }
                        }
                    }
                }
                count++
            }
        }
        return count
    }
}
