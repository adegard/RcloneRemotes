package com.remotes.rclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remotes.rclone.ui.screens.*
import com.remotes.rclone.ui.theme.RcloneRemotesTheme

class MainActivity : ComponentActivity() {

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op, we proceed regardless */ }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            RcloneRemotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RcloneApp()
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            perms.addAll(
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        }
        if (perms.isNotEmpty()) {
            storagePermissionLauncher.launch(perms.toTypedArray())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            manageStorageLauncher.launch(intent)
        }
    }
}

@Composable
fun RcloneApp(vm: MainViewModel = viewModel()) {
    val screen by vm.screen.collectAsState()
    val remotes by vm.remotes.collectAsState()
    val selectedRemote by vm.selectedRemote.collectAsState()
    val currentPath by vm.currentPath.collectAsState()
    val items by vm.items.collectAsState()
    val quota by vm.quota.collectAsState()
    val statusMsg by vm.statusMsg.collectAsState()
    val errorMsg by vm.errorMsg.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val csvContent by vm.csvContent.collectAsState()
    val csvFileName by vm.csvFileName.collectAsState()
    val textContent by vm.textContent.collectAsState()
    val textFileName by vm.textFileName.collectAsState()

    when (screen) {
        is MainViewModel.Screen.RemoteSelector -> {
            RemoteSelectorScreen(
                remotes = remotes,
                errorMsg = errorMsg,
                onSelect = { vm.selectRemote(it) },
                onSettings = { vm.goToSettings() },
                onRefresh = { vm.loadRemotes() },
                isLoading = isLoading
            )
        }
        is MainViewModel.Screen.FileBrowser -> {
            FileBrowserScreen(
                remote = selectedRemote,
                path = currentPath,
                items = items,
                quota = quota,
                statusMsg = statusMsg,
                errorMsg = errorMsg,
                isLoading = isLoading,
                onBack = { vm.goBack() },
                onNavigateUp = { vm.navigateUp() },
                onOpenItem = { vm.openFile(it) },
                onCreateFile = { vm.createFile(it) },
                onCreateFolder = { vm.createFolder(it) },
                onDeleteItem = { vm.deleteItem(it) },
                onRenameItem = { item, name -> vm.renameItem(item, name) },
                onDownloadItem = { vm.downloadFile(it) },
                onUploadFile = { vm.uploadFile(it) },
                onSyncLocalRemote = { vm.goToSyncLocalRemote() },
                onSyncRemoteRemote = { vm.goToSyncRemoteRemote() },
                onRefresh = { vm.refresh() },
                onRemoteSelector = { vm.goToRemoteSelector() },
                onMoveItem = { item, dest -> vm.moveItem(item, dest) }
            )
        }
        is MainViewModel.Screen.CsvEditor -> {
            CsvEditorScreen(
                fileName = csvFileName,
                csvContent = csvContent,
                statusMsg = statusMsg,
                errorMsg = errorMsg,
                onSave = { vm.saveCsv(it) },
                onBack = { vm.goBack() }
            )
        }
        is MainViewModel.Screen.TextEditor -> {
            TextEditorScreen(
                fileName = textFileName,
                textContent = textContent,
                statusMsg = statusMsg,
                errorMsg = errorMsg,
                onSave = { vm.saveTextFile(it) },
                onBack = { vm.goBack() }
            )
        }
        is MainViewModel.Screen.SyncLocalRemote -> {
            SyncLocalRemoteScreen(
                remote = selectedRemote,
                currentPath = currentPath,
                isLoading = isLoading,
                statusMsg = statusMsg,
                errorMsg = errorMsg,
                onSync = { local, remote -> vm.syncLocalToRemote(local, remote) },
                onBack = { vm.goBack() }
            )
        }
        is MainViewModel.Screen.SyncRemoteRemote -> {
            SyncRemoteRemoteScreen(
                isLoading = isLoading,
                statusMsg = statusMsg,
                errorMsg = errorMsg,
                onSync = { sR, sP, dR, dP -> vm.syncRemoteToRemote(sR, sP, dR, dP) },
                onBack = { vm.goBack() }
            )
        }
        is MainViewModel.Screen.Settings -> {
            SettingsScreen(onBack = { vm.goBack() })
        }
    }
}
