package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File

data class EncodedVideoPayload(
    val videoBase64: String,
    val thumbBase64: String,
    val durationMs: Long
)

object MediaUtils {

    fun encodeImageUriToBase64(context: Context, uri: Uri, maxDimPx: Int, maxChars: Int): String? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDimPx * 2 || bounds.outHeight / sampleSize > maxDimPx * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null
            val scale = minOf(maxDimPx.toFloat() / original.width, maxDimPx.toFloat() / original.height, 1f)
            val target = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original

            var quality = 78
            var encoded = ""
            while (quality >= 35) {
                val baos = ByteArrayOutputStream()
                target.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                if (encoded.length <= maxChars) break
                quality -= 12
            }
            if (target !== original) target.recycle()
            original.recycle()
            if (encoded.length <= maxChars) encoded else null
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to encode image", e)
            null
        }
    }

    fun encodeVideoUriToPayload(context: Context, uri: Uri): Result<EncodedVideoPayload> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("Could not read selected video."))
            // Keep under 620KB raw (~830KB Base64) so it fits within Firestore's 1MB document limit
            if (bytes.size > 620_000) {
                return Result.failure(
                    IllegalArgumentException("Video is too large (${bytes.size / 1024} KB). Please pick a short clip under 600 KB.")
                )
            }
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frame = retriever.getFrameAtTime(0L)
            retriever.release()

            val thumbBase64 = if (frame != null) {
                val scale = minOf(360f / frame.width, 360f / frame.height, 1f)
                val scaled = Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * scale).toInt().coerceAtLeast(1),
                    (frame.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                if (scaled !== frame) scaled.recycle()
                frame.recycle()
                Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            } else ""

            val videoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Result.success(EncodedVideoPayload(videoBase64, thumbBase64, durationMs))
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to encode video", e)
            Result.failure(e)
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun writeBase64VideoToCache(context: Context, messageId: String, base64: String): File? {
        return try {
            val file = File(context.cacheDir, "msn_vid_$messageId.mp4")
            if (!file.exists()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                file.writeBytes(bytes)
            }
            file
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to materialize video file", e)
            null
        }
    }

    fun isVideoUri(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri) ?: return false
        return mime.startsWith("video/")
    }
}
