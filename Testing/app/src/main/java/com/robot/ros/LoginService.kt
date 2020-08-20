package com.robot.ros

import org.ros.exception.RemoteException
import org.ros.node.ConnectedNode
import org.ros.node.service.ServiceClient
import org.ros.node.service.ServiceResponseListener

import com.robot.ros.messages.*

import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.ros.exception.ServiceNotFoundException

class LoginService (private val connectedNode: ConnectedNode) {
    companion object {
        private const val TAG = "LoginService"
        private const val SERVICE_NAME = "/rop_store_login"
        const val NODE = "androidClient/rop_login_client"
    }

    var client: ServiceClient<LoginFromAppRequest, LoginFromAppResponse>? = null

    fun login(name:String, password: String, result: ((String?, Boolean?) -> Unit)) {
        doAsync {
            if (client==null) {
                try {
                    client = connectedNode.newServiceClient(SERVICE_NAME, LoginFromApp._TYPE)
                }
                catch (ex: ServiceNotFoundException) {
                    uiThread {
                        result(LoginFromApp._TYPE + " not found", false)
                    }
                }
            }

            if (client!=null) {
                val request: LoginFromAppRequest = client!!.newMessage()
                request.userName = name
                request.password = password

                client?.call(request, object : ServiceResponseListener<LoginFromAppResponse?> {
                    override fun onSuccess(response: LoginFromAppResponse?) {
                        val success =  when (response?.status) {
                            1.toByte() -> true
                            else -> false
                        }
                        uiThread {
                            result(response?.message, success)
                        }
                    }

                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(e.message, false)
                        }
                    }
                })
            } else {
                uiThread {
                    result(LoginFromApp._TYPE + " not found", false)
                }
            }
        }

    }
}
