package com.demon.qboxing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.demon.qboxing.databinding.ListImgsBinding

/**
 * @author DeMon
 * Created on 2021/1/14.
 * E-mail 757454343@qq.com
 * Desc:
 */
class ImgsAdapter : RecyclerView.Adapter<ImgsAdapter.Holder>() {

    var datas = mutableListOf<ImageMedia>()

    class Holder(val binding: ListImgsBinding) : RecyclerView.ViewHolder(binding.root) {
        val mContext = binding.root.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val bing = ListImgsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(bing)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val media = datas[position]
        val options = RequestOptions().override(SIZE_ORIGINAL)
        val path = if (media.compressPath.isEmpty()) {
            media.newPath
        } else {
            media.compressPath
        }
        Glide.with(holder.mContext).asBitmap().apply(options).load(path).into(holder.binding.img)
    }

    override fun getItemCount(): Int = datas.size
}