package com.robot.ros

import android.util.Log
import com.robot.model.MediaModel

import com.robot.ros.messages.*
import com.robot.utils.writeFile

import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.ros.exception.RemoteException
import org.ros.exception.ServiceNotFoundException
import org.ros.node.ConnectedNode
import org.ros.node.service.ServiceClient
import org.ros.node.service.ServiceResponseListener

import java.io.File

class MediaService (private val connectedNode: ConnectedNode) {
    companion object {
        private const val TAG = "MediaService"
        private const val MEDIA_SERVICE_NAME = "/rop_get_media_names_list"
        private const val FILE_SERVICE_NAME = "/rop_get_file"
        const val NODE = "androidClient/rop_media_service"
    }

    private var mediaClient: ServiceClient<GetMediaNamesListRequest, GetMediaNamesListResponse>? = null
    private var fileClient: ServiceClient<RopGetFileRequest, RopGetFileResponse>? = null

    fun getFile(path:String, result: ((File?, Boolean?, String?) -> Unit)) {
        doAsync {
            Log.e(TAG, "start load file $path")

            if (fileClient==null) {
                fileClient = connectedNode.newServiceClient(FILE_SERVICE_NAME, RopGetFile._TYPE)
            }

            if (fileClient!=null) {
                val request: RopGetFileRequest = fileClient!!.newMessage()
                request.path = path

                fileClient?.call(request, object : ServiceResponseListener<RopGetFileResponse?> {
                    override fun onSuccess(response: RopGetFileResponse?) {
                        Log.e(TAG, "end load file $path")

                        if(response?.status == true && response.buffer != null) {
                            val buffer = response.buffer
                            val file = buffer.writeFile(path)
                            if(file?.exists() == true) {
                                uiThread {
                                    result(file, true, null)
                                }
                            }
                            else {
                                uiThread {
                                    result(null, false, "Error saving file")
                                }
                            }
                        }
                        else {
                            uiThread {
                                result(null, false, response?.message)
                            }
                        }
                    }
                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(null, false, e.message)
                        }
                    }
                })
            } else {
                uiThread {
                    result(null, false, GetMediaNamesList._TYPE + " not found")
                }
            }
        }
    }

    fun getMediaFiles(mediaType:MediaModel.Type, result: ((MutableList<String>?, Boolean?, String?) -> Unit)) {
        doAsync {
            if (mediaClient==null) {
                try {
                    mediaClient = connectedNode.newServiceClient(MEDIA_SERVICE_NAME, GetMediaNamesList._TYPE)
                }
                catch (ex: ServiceNotFoundException) {
                    uiThread {
                        result(null, false, GetMediaNamesList._TYPE + " not found")
                    }
                }
            }

            if (mediaClient!=null) {
                val request: GetMediaNamesListRequest = mediaClient!!.newMessage()
                if (mediaType == MediaModel.Type.PHOTO) {
                    request.mediaType = "photo"
                }
                else if(mediaType == MediaModel.Type.VIDEO) {
                    request.mediaType = "video"
                }
                else if(mediaType == MediaModel.Type.LOCATION) {
                    request.mediaType = "location"
                }

                mediaClient?.call(request, object : ServiceResponseListener<GetMediaNamesListResponse?> {
                    override fun onSuccess(response: GetMediaNamesListResponse?) {
                        Log.e(TAG,"onSuccess")

                        val success =  when (response?.status) {
                            1.toByte() -> true
                            else -> false
                        }
                        val media = response?.nameList
                        val message = response?.message
                        uiThread {
                            result(media, success, message)
                        }
                    }

                    override fun onFailure(e: RemoteException) {
                        uiThread {
                            result(null, false, e.message)
                        }
                    }
                })
            } else {
                uiThread {
                    result(null, false, GetMediaNamesList._TYPE + " not found")
                }
            }
        }
    }
}