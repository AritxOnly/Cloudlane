package com.aritxonly.cloudlane.data

import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import android.net.Uri
import com.aritxonly.cloudlane.domain.PhotoRules
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PhotoScanner(private val context: Context) {
    companion object { private val mutex = Mutex() }

    suspend fun scan(): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val index = PhotoIndex(context)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            val generation = System.currentTimeMillis()
            val today = LocalDate.now()
            var count = 0
            var processed = 0
            try {
                val fields = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.DATE_ADDED)
                val cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fields,
                    "is_trashed=0 AND is_pending=0", null, "date_added DESC") ?: error("无法读取相册")
                cursor.use { c ->
                    while (c.moveToNext()) {
                        ensureActive()
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
                        val name = c.getString(1) ?: "照片"
                        val size = c.getLong(2)
                        val modified = c.getLong(3)
                        val path = c.getString(4) ?: ""
                        count++
                        var analysis = index.cached(uri.toString(), name, path, size, modified)
                        if (analysis == null) {
                            if (processed >= 80) continue
                            val hash = try {
                                runInterruptible { hash(uri) }
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { continue }
                            val text = if (PhotoRules.isScreenshot(name, path)) {
                                try {
                                    suspendCancellableCoroutine { continuation ->
                                        recognizer.process(InputImage.fromFilePath(context, uri))
                                            .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.text) }
                                            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                                    }
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) { continue }
                            } else ""
                            analysis = CachedAnalysis(hash, PhotoRules.evidence(name, path, text))
                            processed++
                        }
                        val age = (generation / 1000 - c.getLong(5)) / 86400
                        val reason = PhotoRules.reason(analysis.evidence, age, today).orEmpty()
                        index.save(Photo(uri.toString(), name, size, modified, reason, false), path, analysis, generation)
                    }
                }
                PhotoRules.duplicateExtras(index.hashes(generation)).forEach(index::duplicate)
                index.hideUnseen(generation)
                context.getSharedPreferences("settings", 0).edit()
                    .putInt("total", count).putLong("lastScan", System.currentTimeMillis())
                    .putInt("processed", processed).apply()
                count
            } finally {
                recognizer.close()
                index.close()
            }
        }
    }

    private fun hash(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(65536)
            while (true) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                val length = stream.read(buffer)
                if (length < 0) break
                digest.update(buffer, 0, length)
            }
        } ?: error("照片不可读")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
