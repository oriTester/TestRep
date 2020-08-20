package com.robot.ui.talk

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.robot.application.MainApplication

import com.robot.R
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest

import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener

import kotlin.concurrent.schedule
import kotlinx.android.synthetic.main.talk_fragment.*
import kotlinx.android.synthetic.main.talk_bottom_layout.view.*
import net.gotev.speech.Speech

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kaopiz.kprogresshud.KProgressHUD

import com.robot.activity.RobotActivity
import com.robot.activity.VideoPlayerActivity
import com.robot.ros.CommandManager
import com.robot.ros.RosService
import com.robot.utils.PreferencesManager
import com.robot.utils.onPressTint
import com.robot.model.MediaModel
import com.robot.ui.navigation.RecordVideoFragment

import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import net.gotev.speech.GoogleVoiceTypingDisabledException
import net.gotev.speech.SpeechRecognitionNotAvailable
import net.gotev.speech.SpeechUtil
import net.gotev.speech.SpeechDelegate

import java.util.*


class TalkFragment : Fragment(), SpeechDelegate {

    companion object {
        fun newInstance() = TalkFragment()
    }

    private lateinit var viewModel: TalkViewModel
    private var messageAdapter: MessageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.talk_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TalkViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()

        addListeners()

        messageAdapter =
            MessageAdapter(RobotActivity.self)
        messages_view.adapter = messageAdapter

        val name = PreferencesManager.getString(PreferencesManager.USER_NAME)

        if(MessageManager.instance.messages.count() == 0) {
            MessageManager.instance.messages.add(Message(
                "Hi $name, can I help you?",
                true
            ))
        }
        messageAdapter?.add(MessageManager.instance.messages.toList())

