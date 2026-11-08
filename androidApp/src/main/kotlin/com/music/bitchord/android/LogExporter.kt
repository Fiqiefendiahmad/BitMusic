package com.music.bitchord.android

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps a small rotating text log in the app's private storage and exports a
 * snapshot of it (plus any recorded crash trace) into the public Downloads
 * folder so it can be shared from a device without adb.
 */
object LogExporter {
    private const val LOG_FILE = "bitmusic.log"
    private const val MAX_LOG_BYTES = 512 * 1024

    fun append(context: Context, level: Char, tag: String, message: String, error: Throwable? = null) {
        runCatching {
            val file = context.filesDir.resolve(LOG_FILE)
            if (file.exists() && file.length() > MAX_LOG_BYTES) {
                file.writeText("") // simple rotation
            }
            file.appendText(buildString {
                append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                append(" ")
                append(level)
                append("/")
                append(tag)
                append(": ")
                append(message)
                append("\n")
                error?.let {
                    append(android.util.Log.getStackTraceString(it).trimEnd())
                    append("\n")
                }
            })
        }
    }

    fun export(context: Context): Result<String> = runCatching {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "bitmusic_log_$stamp.txt"
        val content = buildString {
            appendLine("BitMusic Android log export")
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine()
            val log = context.filesDir.resolve(LOG_FILE)
            if (log.exists() && log.length() > 0) {
                appendLine("===== bitmusic.log =====")
                appendLine(log.readText())
            }
            val crash = context.filesDir.resolve("crash_last.txt")
            if (crash.exists() && crash.length() > 0) {
                appendLine("===== crash_last.txt =====")
                appendLine(crash.readText())
            }
            if (!log.exists() && !crash.exists()) {
                appendLine("No logs recorded yet.")
            }
        }
        writeToDownloads(context, fileName, content)
        fileName
    }

    private fun writeToDownloads(context: Context, fileName: String, content: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create Downloads entry")
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                ?: error("Could not open Downloads stream")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            File(dir, fileName).writeText(content)
        }
        return fileName
    }
}
