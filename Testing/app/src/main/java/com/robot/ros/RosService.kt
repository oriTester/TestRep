package com.robot.ros

import android.util.Log
import com.robot.R
import com.robot.model.MediaModel
import com.robot.ros.messages.GetMediaNamesList
import com.robot.ros.messages.LoginFromApp
import com.robot.ros.messages.RopBrainService
import com.robot.utils.PreferencesManager
import de.blinkt.openvpn.core.OpenVPNService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.ros.address.InetAddressFactory
import org.ros.android.view.RosImageView
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import sensor_msgs.CompressedImage
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.*
import kotlin.concurrent.schedule


class RosService(uri: URI, private val nodeMainExecutor: NodeMainExecutor) {

    companion object {

        private var INSTANCE: RosService? = null
        private var TAG = "RosService"

        var instance: RosService
            get() {
                return INSTANCE!!
            }
            set(value) {
                INSTANCE = value
            }
    }


    private var loginService: LoginService? = null
    private var mediaService: MediaService? = null
    private var brainService: BrainService? = null
    private var webRTCService: WebRTCService? = null
    var webRTCServer: WebRTCServer? = null
    private var batteryListener: BatteryListener? = null

    private var publisher:BrainPublisher

    var onDidChangeBatteryLevel: ((Int) -> Unit)? = null
    var onCallRequested: (() -> Unit)? = null
    var onCallFailed: (() -> Unit)? = null

    private var nodeConfiguration: NodeConfiguration

    init {
        nodeConfiguration = if (RopVariables.isVPNMode) {
            val ip = OpenVPNService.vpnIP
            NodeConfiguration.newPublic(ip)
        } else {
            NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().hostAddress)
        }
        nodeConfiguration.masterUri = uri

        nodeMainExecutor.execute(object : AbstractNodeMain() {
            override fun getDefaultNodeName(): GraphName {
                return GraphName.of(LoginService.NODE)
            }

            override fun onStart(connectedNode: ConnectedNode) {
                loginService = LoginService(connectedNode)
            }
        }, nodeConfiguration)

        nodeMainExecutor.execute(object : AbstractNodeMain() {
            override fun getDefaultNodeName(): GraphName {
                return GraphName.of(MediaService.NODE)
            }

            override fun onStart(connectedNode: ConnectedNode) {
                mediaService = MediaService(connectedNode)
            }
        }, nodeConfiguration)

        nodeMainExecutor.execute(object : AbstractNodeMain() {
            override fun getDefaultNodeName(): GraphName {
                return GraphName.of(BrainService.NODE)
            }

            override fun onStart(connectedNode: ConnectedNode) {
                brainService = BrainService(connectedNode)
            }
        }, nodeConfiguration)

        nodeMainExecutor.execute(object : AbstractNodeMain() {
            override fun getDefaultNodeName(): GraphName {
                return GraphName.of(WebRTCService.NODE)
            }

            override fun onStart(connectedNode: ConnectedNode) {
                webRTCService = WebRTCService(connectedNode)
            }
        }, nodeConfiguration)


        publisher = BrainPublisher()
        nodeMainExecutor.execute(publisher, nodeConfiguration)

        batteryListener = BatteryListener()
        batteryListener?.onDidChangeBatteryLevel = {
            onDidChangeBatteryLevel?.invoke(it)
        }
        nodeMainExecutor.execute(
            batteryListener,
            nodeConfiguration.setNodeName(BatteryListener.NODE)
        )

        webRTCServer = WebRTCServer()
        webRTCServer?.onCallFailed = {
            onCallFailed?.invoke();
        }
        webRTCServer?.onCallRequested = {
            onCallRequested?.invoke();
        }

        nodeMainExecutor.execute(webRTCServer, nodeConfiguration.setNodeName(WebRTCServer.NODE))

