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

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.bilibili.boxing.Boxing
import com.bilibili.boxing.utils.BoxingFileHelper
import com.bilibili.boxing.utils.isExistScope
import com.bilibili.boxing.utils.uriToFile

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

    /**
     * 使用newPath代替path，兼容了AndroidQ无法获取非作用域文件的问题
     */
    var newPath: String = ""
        get() {
            if (field.isNotEmpty()) return field
            field = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !path.isExistScope()) {
                uri.uriToFile(Boxing.mContext)?.absolutePath ?: ""
            } else {
                path
            }
            return field
        }
    var mSize: String = ""
    var uri: Uri? = null
        get() {
            if (field != null) return field
            field = BoxingFileHelper.getFileUri(Boxing.mContext, this)
            return field
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