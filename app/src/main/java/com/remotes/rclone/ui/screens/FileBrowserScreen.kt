package com.remotes.rclone.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remotes.rclone.model.FileItem
import com.remotes.rclone.model.QuotaInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(
    remote: String,
    path: String,
    items: List<FileItem>,
    quota: QuotaInfo?,
    statusMsg: String,
    errorMsg: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onNavigateUp: () -> Unit,
    onOpenItem: (FileItem) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteItem: (FileItem) -> Unit,
    onRenameItem: (FileItem, String) -> Unit,
    onDownloadItem: (FileItem) -> Unit,
    onUploadFile: (Uri) -> Unit,
    onSyncLocalRemote: () -> Unit,
    onSyncRemoteRemote: () -> Unit,
    onRefresh: () -> Unit,
    onRemoteSelector: () -> Unit,
    onMoveItem: (FileItem, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogType by remember { mutableStateOf("file") }
    var showDeleteDialog by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showMoveDialog by remember { mutableStateOf<FileItem?>(null) }
    var showMovePicker by remember { mutableStateOf(false) }
    var moveDestPath by remember { mutableStateOf("") }
    var contextMenu by remember { mutableStateOf<FileItem?>(null) }

    if (showMovePicker) {
        RemoteFolderPicker(
            remote = remote,
            initialPath = path,
            onPick = { moveDestPath = it; showMovePicker = false },
            onDismiss = { showMovePicker = false }
        )
        return
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onUploadFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$remote: $path",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (quota != null) {
                            Text(
                                text = "${quota.used} / ${quota.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (path.isNotEmpty()) onNavigateUp() else onRemoteSelector()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { createDialogType = "file"; showCreateDialog = true }) {
                        Icon(Icons.Default.NoteAdd, "New File")
                    }
                    IconButton(onClick = { createDialogType = "folder"; showCreateDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, "New Folder")
                    }
                    IconButton(onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    }) {
                        Icon(Icons.Default.Upload, "Upload")
                    }
                    IconButton(onClick = onSyncLocalRemote) {
                        Icon(Icons.Default.SyncAlt, "Sync Local")
                    }
                    IconButton(onClick = onSyncRemoteRemote) {
                        Icon(Icons.Default.SwapHoriz, "Sync Remote")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (statusMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = statusMsg,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = item.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            if (item.isFile) {
                                Text(
                                    text = item.sizeFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingContent = {
                            Text(
                                text = item.icon,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { contextMenu = item }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenItem(item) },
                            onLongClick = { contextMenu = item }
                        )
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        if (showCreateDialog) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = {
                    Text(if (createDialogType == "file") "New File" else "New Folder")
                },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = {
                            Text(if (createDialogType == "file") "File name" else "Folder name")
                        },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                if (createDialogType == "file") onCreateFile(name) else onCreateFolder(name)
                                showCreateDialog = false
                            }
                        }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                }
            )
        }

        showDeleteDialog?.let { item ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete '${item.name}'?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteItem(item)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }

        showRenameDialog?.let { item ->
            var newName by remember { mutableStateOf(item.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("Rename '${item.name}'") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newName.isNotBlank() && newName != item.name) {
                                onRenameItem(item, newName)
                                showRenameDialog = null
                            }
                        }
                    ) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
                }
            )
        }

        contextMenu?.let { item ->
            AlertDialog(
                onDismissRequest = { contextMenu = null },
                title = { Text(item.name) },
                text = {
                    Column {
                        TextButton(onClick = { onOpenItem(item); contextMenu = null }) {
                            Icon(Icons.Default.OpenInNew, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open")
                        }
                        if (item.isFile) {
                            TextButton(onClick = { onDownloadItem(item); contextMenu = null }) {
                                Icon(Icons.Default.Download, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download")
                            }
                        }
                        TextButton(onClick = { showRenameDialog = item; contextMenu = null }) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rename")
                        }
                        TextButton(onClick = { showMoveDialog = item; contextMenu = null }) {
                            Icon(Icons.Default.DriveFileMove, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Move")
                        }
                        TextButton(
                            onClick = { showDeleteDialog = item; contextMenu = null },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { contextMenu = null }) { Text("Close") }
                }
            )
        }

        showMoveDialog?.let { item ->
            AlertDialog(
                onDismissRequest = { showMoveDialog = null },
                title = { Text("Move '${item.name}'") },
                text = {
                    OutlinedTextField(
                        value = moveDestPath,
                        onValueChange = { moveDestPath = it },
                        label = { Text("Destination folder path") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showMovePicker = true }) {
                                Icon(Icons.Default.Folder, "Browse folders")
                            }
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onMoveItem(item, moveDestPath)
                            showMoveDialog = null
                        }
                    ) { Text("Move") }
                },
                dismissButton = {
                    TextButton(onClick = { showMoveDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}
