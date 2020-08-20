package com.robot.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

import androidx.fragment.app.Fragment
import com.robot.BuildConfig
import com.robot.R
import com.robot.activity.RobotActivity
import com.robot.activity.WebRTCActivity
import com.robot.ros.DataManager
import com.robot.ros.RosService
import com.robot.ui.media.MediaFragment
import com.robot.ui.navigation.NavigateFragment
import com.robot.ui.talk.TalkFragment
import com.robot.utils.PreferencesManager
import com.robot.utils.onPressTint
import kotlinx.android.synthetic.main.dialog_app_info.view.*
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.talk_bottom_layout.view.*
import org.jetbrains.anko.runOnUiThread

class MainFragment : Fragment() {

    private val termsUri = "https://xtendrobotics.com"

    var dialog: AlertDialog? = null

    private val menuPopUp by lazy {
       MenuPopUp()
    }

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    @SuppressLint("ResourceAsColor")
    override fun onStart() {
        super.onStart()

        val name = PreferencesManager.getString(PreferencesManager.USER_NAME)
        if(name!=null) {
            message.text = "Hi, $name"
        }

        DataManager.instance.reload()

        menuButton.onPressTint {
            showPopupMenu(menuButton)
        }

        navigateLabel.onPressTint {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.addToBackStack(null)
            transaction?.add(R.id.container,
                    NavigateFragment.newInstance())
                ?.commit()
        }

        talkLayout.speakButton.setOnClickListener{
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.addToBackStack(null)
            transaction?.add(R.id.container, TalkFragment.newInstance())
                ?.commit()
        }

        teleLabel.onPressTint{
            val intent = Intent(RobotActivity.self, WebRTCActivity::class.java)
            startActivity(intent)
        }

        mediaLabel.onPressTint{
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.addToBackStack(null)
            transaction?.add(R.id.container, MediaFragment.newInstance())
                ?.commit()
        }

        RosService.instance.onDidConnect = {
            if (it) {
                onlineLabel.text = getString(R.string.online)
            }
            else {
                onlineLabel.text = getString(R.string.offline)
            }
        }

        RosService.instance.onDidChangeBatteryLevel = {level ->
            context?.runOnUiThread {
                if (level > 0) {
                    activityBattery.visibility = View.GONE
                    activityIndicator.visibility = View.GONE
                    batteryLabel.visibility = View.VISIBLE
                    batteryLabel.text = """$level%"""
                }
            }
        }
    }

    private fun showPopupMenu(v: View) {
        dimView.visibility = View.VISIBLE

        menuPopUp.showPopupWindow(v, {
            if (it == MenuPopUp.Type.ABOUT) {
                showAppInfo()
            }
            else if (it == MenuPopUp.Type.TERMS) {
                openTerms()
            }
        }, {
            dimView.visibility = View.GONE
        })
    }

    @SuppressLint("SetTextI18n")
    private fun showAppInfo() {
        context?.let {
            val view = LayoutInflater.from(it).inflate(R.layout.dialog_app_info, null)

            view.versionLabel.text = "Version " + BuildConfig.VERSION_NAME;
            view.buildLabel.text = "Build number " + BuildConfig.VERSION_CODE.toString()

            val terms = "Terms of Service."

            val spanTxt = SpannableStringBuilder(
                "Tap to view "
            )
            spanTxt.append(terms)
            spanTxt.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    dialog?.dismiss()
                    openTerms()
                }
            }, spanTxt.length - terms.length, spanTxt.length, 0)

            view.termsLabel.movementMethod = LinkMovementMethod.getInstance()
            view.termsLabel.setText(spanTxt, TextView.BufferType.SPANNABLE)

            view.closeButton.setOnClickListener {
                dialog?.dismiss()
            }

            dialog = AlertDialog.Builder(it).setView(view).show()
        }
    }

    private fun openTerms() {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(termsUri)
        )
        startActivity(browserIntent)
    }
}
