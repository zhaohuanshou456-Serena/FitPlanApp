package com.fitplan.app.ui.screens.day

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.data.entity.ScheduledExercise
import com.fitplan.app.data.repository.SessionSuggestion
import com.fitplan.app.ui.common.smart
import com.fitplan.app.ui.workout.PendingRun
import com.fitplan.app.ui.workout.RunExercise
import com.fitplan.app.util.displayChinese
import com.fitplan.app.util.displayString
import com.fitplan.app.util.epochDayToLocalDate
import com.fitplan.app.util.toEpochDayLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class DayPlanViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FitPlanApp).repository

    val todayEpochDay: Long = LocalDate.now().toEpochDayLong()

    private val _selectedDay = MutableStateFlow(todayEpochDay)
    val selectedDay: StateFlow<Long> = _selectedDay

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    val exercises = repo.observeExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val dayItems = _selectedDay
        .flatMapLatest { repo.observeForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 首次打开确保内置 U/L 方案、要领/易错点动作库被写入（幂等）
        viewModelScope.launch {
            repo.ensureBuiltInPlan()
            repo.ensureCueExercises()
        }
    }

    /** 依据 A→B→C→D 轮换给出该天该练哪节；优先“当前方案”；无方案/该天已有安排返回 null */
    suspend fun suggest(day: Long): SessionSuggestion? =
        repo.nextSuggestedSession(day, (getApplication() as FitPlanApp).activePlan.get())

    fun applySuggestion(day: Long, programId: Long, sessionId: Long) {
        viewModelScope.launch { repo.applyProgramSession(day, programId, sessionId) }
    }

    val selectedDateLabel: String
        get() = _selectedDay.value.epochDayToLocalDate().displayChinese()

    val isToday: Boolean
        get() = _selectedDay.value == todayEpochDay

    fun shift(delta: Int) {
        _selectedDay.update { it + delta }
    }

    fun goToday() {
        _selectedDay.value = todayEpochDay
    }

    fun addScheduled(exerciseId: Long, sets: Int, reps: Int, weight: Double?, rest: Int) {
        viewModelScope.launch {
            val count = repo.scheduledOn(_selectedDay.value).size
            repo.addScheduled(
                ScheduledExercise(
                    dateEpochDay = _selectedDay.value,
                    exerciseId = exerciseId,
                    targetSets = sets,
                    targetReps = reps,
                    weight = weight,
                    restSeconds = rest,
                    sortOrder = count
                )
            )
        }
    }

    fun toggleCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { repo.setScheduledCompleted(id, !completed) }
    }

    fun updateParams(id: Long, sets: Int, reps: Int, weight: Double?, rest: Int) {
        viewModelScope.launch { repo.updateScheduledParams(id, sets, reps, weight, rest) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteScheduled(id) }
    }

    fun clearDate(day: Long) {
        viewModelScope.launch { repo.deleteForDate(day) }
    }

    fun copyLastWorkout() {
        viewModelScope.launch {
            val prev = repo.scheduledBefore(_selectedDay.value)
            if (prev.isEmpty()) {
                _message.value = "该日之前还没有训练记录，无法复制"
                return@launch
            }
            val lastDay = prev.maxOf { it.dateEpochDay }
            val source = prev.filter { it.dateEpochDay == lastDay }
            source.forEach {
                repo.addScheduled(
                    ScheduledExercise(
                        dateEpochDay = _selectedDay.value,
                        exerciseId = it.exerciseId,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                        weight = it.weight,
                        restSeconds = it.restSeconds,
                        sortOrder = it.sortOrder
                    )
                )
            }
            _message.value = "已从 ${lastDay.epochDayToLocalDate().displayChinese()} 复制 ${source.size} 个动作"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@Composable
fun DayPlanScreen(
    onStartWorkout: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    vm: DayPlanViewModel = viewModel()
) {
    val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
    val items by vm.dayItems.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    var suggestion by remember { mutableStateOf<SessionSuggestion?>(null) }
    LaunchedEffect(selectedDay, items.isEmpty()) {
        suggestion = if (items.isEmpty()) vm.suggest(selectedDay) else null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    var picking by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<AddTarget?>(null) }
    var deleteId by remember { mutableStateOf<Long?>(null) }

    val doneCount = items.count { it.isCompleted }
    val progress = if (items.isEmpty()) 0f else doneCount.toFloat() / items.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 日期导航
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = { vm.shift(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "前一天")
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        vm.selectedDateLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (vm.isToday) "今天 · ${LocalDate.now().displayString()}" else LocalDate.now().displayString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.shift(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "后一天")
                }
            }
            // 操作行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                if (!vm.isToday) {
                    TextButton(onClick = { vm.goToday() }) { Text("回到今天") }
                } else {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (items.isEmpty()) "今天尚未安排" else "已完成 $doneCount/${items.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    if (items.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Filled.History, contentDescription = "训练记录")
                }
                IconButton(onClick = { vm.copyLastWorkout() }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "复制上次训练")
                }
                IconButton(onClick = { deleteId = -1L }) {
                    Icon(Icons.Filled.Delete, contentDescription = "清空当日", tint = MaterialTheme.colorScheme.error)
                }
            }

            // 开始训练入口
            if (items.isNotEmpty()) {
                val todo = items.filter { !it.isCompleted }
                if (todo.isNotEmpty()) {
                    Button(
                        onClick = {
                            PendingRun.exercises = todo.map {
                                RunExercise(
                                    exerciseId = it.exerciseId,
                                    name = it.exerciseName,
                                    muscle = it.muscleGroup,
                                    equipment = it.equipment,
                                    sets = it.targetSets,
                                    reps = it.targetReps,
                                    restSeconds = it.restSeconds,
                                    startWeight = it.weight,
                                    scheduledId = it.id
                                )
                            }
                            PendingRun.sourceKind = "DAY"
                            PendingRun.sourceRef = selectedDay
                            PendingRun.title = "训练 · ${vm.selectedDateLabel}"
                            onStartWorkout()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("▶ 开始训练（剩 ${todo.size} 个动作 · 引导计时）")
                    }
                }
            }

            // 内容列表
            if (items.isEmpty()) {
                suggestion?.let { sg ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "方案 · 下一个应练",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(sg.sessionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${sg.programName} · A→B→C→D 轮换，练完自动进下一节",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { vm.applySuggestion(selectedDay, sg.programId, sg.sessionId) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text("一键应用今天") }
                        }
                    }
                }
                com.fitplan.app.ui.common.EmptyHint("这一天还没有安排动作\n点右下角 + 从动作库添加，或一键应用上方方案")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp)
                ) {
                    items(items.size, key = { items[it].id }) { idx ->
                        val row = items[idx]
                        DayItemCard(
                            name = row.exerciseName,
                            meta = "${row.muscleGroup} · ${row.equipment}",
                            sets = row.targetSets,
                            reps = row.targetReps,
                            weight = row.weight,
                            restSeconds = row.restSeconds,
                            completed = row.isCompleted,
                            onToggle = { vm.toggleCompleted(row.id, row.isCompleted) },
                            onEdit = {
                                editingRow = AddTarget(
                                    id = row.id,
                                    isEdit = true,
                                    exerciseId = row.exerciseId,
                                    exerciseName = row.exerciseName,
                                    sets = row.targetSets,
                                    reps = row.targetReps,
                                    weight = row.weight,
                                    rest = row.restSeconds
                                )
                            },
                            onDelete = { deleteId = row.id }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { picking = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加动作")
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp))
    }

    if (picking) {
        ExercisePickerDialog(
            exercises = vm.exercises.collectAsStateWithLifecycle().value,
            onDismiss = { picking = false },
            onPick = { exercise ->
                picking = false
                editingRow = AddTarget(
                    id = 0L,
                    isEdit = false,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    sets = 3,
                    reps = 12,
                    weight = null,
                    rest = 90
                )
            }
        )
    }

    editingRow?.let { target ->
        PlanParamDialog(
            title = if (target.isEdit) "编辑 ${target.exerciseName}" else "加入计划：${target.exerciseName}",
            initialSets = target.sets,
            initialReps = target.reps,
            initialWeight = target.weight,
            initialRest = target.rest,
            onDismiss = { editingRow = null },
            onConfirm = { sets, reps, weight, rest ->
                if (target.isEdit) vm.updateParams(target.id, sets, reps, weight, rest)
                else vm.addScheduled(target.exerciseId, sets, reps, weight, rest)
                editingRow = null
            }
        )
    }

    // 清空当日 / 删除单个
    if (deleteId != null) {
        val isClearAll = deleteId == -1L
        com.fitplan.app.ui.common.ConfirmDialog(
            title = if (isClearAll) "清空当日" else "移除该动作",
            message = if (isClearAll) "确定清空「${vm.selectedDateLabel}」的全部计划吗？" else "从这一天移除这个动作？",
            confirmText = if (isClearAll) "清空" else "移除",
            onConfirm = {
                if (isClearAll) vm.clearDate(selectedDay)
                else deleteId?.let { vm.delete(it) }
            },
            onDismiss = { deleteId = null }
        )
    }
}

