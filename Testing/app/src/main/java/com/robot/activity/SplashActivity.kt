package com.robot.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kaopiz.kprogresshud.KProgressHUD
import com.robot.ros.RopVariables
import com.robot.ros.RosService
import com.robot.utils.PreferencesManager
import com.robot.vpn.VPNManager
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus


class SplashActivity : AppCompatActivity() {

    var hud: KProgressHUD? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PreferencesManager.isIPAdded()) {
            if (RopVariables.isVPNMode) {
                if (VPNManager.instance.getVPNProfileFile().exists()) {
                    startVPN()
                }
                else {
                    openLoginActivity()
                }
            }
            else {
                openRobotActivity()
            }
        }
        else {
           openLoginActivity()
        }
    }

    private fun openLoginActivity() {
        val intent = Intent(this, InitialActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openRobotActivity() {
        val intent = Intent(this, RobotActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startVPN() {
        VPNManager.instance.onVPNStatus = { message: String?, connectionStatus: ConnectionStatus? ->
            runOnUiThread {
                val localized = getString(VpnStatus.getLocalizedState(message))
                hud?.setDetailsLabel(localized)
            }

            if (connectionStatus == ConnectionStatus.LEVEL_CONNECTED || connectionStatus == ConnectionStatus.LEVEL_AUTH_FAILED
                || connectionStatus == ConnectionStatus.LEVEL_NONETWORK || connectionStatus == ConnectionStatus.LEVEL_NOTCONNECTED) {
                    runOnUiThread {
                    hud?.dismiss()

                    if (connectionStatus == ConnectionStatus.LEVEL_CONNECTED) {
                        runOnUiThread {
                            openRobotActivity()
                        }
                    }
                }
            }
        }

        VPNManager.instance.startVPN(activity = this) { _: Boolean, _: String? ->
            runOnUiThread {
                hud = KProgressHUD.create(this)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setLabel("Please wait")
                    .setDetailsLabel("Starting")
                    .setCancellable(false)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.8f)
                    .show()
            }
        }
    }
}