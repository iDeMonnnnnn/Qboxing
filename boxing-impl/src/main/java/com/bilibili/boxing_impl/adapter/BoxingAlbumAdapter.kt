/*
 *  Copyright (C) 2017 Bilibili
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.bilibili.boxing_impl.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bilibili.boxing.BoxingMediaLoader
import com.bilibili.boxing.model.BoxingManager
import com.bilibili.boxing.model.entity.AlbumEntity
import com.bilibili.boxing.model.entity.AlbumEntity.Companion.createDefaultAlbum
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.utils.asTypeNull
import com.bilibili.boxing_impl.R

/**
 * Album window adapter.
 *
 * @author ChenSL
 */
class BoxingAlbumAdapter(context: Context) :
    RecyclerView.Adapter<BoxingAlbumAdapter.AlbumViewHolder>(), View.OnClickListener {
    var currentAlbumPos = 0
    private val mAlums: MutableList<AlbumEntity> = mutableListOf()
    private val mInflater: LayoutInflater
    private var mAlbumOnClickListener: OnAlbumClickListener? = null
    private val mDefaultRes: Int

    fun setAlbumOnClickListener(albumOnClickListener: OnAlbumClickListener?) {
        mAlbumOnClickListener = albumOnClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AlbumViewHolder {
        return AlbumViewHolder(
            mInflater.inflate(
                R.layout.layout_boxing_album_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: AlbumViewHolder,
        position: Int
    ) {
        holder.mCoverImg.setImageResource(mDefaultRes)
        val adapterPos = holder.adapterPosition
        val album = mAlums[adapterPos]
        if (album.hasImages()) {
            val albumName =
                if (TextUtils.isEmpty(album.mBucketName)) {
                    holder.mNameTxt.context.getString(R.string.boxing_default_album_name).also { album.mBucketName = it }
                } else album.mBucketName
            holder.mNameTxt.text = albumName
            val media = album.mImageList[0].asTypeNull<ImageMedia>()
            if (media != null) {
                BoxingMediaLoader.getInstance()
                    .displayThumbnail(holder.mCoverImg, media.uri, 50, 50)
                holder.mCoverImg.setTag(
                    R.string.boxing_app_name,
                    media.path
                )
            }
            holder.mLayout.tag = adapterPos
            holder.mLayout.setOnClickListener(this)
            holder.mCheckedImg.visibility =
                if (album.mIsSelected) View.VISIBLE else View.GONE
            holder.mSizeTxt.text = holder.mSizeTxt.resources.getString(
                R.string.boxing_album_images_fmt,
                album.mCount
            )
        } else {
            holder.mNameTxt.text = UNKNOW_ALBUM_NAME
            holder.mSizeTxt.visibility = View.GONE
        }
    }

    fun addAllData(alums: List<AlbumEntity>) {
        mAlums.clear()
        mAlums.addAll(alums)
        notifyDataSetChanged()
    }

    val alums: List<AlbumEntity>
        get() = mAlums

    val currentAlbum: AlbumEntity?
        get() = if (mAlums.size <= 0) {
            null
        } else mAlums[currentAlbumPos]

    override fun getItemCount(): Int {
        return mAlums.size
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.album_layout) {
            mAlbumOnClickListener?.onClick(v, v.tag as Int)
        }
    }

    class AlbumViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var mCoverImg: ImageView = itemView.findViewById(R.id.album_thumbnail)
        var mNameTxt: TextView = itemView.findViewById(R.id.album_name)
        var mSizeTxt: TextView = itemView.findViewById(R.id.album_size)
        var mLayout: View = itemView.findViewById<View>(R.id.album_layout)
        var mCheckedImg: ImageView = itemView.findViewById(R.id.album_checked)

    }

    interface OnAlbumClickListener {
        fun onClick(view: View?, pos: Int)
    }

    companion object {
        private const val UNKNOW_ALBUM_NAME = "?"
    }

    init {
        mAlums.add(createDefaultAlbum())
        mInflater = LayoutInflater.from(context)
        mDefaultRes = BoxingManager.getInstance().boxingConfig.albumPlaceHolderRes
    }
}