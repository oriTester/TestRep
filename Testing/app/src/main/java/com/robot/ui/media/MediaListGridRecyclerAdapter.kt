package com.robot.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.robot.R
import com.robot.activity.RobotActivity
import com.robot.model.MediaModel

import kotlinx.android.synthetic.main.list_item_grid_media.view.*

class MediaListGridRecyclerAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var listener: OnItemClickListener? = null

    private var listOfMedia = mutableListOf<MediaModel>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_grid_media, parent, false)
        val height: Int = parent.width / 3

        val lp = v.layoutParams as GridLayoutManager.LayoutParams
        lp.height = height
        v.layoutParams = lp

        return MediaListViewHolder(v)
    }

    override fun getItemCount(): Int {
        return listOfMedia.count()
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val movieViewHolder = viewHolder as MediaListViewHolder
        movieViewHolder.bindView(listOfMedia[position])

        viewHolder.itemView.setOnClickListener {
            listener?.onItemClick(position)
        }
    }

    fun setMediaList(list: MutableList<MediaModel>) {
        this.listOfMedia = list
    }

    fun setOnItemClick(listener: OnItemClickListener) {
        this.listener = listener
    }
}

class MediaListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindView(model: MediaModel?) {
        if(model!=null) {
            itemView.imageView.onDidLoadImage = {
                itemView.mediaLabel.visibility = View.GONE
            }
            itemView.imageView.onFailedLoadImage = {
                itemView.mediaLabel.visibility = View.VISIBLE
            }
            itemView.imageView.model = model
            itemView.mediaLabel.text = model.fileName()
        }
    }
}
