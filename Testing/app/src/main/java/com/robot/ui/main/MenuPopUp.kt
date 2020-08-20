package com.robot.ui.main

import RosAndroidJavaFix.RosActivity
import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.robot.R
import com.robot.activity.RobotActivity
import kotlinx.android.synthetic.main.menu_layout.view.*

class MenuPopUp {
    enum class Type {
        ABOUT, LICENSE, TERMS, HELP, MANUAL, SETTINGS
    }

    private var popupWindow: PopupWindow
    private var onSelectItem: ((Type) -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null

    init {
        val inflater = RobotActivity.self.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val popupView: View = inflater.inflate(R.layout.menu_layout, null)

        val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT

        val focusable = true

        popupWindow = PopupWindow(popupView, width, height, focusable)

        popupWindow.setOnDismissListener {
            onDismiss?.invoke()
        }

        popupView.aboutLayout.setOnClickListener {
            popupWindow.dismiss()
            onSelectItem?.invoke(Type.ABOUT)
        }
        popupView.licenseLayout.setOnClickListener {
            popupWindow.dismiss()
            onSelectItem?.invoke(Type.LICENSE)
        }
        popupView.termsLayout.setOnClickListener {
            popupWindow.dismiss()
            onSelectItem?.invoke(Type.TERMS)
        }
        popupView.helpLayout.setOnClickListener {
            popupWindow.dismiss()
            onSelectItem?.invoke(Type.HELP)
        }
        popupView.manualLayout.setOnClickListener {
            popupWindow.dismiss()
            onSelectItem?.invoke(Type.MANUAL)
        }
        popupView.settingsLayout.setOnClickListener {
            popupWindow.dismiss()
            onSelectItem?.invoke(Type.SETTINGS)
        }
    }

    fun showPopupWindow(view: View, listener: ((Type) -> Unit)?, dismiss: (() -> Unit)?) {
        onSelectItem = listener
        onDismiss = dismiss
        popupWindow.showAsDropDown(view)
    }
}