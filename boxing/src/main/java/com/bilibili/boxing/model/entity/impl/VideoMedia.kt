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
package com.bilibili.boxing.model.entity.impl

import android.os.Parcel
import android.os.Parcelable
import com.bilibili.boxing.model.entity.BaseMedia
import java.util.*

/**
 * Entity represent a Video.
 *
 * @author ChenSL
 */
class VideoMedia : BaseMedia {
    var title: String? = null
    private var mDuration: String = ""
    private var dateTaken: String? = null
    private var mimeType: String? = null

    override val type: TYPE
        get() = TYPE.VIDEO

    constructor(builder: Builder) : super(
        builder.mId,
        builder.mPath
    ) {
        title = builder.mTitle
        mDuration = builder.mDuration
        mSize = builder.mSize
        dateTaken = builder.mDateTaken
        mimeType = builder.mMimeType
    }

    var duration: String
        get() = try {
            val duration = mDuration.toLong()
            formatTimeWithMin(duration)
        } catch (e: NumberFormatException) {
            "0:00"
        }
        set(duration) {
            mDuration = duration
        }

    private fun formatTimeWithMin(duration: Long): String {
        if (duration <= 0) {
            return String.format(Locale.US, "%02d:%02d", 0, 0)
        }
        val totalSeconds = duration / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(
                Locale.US, "%02d:%02d", hours * 60 + minutes,
                seconds
            )
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    val sizeByUnit: String
        get() {
            val size = size.toDouble()
            if (size == 0.0) {
                return "0K"
            }
            if (size >= MB) {
                val sizeInM =
                    size / MB
                return String.format(Locale.getDefault(), "%.1f", sizeInM) + "M"
            }
            val sizeInK = size / 1024
            return String.format(Locale.getDefault(), "%.1f", sizeInK) + "K"
        }

    class Builder(val mId: String?, val mPath: String?) {
        var mTitle: String = ""
        var mDuration: String = ""
        var mSize: String = ""
        var mDateTaken: String = ""
        var mMimeType: String = ""
        fun setTitle(title: String?): Builder {
            mTitle = title ?: ""
            return this
        }

        fun setDuration(duration: String?): Builder {
            mDuration = duration ?: ""
            return this
        }

        fun setSize(size: String?): Builder {
            mSize = size ?: ""
            return this
        }

        fun setDataTaken(dateTaken: String?): Builder {
            mDateTaken = dateTaken ?: ""
            return this
        }

        fun setMimeType(type: String?): Builder {
            mMimeType = type ?: ""
            return this
        }

        fun build(): VideoMedia {
            return VideoMedia(this)
        }

    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(title)
        dest.writeString(mDuration)
        dest.writeString(dateTaken)
        dest.writeString(mimeType)
    }

    protected constructor(`in`: Parcel) : super(`in`) {
        title = `in`.readString()
        mDuration = `in`.readString() ?: ""
        dateTaken = `in`.readString()
        mimeType = `in`.readString()
    }

    companion object {
        private const val MB = 1024 * 1024.toLong()
        @JvmField
        val CREATOR: Parcelable.Creator<VideoMedia> =
            object : Parcelable.Creator<VideoMedia> {
                override fun createFromParcel(source: Parcel): VideoMedia? {
                    return VideoMedia(source)
                }

                override fun newArray(size: Int): Array<VideoMedia?> {
                    return arrayOfNulls(size)
                }
            }
    }
}