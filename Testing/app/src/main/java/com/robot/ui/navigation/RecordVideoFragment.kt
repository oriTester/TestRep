package com.robot.ui.navigation

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kaopiz.kprogresshud.KProgressHUD
import com.robot.R
import com.robot.activity.RobotActivity
import com.robot.model.MediaModel
import com.robot.utils.convertSecondsToMmSs
import kotlinx.android.synthetic.main.record_video_fragment.*
import java.io.File

class RecordVideoFragment : Fragment() {

    companion object {
        fun newInstance() = RecordVideoFragment()
    }

    private lateinit var viewModel: RecordVideoViewModel
    private var hud: KProgressHUD? = null
    private var smoothAnimation:ObjectAnimator? = null

    var onVideoRecorded: ((MediaModel) -> Unit)? = null
    var autoStart = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.record_video_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RecordVideoViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        addListeners()

        if(autoStart) {
            autoStart = false
            viewModel.autoStartRecord()
        }
    }

    override fun onPause() {
        viewModel.cancel()
        super.onPause()
    }

    private fun addListeners() {
        viewModel.displayLoading = { title, subtitle ->
            smoothAnimation?.cancel()
            smoothAnimation = null
            
            hud?.dismiss()
            hud = KProgressHUD.create(RobotActivity.self)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(title)
                .setDetailsLabel(subtitle)
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.8f)
                .show()
        }

        viewModel.onStartRecording = {
            hud?.dismiss()
            if (it!=null) {
                RobotActivity.self.displayAlert("Error", it) {
                    val fragmentManager = activity?.supportFragmentManager
                    fragmentManager?.popBackStack()
                }
            }
            else {
                hintLabel.text = getString(R.string.tap_to_stop)
                label.visibility = View.VISIBLE
                timeLabel.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                progressBar.max = viewModel.maxDurationInMilliseconds().toInt()
                timeLabel.text = "00:00"

                recordButton.setImageResource(R.drawable.stop_recording_btn)

                smoothAnimation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progressBar.max)
                smoothAnimation?.duration = viewModel.maxDurationInMilliseconds()
                smoothAnimation?.interpolator = AccelerateInterpolator()
                smoothAnimation?.start()
            }
        }

        viewModel.onStopRecording = { message, mediaModel  ->
            hud?.dismiss()
            if (mediaModel==null) {
                RobotActivity.self.displayAlert("Error", message) {
                    val fragmentManager = activity?.supportFragmentManager
                    fragmentManager?.popBackStack()
                }
            }
            else {
                hintLabel.text = getString(R.string.tap_to_record)
                label.visibility = View.GONE
                timeLabel.visibility = View.GONE
                progressBar.visibility = View.GONE
                recordButton.setImageResource(R.drawable.start_recording_btn)

                val fragmentManager = activity?.supportFragmentManager
                fragmentManager?.popBackStack()
                onVideoRecorded?.invoke(mediaModel)
            }
        }

        viewModel.updateTime = { seconds, progress ->
            activity?.runOnUiThread {
                timeLabel?.text = convertSecondsToMmSs(seconds)
                if(progress >= 100) {
                    viewModel.recording()
                }
            }
        }

        recordButton.setOnClickListener {
            viewModel.recording()
        }
    }
}