        ping()
    }

    fun login(name:String, password: String, result: ((String?, Boolean?) -> Unit)) {
        if (!isConnected) {
            result("Robot is offline", false)
        }
        else if (loginService == null) {
            result(LoginFromApp._TYPE + " not found", false)
        }
        else {
            loginService?.login(name,password,result)
        }
    }

    fun takePhoto(result: ((MediaModel?, Boolean?, String?) -> Unit)) {
        if (!isConnected) {
            result(null, false, "Robot is offline")
        }
        else if (loginService == null) {
            result(null, false, RopBrainService._TYPE + " not found");
        }
        else {
            brainService?.takePhoto { _, result, message, path ->
                if(result == true && path != null) {
                    val model = MediaModel(path, MediaModel.Type.PHOTO)
                    DataManager.instance.addPhoto(model)
                    result(model, true, null)
                }
                else {
                    result(null, false, message)
                }
            }
        }
    }

    fun telepresence(start:Boolean, result: ((Boolean?, String?, String?) -> Unit)?) {
        if (!isConnected) {
            result?.invoke(false, "Robot is offline", null)
        }
        else if (loginService == null) {
            result?.invoke(false, RopBrainService._TYPE + " not found", null);
        }
        else {
            brainService?.telepresence(start) { success, message, id ->
                result?.invoke(success, message, id)
            }
        }
    }

    fun recordVideo(start:Boolean, result: ((MediaModel?, Boolean?, String?) -> Unit)?) {
        if (!isConnected) {
            result?.invoke(null, false, "Robot is offline")
        }
        else if (loginService == null) {
            result?.invoke(null, false, RopBrainService._TYPE + " not found");
        }
        else {
            brainService?.recordVideo(start) { _, success, message, path ->
                if (start) {
                    result?.invoke(null, success, message)
                }
                else {
                    if(success == true && path != null) {
                        val model = MediaModel(path, MediaModel.Type.VIDEO)
                        DataManager.instance.addVideo(model)
                        result?.invoke(model, true, null)
                    }
                    else {
                        result?.invoke(null, false, message)
                    }
                }
            }
        }
    }

    fun getFile(path:String, result: ((File?, Boolean?, String?) -> Unit)) {
        if (!isConnected) {
            result(null, false, "Robot is offline")
        }
        else if (mediaService == null) {
            result(null, false, GetMediaNamesList._TYPE + " not found")
        }
        else {
            mediaService?.getFile(path) { file, success, message ->
                result(file, success, message)
            }
        }
    }

    fun getPhotos(result: ((MutableList<MediaModel>?, Boolean?, String?) -> Unit)) {
        if (!isConnected) {
            result(null, false, "Robot is offline")
        }
        else if (mediaService == null) {
            result(null, false, GetMediaNamesList._TYPE + " not found")
        }
        else {
            mediaService?.getMediaFiles(MediaModel.Type.PHOTO) { list, success, message ->
                if (list!=null) {
                    var files = mutableListOf<MediaModel>()
                    list.forEach {
                        files.add(MediaModel(it, MediaModel.Type.PHOTO))
                    }
                    files.reverse()
                    result(files,success,message)
                }
                else {
                    result(null,success,message)
                }
            }
        }
    }

    fun getVideos(result: ((MutableList<MediaModel>?, Boolean?, String?) -> Unit)) {
        if (!isConnected) {
            result(null, false, "Robot is offline")
        }
        else if (mediaService == null) {
            result(null, false, GetMediaNamesList._TYPE + " not found")
        }
        else {
            mediaService?.getMediaFiles(MediaModel.Type.VIDEO) { list, success, message ->
                if (list!=null) {
                    var files = mutableListOf<MediaModel>()
                    list.forEach {
                        files.add(MediaModel(it, MediaModel.Type.VIDEO))
                    }
                    files.reverse()
                    result(files,success,message)
                }
                else {
                    result(null,success,message)
                }
            }
        }
    }

    fun getLocations(result: ((MutableList<MediaModel>?, Boolean?, String?) -> Unit)) {
        if (!isConnected) {
            result(null, false, "Robot is offline")
        }
        else if (mediaService == null) {
            result(null, false, GetMediaNamesList._TYPE + " not found")
        }
        else {
            mediaService?.getMediaFiles(MediaModel.Type.LOCATION) { list, success, message ->
                if (list!=null) {
                    var files = mutableListOf<MediaModel>()
                    list.forEach {
                        files.add(MediaModel(it, MediaModel.Type.LOCATION))
                    }
                    files.reverse()
                    result(files,success,message)
                }
                else {
                    result(null,success,message)
                }
            }
        }
    }

    fun goToRoom(room:String, result: ((Boolean, String) -> Unit)) {
        if (!isConnected) {
            result(false, "Robot is offline")
        }
        else {
            publisher?.goToRoom(room, result)
        }
    }

    fun move(param:String, result: ((Boolean, String) -> Unit)) {
        Log.e(TAG, param)

        if (!isConnected) {
            result(false, "Robot is offline")
        }
        else {
            publisher?.move(param, result)
        }
    }

    fun sendCommand(commandID:String?, param:String?, result: ((Boolean, String) -> Unit)) {
        if (!isConnected) {
            result(false, "Robot is offline")
        }
        else {
            publisher?.sendCommand(commandID, param, result)
        }
    }

    fun startCamera(cameraView: RosImageView<CompressedImage>) {
        nodeMainExecutor.execute(cameraView, nodeConfiguration.setNodeName("android/camera_view"))
    }

    fun stopCamera(cameraView: RosImageView<CompressedImage>) {
        nodeMainExecutor.shutdownNodeMain(cameraView)
    }

    var onDidConnect: ((Boolean) -> Unit)? = null
    private var isConnected = true
    private fun ping() {
        Log.e(TAG, "call ping")
        doAsync {
            try {
                val address =  InetAddress.getByName(PreferencesManager.getString(PreferencesManager.IP_ADDRESS))
                val socketAddress = InetSocketAddress(address, 11311)
                val socket = Socket()
                val timeoutMs = 2000
                socket.connect(socketAddress, timeoutMs)
                uiThread {
                    if (!isConnected) {
                        isConnected = true
                        onDidConnect?.invoke(isConnected)
                    }
                }
                Timer().schedule(5000) {
                    ping()
                }
                Log.e(TAG, "ping true")
            } catch (e: IOException) {
                Timer().schedule(5000) {
                    ping()
                }
                uiThread {
                    if (isConnected) {
                        isConnected = false
                        onDidConnect?.invoke(isConnected)
                    }
                }
                Log.e(TAG, "ping false")
            }
        }
    }

    fun isRosConnected():Boolean {
        return isConnected
    }

}
