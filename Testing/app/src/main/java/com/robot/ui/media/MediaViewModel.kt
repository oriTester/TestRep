package com.robot.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.robot.model.MediaModel
import com.robot.ros.DataManager
import com.robot.ros.RosService

class MediaViewModel : ViewModel() {
    val photos = MutableLiveData<MutableList<MediaModel>>()
    val videos = MutableLiveData<MutableList<MediaModel>>()

    init {
        photos.value = DataManager.instance.photos
        videos.value = DataManager.instance.videos
    }

    fun loadData() {
        DataManager.instance.loadPhotos { list, _, _ ->
            if(list!=null && list.count() != photos.value?.count()) {
                photos.postValue(list)
            }
            DataManager.instance.loadVideos { list, _, _ ->
                if(list!=null && list.count() != videos.value?.count()) {
                    videos.postValue(list)
                }
            }
        }
    }
}
