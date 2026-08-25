package com.remotes.rclone

import android.content.Context
import com.remotes.rclone.model.FileItem
import com.remotes.rclone.model.QuotaInfo
import java.io.File

object RcloneConfig {

    data class Result(val stdout: String, val stderr: String, val exitCode: Int)

    fun findRcloneBinary(context: Context): String {
        val candidates = listOf(
            "/data/data/com.termux/files/usr/bin/rclone",
            "/system/bin/rclone",
            "/system/xbin/rclone",
            "${context.filesDir.parentFile?.parentFile}/usr/bin/rclone",
            "/data/data/com.termux/files/home/.local/bin/rclone"
        )
        for (c in candidates) {
            if (File(c).exists()) return c
        }
        val localBin = File(context.filesDir, "rclone")
        if (localBin.exists()) return localBin.absolutePath
        return "rclone"
    }

    fun findRcloneConf(context: Context): String {
        val candidates = listOf(
            File("/data/data/com.termux/files/home/storage/downloads/rclone.conf"),
            File(System.getProperty("user.home") ?: "", ".config/rclone/rclone.conf"),
            File(context.filesDir, "rclone.conf")
        )
        for (c in candidates) {
            if (c.exists()) return c.absolutePath
        }
        return ""
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

            Result(stdout, stderr, exitCode)
        } catch (e: java.io.IOException) {
            Result("", "rclone not found at: $rclone", -1)
        } catch (e: Exception) {
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
