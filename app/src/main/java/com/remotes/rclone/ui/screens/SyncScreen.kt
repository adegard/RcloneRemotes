package com.remotes.rclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLocalRemoteScreen(
    remote: String,
    currentPath: String,
    isLoading: Boolean,
    statusMsg: String,
    errorMsg: String,
    onSync: (localPath: String, remotePath: String) -> Unit,
    onBack: () -> Unit
) {
    var localPath by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf(currentPath) }
    var showLocalPicker by remember { mutableStateOf(false) }
    var showRemotePicker by remember { mutableStateOf(false) }

    if (showLocalPicker) {
        LocalFolderPicker(
            initialPath = localPath,
            onPick = { localPath = it; showLocalPicker = false },
            onDismiss = { showLocalPicker = false }
        )
        return
    }

    if (showRemotePicker) {
        RemoteFolderPicker(
            remote = remote,
            initialPath = remotePath,
            onPick = { remotePath = it; showRemotePicker = false },
            onDismiss = { showRemotePicker = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Local -> Remote") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sync a local folder to the remote",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Local path
            OutlinedTextField(
                value = localPath,
                onValueChange = { localPath = it },
                label = { Text("Local folder path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showLocalPicker = true }) {
                        Icon(Icons.Default.Folder, "Browse")
                    }
                }
            )

            // Remote path
            OutlinedTextField(
                value = remotePath,
                onValueChange = { remotePath = it },
                label = { Text("Remote folder path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showRemotePicker = true }) {
                        Icon(Icons.Default.Cloud, "Browse")
                    }
                }
            )

            if (statusMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(text = statusMsg, modifier = Modifier.padding(8.dp))
                }
            }

            if (errorMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = { onSync(localPath, remotePath) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && localPath.isNotBlank()
            ) {
                Text("Start Sync")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncRemoteRemoteScreen(
    availableRemotes: List<String>,
    isLoading: Boolean,
    statusMsg: String,
    errorMsg: String,
    onSync: (srcRemote: String, srcPath: String, dstRemote: String, dstPath: String) -> Unit,
    onBack: () -> Unit
) {
    var srcRemote by remember { mutableStateOf(availableRemotes.firstOrNull() ?: "") }
    var srcPath by remember { mutableStateOf("") }
    var dstRemote by remember { mutableStateOf(availableRemotes.lastOrNull() ?: "") }
    var dstPath by remember { mutableStateOf("") }
    var showSrcPicker by remember { mutableStateOf(false) }
    var showDstPicker by remember { mutableStateOf(false) }

    if (showSrcPicker && srcRemote.isNotEmpty()) {
        RemoteFolderPicker(
            remote = srcRemote,
            initialPath = srcPath,
            onPick = { srcPath = it; showSrcPicker = false },
            onDismiss = { showSrcPicker = false }
        )
        return
    }

    if (showDstPicker && dstRemote.isNotEmpty()) {
        RemoteFolderPicker(
            remote = dstRemote,
            initialPath = dstPath,
            onPick = { dstPath = it; showDstPicker = false },
            onDismiss = { showDstPicker = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Remote -> Remote") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sync from one remote to another",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Source
            Text("Source", fontWeight = FontWeight.Bold)

            if (availableRemotes.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = srcRemote,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source remote") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableRemotes.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = { srcRemote = r; expanded = false }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = srcRemote,
                    onValueChange = { srcRemote = it },
                    label = { Text("Source remote") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = srcPath,
                onValueChange = { srcPath = it },
                label = { Text("Source path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { if (srcRemote.isNotEmpty()) showSrcPicker = true },
                        enabled = srcRemote.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Cloud, "Browse source")
                    }
                }
            )

            HorizontalDivider()

            // Destination
            Text("Destination", fontWeight = FontWeight.Bold)

            if (availableRemotes.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = dstRemote,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination remote") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableRemotes.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = { dstRemote = r; expanded = false }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = dstRemote,
                    onValueChange = { dstRemote = it },
                    label = { Text("Destination remote") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = dstPath,
                onValueChange = { dstPath = it },
                label = { Text("Destination path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { if (dstRemote.isNotEmpty()) showDstPicker = true },
                        enabled = dstRemote.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Cloud, "Browse destination")
                    }
                }
            )

            if (statusMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(text = statusMsg, modifier = Modifier.padding(8.dp))
                }
            }

            if (errorMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = { onSync(srcRemote, srcPath, dstRemote, dstPath) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && srcRemote.isNotBlank() && dstRemote.isNotBlank()
            ) {
                Text("Start Sync")
            }
        }
    }
}
