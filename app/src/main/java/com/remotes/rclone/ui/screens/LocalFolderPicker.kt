package com.remotes.rclone.ui.screens

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFolderPicker(
    initialPath: String = "",
    onPick: (path: String) -> Unit,
    onDismiss: () -> Unit
) {
    var path by remember { mutableStateOf(initialPath.ifEmpty { Environment.getExternalStorageDirectory().absolutePath }) }
    var folders by remember { mutableStateOf<List<File>>(emptyList()) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(path) {
        val dir = File(path)
        if (dir.exists() && dir.isDirectory) {
            folders = dir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
            error = ""
        } else {
            error = "Cannot access: $path"
            folders = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select folder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onPick(path) }
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select here")
                    }
                }
            )
        }
    ) { padding ->
        if (error.isNotEmpty()) {
            Text(
                text = error,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        // Parent directory
        val parent = File(path).parentFile
        if (parent != null && parent.exists()) {
            ListItem(
                headlineContent = { Text("..") },
                leadingContent = { Icon(Icons.Default.SdStorage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable { path = parent.absolutePath }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(folders) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { path = folder.absolutePath }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (folders.isEmpty() && error.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No subfolders",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
