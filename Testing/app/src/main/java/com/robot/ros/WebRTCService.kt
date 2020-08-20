package com.robot.ros

import android.util.Log
import com.robot.ros.messages.RopWebRTCService
import com.robot.ros.messages.RopWebRTCServiceRequest
import com.robot.ros.messages.RopWebRTCServiceResponse
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.uiThread
import org.ros.exception.RemoteException
import org.ros.exception.ServiceNotFoundException
import org.ros.node.ConnectedNode
import org.ros.node.service.ServiceClient
import org.ros.node.service.ServiceResponseListener

class WebRTCService (private val connectedNode: ConnectedNode) {
    companion object {
        private const val TAG = "WebRTCService"
        private const val SERVICE_NAME = "/rop/rop_rtcid"
        const val NODE = "androidClient/rop_webrtc_service"
    }

    var client: ServiceClient<RopWebRTCServiceRequest, RopWebRTCServiceResponse>? = null

    fun requestCall(result: ((Boolean?, String?) -> Unit)) {
        doAsync {
            if (client == null) {
                try{
                    client = connectedNode.newServiceClient(SERVICE_NAME, RopWebRTCService._TYPE)
                }
                catch (ex: ServiceNotFoundException) {
                    Log.e(TAG, ex.getStackTraceString())
                }
                catch (ex: Exception) {
                    Log.e(TAG, ex.getStackTraceString())
                }
            }

            if (client != null) {
                val request: RopWebRTCServiceRequest = client!!.newMessage()
                client?.call(request, object : ServiceResponseListener<RopWebRTCServiceResponse?> {
                    override fun onSuccess(response: RopWebRTCServiceResponse?) {
                        uiThread {
                            result(true, response?.id)
                        }
                    }

                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(false, e.message)
                        }
                    }
                })
            }
            else {
                uiThread {
                    result(false,  RopWebRTCService._TYPE + " not found")
                }
            }
        }
    }
}
