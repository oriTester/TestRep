package com.robot.ui.navigation

import com.robot.R

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.robot.model.MediaModel

import kotlinx.android.synthetic.main.room_dialog.*
import kotlinx.android.synthetic.main.room_item.view.*

class RoomDialog(var activity: Activity, private var adapter: RecyclerView.Adapter<*>) : Dialog(activity),
    View.OnClickListener {

    private var recyclerView: RecyclerView? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.room_dialog)

        recyclerView = recycler_view
        mLayoutManager = LinearLayoutManager(activity)
        recyclerView?.layoutManager = mLayoutManager
        recyclerView?.adapter = adapter
    }

    override fun onClick(v: View?) {
    }
}

class RoomAdapter(
    private val rooms: Array<MediaModel>,
    internal var recyclerViewItemClickListener: RecyclerViewItemClickListener
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, i: Int): RoomViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.room_item, parent, false)
        return RoomViewHolder(v)
    }

    override fun onBindViewHolder(fruitViewHolder: RoomViewHolder, i: Int) {
        fruitViewHolder.mTextView.text = rooms[i].path
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    inner class RoomViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var mTextView: TextView = v.textView

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            recyclerViewItemClickListener.clickOnItem(rooms[this.adapterPosition])
        }
    }

    interface RecyclerViewItemClickListener {
        fun clickOnItem(data: MediaModel)
    }
}


