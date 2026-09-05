package com.fitplan.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitplan.app.FitPlanApp
import com.fitplan.app.data.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as FitPlanApp
    val db = app.database
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var apiKey by remember { mutableStateOf(app.vision.apiKey() ?: "") }
    var modelT by remember { mutableStateOf(app.vision.model()) }

    var importPendingUri by remember { mutableStateOf<Uri?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    BackupManager.exportJson(db).toByteArray(Charsets.UTF_8)
                }
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }.onSuccess {
                    snackbarHostState.showSnackbar("已导出 JSON 备份")
                }.onFailure {
                    snackbarHostState.showSnackbar("导出失败：${it.message}")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    BackupManager.exportBodyCsv(db).toByteArray(Charsets.UTF_8)
                }
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }.onSuccess {
                    snackbarHostState.showSnackbar("已导出身体数据 CSV")
                }.onFailure {
                    snackbarHostState.showSnackbar("导出失败：${it.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) importPendingUri = uri
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "所有数据仅保存在本机，不上传网络。建议定期导出备份到安全位置（例如云盘）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { exportJsonLauncher.launch("fitplan_backup_${System.currentTimeMillis()}.json") }) {
                        Text("导出完整备份 (JSON)")
                    }
                    OutlinedButton(onClick = { exportCsvLauncher.launch("fitplan_body_${System.currentTimeMillis()}.csv") }) {
                        Text("导出身体参数 (CSV)")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI 识别（智谱 GLM）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "用于「身体记录 → AI 识别读数」以及未来「拍食物记卡路里」。图片会上传到智谱，Key 仅存本机。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = modelT, onValueChange = { modelT = it }, label = { Text("模型名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = {
                        app.vision.setApiKey(apiKey.trim())
                        app.vision.setModel(modelT.trim().ifBlank { "glm-4.6v-flashx" })
                        scope.launch { snackbarHostState.showSnackbar("已保存 AI 设置") }
                    }) { Text("保存") }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("恢复数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "从 JSON 备份恢复将【覆盖】当前全部数据，请谨慎操作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) {
                        Text("从备份恢复 (JSON)")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "健身管家 · MVP\n数据仅存本地 · 无账号 · 无云端",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    importPendingUri?.let { uri ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { importPendingUri = null },
            title = { Text("确认恢复") },
            text = { Text("导入该备份将覆盖当前所有动作、计划与身体记录。继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val target = uri
                    importPendingUri = null
                    scope.launch {
                        val json = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(target)?.bufferedReader()?.use { it.readText() }
                        }
                        if (json.isNullOrBlank()) {
                            snackbarHostState.showSnackbar("文件为空或无法读取")
                            return@launch
                        }
                        runCatching {
                            withContext(Dispatchers.IO) { BackupManager.importJson(db, json) }
                        }.onSuccess {
                            snackbarHostState.showSnackbar("恢复完成")
                        }.onFailure {
                            snackbarHostState.showSnackbar("恢复失败：${it.message}")
                        }
                    }
                }) { Text("覆盖并恢复") }
            },
            dismissButton = { TextButton(onClick = { importPendingUri = null }) { Text("取消") } }
        )
    }
}
