package com.remotes.rclone.model

data class FileItem(
    val name: String,
    val type: String,
    val size: Long = 0
) {
    val isDir: Boolean get() = type == "dir"
    val isFile: Boolean get() = type == "file"

    val icon: String
        get() {
            if (isDir) return "\uD83D\uDCC1"
            val lower = name.lowercase()
            return when {
                lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp") -> "\uD83D\uDDBC\uFE0F"
                lower.endsWith(".py") -> "\uD83D\uDC0D"
                lower.endsWith(".sh") -> "\uD83D\uDCBB"
                lower.endsWith(".md") -> "\uD83D\uDCD8"
                lower.endsWith(".gba") || lower.endsWith(".z64") || lower.endsWith(".n64") || lower.endsWith(".v64") -> "\uD83C\uDFAE"
                lower.endsWith(".txt") || lower.endsWith(".json") || lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".ini") || lower.endsWith(".cfg") || lower.endsWith(".csv") -> "\uD83D\uDCC4"
                lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".ogg") || lower.endsWith(".wav") || lower.endsWith(".m4a") -> "\uD83C\uDFB5"
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".mov") -> "\uD83C\uDFAC"
                lower.endsWith(".pdf") -> "\uD83D\uDCD5"
                lower.endsWith(".zip") || lower.endsWith(".tar") || lower.endsWith(".gz") || lower.endsWith(".7z") || lower.endsWith(".rar") -> "\uD83D\uDCE6"
                else -> "\uD83D\uDCC4"
            }
        }

    val sizeFormatted: String
        get() {
            if (isDir) return ""
            if (size < 1024) return "$size B"
            if (size < 1024 * 1024) return "${size / 1024} KB"
            if (size < 1024 * 1024 * 1024) return "${size / (1024 * 1024)} MB"
            return "${size / (1024 * 1024 * 1024)} GB"
        }
}

data class RemoteInfo(
    val label: String,
    val name: String
)

data class QuotaInfo(
    val used: String,
    val total: String
)
