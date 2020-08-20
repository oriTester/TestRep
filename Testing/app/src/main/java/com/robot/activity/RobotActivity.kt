package com.robot.activity

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.robot.BuildConfig
import com.robot.R
import com.robot.application.MainApplication
import com.robot.model.Speechrecognition
import com.robot.ros.RosService
import com.robot.ui.login.LoginFragment
import com.robot.utils.PreferencesManager
import com.robot.utils.parseAs
import net.gotev.speech.Speech
import org.ros.node.NodeMainExecutor
import java.net.URI
import java.util.*
import kotlin.concurrent.timerTask


class RobotActivity : RosAndroidJavaFix.RosActivity("ros_app","Robot", URI.create(robotUri())) {

    companion object {
        lateinit var self: RobotActivity
        var ringtone:Ringtone? = null

        fun robotUri():String {
            return "http://${PreferencesManager.getString(PreferencesManager.IP_ADDRESS)}:11311"
        }
    }

    override fun init(nodeMainExecutor: NodeMainExecutor?) {
        if (nodeMainExecutor!=null) {
            RosService.instance = RosService(masterUri, nodeMainExecutor)
            RosService.instance.onCallRequested = {
              onCallRequested()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_robot)

        if (savedInstanceState == null) {

            Speech.init(this, packageName)

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LoginFragment.newInstance())
                .commitNow()
        }

        supportActionBar?.hide()

        self = this
    }

    override fun onDestroy() {
        super.onDestroy()
        Speech.getInstance().shutdown()
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    private fun checkPermissions()
    {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {}

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener {
            }
            .check()
    }

    fun displayAlert(title:String?, message:String?, onConfirm: (() -> Unit)?) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            onConfirm?.invoke()
        }

        alertDialog.show()
    }

    private fun onCallRequested() {
        val intent = Intent(this, WebRTCActivity::class.java)
        intent.putExtra(WebRTCActivity.IS_INCOMING, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if(MainApplication.isInBackground) {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, notification)
            ringtone?.play()

            showNotification("Incoming call", "Rop face", intent)
              Timer().schedule(timerTask {
                  if(MainApplication.isInBackground) {
                      ringtone?.stop()
                      val main = Intent(applicationContext, WebRTCActivity::class.java)
                      main.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
                      showNotification("Missing incoming call", "Rop face", main)
                  }
              }, 20000)
        }
        else {
            startActivity(intent)
        }
    }

    private fun showNotification(title: String?, body: String?, intent: Intent?) {

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mBuilder = NotificationCompat.Builder(applicationContext, "notify_001")
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
        mBuilder.setContentTitle(title)
        mBuilder.setContentText(body)
        mBuilder.setAutoCancel(true)
        mBuilder.priority = NotificationCompat.PRIORITY_HIGH
        mBuilder.setCategory(NotificationCompat.CATEGORY_CALL)

        if(intent!=null) {
            val notifyPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
           // mBuilder.setFullScreenIntent(notifyPendingIntent,true);
            mBuilder.setContentIntent(notifyPendingIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "channel_001"
            val channel = NotificationChannel(channelId, "Incoming call", NotificationManager.IMPORTANCE_HIGH)
            channel.enableLights(true)
            channel.enableVibration(true)

            notificationManager.createNotificationChannel(channel)

            mBuilder.setChannelId(channelId)
        }

        notificationManager.notify(0, mBuilder.build())
    }
}
