package com.remotes.rclone

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.remotes.rclone.model.FileItem
import com.remotes.rclone.model.QuotaInfo
import java.io.File

object RcloneConfig {

    private const val TAG = "RcloneConfig"
    private const val PREFS_NAME = "rclone_prefs"
    private const val KEY_RCLONE_BIN = "rclone_bin_path"
    private const val KEY_RCLONE_CONF = "rclone_conf_path"

    data class Result(val stdout: String, val stderr: String, val exitCode: Int)
    data class Diagnostic(
        val binPath: String,
        val binExists: Boolean,
        val binExecutable: Boolean,
        val confPath: String,
        val confExists: Boolean,
        val testResult: String
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setRcloneBinaryPath(context: Context, path: String) {
        prefs(context).edit().putString(KEY_RCLONE_BIN, path).apply()
    }

    fun setRcloneConfPath(context: Context, path: String) {
        prefs(context).edit().putString(KEY_RCLONE_CONF, path).apply()
    }

    fun getRcloneBinaryPath(context: Context): String = prefs(context).getString(KEY_RCLONE_BIN, "") ?: ""
    fun getRcloneConfPath(context: Context): String = prefs(context).getString(KEY_RCLONE_CONF, "") ?: ""

    fun findRcloneBinary(context: Context): String {
        val custom = prefs(context).getString(KEY_RCLONE_BIN, "") ?: ""
        if (custom.isNotEmpty()) {
            val f = File(custom)
            if (f.exists()) {
                f.setExecutable(true, false)
                return custom
            }
        }

        // Bundled as native lib (librclone.so) - Android extracts to executable path
        val nativeDir = File(context.applicationInfo.nativeLibraryDir, "librclone.so")
        if (nativeDir.exists()) {
            nativeDir.setExecutable(true, false)
            return nativeDir.absolutePath
        }

        val extDir = context.getExternalFilesDir(null)
        val candidates = listOf(
            File(context.filesDir, "rclone"),
            extDir?.let { File(it, "rclone") },
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "rclone"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "rclone"),
            File("/sdcard/Download/rclone"),
            File("/sdcard/Documents/rclone"),
            File("/data/local/tmp/rclone")
        ).filterNotNull()

        for (c in candidates) {
            if (c.exists()) {
                c.setExecutable(true, false)
                return c.absolutePath
            }
        }

        return "rclone"
    }

    fun findRcloneConf(context: Context): String {
        val custom = prefs(context).getString(KEY_RCLONE_CONF, "") ?: ""
        if (custom.isNotEmpty()) {
            val f = File(custom)
            if (f.exists()) return custom
        }

        val extDir = context.getExternalFilesDir(null)
        val candidates = listOf(
            File(context.filesDir, "rclone.conf"),
            extDir?.let { File(it, "rclone.conf") },
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "rclone.conf"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "rclone.conf"),
            File("/sdcard/Download/rclone.conf"),
            File("/sdcard/Documents/rclone.conf")
        ).filterNotNull()

        for (c in candidates) {
            if (c.exists()) return c.absolutePath
        }

