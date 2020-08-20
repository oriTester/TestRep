package com.robot.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity

import java.io.File

import com.robot.R

import kotlinx.android.synthetic.main.activity_video_player.*

import android.view.WindowManager

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val VIDEO_PATH = "VIDEO_PATH"
    }

    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_player)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        supportActionBar?.hide()

        if (mediaController == null) {
            mediaController =  MediaController(this)
            mediaController?.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
        }

        videoView.setOnPreparedListener {
            videoView.start()
        }

        val intent = intent
        val path = intent?.extras?.getString(VIDEO_PATH)

        if(path!=null) {
            videoView.setVideoURI(Uri.fromFile(File(path)))
            videoView.requestFocus()
        }
    }

    override fun onPause() {
        if(videoView.isPlaying && videoView.canPause()) {
            videoView.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if(videoView.isPlaying) {
            videoView.stopPlayback()
        }
        super.onDestroy()
    }
}
