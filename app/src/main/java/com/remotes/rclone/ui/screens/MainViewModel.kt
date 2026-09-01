package com.remotes.rclone.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remotes.rclone.RcloneConfig
import com.remotes.rclone.model.FileItem
import com.remotes.rclone.model.QuotaInfo
import com.remotes.rclone.model.RemoteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    private val _remotes = MutableStateFlow<List<RemoteInfo>>(emptyList())
    val remotes: StateFlow<List<RemoteInfo>> = _remotes

    private val _selectedRemote = MutableStateFlow("")
    val selectedRemote: StateFlow<String> = _selectedRemote

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private val _items = MutableStateFlow<List<FileItem>>(emptyList())
    val items: StateFlow<List<FileItem>> = _items

    private val _quota = MutableStateFlow<QuotaInfo?>(null)
    val quota: StateFlow<QuotaInfo?> = _quota

    private val _statusMsg = MutableStateFlow("")
    val statusMsg: StateFlow<String> = _statusMsg

    private val _errorMsg = MutableStateFlow("")
    val errorMsg: StateFlow<String> = _errorMsg

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _screen = MutableStateFlow<Screen>(Screen.RemoteSelector)
    val screen: StateFlow<Screen> = _screen

    private val _csvContent = MutableStateFlow("")
    val csvContent: StateFlow<String> = _csvContent

    private val _csvPath = MutableStateFlow("")
    val csvPath: StateFlow<String> = _csvPath

    private val _csvFileName = MutableStateFlow("")
    val csvFileName: StateFlow<String> = _csvFileName

    private val _textContent = MutableStateFlow("")
    val textContent: StateFlow<String> = _textContent

    private val _textPath = MutableStateFlow("")
    val textPath: StateFlow<String> = _textPath

    private val _textFileName = MutableStateFlow("")
    val textFileName: StateFlow<String> = _textFileName

    private val _searchMode = MutableStateFlow(false)
    val searchMode: StateFlow<Boolean> = _searchMode

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults

    sealed class Screen {
        object RemoteSelector : Screen()
        object FileBrowser : Screen()
        object CsvEditor : Screen()
        object TextEditor : Screen()
        object SyncLocalRemote : Screen()
        object SyncRemoteRemote : Screen()
        object Settings : Screen()
    }

    init {
        loadRemotes()
    }

    fun loadRemotes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMsg.value = ""
            val r = RcloneConfig.runCommand(ctx, listOf("listremotes"))
            if (r.exitCode != 0 && r.stdout.isBlank()) {
                _errorMsg.value = "rclone error: ${r.stderr.ifBlank { r.stdout }.ifBlank { "binary not working" }}"
            }
            val raw = r.stdout.lines().filter { it.isNotBlank() }.map { it.trimEnd(':') }
            val builtIn = listOf(
                RemoteInfo("Box", "box"),
                RemoteInfo("Dropbox", "dropbox"),
                RemoteInfo("Google Drive", "gdrive"),
                RemoteInfo("Google Photos", "gphotos"),
                RemoteInfo("OneDrive", "onedrive"),
                RemoteInfo("pCloud", "pcloud")
            )
            val available = builtIn.filter { b ->
                raw.any { it.lowercase() == b.name.lowercase() }
            }
            val extra = raw.filter { r2 ->
                builtIn.none { it.name.lowercase() == r2.lowercase() }
            }.map { RemoteInfo(it.replaceFirstChar { c -> c.uppercase() }, it) }

            _remotes.value = available + extra
            _isLoading.value = false
        }
    }

    fun selectRemote(remote: String) {
        _selectedRemote.value = remote
        _currentPath.value = ""
        _statusMsg.value = ""
        _errorMsg.value = ""
        _screen.value = Screen.FileBrowser
        refresh()
    }

    fun goToRemoteSelector() {
        _screen.value = Screen.RemoteSelector
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val list = RcloneConfig.listDir(ctx, _selectedRemote.value, _currentPath.value)
            _items.value = list
            _quota.value = RcloneConfig.getAbout(ctx, _selectedRemote.value)
            _isLoading.value = false
        }
    }

    fun navigateInto(item: FileItem) {
        val basePath = _currentPath.value
        val newPath = if (basePath.isEmpty()) item.name else "$basePath/${item.name}"
        _currentPath.value = newPath
        _statusMsg.value = ""
        _errorMsg.value = ""
        refresh()
    }

    fun navigateUp(): Boolean {
        val path = _currentPath.value
        if (path.isEmpty()) return false
        val parent = path.substringBeforeLast("/", "")
        _currentPath.value = parent
        _statusMsg.value = ""
        _errorMsg.value = ""
        refresh()
        return true
    }

    fun setSearchMode(enabled: Boolean) {
        _searchMode.value = enabled
        if (!enabled) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun searchFiles(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val results = RcloneConfig.search(ctx, _selectedRemote.value, _currentPath.value, query)
            _searchResults.value = results
            _isLoading.value = false
        }
    }

    fun openSearchResult(item: FileItem) {
        setSearchMode(false)
        refresh()
    }

    fun openFile(item: FileItem) {
        if (item.isDir) {
            navigateInto(item)
            return
        }
        val fullPath = item.fullPath ?: if (_currentPath.value.isEmpty()) item.name else "${_currentPath.value}/${item.name}"
        val lower = item.name.lowercase()

        if (lower.endsWith(".csv")) {
            viewModelScope.launch(Dispatchers.IO) {
                val content = RcloneConfig.catFile(ctx, _selectedRemote.value, fullPath)
                if (content != null) {
                    _csvContent.value = content
                    _csvPath.value = fullPath
                    _csvFileName.value = item.name
                    _screen.value = Screen.CsvEditor
                } else {
                    _errorMsg.value = "Failed to read CSV file"
                }
            }
        } else if (listOf(".py", ".txt", ".md", ".json", ".yaml", ".yml", ".sh", ".xml", ".cfg", ".ini", ".conf", ".log", ".html", ".css", ".js", ".kt", ".java").any { lower.endsWith(it) }) {
            viewModelScope.launch(Dispatchers.IO) {
                val content = RcloneConfig.catFile(ctx, _selectedRemote.value, fullPath)
                if (content != null) {
                    _textContent.value = content
                    _textPath.value = fullPath
                    _textFileName.value = item.name
                    _screen.value = Screen.TextEditor
                } else {
                    _errorMsg.value = "Failed to read file"
                }
            }
        } else {
            // Binary files: download to cache and open with external viewer
            viewModelScope.launch(Dispatchers.IO) {
                _statusMsg.value = "Downloading ${item.name}..."
                val tmpFile = File(ctx.cacheDir, item.name)
                val err = RcloneConfig.copyFromRemote(ctx, _selectedRemote.value, fullPath, tmpFile.absolutePath)
                if (err != null || !tmpFile.exists()) {
                    _errorMsg.value = err ?: "Download failed"
                    _statusMsg.value = ""
                    return@launch
                }
                _statusMsg.value = ""
                try {
                    val uri = FileProvider.getUriForFile(
                        ctx,
                        "${ctx.packageName}.provider",
                        tmpFile
                    )
                    val mime = ctx.contentResolver.getType(uri)
                        ?: when {
                            lower.endsWith(".pdf") -> "application/pdf"
                            lower.endsWith(".png") -> "image/png"
                            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
                            lower.endsWith(".gif") -> "image/gif"
                            lower.endsWith(".mp4") -> "video/mp4"
                            lower.endsWith(".mp3") -> "audio/mpeg"
                            lower.endsWith(".zip") -> "application/zip"
                            lower.endsWith(".epub") -> "application/epub+zip"
                            else -> "*/*"
                        }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(ctx.packageManager) != null) {
                        ctx.startActivity(intent)
                    } else {
                        _errorMsg.value = "No viewer found for $mime"
                    }
                } catch (e: android.content.ActivityNotFoundException) {
                    _errorMsg.value = "No viewer found for this file type"
                } catch (e: Exception) {
                    _errorMsg.value = "Failed to open file: ${e.message}"
                }
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = if (_currentPath.value.isEmpty()) name else "${_currentPath.value}/$name"
            val err = RcloneConfig.writeFile(ctx, _selectedRemote.value, path, "")
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "File '$name' created"
            }
            refresh()
        }
    }

    fun createFolder(name: String) {
        if (RcloneConfig.isReadOnlyPath(_currentPath.value)) {
            _errorMsg.value = "Cannot create folders in 'Shared with me'"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val path = if (_currentPath.value.isEmpty()) name else "${_currentPath.value}/$name"
            val err = RcloneConfig.mkdir(ctx, _selectedRemote.value, path)
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Folder '$name' created"
            }
            refresh()
        }
    }

    private fun itemPath(item: FileItem): String =
        item.fullPath ?: if (_currentPath.value.isEmpty()) item.name else "${_currentPath.value}/${item.name}"

    private fun itemBasePath(item: FileItem): String {
        item.fullPath?.let {
            return it.substringBeforeLast("/", "").let { parent ->
                if (parent == it) "" else parent
            }
        }
        return _currentPath.value
    }

    fun deleteItem(item: FileItem) {
        if (RcloneConfig.isReadOnlyPath(itemBasePath(item))) {
            _errorMsg.value = "Cannot delete in 'Shared with me'"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val path = itemPath(item)
            val err = if (item.isDir) {
                RcloneConfig.deleteFolder(ctx, _selectedRemote.value, path)
            } else {
                RcloneConfig.deleteFile(ctx, _selectedRemote.value, path)
            }
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "'${item.name}' deleted"
            }
            refresh()
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        if (RcloneConfig.isReadOnlyPath(itemBasePath(item))) {
            _errorMsg.value = "Cannot rename in 'Shared with me'"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val basePath = itemBasePath(item)
            val oldPath = itemPath(item)
            val newPath = if (basePath.isEmpty()) newName else "$basePath/$newName"
            val err = RcloneConfig.moveItem(ctx, _selectedRemote.value, oldPath, newPath)
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Renamed to '$newName'"
            }
            refresh()
        }
    }

    fun moveItem(item: FileItem, destPath: String) {
        if (RcloneConfig.isReadOnlyPath(itemBasePath(item))) {
            _errorMsg.value = "Cannot move in 'Shared with me'"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val srcPath = itemPath(item)
            val dstPath = if (destPath.isEmpty()) item.name else "$destPath/${item.name}"
            val err = RcloneConfig.moveItem(ctx, _selectedRemote.value, srcPath, dstPath)
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Moved to /$destPath"
            }
            refresh()
        }
    }

    fun uploadFile(uri: Uri) {
        if (RcloneConfig.isReadOnlyPath(_currentPath.value)) {
            _errorMsg.value = "Cannot upload to 'Shared with me'"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = ctx.contentResolver
            var fileName = "upload"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx >= 0) {
                    fileName = cursor.getString(nameIdx)
                }
            }

            val tmpFile = File(ctx.cacheDir, fileName)
            resolver.openInputStream(uri)?.use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val remotePath = if (_currentPath.value.isEmpty()) fileName else "${_currentPath.value}/$fileName"
            val err = RcloneConfig.copyToRemote(ctx, tmpFile.absolutePath, _selectedRemote.value, remotePath)
            tmpFile.delete()

            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Uploaded '$fileName'"
            }
            refresh()
        }
    }

    fun downloadFile(item: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val remotePath = itemPath(item)
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val localPath = File(downloads, item.name).absolutePath
            val err = RcloneConfig.copyFromRemote(ctx, _selectedRemote.value, remotePath, localPath)
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Downloaded to $localPath"
            }
        }
    }

    fun saveCsv(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val err = RcloneConfig.writeFile(ctx, _selectedRemote.value, _csvPath.value, content)
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "CSV saved"
                _csvContent.value = content
            }
        }
    }

    fun saveTextFile(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val err = RcloneConfig.writeFile(ctx, _selectedRemote.value, _textPath.value, content)
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "File saved"
                _textContent.value = content
            }
        }
    }

    fun goToCsvEditor() {
        _screen.value = Screen.CsvEditor
    }

    fun goToTextEditor() {
        _screen.value = Screen.TextEditor
    }

    fun goToSyncLocalRemote() {
        _screen.value = Screen.SyncLocalRemote
    }

    fun goToSyncRemoteRemote() {
        _screen.value = Screen.SyncRemoteRemote
    }

    fun goToSettings() {
        _screen.value = Screen.Settings
    }

    fun syncLocalToRemote(localPath: String, remotePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _statusMsg.value = "Syncing local -> remote..."
            val err = RcloneConfig.syncLocalToRemote(ctx, localPath, _selectedRemote.value, remotePath)
            _isLoading.value = false
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Sync complete"
            }
            refresh()
        }
    }

    fun syncRemoteToLocal(remotePath: String, localPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _statusMsg.value = "Syncing remote -> local..."
            val err = RcloneConfig.syncRemoteToLocal(ctx, _selectedRemote.value, remotePath, localPath)
            _isLoading.value = false
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Sync complete"
            }
            refresh()
        }
    }

    fun syncRemoteToRemote(srcRemote: String, srcPath: String, dstRemote: String, dstPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _statusMsg.value = "Syncing $srcRemote:$srcPath -> $dstRemote:$dstPath..."
            val err = RcloneConfig.syncRemoteToRemote(ctx, srcRemote, srcPath, dstRemote, dstPath)
            _isLoading.value = false
            if (err != null) {
                _errorMsg.value = err
            } else {
                _statusMsg.value = "Remote-to-remote sync complete"
            }
        }
    }

    fun clearMessages() {
        _statusMsg.value = ""
        _errorMsg.value = ""
    }

    fun goBack() {
        when (_screen.value) {
            Screen.CsvEditor, Screen.TextEditor -> _screen.value = Screen.FileBrowser
            Screen.SyncLocalRemote, Screen.SyncRemoteRemote, Screen.Settings -> _screen.value = Screen.FileBrowser
            Screen.FileBrowser -> navigateUp()
            Screen.RemoteSelector -> {}
        }
    }
}