        return ""
    }

    fun copyFileToInternal(context: Context, sourceUri: android.net.Uri, targetName: String): String? {
        return try {
            if (targetName == "rclone") {
                val target = File("/data/local/tmp/rclone")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                try {
                    val p = Runtime.getRuntime().exec(arrayOf("chmod", "777", target.absolutePath))
                    p.waitFor()
                } catch (_: Exception) {}
                Log.d(TAG, "Copied rclone to ${target.absolutePath} (size=${target.length()}, exec=${target.canExecute()})")
                return target.absolutePath
            }

            val target = File(context.filesDir, targetName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Log.d(TAG, "Copied $targetName to ${target.absolutePath} (size=${target.length()})")
            target.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy $targetName: ${e.message}")
            null
        }
    }

    fun runDiagnostic(context: Context): Diagnostic {
        val binPath = findRcloneBinary(context)
        val confPath = findRcloneConf(context)
        val binFile = File(binPath)
        val confFile = File(confPath)

        val binExists = binFile.exists()
        var binExecutable = false
        if (binExists) {
            binFile.setExecutable(true, false)
            binExecutable = binFile.canExecute()
        }

        val confExists = confFile.exists()

        var testResult = ""
        if (binExists) {
            val args = mutableListOf(binPath)
            if (confExists) {
                args.addAll(listOf("--config", confPath))
            }
            args.addAll(listOf("listremotes"))
            try {
                val pb = ProcessBuilder(args)
                pb.redirectErrorStream(true)
                pb.environment()["HOME"] = context.filesDir.absolutePath
                val proc = pb.start()
                val output = proc.inputStream.bufferedReader().readText()
                val exited = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                val code = if (exited) proc.exitValue() else -1
                testResult = "exit=$code output=[${output.trim()}]"
            } catch (e: Exception) {
                testResult = "error=${e.message}"
            }
        } else {
            testResult = "binary not found at: $binPath"
        }

        return Diagnostic(
            binPath = binPath,
            binExists = binExists,
            binExecutable = binExecutable,
            confPath = confPath,
            confExists = confExists,
            testResult = testResult
        )
    }

    fun runCommand(
        context: Context,
        args: List<String>,
        input: String? = null,
        timeoutMs: Long = 60_000
    ): Result {
        val rclone = findRcloneBinary(context)
        val confPath = findRcloneConf(context)

        val cmd = mutableListOf(rclone)
        if (confPath.isNotEmpty()) {
            cmd.addAll(listOf("--config", confPath))
        }
        cmd.addAll(args)

        Log.d(TAG, "Running: ${cmd.joinToString(" ")}")

        return try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(false)
            pb.environment()["HOME"] = context.filesDir.absolutePath

            val proc = pb.start()

            if (input != null) {
                proc.outputStream.bufferedWriter().use { it.write(input) }
            }

            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()

            val finished = proc.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            val exitCode = if (finished) proc.exitValue() else -1

            Log.d(TAG, "exit=$exitCode stdout=[${stdout.take(200)}] stderr=[${stderr.take(200)}]")
            Result(stdout, stderr, exitCode)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IOException: ${e.message}")
            Result("", "rclone not found at: $rclone - ${e.message}", -1)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            Result("", e.message ?: "Unknown error", -1)
        }
    }

    fun listRemotes(context: Context): List<String> {
        val r = runCommand(context, listOf("listremotes"))
        if (r.exitCode != 0) return emptyList()
        return r.stdout.lines().filter { it.isNotBlank() }.map { it.trimEnd(':') }
    }

    fun getAbout(context: Context, remote: String): QuotaInfo? {
        val r = runCommand(context, listOf("about", "$remote:"))
        if (r.exitCode != 0) return null
        var total: String? = null
        var used: String? = null
        for (line in r.stdout.lines()) {
            val trimmed = line.trim()
            if (trimmed.lowercase().startsWith("total:")) {
                total = trimmed.substringAfter(":").trim()
            } else if (trimmed.lowercase().startsWith("used:")) {
                used = trimmed.substringAfter(":").trim()
            }
        }
        if (total != null && used != null) {
            return QuotaInfo(used, total)
        }
        return null
    }

    fun listDir(context: Context, remote: String, path: String): List<FileItem> {
        val fullPath = if (path.isEmpty()) "$remote:" else "$remote:$path"
        val r = runCommand(context, listOf("lsjson", fullPath))
        if (r.exitCode != 0) return emptyList()

        return try {
            val arr = org.json.JSONArray(r.stdout)
            val items = mutableListOf<FileItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(
                    FileItem(
                        name = obj.getString("Name"),
                        type = if (obj.optBoolean("IsDir", false)) "dir" else "file",
                        size = obj.optLong("Size", 0)
                    )
                )
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun catFile(context: Context, remote: String, path: String): String? {
        val fullPath = if (path.isEmpty()) "$remote:" else "$remote:$path"
        val r = runCommand(context, listOf("cat", fullPath))
        if (r.exitCode != 0) return null
        return r.stdout
    }

    fun writeFile(context: Context, remote: String, path: String, content: String): String? {
        val fullPath = "$remote:$path"
        val r = runCommand(context, listOf("rcat", fullPath), input = content)
        return if (r.exitCode != 0) r.stderr else null
    }

    fun deleteFile(context: Context, remote: String, path: String): String? {
        val r = runCommand(context, listOf("deletefile", "$remote:$path"))
        return if (r.exitCode != 0) r.stderr else null
    }

    fun deleteFolder(context: Context, remote: String, path: String): String? {
        val r = runCommand(context, listOf("purge", "$remote:$path"))
        return if (r.exitCode != 0) r.stderr else null
    }

    fun moveItem(context: Context, remote: String, srcPath: String, dstPath: String): String? {
        ensureParent(context, remote, dstPath)
        val r = runCommand(context, listOf("moveto", "$remote:$srcPath", "$remote:$dstPath"))
        return if (r.exitCode != 0) r.stderr else null
    }

    fun mkdir(context: Context, remote: String, path: String): String? {
        val r = runCommand(context, listOf("mkdir", "$remote:$path"))
        return if (r.exitCode != 0) r.stderr else null
    }

    fun copyToRemote(context: Context, localPath: String, remote: String, remotePath: String): String? {
        ensureParent(context, remote, remotePath)
        val r = runCommand(context, listOf("copyto", localPath, "$remote:$remotePath"))
        return if (r.exitCode != 0) r.stderr else null
    }

    fun copyFromRemote(context: Context, remote: String, remotePath: String, localPath: String): String? {
        val r = runCommand(context, listOf("copyto", "$remote:$remotePath", localPath))
        return if (r.exitCode != 0) r.stderr else null
    }

    fun syncLocalToRemote(context: Context, localPath: String, remote: String, remotePath: String): String? {
        val r = runCommand(context, listOf("sync", localPath, "$remote:$remotePath"), timeoutMs = 300_000)
        return if (r.exitCode != 0) r.stderr else null
    }

    fun syncRemoteToLocal(context: Context, remote: String, remotePath: String, localPath: String): String? {
        val r = runCommand(context, listOf("sync", "$remote:$remotePath", localPath), timeoutMs = 300_000)
        return if (r.exitCode != 0) r.stderr else null
    }

    fun syncRemoteToRemote(
        context: Context,
        srcRemote: String, srcPath: String,
        dstRemote: String, dstPath: String
    ): String? {
        val r = runCommand(
            context,
            listOf("sync", "$srcRemote:$srcPath", "$dstRemote:$dstPath"),
            timeoutMs = 600_000
        )
        return if (r.exitCode != 0) r.stderr else null
    }

    private fun ensureParent(context: Context, remote: String, path: String) {
        val parent = path.substringBeforeLast("/", "")
        if (parent.isNotEmpty()) {
            mkdir(context, remote, parent)
        }
    }

    fun supportsMkdir(remote: String): Boolean {
        val noMkdir = listOf("gphotos", "imagekit", "box")
        return noMkdir.none { remote.lowercase().contains(it) }
    }

    fun isReadOnlyPath(path: String): Boolean {
        return path.lowercase().startsWith("shared with me")
    }
}
