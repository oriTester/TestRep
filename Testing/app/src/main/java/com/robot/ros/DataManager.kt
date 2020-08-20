package com.robot.ros

import com.robot.model.MediaModel
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import android.util.Log

class DataManager {

    companion object {
        private var INSTANCE: DataManager? = null
        private var TAG = "DataManager"

        val instance: DataManager
            get() {
                if (INSTANCE == null) {
                    INSTANCE = DataManager()
                }
                return INSTANCE!!
            }
    }

    private var statuses = hashMapOf<String, Int>()

    var photos = mutableListOf<MediaModel>()
    var videos = mutableListOf<MediaModel>()
    var locations = mutableListOf<MediaModel>()

    fun reload() {
        loadPhotos { _, _, _ ->
            loadVideos { _, _, _ ->
                loadLocations { _, _, _ ->

                }
            }
        }
    }

    fun loadPhotos(result: ((MutableList<MediaModel>?, Boolean?, String?) -> Unit)? = null) {
        RosService.instance.getPhotos { list, success, message ->
            if (list!=null) {
                photos.clear()
                photos.addAll(list)
                downloadPhotos()
            }
            result?.invoke(list, success, message)
        }
    }

    fun loadVideos(result: ((MutableList<MediaModel>?, Boolean?, String?) -> Unit)? = null) {
        RosService.instance.getVideos { list, success, message ->
            if (list!=null) {
                videos.clear()
                videos.addAll(list)
            }
            result?.invoke(list, success, message)
        }
    }

    fun loadLocations(result: ((MutableList<MediaModel>?, Boolean?, String?) -> Unit)? = null) {
        RosService.instance.getLocations { list, success, message ->
            if (list!=null) {
                locations.clear()
                locations.addAll(list)
            }
            result?.invoke(list, success, message)
        }
    }

    fun addPhoto(model:MediaModel) {
        photos.add(0, model)
    }

    fun addVideo(model:MediaModel) {
        videos.add(0, model)
    }

    private fun downloadPhotos() {
        photos.forEach {
            if (!statuses.containsKey(it.path) && !it.isFileExist()) {
                RosService.instance.getFile(it.path) { _, _, _ ->
                    statuses[it.path] = 1
                    downloadPhotos()
                }
                return
            }
        }
    }
}