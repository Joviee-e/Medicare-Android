package com.example.medicare

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import java.io.File

object ProfileImageManager {
    private const val FILE_NAME = "profile_picture.jpg"

    fun saveProfilePicture(context: Context, sourceUri: Uri): Boolean {
        return try {
            val tempFile = File(context.filesDir, "profile_picture_temp.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // If copy succeeded, safely rename temp file to the permanent name
            val permanentFile = File(context.filesDir, FILE_NAME)
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

    fun displayProfilePicture(context: Context, imageView: ImageView) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists() && file.length() > 0) {
            try {
                // Clear any default tint
                imageView.imageTintList = null
                imageView.clearColorFilter()
                imageView.setImageURI(Uri.fromFile(file))
            } catch (e: Exception) {
                e.printStackTrace()
                loadDefaultAvatar(context, imageView)
            }
        } else {
            loadDefaultAvatar(context, imageView)
        }
    }

    private fun loadDefaultAvatar(context: Context, imageView: ImageView) {
        imageView.setImageResource(R.drawable.ic_profile_filled)
        // Restore standard tint
        imageView.imageTintList = context.getColorStateList(R.color.on_secondary_container)
    }
}
