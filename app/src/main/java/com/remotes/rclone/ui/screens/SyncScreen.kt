package com.remotes.rclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sync a local folder to the remote",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = localPath,
                onValueChange = { localPath = it },
                label = { Text("Local folder path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = remotePath,
                onValueChange = { remotePath = it },
                label = { Text("Remote folder path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
    isLoading: Boolean,
    statusMsg: String,
    errorMsg: String,
    onSync: (srcRemote: String, srcPath: String, dstRemote: String, dstPath: String) -> Unit,
    onBack: () -> Unit
) {
    var srcRemote by remember { mutableStateOf("") }
    var srcPath by remember { mutableStateOf("") }
    var dstRemote by remember { mutableStateOf("") }
    var dstPath by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sync from one remote to another",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = srcRemote,
                onValueChange = { srcRemote = it },
                label = { Text("Source remote (e.g. gdrive)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = srcPath,
                onValueChange = { srcPath = it },
                label = { Text("Source path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            OutlinedTextField(
                value = dstRemote,
                onValueChange = { dstRemote = it },
                label = { Text("Destination remote (e.g. dropbox)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dstPath,
                onValueChange = { dstPath = it },
                label = { Text("Destination path") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
