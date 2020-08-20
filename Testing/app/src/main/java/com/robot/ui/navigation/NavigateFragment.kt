package com.robot.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kaopiz.kprogresshud.KProgressHUD
import com.robot.R
import com.robot.activity.RobotActivity
import com.robot.model.MediaModel
import com.robot.ros.RosService
import com.robot.ui.talk.TalkFragment
import com.robot.utils.onPressTint
import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.main_fragment.talkLayout
import kotlinx.android.synthetic.main.navigate_fragment.*
import kotlinx.android.synthetic.main.talk_bottom_layout.view.*
import org.ros.android.BitmapFromCompressedImage
import org.ros.android.view.RosImageView
import sensor_msgs.CompressedImage

class NavigateFragment : Fragment(),
    RoomAdapter.RecyclerViewItemClickListener {

    companion object {
        fun newInstance() = NavigateFragment()
    }

    private lateinit var viewModel: NavigateViewModel
    private var roomDialog: RoomDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.navigate_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(NavigateViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()

        viewModel.loadData()

        addListeners()

        setupCamera()
    }

    override fun onResume() {
        super.onResume()

        startCamera()
    }

    override fun onPause() {
        super.onPause()

        stopCamera()
    }

    private fun addListeners() {
        photoLabel.onPressTint {
            val hud = KProgressHUD.create(RobotActivity.self)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait")
                .setDetailsLabel("Take photo")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.8f)
                .show()

            RosService.instance.takePhoto { mediaModel, _, message ->
                hud.dismiss()
                if(mediaModel!=null) {
                    val photos = listOf(mediaModel)
                    val viewer = StfalconImageViewer.Builder<MediaModel>(context, photos) { view, model ->
                        Picasso.get().load(model.file()).into(view)
                    }
                    viewer.show()
                }
                else {
                    RobotActivity.self.displayAlert("Error", message, null)
                }
            }
        }

        videoLabel.onPressTint {
            val fragment = RecordVideoFragment.newInstance()
            fragment.onVideoRecorded = {
                RobotActivity.self.displayAlert(null, getString(R.string.video_taken, "\"" + it.fileName() + "\""), null)
//                val intent = Intent(RobotActivity.self, VideoPlayerActivity::class.java)
//                intent.putExtra(VideoPlayerActivity.VIDEO_PATH, it.absolutePath)
//                startActivity(intent)
            }
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.addToBackStack(null)
            transaction?.add(R.id.container, fragment)
                ?.commit()
        }

        goLabel.onPressTint {
            showRooms()
        }

        arrowDown.onPressTint {
            stopButton.visibility = View.VISIBLE
            viewModel.sideCount = 0
            if (viewModel.isForward) {
                viewModel.upCount--
                if (viewModel.upCount <= 1) {
                    viewModel.isForward = false
                    viewModel.isBackward = true
                }
            }
            else {
                viewModel.isBackward = true
                viewModel.upCount++
            }

            if (viewModel.upCount < 0) {
                viewModel.upCount = 0
            }

            var speed = 1.0
            if (viewModel.upCount > 1) {
                speed = 1.0 + ((viewModel.upCount - 1) * viewModel.step)
            }
            val formatted = String.format("%.1f", speed) ;

            move("backward $formatted")
        }

        arrowUp.onPressTint{
            stopButton.visibility = View.VISIBLE
            viewModel.sideCount = 0
            if (viewModel.isBackward) {
                viewModel.upCount--
                if (viewModel.upCount <= 1) {
                    viewModel.isForward = true
                    viewModel.isBackward = false
                }
            }
            else {
                viewModel.isForward = true
                viewModel.upCount++
            }

            if (viewModel.upCount < 0) {
                viewModel.upCount = 0
            }

            var speed = 1.0
            if (viewModel.upCount > 1) {
                speed = 1.0 + ((viewModel.upCount - 1) * viewModel.step)
            }
            val formatted = String.format("%.1f", speed) ;

            move("forward $formatted")
        }

        arrowLeft.onPressTint{
            stopButton.visibility = View.VISIBLE

            viewModel.upCount = 0

            if (viewModel.isRight) {
                viewModel.sideCount--

                if (viewModel.sideCount <= 1) {
                    viewModel.isLeft = true
                    viewModel.isRight = false
                }
            }
            else {
                viewModel.isLeft = true
                viewModel.sideCount++
            }

            if (viewModel.sideCount < 0) {
                viewModel.sideCount = 0
            }

            var speed = 1.0
            if (viewModel.sideCount > 1) {
                speed = 1.0 + ((viewModel.sideCount - 1) * viewModel.step)
            }
            val formatted = String.format("%.1f", speed) ;

            move("tleft $formatted")
        }

        arrowRight.onPressTint{
            stopButton.visibility = View.VISIBLE

            viewModel.upCount = 0

            if (viewModel.isLeft) {
                viewModel.sideCount--

                if (viewModel.sideCount <= 1) {
                    viewModel.isRight = true
                    viewModel.isLeft = false
                }
            }
            else {
                viewModel.isRight = true
                viewModel.sideCount++
            }

            if (viewModel.sideCount < 0) {
                viewModel.sideCount = 0
            }

            var speed = 1.0
            if (viewModel.sideCount > 1) {
                speed = 1.0 + ((viewModel.sideCount - 1) * viewModel.step)
            }
            val formatted = String.format("%.1f", speed) ;

            move("tright $formatted")
        }


        stopButton.setOnClickListener {
            stopButton.visibility = View.GONE
            viewModel.sideCount = 0
            viewModel.upCount = 0
            viewModel.isBackward = false
            viewModel.isForward = false
            viewModel.isLeft= false
            viewModel.isRight = false
            move("stop")
        }

        talkLayout.homeButton.onPressTint{
            val fragmentManager = activity?.supportFragmentManager
            fragmentManager?.popBackStack()
        }
        talkLayout.speakButton.setOnClickListener{
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.addToBackStack(null)
            transaction?.add(R.id.container, TalkFragment.newInstance())
                ?.commit()
        }

        talkLayout.homeButton.visibility = View.VISIBLE
    }

    private fun setupCamera() {
        val cm = cameraView as RosImageView<CompressedImage>
        cm.setMessageType(CompressedImage._TYPE)
        cm.setTopicName("/camera/rgb/image_raw/compressed")

        cm.setMessageToBitmapCallable(BitmapFromCompressedImage())
    }

    private fun startCamera() {
        RosService.instance.startCamera(cameraView as RosImageView<CompressedImage>)
    }

    private fun stopCamera() {
        RosService.instance.stopCamera(cameraView as RosImageView<CompressedImage>)
    }

    private fun showRooms() {
        val dataAdapter =
            RoomAdapter(viewModel.rooms.toTypedArray(), this)
        roomDialog = RoomDialog(
            RobotActivity.self,
            dataAdapter
        )
        roomDialog?.show()
    }

    override fun clickOnItem(data: MediaModel) {
        roomDialog?.dismiss()

        RosService.instance.goToRoom(data.path) { _, message ->
            stopButton.visibility = View.VISIBLE
            Toast.makeText(
                RobotActivity.self,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun move(param:String) {
        RosService.instance.move(param) { _, message ->
            Toast.makeText(
                RobotActivity.self,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
