package com.robot.model

import android.net.Uri
import com.robot.application.MainApplication
import java.io.File


class MediaModel (val path:String, val type:Type) {
    enum class Type {
        VIDEO, PHOTO, LOCATION
    }

    fun isFileExist(): Boolean {
        return file().exists()
    }

    fun file(): File {
        val directory = MainApplication.filesDirectory
        return File(directory + File.separator + fileName())
    }

    fun fileName(): String {
        return path.substringAfterLast("/")
    }

    fun uriString(): String {
        return Uri.fromFile(file()).toString()
    }
}