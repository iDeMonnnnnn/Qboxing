package com.demon.qxing

import android.net.Uri
import android.widget.ImageView
import com.bilibili.boxing.loader.IBoxingCallback
import com.bilibili.boxing.loader.IBoxingMediaLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

/**
 * @author DeMon
 * Created on 2020/11/5.
 * E-mail 757454343@qq.com
 * Desc:*/
class GlideLoader : IBoxingMediaLoader {

    override fun displayThumbnail(img: ImageView, absPath: Uri?, width: Int, height: Int) {
        val options = RequestOptions().error(R.drawable.ic_qf_image).placeholder(R.drawable.ic_qf_image).centerCrop()
        //thumbnail缩略图
        Glide.with(img).asBitmap().thumbnail(0.5f).apply(options).load(absPath).into(img)
    }

    override fun displayRaw(img: ImageView, absPath: Uri?, width: Int, height: Int, callback: IBoxingCallback?) {
        val options = RequestOptions().error(R.drawable.ic_qf_image).placeholder(R.drawable.ic_qf_image).override(Target.SIZE_ORIGINAL)
        Glide.with(img).asBitmap().apply(options).load(absPath).into(img)
    }
}
