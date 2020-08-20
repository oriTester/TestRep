package com.robot.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kaopiz.kprogresshud.KProgressHUD
import com.robot.R
import com.robot.activity.InitialActivity
import com.robot.application.MainApplication
import com.robot.ros.RopVariables
import com.robot.ros.RosService
import com.robot.utils.hideKeyboard
import com.robot.vpn.VPNManager
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.android.synthetic.main.ip_address_fragment.*
import java.util.*


class IpAddressFragment : Fragment() {

    companion object {
        fun newInstance() = IpAddressFragment()
    }

    var onDidConnect: (() -> Unit)? = null
    var hud:KProgressHUD? = null

    private lateinit var viewModel: IpAddressViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ip_address_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(IpAddressViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()

        (requireActivity() as InitialActivity).supportActionBar!!.hide()

        viewModel.onDidConnect = {
            if (it) {
                onDidConnect?.invoke()
                connectButton.revertAnimation()
            } else {
               android.os.Handler().postDelayed({
                   connectButton.revertAnimation()
                }, 1000)

                Toast.makeText(
                    MainApplication.applicationContext(),
                    R.string.ros_not_ready,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        connectButton.setOnClickListener {
            if (!VPNManager.instance.isVPNActive() && RopVariables.isVPNMode) {
                InitialActivity.self.onProfilePicked = {
                    activity?.runOnUiThread {
                        startVPN()
                    }
                }

                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE,false)
                activity?.startActivityForResult(Intent.createChooser(intent, getString(R.string.select_vpn_profile)), InitialActivity.IMPORT_FILE_REQUEST)
            }
            else {
                when {
                    ipField.text.toString().isNullOrEmpty() -> {
                        ipInputLayout.error = getString(R.string.ip_empty)
                    }
                    else -> {
                        ipInputLayout.error = null
                        connectButton.startAnimation()
                        context?.hideKeyboard(ipField)
                        viewModel.checkIsRobotAlive(ipField.text.toString())
                    }
                }
            }
        }

        ipField.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ipInputLayout.error = null
            }
        })

        if (RopVariables.isVPNMode) {
            if (VPNManager.instance.isVPNActive()) {
                inputView.visibility = View.VISIBLE
                titleView.text = getString(R.string.enter_ip_address)
                connectButton.text = getString(R.string.connect)
            }
            else {
                inputView.visibility = View.GONE
                titleView.text = getString(R.string.choose_vpn_profile)
                connectButton.text = getString(R.string.choose)
            }
        }
        else {
            inputView.visibility = View.VISIBLE
            titleView.text = getString(R.string.enter_ip_address)
            connectButton.text = getString(R.string.connect)
        }
    }

    private fun startVPN() {
        VPNManager.instance.onVPNStatus = { message: String?, connectionStatus: ConnectionStatus? ->
            activity?.runOnUiThread {
                val localized = getString(VpnStatus.getLocalizedState(message))
                hud?.setDetailsLabel(localized)
            }

            if (connectionStatus == ConnectionStatus.LEVEL_CONNECTED || connectionStatus == ConnectionStatus.LEVEL_AUTH_FAILED
                || connectionStatus == ConnectionStatus.LEVEL_NONETWORK || connectionStatus == ConnectionStatus.LEVEL_NOTCONNECTED) {
                activity?.runOnUiThread {
                    hud?.dismiss()

                    if (connectionStatus == ConnectionStatus.LEVEL_CONNECTED) {
                        inputView.visibility = View.VISIBLE
                        titleView.text = getString(R.string.enter_ip_address)
                        connectButton.text = getString(R.string.connect)
                    }
                }
            }
        }

        VPNManager.instance.startVPN(activity = InitialActivity.self) { result: Boolean, error: String? ->
            if (!result && error != null) {
                activity?.runOnUiThread {
                    InitialActivity.self.displayAlert(getString(R.string.error), error)
                }
            }
            else {
                activity?.runOnUiThread {
                    hud = KProgressHUD.create(context!!)
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
}