/** 用于「新增/编辑」的统一目标参数 */
private data class AddTarget(
    val id: Long,
    val isEdit: Boolean,
    val exerciseId: Long,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: Double?,
    val rest: Int
)

@Composable
private fun DayItemCard(
    name: String,
    meta: String,
    sets: Int,
    reps: Int,
    weight: Double?,
    restSeconds: Int,
    completed: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Checkbox(checked = completed, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val weightText = weight?.let { "${it.smart()}kg" } ?: "自重"
                Text(
                    "$sets 组 × $reps 次  ·  $weightText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "组间休息 ${restSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "编辑") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit
) {
    val grouped = remember(exercises) {
        exercises.groupBy { it.muscleGroup }.toSortedMap()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要加入的动作") },
        text = {
            if (exercises.isEmpty()) {
                Text("动作库为空，请先到「动作」页添加动作。")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    grouped.forEach { (group, list) ->
                        item(key = "picker-head-$group") {
                            Text(
                                group,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(list.size, key = { "picker-${list[it].id}" }) { idx ->
                            val ex = list[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(ex) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${ex.equipment}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
private fun PlanParamDialog(
    title: String,
    initialSets: Int,
    initialReps: Int,
    initialWeight: Double?,
    initialRest: Int,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, reps: Int, weight: Double?, rest: Int) -> Unit
) {
    var sets by remember { mutableStateOf(initialSets.toString()) }
    var reps by remember { mutableStateOf(initialReps.toString()) }
    var weight by remember { mutableStateOf(initialWeight?.let { it.smart() } ?: "") }
    var rest by remember { mutableStateOf(initialRest.toString()) }

    val setsV = sets.toIntOrNull() ?: 0
    val repsV = reps.toIntOrNull() ?: 0
    val weightV = weight.toDoubleOrNull()
    val restV = rest.toIntOrNull() ?: 90

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.fitplan.app.ui.common.NumberField(
                        label = "组数",
                        value = sets,
                        onValueChange = { sets = it },
                        modifier = Modifier.weight(1f)
                    )
                    com.fitplan.app.ui.common.NumberField(
                        label = "每次数",
                        value = reps,
                        onValueChange = { reps = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                com.fitplan.app.ui.common.NumberField(
                    label = "重量",
                    value = weight,
                    onValueChange = { weight = it },
                    suffix = "kg",
                    placeholder = "留空 = 自重",
                    modifier = Modifier.fillMaxWidth()
                )
                com.fitplan.app.ui.common.NumberField(
                    label = "组间休息",
                    value = rest,
                    onValueChange = { rest = it },
                    suffix = "秒",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = setsV > 0 && repsV > 0,
                onClick = { onConfirm(setsV, repsV, weightV, restV) }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
