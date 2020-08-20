package com.robot.vpn

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

import com.robot.R
import com.robot.application.MainApplication

import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.core.*
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class VPNManager: ByteCountListener, VpnStatus.StateListener  {

    companion object {
        private var INSTANCE: VPNManager? = null
        private var TAG = "VPNManager"
        private var mService: IOpenVPNServiceInternal? = null

        val instance: VPNManager
            get() {
                if (INSTANCE == null) {
                    INSTANCE = VPNManager()
                }
                return INSTANCE!!
            }
    }

    private val mConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                mService = IOpenVPNServiceInternal.Stub.asInterface(service)
            }
            override fun onServiceDisconnected(arg0: ComponentName) {
                mService = null
            }
        }

    private var onVPNStarted: ((Boolean, String?) -> Unit)? = null
    var onVPNStatus: ((String?, ConnectionStatus?) -> Unit)? = null

    fun isVPNActive(): Boolean {
        return VpnStatus.isVPNActive()
    }

    fun startVPN(activity: Activity, completed: ((Boolean, String?) -> Unit)) {
        onVPNStarted = completed

        if (VpnStatus.isVPNActive()) {
            onVPNStarted?.invoke(true, null)
        }
        else {
            if (getVPNProfileFile().exists()) {

                VpnStatus.addStateListener(this)
                VpnStatus.addByteCountListener(this)

                val serviceConnection = Intent(activity, OpenVPNService::class.java)
                serviceConnection.action = OpenVPNService.START_SERVICE
                MainApplication.applicationContext().bindService(serviceConnection, mConnection, Context.BIND_AUTO_CREATE)

                val bufferedReader = BufferedReader(FileReader(getVPNProfileFile()))

                val cp = ConfigParser()
                cp.parseConfig(bufferedReader)

                val vp = cp.convertProfile()
                val vpl = ProfileManager.getInstance(MainApplication.applicationContext())
                vp.mName = Build.MODEL
                vp.mUsername = null
                vp.mPassword = null
                vpl.addProfile(vp)
                vpl.saveProfile(MainApplication.applicationContext(), vp)
                vpl.saveProfileList(MainApplication.applicationContext())

                val intent = Intent(
                    MainApplication.applicationContext(),
                    LaunchVPN::class.java
                )
                intent.putExtra(LaunchVPN.EXTRA_KEY, vp.uuid.toString())
                intent.action = Intent.ACTION_MAIN

                activity.startActivity(intent)

                onVPNStarted?.invoke(true, null)
            }
            else {
                onVPNStarted?.invoke(false, MainApplication.applicationContext().getString(R.string.profile_not_found))
            }
        }
    }

    fun stopVPN() {
        if (VpnStatus.isVPNActive()) {
            ProfileManager.setConnectedVpnProfileDisconnected(MainApplication.applicationContext())
            if (mService != null) {
                try {
                    mService?.stopVPN(false)
                } catch (e: RemoteException) {
                    VpnStatus.logException(e)
                }
            }
        }
    }

    fun getVPNProfileFile():File {
        val name = "vpn.ovpn"
        val directory = MainApplication.filesDirectory
        return File(directory + File.separator + name)
    }

    override fun updateByteCount(`in`: Long, out: Long, diffIn: Long, diffOut: Long) {}

    override fun updateState(state: String?, logmessage: String?, localizedResId: Int, level: ConnectionStatus?) {
        Log.e(TAG, state)
        Log.e(TAG, level.toString())
        onVPNStatus?.invoke(state, level)
    }

    override fun setConnectedVPN(uuid: String?) {}
}