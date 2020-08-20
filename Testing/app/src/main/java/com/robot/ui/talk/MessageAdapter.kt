package com.robot.ui.talk

import com.robot.R

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.robot.model.MediaModel
import com.robot.ros.RosService

import java.util.ArrayList

class Message(val text: String, val isCurrentUser: Boolean) {
    var media: MediaModel? = null
}

class MessageAdapter(internal var context: Context) : BaseAdapter() {

    private var messages: MutableList<Message> = ArrayList<Message>()

    fun add(message: Message) {
        this.messages.add(message)
        notifyDataSetChanged()
    }

    fun add(messages: List<Message>) {
        this.messages.addAll(messages)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return messages.size
    }

    override fun getItem(i: Int): Any {
        return messages[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        var convertView = convertView

        val holder = MessageViewHolder()

        val messageInflater =
            context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val message = messages[i]

        if (!message.isCurrentUser) {
            convertView = messageInflater.inflate(R.layout.my_message_item, null)
            holder.messageBody = convertView.findViewById(R.id.message_body) as TextView
            convertView.tag = holder
            holder.messageBody!!.text = message.text
        } else {
            if (message.media == null) {
                convertView = messageInflater.inflate(R.layout.ros_message_item, null)
                holder.avatar = convertView.findViewById(R.id.avatar) as View
                holder.name = convertView.findViewById(R.id.name) as TextView
                holder.messageBody = convertView.findViewById(R.id.message_body) as TextView
                convertView.tag = holder

                holder.name!!.text = "Friday"
                holder.messageBody!!.text = message.text
            } else {
                convertView = messageInflater.inflate(R.layout.ros_message_photo_item, null)
                holder.avatar = convertView.findViewById(R.id.avatar) as View
                holder.name = convertView.findViewById(R.id.name) as TextView
                holder.messageImage = convertView.findViewById(R.id.message_image) as com.facebook.drawee.view.SimpleDraweeView
                convertView.tag = holder

                holder.name!!.text = "Friday"

                if(message.media!!.type == MediaModel.Type.PHOTO) {
                    holder.messageImage?.setImageURI(message.media!!.uriString())
                }
                else {
                    holder.messageImage?.setActualImageResource(R.drawable.video_bg)
                }
            }

        }

        return convertView
    }

}

internal class MessageViewHolder {
    var avatar: View? = null
    var name: TextView? = null
    var messageBody: TextView? = null
    var messageImage: com.facebook.drawee.view.SimpleDraweeView? = null
}