package com.robot.ui.media

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kaopiz.kprogresshud.KProgressHUD

import com.robot.R
import com.robot.activity.RobotActivity
import com.robot.activity.VideoPlayerActivity
import com.robot.model.MediaModel
import com.robot.ros.RosService
import com.robot.ui.talk.TalkFragment
import com.robot.utils.onPressTint
import com.robot.utils.setTintColor

import kotlinx.android.synthetic.main.media_fragment.*
import kotlinx.android.synthetic.main.talk_bottom_layout.view.*

import com.stfalcon.imageviewer.StfalconImageViewer
import com.squareup.picasso.Picasso

class MediaFragment : Fragment() {

    companion object {
        fun newInstance() = MediaFragment()
    }

    private lateinit var viewModel: MediaViewModel
    private val listAdapter = MediaListGridRecyclerAdapter()
    private var recyclerView: RecyclerView? = null
    private var selectedType = MediaModel.Type.PHOTO
    private var oldPosition = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.media_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()

        recyclerView = RobotActivity.self.findViewById<RecyclerView>(R.id.mediaRecyclerView)

        recyclerView?.layoutManager = GridLayoutManager(context,3)
        recyclerView?.addItemDecoration(GridItemDecoration(10, 3))


        recyclerView?.adapter = listAdapter
        listAdapter.setOnItemClick(object: MediaListGridRecyclerAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                showGallery(position)
            }
        })

        viewModel.photos.observe(this, Observer {
            if (viewModel.photos.value != null && selectedType == MediaModel.Type.PHOTO) {
                listAdapter.setMediaList(viewModel.photos.value!!)
            }
        })
        viewModel.videos.observe(this, Observer {
            if (viewModel.videos.value != null && selectedType == MediaModel.Type.VIDEO) {
                listAdapter.setMediaList(viewModel.videos.value!!)
            }
        })

        viewModel.loadData()

        addListeners()

        if (selectedType == MediaModel.Type.VIDEO) {
            selectView(videosLabel)
        } else {
            selectView(photosLabel)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MediaViewModel::class.java)
    }

    private fun addListeners() {
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

        photosLabel.setOnClickListener {
            oldPosition = -1
            selectView(photosLabel)
        }

        videosLabel.setOnClickListener {
            oldPosition = -1
            selectView(videosLabel)
        }

        talkLayout.homeButton.visibility = View.VISIBLE
    }

    private fun showGallery(position: Int) {
        oldPosition = position
        if (selectedType == MediaModel.Type.PHOTO) {
            val photos = viewModel.photos.value?.toList()
            val viewer = StfalconImageViewer.Builder<MediaModel>(context, photos) { view, model ->
                if (model.isFileExist()) {
                    Picasso.get().load(model.file()).into(view)
                } else {
                    RosService.instance.getFile(model.path) { file, _, _ ->
                        if(file?.exists() == true) {
                            Picasso.get().load(model.file()).into(view)
                        }
                        else {
                            view.setImageResource(R.drawable.ic_failure)
                        }
                    }
                }
            }
            viewer.allowSwipeToDismiss(false)
            viewer.withStartPosition(position)
            viewer.show()
        }
        else {
            val video = viewModel.videos.value?.toList()?.get(position)
            if(video!=null && video.isFileExist()) {
                val intent = Intent(RobotActivity.self, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.VIDEO_PATH, video.file().absolutePath)
                startActivity(intent)
            }
            else if(video!=null && !video.isFileExist()) {
                val hud = KProgressHUD.create(RobotActivity.self)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setLabel("Please wait")
                    .setDetailsLabel("Downloading")
                    .setCancellable(false)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.8f)
                    .show()

                RosService.instance.getFile(video.path) { file, _, _ ->
                    hud.dismiss()
                    if(file!=null) {
                        val intent = Intent(RobotActivity.self, VideoPlayerActivity::class.java)
                        intent.putExtra(VideoPlayerActivity.VIDEO_PATH, video.file().absolutePath)
                        startActivity(intent)
                    }
                    else {
                        RobotActivity.self.displayAlert(null, getString(R.string.video_not_ready), onConfirm = null)
                    }
                }
            }
        }
    }

    private fun selectView(view: TextView) {
        photosLabel.setTextColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
        videosLabel.setTextColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
        videosLabel.setBackgroundColor(Color.TRANSPARENT)
        photosLabel.setBackgroundColor(Color.TRANSPARENT)
        photosLabel.setTintColor(R.color.colorPrimaryDark)
        videosLabel.setTintColor(R.color.colorPrimaryDark)

        view.setTextColor(Color.WHITE)
        view.setTintColor(R.color.white)
        view.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorAccent))

        if (view == photosLabel) {
            selectedType = MediaModel.Type.PHOTO
            listAdapter.setMediaList(viewModel.photos.value!!)
        } else {
            selectedType = MediaModel.Type.VIDEO
            listAdapter.setMediaList(viewModel.videos.value!!)
        }

        recyclerView?.removeAllViews()

        if (oldPosition > 0) {
            recyclerView?.scrollToPosition(oldPosition)
        }
    }
}

//       val hierarchyBuilder = GenericDraweeHierarchyBuilder.newInstance(resources)
//            .setFailureImage(R.drawable.ic_failure)
//        val photos = viewModel.photos.value?.toList()
//        val viewer = ImageViewer.Builder<Any?>(context, photos)
//            .setStartPosition(position)
//            .setCustomDraweeHierarchyBuilder(hierarchyBuilder)
//            .setFormatter(ImageViewer.Formatter<MediaModel> {
//                    media -> media.uriString() + "1"
//            })
//        viewer.show()

//      Picasso.get().load(model.file()).into(view)