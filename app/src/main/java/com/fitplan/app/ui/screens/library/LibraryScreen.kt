package com.fitplan.app.ui.screens.library

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.entity.Exercise
import com.fitplan.app.ui.common.DropdownField
import com.fitplan.app.ui.common.EmptyHint
import com.fitplan.app.ui.common.SectionHeader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val MUSCLE_GROUPS = listOf("胸", "背", "腿", "臀", "肩", "手臂", "核心", "有氧", "其他")
val EQUIPMENTS = listOf("杠铃", "哑铃", "器械", "绳索", "自重", "有氧器械", "其他")

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FitPlanApp).repository

    val exercises = repo.observeExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun save(exercise: Exercise) {
        viewModelScope.launch {
            if (exercise.id == 0L) repo.addExercise(exercise.copy(isCustom = true))
            else repo.updateExercise(exercise.copy(isCustom = true))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteExercise(id) }
    }
}

@Composable
fun LibraryScreen(vm: LibraryViewModel = viewModel()) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Exercise?>(null) }
    var deleteTarget by remember { mutableStateOf<Exercise?>(null) }

    val grouped = remember(exercises) {
        exercises.groupBy { it.muscleGroup }.toSortedMap().toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 88.dp)
        ) {
            item {
                Text(
                    "动作库",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            if (exercises.isEmpty()) {
                item { EmptyHint("还没有动作，点右下角 + 添加") }
            }
            grouped.forEach { (group, list) ->
                item(key = "head-$group") { SectionHeader(group) }
                items(list.size, key = { "ex-${list[it].id}" }) { idx ->
                    val ex = list[idx]
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editTarget = ex }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "${ex.muscleGroup} · ${ex.equipment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!ex.note.isNullOrBlank()) {
                                    Text(
                                        ex.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { editTarget = ex }) {
                                Icon(Icons.Filled.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { deleteTarget = ex }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新增动作")
        }
    }

    if (showAdd) {
        ExerciseEditDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = {
                vm.save(it)
                showAdd = false
            }
        )
    }
    editTarget?.let { target ->
        ExerciseEditDialog(
            initial = target,
            onDismiss = { editTarget = null },
            onSave = {
                vm.save(it)
                editTarget = null
            }
        )
    }
    deleteTarget?.let { target ->
        com.fitplan.app.ui.common.ConfirmDialog(
            title = "删除动作",
            message = "确定删除「${target.name}」吗？历史计划条目会一并移除。",
            onConfirm = { vm.delete(target.id) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ExerciseEditDialog(
    initial: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var group by remember { mutableStateOf(initial?.muscleGroup ?: MUSCLE_GROUPS[0]) }
    var equip by remember { mutableStateOf(initial?.equipment ?: EQUIPMENTS[0]) }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增动作" else "编辑动作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(label = "目标肌群", value = group, options = MUSCLE_GROUPS, onSelect = { group = it })
                DropdownField(label = "器械/形式", value = equip, options = EQUIPMENTS, onSelect = { equip = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可空）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Exercise(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            muscleGroup = group,
                            equipment = equip,
                            note = note.trim().ifBlank { null },
                            isCustom = true
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
