package com.robot.ui.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.robot.model.MediaModel
import com.robot.ros.DataManager

class NavigateViewModel : ViewModel() {
    var rooms = mutableListOf<MediaModel>()
    val videos = MutableLiveData<MutableList<MediaModel>>()

    var isBackward = false
    var isForward = false

    var isLeft= false
    var isRight = false

    var upCount = 0
    var sideCount = 0
    var step = 0.1


    init {
        rooms.addAll(DataManager.instance.locations)
    }

    fun loadData() {
        DataManager.instance.loadLocations { list, _, _ ->
            if(list!=null && list.count() != rooms.count()) {
                rooms.clear()
                rooms.addAll(list)
            }
        }
    }
}
