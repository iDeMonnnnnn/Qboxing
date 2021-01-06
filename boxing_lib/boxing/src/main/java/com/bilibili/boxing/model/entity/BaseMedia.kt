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
package com.bilibili.boxing.model.entity

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import com.bilibili.boxing.Boxing
import com.bilibili.boxing.getFileFromUriN
import com.bilibili.boxing.getFileFromUriQ
import com.bilibili.boxing.uriToFile

/**
 * The base entity for media.
 *
 * @author ChenSL
 */
abstract class BaseMedia : Parcelable {
    enum class TYPE {
        IMAGE, VIDEO
    }

    var id: String = ""
    var path: String = ""
    var mSize: String = ""
    var sandboxPath: String? = null
        get() {
            if (field != null) return field
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri.uriToFile(Boxing.mContext)?.absolutePath
            } else {
                path
            }
        }
    var uri: Uri? = null
        get() {
            if (field != null) return field
            return if (id.isEmpty()) {
                null
            } else {
                if (type == TYPE.IMAGE) {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong())
                } else {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toLong())
                }
            }
        }

    constructor(id: String?, path: String?) {
        this.id = id ?: ""
        this.path = path ?: ""
    }

    abstract val type: TYPE

    val size: Long
        get() = try {
            val result = mSize.toLong()
            if (result > 0) result else 0
        } catch (size: NumberFormatException) {
            0
        }

    open fun setSize(size: String) {
        mSize = size
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(path)
        dest.writeString(id)
        dest.writeString(mSize)
    }

    override fun toString(): String {
        return "BaseMedia(id='$id', path='$path,uri='$uri')"
    }


    protected constructor(`in`: Parcel) {
        path = `in`.readString() ?: ""
        id = `in`.readString() ?: ""
        mSize = `in`.readString() ?: ""
    }

}