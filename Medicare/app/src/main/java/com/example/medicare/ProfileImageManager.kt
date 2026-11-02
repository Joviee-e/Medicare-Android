package com.example.medicare

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.ContextCompat
import java.io.File

object ProfileImageManager {

    private fun getFileNameForUser(context: Context): String {
        val sessionManager = SessionManager(context)
        val userId = sessionManager.getUserId()
        return if (!userId.isNullOrEmpty()) {
            "profile_picture_${userId}.jpg"
        } else {
            "profile_picture_guest.jpg"
        }
    }

    private fun cleanupLegacyFile(context: Context) {
        try {
            // Delete old non-user-scoped profile picture if it exists so it doesn't leak to other accounts
            val legacyFile = File(context.filesDir, "profile_picture.jpg")
            if (legacyFile.exists()) {
                legacyFile.delete()
            }
            val tempFile = File(context.filesDir, "profile_picture_temp.jpg")
            if (tempFile.exists()) {
                tempFile.delete()
            }
        } catch (_: Exception) {}
    }

    fun hasCustomProfilePicture(context: Context): Boolean {
        cleanupLegacyFile(context)
        val fileName = getFileNameForUser(context)
        val file = File(context.filesDir, fileName)
        return file.exists() && file.length() > 0
    }

    fun saveProfilePicture(context: Context, sourceUri: Uri): Boolean {
        cleanupLegacyFile(context)
        return try {
            val fileName = getFileNameForUser(context)
            val tempFile = File(context.filesDir, "temp_${fileName}")
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val permanentFile = File(context.filesDir, fileName)
            if (tempFile.exists() && tempFile.length() > 0) {
                if (permanentFile.exists()) {
                    permanentFile.delete()
                }
                tempFile.renameTo(permanentFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteProfilePicture(context: Context): Boolean {
        cleanupLegacyFile(context)
        val fileName = getFileNameForUser(context)
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }

    fun displayProfilePicture(context: Context, imageView: ImageView) {
        cleanupLegacyFile(context)
        val fileName = getFileNameForUser(context)
        val file = File(context.filesDir, fileName)
        if (file.exists() && file.length() > 0) {
            try {
                // Clear any default tint and padding when displaying user's real photo
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.imageTintList = null
                imageView.clearColorFilter()
                imageView.setImageURI(null) // Clear cached view memory
                imageView.setImageURI(Uri.fromFile(file))
            } catch (e: Exception) {
                e.printStackTrace()
                loadDefaultAvatar(context, imageView)
            }
        } else {
            loadDefaultAvatar(context, imageView)
        }
    }

    fun loadDefaultAvatar(context: Context, imageView: ImageView) {
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        imageView.setPadding(pad, pad, pad, pad)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.setImageResource(R.drawable.ic_profile_filled)
        imageView.imageTintList = context.getColorStateList(R.color.on_secondary_container)
        imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.secondary_container))
    }
}
