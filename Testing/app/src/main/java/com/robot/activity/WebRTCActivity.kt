package com.robot.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Point
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.robot.R
import com.robot.network.Network
import com.robot.network.StreamInfo
import com.robot.ros.BatteryListener
import com.robot.ros.RosService
import fr.pchab.webrtcclient.PeerConnectionParameters
import fr.pchab.webrtcclient.WebRtcClient
import fr.pchab.webrtcclient.WebRtcClient.RtcListener
import kotlinx.android.synthetic.main.activity_rtc.*
import org.json.JSONException
import org.webrtc.MediaStream
import org.webrtc.VideoRenderer
import org.webrtc.VideoRendererGui
import org.webrtc.VideoRendererGui.ScalingType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WebRTCActivity : AppCompatActivity(), RtcListener {
    companion object {
        private const val TAG = "WebRTCActivity"
        const val IS_INCOMING = "incoming"
        private const val VIDEO_CODEC_VP9 = "VP9"
        private const val AUDIO_CODEC_OPUS = "opus"

        // Local preview screen position before call is connected.
        private const val LOCAL_X_CONNECTING = 0
        private const val LOCAL_Y_CONNECTING = 0
        private const val LOCAL_WIDTH_CONNECTING = 100
        private const val LOCAL_HEIGHT_CONNECTING = 100

        // Local preview screen position after call is connected.
        private const val LOCAL_X_CONNECTED = 75
        private const val LOCAL_Y_CONNECTED = 0
        private const val LOCAL_WIDTH_CONNECTED = 25
        private const val LOCAL_HEIGHT_CONNECTED = 25

        // Remote video screen position
        private const val REMOTE_X = 0
        private const val REMOTE_Y = 0
        private const val REMOTE_WIDTH = 100
        private const val REMOTE_HEIGHT = 100

        private val SCALING_TYPE = ScalingType.SCALE_ASPECT_FILL

        private const val SOCKED_ADDRESS = "http://friday.hyperlync.com:3131/"

        private const val CALLER_ID = "ClientAndroidApplication"
        private const val ROBOT_ID = "RopFaceApplication"
        private const val CALL_MSG_TYPE = "init"
    }

    private var localRender: VideoRenderer.Callbacks? = null
    private var remoteRender: VideoRenderer.Callbacks? = null
    private var client: WebRtcClient? = null
    private var ringtonePlayer:MediaPlayer? = null
    private var connectedPlayer:MediaPlayer? = null

    private var isIncoming = false;

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isIncoming = intent.getBooleanExtra(IS_INCOMING, false);

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_rtc)

        glViewCall.preserveEGLContextOnPause = true
        glViewCall.keepScreenOn = true
        glViewCall.setZOrderOnTop(false)

        checkPermissions()

        // local and remote render
        remoteRender = VideoRendererGui.create(
            REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT, SCALING_TYPE,
            false
        )
        localRender = VideoRendererGui.create(
            LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, SCALING_TYPE,
            true
        )

        stopCallButton.setOnClickListener {

            onBackPressed()
        }
        switchCameraButton.setOnClickListener {
            client?.switchCamera()
        }

        supportActionBar?.hide()
    }

    override fun onBackPressed() {
        stopCall()
        super.onBackPressed()
    }

    public override fun onPause() {
        super.onPause()
        stopAudio()
        glViewCall.onPause()
        client?.onPause()
    }

    public override fun onResume() {
        super.onResume()
        glViewCall.onResume()
        client?.onResume()
    }

    public override fun onDestroy() {
        client?.onDestroy()
        stopAudio()
        super.onDestroy()
    }

    private fun playAudio() {
        ringtonePlayer = MediaPlayer.create(this, R.raw.voip_ringback)
        ringtonePlayer?.isLooping = true
        ringtonePlayer?.start()
    }

    private fun stopAudio() {
        connectedPlayer?.stop()
        ringtonePlayer?.stop()
    }

    private fun checkPermissions()
    {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                     if(report.areAllPermissionsGranted()) {
                         if(!isIncoming) {
                             playAudio()
                         }
                         VideoRendererGui.setView(glViewCall) {
                             initClient()
                         }
                     }
                    else{
                         displayAlert("Error","To make a call, you must provide permissions for the camera and record audio") {
                             stopCall()
                         }
                     }
                }
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

    private fun initClient() {
        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val params = PeerConnectionParameters(
            true,
            false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9,
            true,
            1, AUDIO_CODEC_OPUS, true)
        client = WebRtcClient(this, SOCKED_ADDRESS, params, VideoRendererGui.getEGLContext())
    }

    private fun loadStreams() {
        Network.instance.api.streams().enqueue(object : Callback<List<StreamInfo>> {
            override fun onFailure(call: Call<List<StreamInfo>>, t: Throwable) {
                displayAlert("Error",t.message) {
                    stopCall()
                }
            }
            override fun onResponse( call: Call<List<StreamInfo>>, response: Response<List<StreamInfo>>) {
                val streams = response.body()

                if (streams!=null) {
                    var robotId: String? = null

                    streams.forEach {
                        if (it.name == ROBOT_ID) {
                            robotId = it.id
                        }
                    }

                    if (robotId != null) {
                        makeCall(robotId)
                    }
                }
                else {
                    stopAudio()

                    displayAlert("Error","Can't communicate with server") {
                        stopCall()
                    }
                }
            }
        })
    }

    @Throws(JSONException::class)
    fun makeCall(callerId: String?) {
        client?.sendMessage(callerId, CALL_MSG_TYPE, null)
    }

    private fun startCamera() {
        client?.start(CALLER_ID)
    }

    private fun stopCall() {
        // send stop Telepresence
        RosService.instance.telepresence(false) { success, error, id ->
            if(!error.isNullOrEmpty()) {
                displayAlert("Error",error) {

                }
            }

            Log.d(WebRTCActivity.TAG, "stopCall Telepresence is called")
            glViewCall.onPause()
            finish()

        }
        
    }

    //region RTC Events

    override fun onCallReady(callId: String) {
        startCamera()

        if(isIncoming) {
            RosService.instance.webRTCServer?.setCallerId(callId)
        } else {
            //loadStreams()
            RosService.instance.telepresence(true) { success, error, id ->
                stopAudio()
                if(success == true && !id.isNullOrEmpty()) {
                    makeCall(id)
                }
                else if(!id.isNullOrEmpty()) {
                    displayAlert("Error",error) {
                        stopCall()
                    }
                }
                else {
                    stopCall()
                }
            }
        }
    }

    override fun onStatusChanged(newStatus: WebRtcClient.WebRtcStatus) {
        runOnUiThread {
            if(newStatus == WebRtcClient.WebRtcStatus.CONNECTED) {
                connectedPlayer = MediaPlayer.create(this, R.raw.voip_connecting)
                connectedPlayer?.isLooping = false
                connectedPlayer?.start()
            }
            else if(isIncoming) {
                stopCall()
            }
        }
    }

    override fun onLocalStream(localStream: MediaStream) {
        localStream.videoTracks[0].addRenderer(VideoRenderer(localRender))
        VideoRendererGui.update(
            localRender,
            LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, SCALING_TYPE,
            false
        )
    }

    override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
        remoteStream.videoTracks[0].addRenderer(VideoRenderer(remoteRender))
        VideoRendererGui.update(
            remoteRender,
            REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT, SCALING_TYPE,
            false
        )
        VideoRendererGui.update(
            localRender,
            LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED, SCALING_TYPE,
            false
        )
    }

    override fun onRemoveRemoteStream(endPoint: Int) {
        VideoRendererGui.update(
            localRender,
            LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, SCALING_TYPE,
            false
        )
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

    //endregion
}