        messages_view.setSelection(messages_view.count - 1)
    }

    private fun addListeners() {
        talkLayout.titleLabel.text = "Tap to talk"

        talkLayout.homeButton.onPressTint{
            val fragmentManager = activity?.supportFragmentManager
            fragmentManager?.popBackStack()
        }

        talkLayout.speakButton.setOnClickListener{
            if (Speech.getInstance().isListening) {
                progressLayout.visibility = View.GONE
                talkLayout.titleLabel.visibility = View.VISIBLE
                Speech.getInstance().stopListening()
            } else {
                checkPermissions()
            }
        }

        talkLayout.homeButton.visibility = View.VISIBLE

        messages_view.setOnItemClickListener { parent, view, position, id ->
            val msg = MessageManager.instance.messages[position]
            if(msg.media!=null) {
                showMedia(msg.media!!)
            }
        }
    }

    //region Speak
    private fun checkPermissions() {
        Dexter.withActivity(RobotActivity.self)
            .withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    onRecordAudioPermissionGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    Toast.makeText(
                        MainApplication.applicationContext(),
                        R.string.permission_required,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {}
            }).check()
    }

    private fun onRecordAudioPermissionGranted() {
        try {
            Speech.getInstance().stopTextToSpeech()
            progressLayout.visibility = View.VISIBLE
            talkLayout.titleLabel.visibility = View.INVISIBLE
            Speech.getInstance().startListening(progress, this)
        } catch (exc: SpeechRecognitionNotAvailable) {
            showSpeechNotSupportedDialog()

        } catch (exc: GoogleVoiceTypingDisabledException) {
            showEnableGoogleVoiceTyping()
        }
    }

    private fun showSpeechNotSupportedDialog() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> SpeechUtil.redirectUserToGoogleAppOnPlayStore(
                    MainApplication.applicationContext()
                )

                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }

        val builder = AlertDialog.Builder(MainApplication.applicationContext())
        builder.setMessage(R.string.speech_not_available)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, dialogClickListener)
            .setNegativeButton(R.string.no, dialogClickListener)
            .show()
    }

    private fun showEnableGoogleVoiceTyping() {
        val builder = AlertDialog.Builder(MainApplication.applicationContext())
        builder.setMessage(R.string.enable_google_voice_typing)
            .setCancelable(true)
            .setPositiveButton(R.string.ok
            ) { _, _ ->
            }
            .show()
    }

    override fun onSpeechResult(result: String?) {
        finishSpeech(result)
    }

    override fun onSpeechRmsChanged(value: Float) {
    }

    override fun onSpeechPartialResults(results: MutableList<String>?) {
    }

    override fun onStartOfSpeech() {
    }

    private fun finishSpeech(result: String?) {
        progressLayout.visibility = View.GONE
        talkLayout.titleLabel.visibility = View.VISIBLE

        if (!result.isNullOrEmpty()) {
            val questionMsg = Message(result, false)

            MessageManager.instance.messages.add(questionMsg)
            messageAdapter?.add(questionMsg)

            messages_view.setSelection(messages_view.count - 1)

            val command = CommandManager.instance.getResult(result)

            if(command.command!=null) {
                when (command?.command?.id) {
                    "TakePicture" -> {
                        takePhoto()
                    }
                    "TakeVideo" -> {
                        recordVideo()
                    }
                    else -> {
                        RosService.instance.sendCommand(command?.command?.id, command?.command?.params) { success, message ->
                            answer(message)
                        }
                    }
                }
            }
            else {
                Timer().schedule(500) {
                    answer(command.text)
                }
            }
        }
    }

    private fun answer(text: String, play: Boolean = true, media:MediaModel? = null) {
        if(play) {
            Speech.getInstance().say(text)
        }
        RobotActivity.self.runOnUiThread(Runnable {
            val answerMsg = Message(text, true)
            answerMsg.media = media

            MessageManager.instance.messages.add(answerMsg)
            messageAdapter?.add(answerMsg)

            messages_view.setSelection(messages_view.count - 1)
        })
    }

    private fun takePhoto() {
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
                answer("Photo taken", false, mediaModel)
                showMedia(mediaModel)
            }
            else {
                message?.let { answer(it) }
            }
        }
    }

    private fun recordVideo() {
        val hud = KProgressHUD.create(RobotActivity.self)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setLabel("Please wait")
            .setDetailsLabel("Start recording")
            .setCancellable(false)
            .setAnimationSpeed(2)
            .setDimAmount(0.8f)
            .show()

        RosService.instance.recordVideo(true) { _, result, message ->
            hud.dismiss()
            if (result == true) {
                val fragment = RecordVideoFragment.newInstance()
                fragment.onVideoRecorded = {
                    val media = MediaModel(it.path, MediaModel.Type.VIDEO)
                    answer(getString(R.string.video_taken, "\"" + it.fileName() + "\""),false, media)
                   // showMedia(media)
                }
                fragment.autoStart = true
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.addToBackStack(null)
                transaction?.add(R.id.container, fragment)
                    ?.commit()
            } else {
                message?.let { answer(it) }
            }
        }
    }

    private fun showMedia(media: MediaModel) {
        if(media.type == MediaModel.Type.VIDEO) {
 //           val intent = Intent(RobotActivity.self, VideoPlayerActivity::class.java)
//            intent.putExtra(VideoPlayerActivity.VIDEO_PATH, media.file().absolutePath)
//            startActivity(intent)
            if(media !=null && media.isFileExist()) {
                val intent = Intent(RobotActivity.self, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.VIDEO_PATH, media.file().absolutePath)
                startActivity(intent)
            }
            else if(media !=null && !media.isFileExist()) {
                val hud = KProgressHUD.create(RobotActivity.self)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setLabel("Please wait")
                    .setDetailsLabel("Downloading")
                    .setCancellable(false)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.8f)
                    .show()

                RosService.instance.getFile(media.path) { file, _, message ->
                    hud.dismiss()
                    if(file!=null) {
                        val intent = Intent(RobotActivity.self, VideoPlayerActivity::class.java)
                        intent.putExtra(VideoPlayerActivity.VIDEO_PATH, media.file().absolutePath)
                        startActivity(intent)
                    }
                    else {
                        RobotActivity.self.displayAlert(null, getString(R.string.video_not_ready), onConfirm = null)
                    }
                }
            }
        }
        else {
            val photos = listOf(media)
            val viewer = StfalconImageViewer.Builder<MediaModel>(context, photos) { view, model ->
                Picasso.get().load(model.file()).into(view)
            }
            viewer.show()
        }
    }


    //endregion

}
