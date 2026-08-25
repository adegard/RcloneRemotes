package com.remotes.rclone.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remotes.rclone.RcloneConfig
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rcloneBin = remember { RcloneConfig.findRcloneBinary(context) }
    val rcloneConf = remember { RcloneConfig.findRcloneConf(context) }
    val confExists = remember { File(rcloneConf).exists() }
    val binExists = remember { File(rcloneBin).exists() }

    var confText by remember {
        mutableStateOf(
            if (confExists) try {
                File(rcloneConf).readText()
            } catch (e: Exception) {
                "Error reading config"
            } else ""
        )
    }

    val confPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val target = File(context.filesDir, "rclone.conf")
                    target.outputStream().use { output -> input.copyTo(output) }
                    confText = target.readText()
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rclone Binary", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rcloneBin,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (binExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (binExists) "Found" else "Not found",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (binExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Rclone Config", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (confExists) rcloneConf else "Not found",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (confExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            confPicker.launch(arrayOf("*/*"))
                        }) {
                            Icon(Icons.Default.FolderOpen, "Import config")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Config Contents", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (confText.isNotEmpty()) {
                        Text(
                            text = confText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "No configuration loaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
