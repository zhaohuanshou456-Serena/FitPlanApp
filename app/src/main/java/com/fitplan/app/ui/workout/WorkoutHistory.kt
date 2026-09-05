package com.fitplan.app.ui.workout

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.fitplan.app.data.workout.WorkoutSession
import com.fitplan.app.data.workout.WorkoutSet
import com.fitplan.app.ui.common.smart
import com.fitplan.app.util.timestampToDateString
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FitPlanApp).repository
    val sessions = repo.observeWorkoutSessions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    suspend fun setsOf(sessionId: Long): List<WorkoutSet> = repo.setsOfWorkout(sessionId)
    suspend fun delete(id: Long) = repo.deleteWorkout(id)
}

@Composable
fun WorkoutHistoryScreen(onBack: () -> Unit, vm: WorkoutHistoryViewModel = viewModel()) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var setsMap by remember { mutableStateOf<Map<Long, List<WorkoutSet>>>(emptyMap()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            androidx.compose.material3.TextButton(onClick = onBack) { Text("返回") }
            Text("训练记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (sessions.isEmpty()) {
            com.fitplan.app.ui.common.EmptyHint("还没有训练记录\n用「开始训练」完成一次后这里会显示")
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
                items(sessions.size, key = { sessions[it].id }) { i ->
                    val s = sessions[i]
                    val expanded = expandedId == s.id
                    val sets = setsMap[s.id]
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).clickable {
                                    if (expanded) {
                                        expandedId = null
                                    } else {
                                        expandedId = s.id
                                        if (sets == null) {
                                            scope.launch { setsMap = setsMap + (s.id to vm.setsOf(s.id)) }
                                        }
                                    }
                                }.padding(vertical = 4.dp)) {
                                    Text(
                                        s.title ?: sourceLabel(s.sourceKind),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        timeLabel(s),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${sets?.size ?: "?"} 组",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                val arrIcon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                                Icon(arrIcon, contentDescription = null)
                                IconButton(onClick = {
                                    scope.launch {
                                        vm.delete(s.id)
                                        setsMap = setsMap - s.id
                                        if (expandedId == s.id) expandedId = null
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (expanded && sets != null) {
                                sets.groupBy { it.exerciseId }.forEach { (_, list) ->
                                    val name = list.first().exerciseName
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    list.forEach { st ->
                                        Text(
                                            "  ${st.setIndex}. ${st.weightKg?.let { "${it.smart()}kg" } ?: "自重"} × ${st.reps} 次",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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

private fun sourceLabel(kind: String): String = when (kind) {
    "DAY" -> "按计划训练"
    "PLAN" -> "方案训练"
    else -> "自由训练"
}

private fun timeLabel(s: WorkoutSession): String {
    val start = s.startedAt.timestampToDateString()
    return if (s.endedAt != null) "$start · 已完成" else "$start · 未结束"
}
