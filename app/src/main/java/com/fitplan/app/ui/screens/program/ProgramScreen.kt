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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.program.Program
import com.fitplan.app.data.program.ProgramItem
import com.fitplan.app.data.program.ProgramSession
import com.fitplan.app.ui.workout.PendingRun
import com.fitplan.app.ui.workout.RunExercise
import com.fitplan.app.util.epochDayToLocalDate
import com.fitplan.app.util.toEpochDayLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** UI 层：方案内一节里的一个动作 */
data class PItemUI(
    val id: Long,
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

    private val _activeId = MutableStateFlow<Long?>(null)
    val activeId: StateFlow<Long?> = _activeId

    val exercises = repo.observeExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

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
                            id = it.id,
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
        _activeId.value = (application as FitPlanApp).activePlan.get()
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

    suspend fun exportJson(): String = repo.exportProgramsJson()

    suspend fun importJson(json: String): Int {
        val n = repo.importProgramsJson(json)
        rebuild()
        return n
    }

    fun createProgram(name: String, sessionCount: Int) {
        viewModelScope.launch {
            val pid = repo.insertProgram(Program(name = name, dayCount = sessionCount))
            for (i in 1..sessionCount) {
                repo.insertSession(ProgramSession(programId = pid, sessionOrder = i, name = "节 $i"))
            }
            rebuild()
        }
    }

    fun addItem(sessionId: Long, exerciseId: Long, sets: Int, repMin: Int, repMax: Int, rest: Int, weight: Double?) {
        viewModelScope.launch {
            repo.insertProgramItem(
                ProgramItem(
                    sessionId = sessionId, exerciseId = exerciseId, itemOrder = 99,
                    targetSets = sets, repMin = repMin, repMax = repMax,
                    restSeconds = rest, weightKg = weight, enableProgressive = true
                )
            )
            rebuild()
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repo.deleteProgramItem(itemId)
            rebuild()
        }
    }

    fun setActive(programId: Long) {
        (application as FitPlanApp).activePlan.set(programId)
        _activeId.value = programId
    }

    fun renameSession(sessionId: Long, newName: String) {
        viewModelScope.launch {
            repo.renameSession(sessionId, newName)
            rebuild()
        }
    }
}

@Composable
fun ProgramScreen(onStartWorkout: () -> Unit, vm: ProgramViewModel = viewModel()) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    var expandedSession by remember { mutableStateOf<Long?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var showNewProgram by remember { mutableStateOf(false) }
    var addSessionId by remember { mutableStateOf<Long?>(null) }
    var pickedExercise by remember { mutableStateOf<Exercise?>(null) }
    var renameSessionId by remember { mutableStateOf<Long?>(null) }
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val activeId by vm.activeId.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { vm.exportJson().toByteArray(Charsets.UTF_8) }
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                    .onSuccess { status = "已导出方案 JSON" }
                    .onFailure { status = "导出失败：${it.message}" }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    } ?: ""
                    val n = vm.importJson(json)
                    status = "已导入 $n 套方案"
                }.onFailure { status = "导入失败：${it.message}" }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "我的方案",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            OutlinedButton(onClick = { exportLauncher.launch("fitplan_plans_${System.currentTimeMillis()}.json") }) {
                Text("导出(JSON)")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) {
                Text("导入(JSON)")
            }
            Button(onClick = { showNewProgram = true }) { Text("＋ 新建方案") }
        }
        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                    if (prog.id == activeId) {
                                        Text(
                                            "当前训练方案",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        OutlinedButton(onClick = { vm.setActive(prog.id) }) { Text("设为当前") }
                                    }
                                }
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
                                        Text("第 ${s.order} 节", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(s.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(onClick = { renameSessionId = s.id }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "重命名小节")
                                    }
                                    OutlinedButton(onClick = { expandedSession = if (expanded) null else s.id }) {
                                        Text(if (expanded) "收起" else "查看动作")
                                    }
                                }
                                if (expanded) {
                                    s.items.forEach { item ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
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
                                            IconButton(onClick = { vm.deleteItem(item.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "删除动作", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                        OutlinedButton(onClick = { addSessionId = s.id; pickedExercise = null }) { Text("添加动作") }
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

    if (showNewProgram) {
        NewProgramDialog(
            onDismiss = { showNewProgram = false },
            onCreate = { name, count ->
                vm.createProgram(name, count)
                showNewProgram = false
            }
        )
    }
    addSessionId?.let { sid ->
        val ex = pickedExercise
        if (ex == null) {
            ExerciseListDialog(
                exercises = exercises,
                onDismiss = { addSessionId = null },
                onPick = { pickedExercise = it }
            )
        } else {
            AddItemParamsDialog(
                exercise = ex,
                onDismiss = { pickedExercise = null; addSessionId = null },
                onConfirm = { sets, repMin, repMax, rest, weight ->
                    vm.addItem(sid, ex.id, sets, repMin, repMax, rest, weight)
                    pickedExercise = null
                    addSessionId = null
                }
            )
        }
    }
    renameSessionId?.let { sid ->
        val current = programs.asSequence()
            .flatMap { it.sessions.asSequence() }
            .firstOrNull { it.id == sid }?.name ?: ""
        RenameSessionDialog(
            currentName = current,
            onDismiss = { renameSessionId = null },
            onRename = { name ->
                if (name.isNotBlank()) vm.renameSession(sid, name.trim())
                renameSessionId = null
            }
        )
    }
}

@Composable
private fun RenameSessionDialog(currentName: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名小节") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("小节名称") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onRename(name) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NewProgramDialog(onDismiss: () -> Unit, onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("4") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建方案") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("方案名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = count, onValueChange = { count = it.filter { c -> c.isDigit() } }, label = { Text("每周几节(1-10)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val c = count.toIntOrNull()?.coerceIn(1, 10) ?: 4
                onCreate(name.trim(), c)
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ExerciseListDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要加入的动作") },
        text = {
            if (exercises.isEmpty()) { Text("动作库为空，请先到「动作」添加动作。") }
            else {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(exercises.size, key = { exercises[it].id }) { i ->
                        val ex = exercises[i]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 10.dp)
                        ) {
                            Text("${ex.name} · ${ex.muscleGroup}/${ex.equipment}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddItemParamsDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, repMin: Int, repMax: Int, restSeconds: Int, weight: Double?) -> Unit
) {
    var sets by remember { mutableStateOf("3") }
    var repMin by remember { mutableStateOf("8") }
    var repMax by remember { mutableStateOf("12") }
    var rest by remember { mutableStateOf("90") }
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.fitplan.app.ui.common.NumberField(label = "组数", value = sets, onValueChange = { sets = it }, modifier = Modifier.weight(1f))
                    com.fitplan.app.ui.common.NumberField(label = "最少次", value = repMin, onValueChange = { repMin = it }, modifier = Modifier.weight(1f))
                    com.fitplan.app.ui.common.NumberField(label = "最多次", value = repMax, onValueChange = { repMax = it }, modifier = Modifier.weight(1f))
                }
                com.fitplan.app.ui.common.NumberField(label = "休息秒", value = rest, onValueChange = { rest = it }, modifier = Modifier.fillMaxWidth())
                com.fitplan.app.ui.common.NumberField(label = "起始重量", value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } }, suffix = "kg", placeholder = "留空=自重", modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = sets.toIntOrNull() ?: 3
                val mn = repMin.toIntOrNull() ?: 8
                val mx = repMax.toIntOrNull() ?: 12
                onConfirm(s, mn, mx, rest.toIntOrNull() ?: 90, weight.toDoubleOrNull())
            }) { Text("加入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
