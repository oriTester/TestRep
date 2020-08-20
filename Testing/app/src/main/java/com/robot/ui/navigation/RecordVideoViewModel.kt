package com.robot.ui.navigation

import androidx.lifecycle.ViewModel
import com.robot.model.MediaModel
import com.robot.ros.RosService
import java.io.File
import java.util.*

class RecordVideoViewModel : ViewModel() {

    private var isRecording = false
    private var seconds:Long = 0
    private val maxDuration:Long = 180
    private val timer = Timer()
    private var isCancel = false

    var onStartRecording: ((String?) -> Unit)? = null
    var onStopRecording: ((String?, MediaModel?) -> Unit)? = null
    var updateTime: ((Long, Int) -> Unit)? = null
    var displayLoading: ((String, String) -> Unit)? = null

    fun maxDurationInMilliseconds(): Long {
        return maxDuration * 1000
    }

    fun cancel() {
        if(isRecording) {
            isCancel = true
            isRecording = false
            stop()
        }
    }

    fun recording() {
        isRecording = !isRecording
        if (isRecording) {
            start()
        } else {
            stop()
        }
    }

    fun autoStartRecord() {
        isRecording = true
        seconds = 0
        timer.schedule(object : TimerTask() {
            override fun run() {
                seconds += 1
                val progress = (seconds.toDouble()/maxDuration.toDouble())
                val result = (progress * 100.0)
                updateTime?.invoke(seconds, result.toInt())
            }
        }, 1, 1000)
        onStartRecording?.invoke(null)
    }

    private fun start() {
        seconds = 0
        displayLoading?.invoke("Please wait", "Start recording")
        RosService.instance.recordVideo(isRecording) { _, result, message ->
            if (result == true) {
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        seconds += 1
                        val progress = (seconds.toDouble()/maxDuration.toDouble())
                        val result = (progress * 100.0)
                        updateTime?.invoke(seconds, result.toInt())
                    }
                }, 1, 1000)
                onStartRecording?.invoke(null)
            } else {
                onStartRecording?.invoke(message)
            }
        }
    }

    private fun stop() {
        timer.cancel()
        if(!isCancel) {
            displayLoading?.invoke("Please wait", "Stop recording")
            RosService.instance.recordVideo(isRecording) { mediaModel, result, message ->
                if (result == true && mediaModel!= null) {
                    onStopRecording?.invoke(null, mediaModel)
                } else {
                    onStopRecording?.invoke(message, null)
                }
            }
        }
        else {
            RosService.instance.recordVideo(isRecording, result = null)
        }
    }
}
