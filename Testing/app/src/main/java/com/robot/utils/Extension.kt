package com.robot.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.robot.R
import com.robot.application.MainApplication
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

fun TextView.setTintColor(color: Int) {
    val drawable = this.compoundDrawables[1]
    drawable.colorFilter =
        PorterDuffColorFilter(
            ContextCompat.getColor(this.context, color),
            PorterDuff.Mode.SRC_IN
        )
}

fun Context.hideKeyboard(view: View) {
    val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun TextView.onPressTint(listener: (View) -> Unit) {
    this.setOnTouchListener { v, event ->
        val drawable = this.compoundDrawables[1]
        if (event?.action == MotionEvent.ACTION_DOWN) {
            this.setTextColor(ContextCompat.getColor(this.context, R.color.colorPrimaryDark))
            drawable.colorFilter =
                PorterDuffColorFilter(
                    ContextCompat.getColor(this.context, R.color.colorPrimaryDark),
                    PorterDuff.Mode.SRC_IN
                )
        } else if (event?.action == MotionEvent.ACTION_UP) {
            this.setTextColor(ContextCompat.getColor(this.context, R.color.colorAccent))
            drawable.colorFilter =
                PorterDuffColorFilter(
                    ContextCompat.getColor(this.context, R.color.colorAccent),
                    PorterDuff.Mode.SRC_IN
                )
        }
        v?.onTouchEvent(event) ?: true
    }
    this.setOnClickListener {
        listener.invoke(this)
    }
}

fun ImageView.onPressTint(listener: (View) -> Unit) {
    this.setOnTouchListener { v, event ->
        if (event?.action == MotionEvent.ACTION_DOWN) {
            this.setColorFilter(ContextCompat.getColor(this.context, R.color.colorPrimaryDark))
        } else if (event?.action == MotionEvent.ACTION_UP) {
            this.setColorFilter(ContextCompat.getColor(this.context, R.color.colorAccent))
        }
        v?.onTouchEvent(event) ?: true
    }

    this.setOnClickListener {
        listener.invoke(this)
    }
}

fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).toInt()
}

fun dpToPxF(dp: Float): Float {
    return (dp * Resources.getSystem().displayMetrics.density)
}

fun org.jboss.netty.buffer.ChannelBuffer.writeFile(path: String): File? {
    val size = this.readableBytes()
    val name = path.substringAfterLast("/")
    val directory = MainApplication.filesDirectory
    val file =  File(directory + File.separator + name)

    try {
        FileOutputStream(file).use { output ->
            output.channel.use { fileChannel ->
                val byteBuffer: ByteBuffer = this.toByteBuffer()
                var written = 0
                while (written < size) {
                    written += fileChannel.write(byteBuffer)
                }
                fileChannel.force(false)
                return file
            }
        }
    } catch (e: IOException) { }
    return null
}

fun convertSecondsToMmSs(seconds: Long): String? {
    val s = seconds % 60
    val m = seconds / 60 % 60
    return String.format("%02d:%02d", m, s)
}