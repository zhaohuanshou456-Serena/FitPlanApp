package com.fitplan.app.ui.screens.program

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.FitPlanApp
import com.fitplan.app.ui.workout.PendingRun
import com.fitplan.app.ui.workout.RunExercise
import com.fitplan.app.util.epochDayToLocalDate
import com.fitplan.app.util.toEpochDayLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** UI 层：方案内一节里的一个动作 */
data class PItemUI(
    val exerciseId: Long,
    val exerciseName: String,
    val muscle: String,
    val equipment: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val weightKg: Double?,
    val cue: String?
)

data class PSessionUI(
    val id: Long,
    val order: Int,
    val name: String,
    val items: List<PItemUI>
)

data class PProgramUI(
    val id: Long,
    val name: String,
    val goal: String?,
    val sessions: List<PSessionUI>
)

class ProgramViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FitPlanApp).repository

    private val _programs = MutableStateFlow<List<PProgramUI>>(emptyList())
    val programs: StateFlow<List<PProgramUI>> = _programs

    init {
        viewModelScope.launch {
            repo.ensureBuiltInPlan()
            rebuild()
            // 方案增删时刷新
            repo.observePrograms().collect { rebuild() }
        }
    }

    private suspend fun rebuild() {
        val result = mutableListOf<PProgramUI>()
        for (p in repo.programsAll()) {
            val sessionUIs = mutableListOf<PSessionUI>()
            for (s in repo.sessionsOf(p.id)) {
                val itemUIs = mutableListOf<PItemUI>()
                for (it in repo.itemsOf(s.id)) {
                    val ex = repo.exerciseById(it.exerciseId) ?: continue
                    itemUIs.add(
                        PItemUI(
                            exerciseId = it.exerciseId,
                            exerciseName = ex.name,
                            muscle = ex.muscleGroup,
                            equipment = ex.equipment,
                            sets = it.targetSets,
                            repMin = it.repMin,
                            repMax = it.repMax,
                            restSeconds = it.restSeconds,
                            weightKg = it.weightKg,
                            cue = it.formCue
                        )
                    )
                }
                sessionUIs.add(PSessionUI(s.id, s.sessionOrder, s.name, itemUIs))
            }
            result.add(PProgramUI(p.id, p.name, p.goal, sessionUIs))
        }
        _programs.value = result
    }

    /** 把某节生成可直接进入引导器的动作列表（供“开始训练”用） */
    suspend fun sessionToRun(sessionId: Long): List<RunExercise> {
        val out = mutableListOf<RunExercise>()
        for (it in repo.itemsOf(sessionId)) {
            val ex = repo.exerciseById(it.exerciseId) ?: continue
            out.add(
                RunExercise(
                    exerciseId = it.exerciseId,
                    name = ex.name,
                    muscle = ex.muscleGroup,
                    equipment = ex.equipment,
                    sets = it.targetSets,
                    reps = it.repMax,
                    restSeconds = it.restSeconds,
                    startWeight = it.weightKg
                )
            )
        }
        return out
    }

    fun applyToToday(programId: Long, sessionId: Long) {
        viewModelScope.launch {
            repo.applyProgramSession(LocalDate.now().toEpochDayLong(), programId, sessionId)
        }
    }
}

@Composable
fun ProgramScreen(onStartWorkout: () -> Unit, vm: ProgramViewModel = viewModel()) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    var expandedSession by remember { mutableStateOf<Long?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "我的方案",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (programs.isEmpty()) {
            com.fitplan.app.ui.common.EmptyHint("还没有方案\n首次打开今日会自动内置一套 U/L 方案")
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
                programs.forEach { prog ->
                    item(key = "prog-${prog.id}") {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(prog.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (!prog.goal.isNullOrBlank()) {
                                    Text(prog.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("A→B→C→D 轮换 · 共 ${prog.sessions.size} 节", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    items(prog.sessions.size, key = { "sess-${prog.sessions[it].id}" }) { si ->
                        val s = prog.sessions[si]
                        val expanded = expandedSession == s.id
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("第 $s.order 节", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(s.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    }
                                    OutlinedButton(onClick = { expandedSession = if (expanded) null else s.id }) {
                                        Text(if (expanded) "收起" else "查看动作")
                                    }
                                }
                                if (expanded) {
                                    s.items.forEach { item ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                "${item.sets}×${item.repMin}–${item.repMax} · ${item.exerciseName} · ${item.equipment}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (item.weightKg != null) {
                                                Text("起始 ${item.weightKg}kg", style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (!item.cue.isNullOrBlank()) {
                                                Text("要领：${item.cue}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("休息 ${item.restSeconds}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                        Button(onClick = { vm.applyToToday(prog.id, s.id) }) { Text("应用到今天") }
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                PendingRun.exercises = vm.sessionToRun(s.id)
                                                PendingRun.sourceKind = "PLAN"
                                                PendingRun.sourceRef = s.id
                                                PendingRun.title = s.name
                                                onStartWorkout()
                                            }
                                        }) { Text("开始训练") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
