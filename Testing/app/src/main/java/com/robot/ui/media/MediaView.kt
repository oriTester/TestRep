package com.robot.ui.media

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import com.robot.model.MediaModel
import com.robot.ros.RosService
import java.io.File

//fun MediaView(context: Context) {
//    super(context)
//

//}

class MediaView : com.facebook.drawee.view.SimpleDraweeView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    var model:MediaModel? = null
        set(value) {
            field = value
            load()
        }

    var onDidLoadImage: (() -> Unit)? = null
    var onFailedLoadImage: (() -> Unit)? = null

    override fun onDetach() {
        super.onDetach()
    }

    private fun load() {
        if(model?.type == MediaModel.Type.PHOTO) {
            if(model?.isFileExist() == true) {
                setImageURI(model!!.uriString())
                onDidLoadImage?.invoke()
            }
            else if (model != null) {
                RosService.instance.getFile(model!!.path) { file, _, _ ->
                    if(file?.exists() == true) {
                        setImageURI(model!!.uriString())
                        onDidLoadImage?.invoke()
                    }
                }
            }
        }
        else {
            setImageURI("")
            onFailedLoadImage?.invoke()
        }
    }
}