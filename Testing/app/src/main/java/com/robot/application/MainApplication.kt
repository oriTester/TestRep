package com.robot.application

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.robot.activity.RobotActivity
import com.robot.ros.CommandManager
import com.robot.vpn.VPNManager


class AppLifecycleObserver : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        RobotActivity.ringtone?.stop()
        MainApplication.isInBackground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        MainApplication.isInBackground = true
    }

    companion object {
        val TAG = AppLifecycleObserver::class.java.name
    }
}

class MainApplication : Application() {

    init {
        instance = this
    }

    companion object {
        var instance: MainApplication? = null
        var filesDirectory: String? = null
        var isInBackground = false

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        val config = ImagePipelineConfig.newBuilder(this)
            .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
            .setResizeAndRotateEnabledForNetwork(true)
            .setDownsampleEnabled(true)
            .build()
        Fresco.initialize(this, config)

        filesDirectory = this.externalCacheDir?.absolutePath

        CommandManager.instance

        val appLifecycleObserver = AppLifecycleObserver()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }

    override fun onTerminate() {
        VPNManager.instance.stopVPN()

        super.onTerminate()
    }

}