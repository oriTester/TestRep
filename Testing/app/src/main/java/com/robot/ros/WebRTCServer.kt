package com.robot.ros

import android.util.Log

import com.robot.ros.messages.RopWebRTCService
import com.robot.ros.messages.RopWebRTCServiceRequest
import com.robot.ros.messages.RopWebRTCServiceResponse

import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.service.ServiceResponseBuilder

class WebRTCServer : AbstractNodeMain() {

    companion object {
        private const val TAG = "WebRTCServer"
        private const val SERVICE_NAME = "/rop_webrtc_client"
        private const val GRAPH = "androidClient/webRTCServiceServer"
        const val NODE = "androidClient/rop_webrtc_server"
    }

    private var isWaitingId = false
    private var callerId = ""

    fun setCallerId(id: String) {
        callerId = id
        isWaitingId = false
    }

    var onCallRequested: (() -> Unit)? = null
    var onCallFailed: (() -> Unit)? = null

    override fun getDefaultNodeName(): GraphName {
        return GraphName.of(GRAPH)
    }

    override fun onStart(connectedNode: ConnectedNode) {
        connectedNode.newServiceServer(SERVICE_NAME, RopWebRTCService._TYPE,
            ServiceResponseBuilder<RopWebRTCServiceRequest?, RopWebRTCServiceResponse> { _, response ->
                Log.d(TAG, "WebRTCServer receive request")
                onCallRequested?.invoke()
                isWaitingId = true
                for (i in 0..40) {
                    if (isWaitingId) {
                        try {
                            Log.d(TAG, "Sleep...")
                            Thread.sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    } else {
                        if (callerId.isNotEmpty()) {
                            response.id = callerId
                        } else {
                            response.id = ""
                        }
                    }
                }
                if (callerId.isEmpty() && isWaitingId) {
                    response.id = ""
                    onCallFailed?.invoke()
                }
            })
    }

}