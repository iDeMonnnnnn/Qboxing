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
package com.bilibili.boxing_impl.view

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.bilibili.boxing.BoxingMediaLoader
import com.bilibili.boxing.model.BoxingManager.Companion.getInstance
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.model.entity.impl.VideoMedia
import com.bilibili.boxing_impl.BoxingResHelper.mediaCheckedRes
import com.bilibili.boxing_impl.BoxingResHelper.mediaUncheckedRes
import com.bilibili.boxing_impl.R
import com.bilibili.boxing_impl.WindowManagerHelper.getScreenHeight
import com.bilibili.boxing_impl.WindowManagerHelper.getScreenWidth

/**
 * A media layout for [androidx.recyclerview.widget.RecyclerView] item, including image and video <br></br>
 *
 * @author ChenSL
 */
class MediaItemLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mScreenType: ScreenType
    private var media_item: ImageView
    private var media_item_check: ImageView
    private var media_font_layout: View
    private var video_layout: FrameLayout

    private enum class ScreenType(var value: Int) {
        SMALL(100), NORMAL(180), LARGE(320);

    }

    private fun setImageRect(context: Context) {
        val screenHeight =
            getScreenHeight(context)
        val screenWidth = getScreenWidth(context)
        var width = 100
        if (screenHeight != 0 && screenWidth != 0) {
            width =
                (screenWidth - resources.getDimensionPixelOffset(R.dimen.boxing_media_margin) * 4) / 3
        }
        media_item.layoutParams.width = width
        media_item.layoutParams.height = width
        media_font_layout.layoutParams.width = width
        media_font_layout.layoutParams.height = width
    }

    private fun getScreenType(context: Context): ScreenType {
        val type =
            context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val result: ScreenType
        result = when (type) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> ScreenType.SMALL
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> ScreenType.NORMAL
            Configuration.SCREENLAYOUT_SIZE_LARGE -> ScreenType.LARGE
            else -> ScreenType.NORMAL
        }
        return result
    }

    fun setImageRes(@DrawableRes imageRes: Int) {
        media_item.setImageResource(imageRes)
    }

    fun setMedia(media: BaseMedia) {
        if (media is ImageMedia) {
            video_layout.visibility = View.GONE
            setCover(media.uri)
        } else if (media is VideoMedia) {
            video_layout.visibility = View.VISIBLE
            val durationTxt =
                video_layout.findViewById<TextView>(R.id.video_duration_txt)
            durationTxt.text = media.duration
            durationTxt.setCompoundDrawablesWithIntrinsicBounds(
                getInstance().boxingConfig.videoDurationRes,
                0,
                0,
                0
            )
            video_layout.findViewById<TextView>(R.id.video_size_txt)?.text =
                media.sizeByUnit
            setCover(media.uri)
        }
    }

    private fun setCover(path: Uri?) {
        if (path == null) {
            return
        }
        media_item.setTag(R.string.boxing_app_name, path)
        BoxingMediaLoader.getInstance()
            .displayThumbnail(media_item, path, mScreenType.value, mScreenType.value)
    }

    fun setChecked(isChecked: Boolean) {
        if (isChecked) {
            media_font_layout.visibility = View.VISIBLE
            media_item_check.setImageDrawable(ContextCompat.getDrawable(context, mediaCheckedRes))
        } else {
            media_font_layout.visibility = View.GONE
            media_item_check.setImageDrawable(ContextCompat.getDrawable(context, mediaUncheckedRes))
        }
    }

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.layout_boxing_media_item, this, true)
        media_item = findViewById(R.id.media_item)
        media_font_layout = findViewById(R.id.media_font_layout)
        video_layout = findViewById(R.id.video_layout)
        media_item_check = findViewById(R.id.media_item_check)
        mScreenType = getScreenType(context)
        setImageRect(context)
    }
}