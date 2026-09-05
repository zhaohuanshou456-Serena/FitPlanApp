package com.fitplan.app.ui.workout

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.program.ExerciseProgression
import com.fitplan.app.data.program.ProgressionRules
import com.fitplan.app.data.workout.WorkoutSession
import com.fitplan.app.data.workout.WorkoutSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class WorkoutRunnerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FitPlanApp).repository

    val exercises: List<RunExercise> = PendingRun.exercises.ifEmpty { emptyList() }
    val title: String = PendingRun.title ?: "训练"

    private val _sessionId = MutableStateFlow<Long?>(null)
    val sessionId: StateFlow<Long?> = _sessionId

    /** 已完成全部组数（index=完成）的动作下标 */
    val completed = MutableStateFlow<Set<Int>>(emptySet())

    init {
        viewModelScope.launch {
            if (exercises.isNotEmpty()) {
                _sessionId.value = repo.beginWorkout(
                    WorkoutSession(title = title, sourceKind = PendingRun.sourceKind, sourceRef = PendingRun.sourceRef)
                )
            }
        }
    }

    fun markCompleted(index: Int) {
        completed.update { it + index }
    }

    suspend fun persistSet(set: WorkoutSet) {
        repo.logWorkoutSet(set)
    }

    /** 训练结束：结束会话、把来自某天的动作打勾、更新渐进基线 */
    suspend fun finish(doneScheduledIds: List<Long>) {
        val sid = _sessionId.value ?: return
        repo.endWorkout(sid)
        doneScheduledIds.forEach { if (it > 0L) repo.setScheduledCompleted(it, true) }
        val logged = repo.setsOfWorkout(sid)
        exercises.forEach { ex ->
            val exSets = logged.filter { it.exerciseId == ex.exerciseId }
            if (exSets.isNotEmpty()) {
                val best = exSets.maxByOrNull { it.reps }
                val weightAtBest = best?.weightKg
                if (weightAtBest != null) {
                    val prev = repo.progressionOf(ex.exerciseId)
                    val prevWeight = prev?.bestWeightKg
                    val newBestWeight = if (prevWeight == null) weightAtBest else max(prevWeight, weightAtBest)
                    val newBestReps = max(prev?.bestReps ?: 0, best.reps)
                    val sug = ProgressionRules.suggestNext(ex.reps, ex.reps, newBestWeight, newBestReps)
                    repo.saveProgression(
                        ExerciseProgression(
                            exerciseId = ex.exerciseId,
                            bestWeightKg = newBestWeight,
                            bestReps = newBestReps,
                            suggestedWeightKg = sug,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutRunnerScreen(onExit: () -> Unit, vm: WorkoutRunnerViewModel = viewModel()) {
    if (vm.exercises.isEmpty()) {
        Text("没有可训练的动作", modifier = Modifier.padding(24.dp))
        Button(onClick = onExit, modifier = Modifier.padding(16.dp)) { Text("返回") }
        return
    }

    val exercises = vm.exercises
    val setsDone = remember { exercises.map { 0 }.toMutableStateList() }
    var weightText by remember { mutableStateOf(weightPrefill(exercises.firstOrNull()?.startWeight)) }
    var repsText by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }

    // 休息状态
    var resting by remember { mutableStateOf(false) }
    var restRemaining by remember { mutableIntStateOf(0) }
    var restNonce by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val current = exercises.indexOfFirst { setsDone[it] < exercises[it].sets }
    val currentEx = if (current in exercises.indices) exercises[current] else null
    val curSetNum = if (current >= 0) setsDone[current] + 1 else 0

    fun advance() {
        resting = false
        val next = exercises.indexOfFirst { setsDone[it] < exercises[it].sets }
        if (next == -1) {
            finished = true
            return
        }
        // 进入一个新的动作（其第 1 组前）才重置起始重量；同动作内保留用户改过的重量
        if (setsDone[next] == 0) weightText = weightPrefill(exercises[next].startWeight)
        repsText = ""
    }

    LaunchedEffect(restNonce) {
        if (resting) {
            while (restRemaining > 0) {
                delay(1000)
                if (restRemaining > 0) restRemaining -= 1
            }
            if (resting) advance()
        }
    }

    fun completeSet() {
        val i = current
        val ex = exercises.getOrNull(i) ?: return
        val reps = repsText.toIntOrNull()
        if (reps == null || reps <= 0) return
        val w = weightText.toDoubleOrNull()
        val sid = vm.sessionId.value
        scope.launch {
            sid?.let {
                vm.persistSet(
                    WorkoutSet(
                        sessionId = it,
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.name,
                        muscleGroup = ex.muscle,
                        setIndex = setsDone[i] + 1,
                        weightKg = w,
                        reps = reps,
                        plannedReps = ex.reps,
                        restSeconds = ex.restSeconds
                    )
                )
            }
        }
        setsDone[i] = setsDone[i] + 1
        if (setsDone[i] >= ex.sets) vm.markCompleted(i)
        repsText = ""
        val anyLeft = exercises.indexOfFirst { setsDone[it] < exercises[it].sets } != -1
        if (!anyLeft) {
            finished = true
        } else {
            // 开始组间/动作间休息
            restRemaining = ex.restSeconds
            resting = true
            restNonce += 1
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(vm.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("动作 ${(current + 1).coerceAtLeast(0)} / ${exercises.size}", style = MaterialTheme.typography.bodyMedium)

        if (finished) {
            Spacer(Modifier.height(24.dp))
            Text("本次训练完成！", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("已逐组记录你的重量与次数。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            val doneIds = exercises.mapIndexedNotNull { i, ex -> if (setsDone[i] >= ex.sets) ex.scheduledId else null }
            Button(
                onClick = {
                    scope.launch {
                        vm.finish(doneIds)
                        PendingRun.exercises = emptyList()
                        onExit()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存并结束") }
            return@Column
        }

        Spacer(Modifier.height(8.dp))
        // 总进度
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            exercises.forEachIndexed { i, _ ->
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (setsDone[i] >= exercises[i].sets) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val ex = currentEx ?: return@Column
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(ex.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${ex.muscle} · ${ex.equipment}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "第 $curSetNum / ${ex.sets} 组",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "目标 ${ex.reps} 次${if (ex.startWeight == null) "（自重）" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                        label = { Text("实际次数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (ex.startWeight == null) "自重可留空" else "重量(kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (resting) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                ) {
                    Text("组间休息", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatRest(restRemaining),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            restNonce += 1
                            resting = false
                            advance()
                        }) { Text("跳过休息") }
                        OutlinedButton(onClick = { restNonce += 1; resting = false; advance() }) { Text("开始下一组") }
                    }
                }
            }
        } else {
            Button(
                onClick = { completeSet() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = (repsText.toIntOrNull() ?: 0) > 0
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("完成本组（${ex.restSeconds}s 休息）")
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                restNonce += 1
                resting = false
                finished = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Icon(Icons.Filled.Close, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("提前结束") }
    }
}

private fun weightPrefill(w: Double?): String = if (w == null) "" else {
    val r = (w * 10).toInt() / 10.0
    if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

private fun formatRest(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "${m}:${if (s < 10) "0" else ""}$s" else "$s"
}
