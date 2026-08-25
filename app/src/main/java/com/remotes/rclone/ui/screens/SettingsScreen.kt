package com.remotes.rclone.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.ContentCopy
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
    val customBin = remember { RcloneConfig.getRcloneBinaryPath(context) }
    val customConf = remember { RcloneConfig.getRcloneConfPath(context) }
    val binFound = remember { File(rcloneBin).exists() }
    val confFound = remember { File(rcloneConf).exists() }

    var statusMsg by remember { mutableStateOf("") }

    val binPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val path = RcloneConfig.copyFileToInternal(context, it, "rclone")
            if (path != null) {
                RcloneConfig.setRcloneBinaryPath(context, path)
                statusMsg = "rclone binary imported successfully"
            } else {
                statusMsg = "Failed to import rclone binary"
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
                statusMsg = "rclone.conf imported successfully"
            } else {
                statusMsg = "Failed to import rclone.conf"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
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
            Text(
                text = "Setup rclone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "This app needs the rclone binary and a rclone.conf config file. " +
                        "Copy both files to your device, then import them below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // RCLONE BINARY SECTION
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (binFound) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("rclone binary", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (customBin.isNotEmpty()) {
                        Text(
                            text = "Custom: $customBin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Active: $rcloneBin",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (binFound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To get the rclone binary, run in Termux:\n" +
                                "cp /data/data/com.termux/files/usr/bin/rclone \\\n" +
                                "  /sdcard/Download/rclone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

            // RCLONE CONF SECTION
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (confFound) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("rclone.conf", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (customConf.isNotEmpty()) {
                        Text(
                            text = "Custom: $customConf",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Active: $rcloneConf",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (confFound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To get rclone.conf, run in Termux:\n" +
                                "cp ~/.config/rclone/rclone.conf \\\n" +
                                "  /sdcard/Download/rclone.conf",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

            if (statusMsg.isNotEmpty()) {
                Snackbar(
                    action = {
                        TextButton(onClick = { statusMsg = "" }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(statusMsg)
                }
            }
        }
    }
}
