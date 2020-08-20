package com.robot.ros


import android.os.Handler
import com.robot.application.MainApplication
import com.robot.ros.messages.RopBrainService
import com.robot.ros.messages.RopBrainServiceRequest
import com.robot.ros.messages.RopBrainServiceResponse
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.ros.exception.RemoteException
import org.ros.exception.ServiceNotFoundException
import org.ros.node.ConnectedNode
import org.ros.node.service.ServiceClient
import org.ros.node.service.ServiceResponseListener
import java.io.File

class BrainService (private val connectedNode: ConnectedNode) {
    companion object {
        private const val TAG = "BrainService"
        private const val SERVICE_NAME = "/rop/rop_brain_service"
        const val NODE = "androidClient/rop_brain_service"
    }

    var client: ServiceClient<RopBrainServiceRequest, RopBrainServiceResponse>? = null

    fun telepresence(start:Boolean, result: ((Boolean?, String?, String?) -> Unit)) {
        doAsync {
            if (client == null) {
                try {
                    client = connectedNode.newServiceClient(SERVICE_NAME, RopBrainService._TYPE)
                }
                catch (ex: ServiceNotFoundException) {
                    uiThread {
                        result(false, RopBrainService._TYPE + " not found", null)
                    }
                }
            }

            if (client != null) {
                val request: RopBrainServiceRequest = client!!.newMessage()
                request.commandID = "Telepresence"
                request.source = "App"
                if (start) {
                    request.params = "start"
                } else {
                    request.params = "stop"
                }

                client?.call(request, object : ServiceResponseListener<RopBrainServiceResponse?> {
                    override fun onSuccess(response: RopBrainServiceResponse?) {
                        val userData = response?.userData
                        val message = response?.message
                        val status = response?.status
                        val cmd = response?.command


                        if (start) {
                            if (response?.status == true) {
                                uiThread {
                                    val id = response?.userData
                                    result(true, null, id)
                                }
                            } else {
                                uiThread {
                                    result(false, response?.message, null)
                                }
                            }
                        }
                        else {
                            val id = response?.userData

                            if (response?.status == true && id != null) {
                                uiThread {
                                    result(true, null, id)
                                }
                            } else {
                                uiThread {
                                    result(true, response?.message, null)
                                }
                            }
                        }
                    }

                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(false, e.message, null)
                        }
                    }
                })
            } else {
                uiThread {
                    result(false, RopBrainService._TYPE + " not found", null)
                }
            }
        }
    }

    fun recordVideo(start:Boolean, result: ((File?, Boolean?, String?, String?) -> Unit)) {
        doAsync {
            if (client == null) {
                try {
                    client = connectedNode.newServiceClient(SERVICE_NAME, RopBrainService._TYPE)
                }
                catch (ex: ServiceNotFoundException) {
                    uiThread {
                        result(null, false, RopBrainService._TYPE + " not found", null)
                    }
                }
            }

            if (client != null) {
                val request: RopBrainServiceRequest = client!!.newMessage()
                request.commandID = "TakeVideo"
                request.source = "App"
                if (start) {
                    request.params = "start"
                } else {
                    request.params = "stop"
                }

                client?.call(request, object : ServiceResponseListener<RopBrainServiceResponse?> {
                    override fun onSuccess(response: RopBrainServiceResponse?) {

                        if (start) {
                            if (response?.status == true) {
                                uiThread {
                                    result(null, true, null, null)
                                }
                            } else {
                                uiThread {
                                    result(null, false, response?.message, null)
                                }
                            }
                        }
                        else {
                            val path = response?.userData

                            if (response?.status == true && path != null) {
                                val name = path.substringAfterLast("/")
                                val directory = MainApplication.filesDirectory
                                val file =  File(directory + File.separator + name)
                                uiThread {
                                    result(file, true, null, path)
                                }
//                                Thread.sleep(3000) //wait 3 sec and download file
//
//                                RosService.instance.getFile(path) { file, success, message ->
//                                    uiThread {
//                                        result(file, success, message, path)
//                                    }
//                                }
                            } else {
                                uiThread {
                                    result(null, false, response?.message, null)
                                }
                            }
                        }
                    }

                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(null, false, e.message, null)
                        }
                    }
                })
            } else {
                uiThread {
                    result(null, false, RopBrainService._TYPE + " not found", null)
                }
            }
        }
    }

    fun takePhoto(result: ((File?, Boolean?, String?, String?) -> Unit)) {
        doAsync {
            if (client==null) {
                client = connectedNode.newServiceClient(SERVICE_NAME, RopBrainService._TYPE)
            }

            if (client!=null) {
                val request: RopBrainServiceRequest = client!!.newMessage()
                request.commandID = "TakePicture"
                request.source = "App"

                client?.call(request, object : ServiceResponseListener<RopBrainServiceResponse?> {
                    override fun onSuccess(response: RopBrainServiceResponse?) {
                        val path = response?.userData

                        if(response?.status == true && path!=null) {
                           RosService.instance.getFile(path) { file, success, message ->
                               uiThread {
                                   result(file, success, message, path)
                               }
                            }
                        }
                        else {
                            uiThread {
                                result(null, false, response?.message, null)
                            }
                        }
                    }

                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(null, false, e.message, null)
                        }
                    }
                })
            } else {
                uiThread {
                    result(null, false, RopBrainService._TYPE + " not found", null)
                }
            }
        }

    }
}
