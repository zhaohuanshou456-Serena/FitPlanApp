package com.fitplan.app.ui.screens.body

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.entity.BodyRecord
import com.fitplan.app.ui.body.MetricCatalog
import com.fitplan.app.ui.body.TrendChart
import com.fitplan.app.ui.common.smart
import com.fitplan.app.util.timestampToDateString
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.OutlinedButton
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fitplan.app.util.VisionApi
import org.json.JSONObject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BodyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FitPlanApp).repository

    val records = repo.observeBodyAscending().stateIn(
        viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedMetric = mutableStateOf(MetricCatalog.metrics.first().key)
    val selectedMetric: String get() = _selectedMetric.value

    fun selectMetric(key: String) {
        _selectedMetric.value = key
    }

    fun save(record: BodyRecord) {
        viewModelScope.launch {
            if (record.id == 0L) repo.addBodyRecord(record.copy(timestamp = System.currentTimeMillis()))
            else repo.updateBodyRecord(record)
        }
    }

    fun delete(record: BodyRecord) {
        viewModelScope.launch { repo.deleteBodyRecord(record) }
    }
}

@Composable
fun BodyScreen(vm: BodyViewModel = viewModel()) {
    val records by vm.records.collectAsStateWithLifecycle()
    val metricDef = remember(vm.selectedMetric) { MetricCatalog.byKey(vm.selectedMetric) }

    val series = remember(records, vm.selectedMetric) {
        records.mapNotNull { r ->
            metricDef.extract(r)?.let { v -> r.timestamp to v }
        }
    }
    val latestValue = series.lastOrNull()?.second
    val delta = if (series.size >= 2) series[series.size - 1].second - series[series.size - 2].second else null
    val latestRecord = records.lastOrNull()

    var formRecord by remember { mutableStateOf<BodyRecord?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<BodyRecord?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 88.dp)
        ) {
            item {
                Text(
                    "身体",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            // 最新读数全览卡片
            item {
                OverviewGrid(latestRecord = latestRecord)
            }

            // 指标选择
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricCatalog.metrics.forEach { m ->
                        FilterChip(
                            selected = vm.selectedMetric == m.key,
                            onClick = { vm.selectMetric(m.key) },
                            label = { Text(m.label) }
                        )
                    }
                }
            }

            // 该指标最新值与变化 + 趋势图
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${metricDef.label}趋势", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            if (latestValue != null) {
                                Text(
                                    "${latestValue.smart()} ${metricDef.unit}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (delta != null) {
                            val up = delta > 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (up) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.height(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "较上次 ${delta.smart()} ${metricDef.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TrendChart(points = series.takeLast(60))
                        Text(
                            "共 ${series.size} 次记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                TextButton(onClick = { showNew = true }) {
                    Text("＋ 新增一次读数", color = MaterialTheme.colorScheme.primary)
                }
            }

            if (records.isEmpty()) {
                item {
                    com.fitplan.app.ui.common.EmptyHint("还没有身体参数记录\n点上面「新增一次读数」录入")
                }
            } else {
                item { com.fitplan.app.ui.common.SectionHeader("历史记录") }
                items(records.size, key = { records[records.size - 1 - it].id }) { i ->
                    val r = records[records.size - 1 - i] // 倒序，最新在前
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.timestamp.timestampToDateString(), fontWeight = FontWeight.Medium)
                                Text(
                                    listOfNotNull(
                                        r.weightKg?.let { "体重 ${it.smart()}kg" },
                                        r.bodyFatPct?.let { "体脂 ${it.smart()}%" },
                                        r.muscleKg?.let { "肌肉 ${it.smart()}kg" }
                                    ).joinToString(" · ").ifBlank { "（未填体重/体脂/肌肉）" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { formRecord = r }) {
                                Icon(Icons.Filled.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { deleteTarget = r }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNew = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新增读数")
        }
    }

    if (showNew) {
        BodyRecordDialog(
            record = null,
            onDismiss = { showNew = false },
            onSave = {
                vm.save(it)
                showNew = false
            }
        )
    }
    formRecord?.let { record ->
        BodyRecordDialog(
            record = record,
            onDismiss = { formRecord = null },
            onSave = {
                vm.save(it)
                formRecord = null
            }
        )
    }

    deleteTarget?.let { target ->
        com.fitplan.app.ui.common.ConfirmDialog(
            title = "删除记录",
            message = "删除 ${target.timestamp.timestampToDateString()} 的这条身体记录？",
            onConfirm = { vm.delete(target) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun OverviewGrid(latestRecord: BodyRecord?) {
    val items = listOf(
        "体重" to latestRecord?.weightKg?.let { "${it.smart()}kg" },
        "体脂率" to latestRecord?.bodyFatPct?.let { "${it.smart()}%" },
        "肌肉量" to latestRecord?.muscleKg?.let { "${it.smart()}kg" },
        "BMI" to latestRecord?.bmi?.let { it.smart() }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        value ?: "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (value == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyRecordDialog(
    record: BodyRecord?, // null 表示新增
    onDismiss: () -> Unit,
    onSave: (BodyRecord) -> Unit
) {
    val initial: (String) -> String = { key ->
        record?.let { MetricCatalog.byKey(key).extract(it)?.let { v -> v.smart() } } ?: ""
    }
    val values = remember(record) {
        MetricCatalog.metrics.map { initial(it.key) }.toMutableStateList()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoPath by remember { mutableStateOf(record?.photoPath) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val dest = withContext(Dispatchers.IO) {
                    val f = File(context.filesDir, "body_${System.currentTimeMillis()}.jpg")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inp -> f.outputStream().use { inp.copyTo(it) } }
                        f.absolutePath
                    } catch (e: Exception) { null }
                }
                dest?.let { photoPath = it }
            }
        }
    }

    val appKey = remember { (context.applicationContext as FitPlanApp).vision.apiKey() }
    val model = remember { (context.applicationContext as FitPlanApp).vision.model() }
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var aiRunning by remember { mutableStateOf(false) }

    fun recognize() {
        val path = photoPath ?: run { aiStatus = "请先拍照/选图"; return }
        val key = appKey ?: run { aiStatus = "请先在「设置」填 AI Key"; return }
        aiRunning = true
        aiStatus = "识别中…"
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { File(path).readBytes() }
                val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                val raw = VisionApi.recognize(key, model, dataUrl)
                val obj = JSONObject(VisionApi.extractJson(raw))
                val keys = mapOf(
                    "weight_kg" to "weight", "body_fat_pct" to "bodyFat", "muscle_kg" to "muscle",
                    "bone_kg" to "bone", "water_pct" to "water", "bmi" to "bmi",
                    "bmr_kcal" to "bmr", "visceral" to "visceralFat", "waist_cm" to "waist"
                )
                MetricCatalog.metrics.forEachIndexed { i, md ->
                    val gk = keys.entries.firstOrNull { it.value == md.key }?.key
                    if (gk != null && obj.has(gk) && !obj.isNull(gk)) {
                        values[i] = obj.optDouble(gk).toString()
                    }
                }
                aiStatus = "已识别，请核对"
            } catch (e: Exception) {
                aiStatus = "识别失败：${e.message}"
            } finally {
                aiRunning = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "新增身体读数" else "编辑身体读数") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    if (record == null) "按人体分析仪读数填写，未测的项留空即可。" else record.timestamp.timestampToDateString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pick.launch("image/*") }) { Text("拍照/选图") }
                    photoPath?.let { path ->
                        val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                        bmp?.let { b ->
                            Spacer(Modifier.width(8.dp))
                            Image(
                                bitmap = b.asImageBitmap(),
                                contentDescription = "读数照片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { recognize() }, enabled = !aiRunning) { Text("AI 识别读数") }
                    aiStatus?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                MetricCatalog.metrics.forEachIndexed { i, m ->
                    com.fitplan.app.ui.common.NumberField(
                        label = m.label,
                        value = values[i],
                        onValueChange = { values[i] = it },
                        suffix = m.unit.ifBlank { null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = values.any { it.isNotBlank() },
                onClick = {
                    var base = record ?: BodyRecord(timestamp = 0L)
                    values.forEachIndexed { i, text ->
                        if (text.isNotBlank()) {
                            val v = text.toDoubleOrNull()
                            if (v != null) base = MetricCatalog.withValue(base, MetricCatalog.metrics[i].key, v)
                        }
                    }
                    onSave(base.copy(photoPath = photoPath))
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
