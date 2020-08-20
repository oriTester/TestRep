package com.robot.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.robot.utils.PreferencesManager
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class IpAddressViewModel : ViewModel() {

    var onDidConnect: ((Boolean) -> Unit)? = null

    fun checkIsRobotAlive(ip:String) {
        doAsync {
            try {
                val address =  InetAddress.getByName(ip)
                val socketAddress = InetSocketAddress(address, 11311)
                val socket = Socket()
                val timeoutMs = 2000
                socket.connect(socketAddress, timeoutMs)
                uiThread {
                    saveIpAddress(ip)
                    onDidConnect?.invoke(true)
                }
            } catch (e: IOException) {
                uiThread {
                    onDidConnect?.invoke(false)
                }
            }
        }
    }

    private fun saveIpAddress(ip:String) {
        PreferencesManager.putString(PreferencesManager.IP_ADDRESS, ip)
    }
}
