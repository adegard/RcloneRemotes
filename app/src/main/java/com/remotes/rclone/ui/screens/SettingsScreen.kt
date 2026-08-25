package com.remotes.rclone.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remotes.rclone.RcloneConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var diag by remember { mutableStateOf<RcloneConfig.Diagnostic?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun runDiag() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val d = RcloneConfig.runDiagnostic(context)
            withContext(Dispatchers.Main) {
                diag = d
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { runDiag() }

    val binPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val path = RcloneConfig.copyFileToInternal(context, it, "rclone")
            if (path != null) {
                RcloneConfig.setRcloneBinaryPath(context, path)
                runDiag()
            }
        }
    }

    val confPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val path = RcloneConfig.copyFileToInternal(context, it, "rclone.conf")
            if (path != null) {
                RcloneConfig.setRcloneConfPath(context, path)
                runDiag()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { runDiag() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            diag?.let { d ->
                // BINARY STATUS
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (d.binExists) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (d.binExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("rclone binary", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Path: ${d.binPath}", style = MaterialTheme.typography.bodySmall)
                        Text("Exists: ${d.binExists}", style = MaterialTheme.typography.bodySmall)
                        Text("Executable: ${d.binExecutable}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { binPicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import rclone binary")
                        }
                    }
                }

                // CONF STATUS
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (d.confExists) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (d.confExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("rclone.conf", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (d.confPath.isNotEmpty()) "Path: ${d.confPath}" else "No config found",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Exists: ${d.confExists}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { confPicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import rclone.conf")
                        }
                    }
                }

                // TEST RESULT
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Test: rclone listremotes", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = d.testResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (d.testResult.contains("exit=0"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                // INSTRUCTIONS
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Setup Instructions", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. In Termux, copy the rclone binary:\n" +
                                    "\n" +
                                    "  cp \$(which rclone) /sdcard/Download/rclone\n" +
                                    "\n" +
                                    "2. Copy your config:\n" +
                                    "\n" +
                                    "  cp ~/.config/rclone/rclone.conf /sdcard/Download/rclone.conf\n" +
                                    "\n" +
                                    "3. Open this app's settings and import both files using the buttons above.\n" +
                                    "\n" +
                                    "4. Tap Refresh to verify.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
