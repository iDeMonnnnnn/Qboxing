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

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.utils.*
import java.io.File
import java.io.FileInputStream

/**
 * Id and absolute path is necessary.Builder Mode can be used too.
 * compress image through [.compress].
 *
 * @author ChenSL
 */
class ImageMedia : BaseMedia, Parcelable {
    var isSelected = false
    private var mThumbnailPath: String = ""
    var compressPath: String = ""
    var height = 0
    var width = 0
    private var imageType: IMAGE_TYPE = IMAGE_TYPE.PNG
    private var mMimeType: String? = ""

    enum class IMAGE_TYPE {
        PNG, JPG, GIF
    }

    constructor(id: String?, imagePath: String?) : super(id, imagePath)

    constructor(file: File) : super(System.currentTimeMillis().toString(), file.absolutePath) {
        mSize = file.length().toString()
        uri = Uri.fromFile(file)
        isSelected = true
    }

    constructor(builder: Builder) : super(
        builder.mId,
        builder.mImagePath
    ) {
        mThumbnailPath = builder.mThumbnailPath
        mSize = builder.mSize
        height = builder.mHeight
        isSelected = builder.mIsSelected
        width = builder.mWidth
        mMimeType = builder.mMimeType
        imageType = getImageTypeByMime(builder.mMimeType)
    }

    override val type: TYPE
        get() = TYPE.IMAGE

    val isGifOverSize: Boolean
        get() = isGif && size > MAX_GIF_SIZE

    private val isGif: Boolean
        get() = imageType == IMAGE_TYPE.GIF

    fun compress(imageCompressor: ImageCompressor?): Boolean {
        return CompressTask.compress(
            imageCompressor,
            this,
            MAX_IMAGE_SIZE
        )
    }

    /**
     * @param maxSize the proximate max size for compression
     * @return may be a little bigger than expected for performance.
     */
    fun compress(
        imageCompressor: ImageCompressor?,
        maxSize: Long
    ): Boolean {
        return CompressTask.compress(imageCompressor, this, maxSize)
    }

    /**
     * get mime type displayed in database.
     *
     * @return "image/gif" or "image/jpeg".
     */
    private val mimeType: String
        get() {
            return when (imageType) {
                IMAGE_TYPE.GIF -> {
                    "image/gif"
                }
                IMAGE_TYPE.JPG -> {
                    "image/jpeg"
                }
                else -> "image/jpeg"
            }
        }

    private fun getImageTypeByMime(mimeType: String?): IMAGE_TYPE {
        return when {
            !TextUtils.isEmpty(mimeType) -> {
                when (mimeType) {
                    "image/gif" -> {
                        IMAGE_TYPE.GIF
                    }
                    "image/png" -> {
                        IMAGE_TYPE.PNG
                    }
                    else -> {
                        IMAGE_TYPE.JPG
                    }
                }
            }
            else -> IMAGE_TYPE.PNG
        }
    }

    @Suppress("UNUSED")
    fun removeExif() {
        BoxingExifHelper.removeExif(path)
    }

    /**
     * save image to MediaStore.兼容AndroidQ
     */
    fun saveMediaStore(cr: ContentResolver?) {
        BoxingExecutor.getInstance()
            .runWorker(Runnable {
                if (cr != null && !TextUtils.isEmpty(id)) {
                    val values = ContentValues()
                    values.put(MediaStore.Images.Media.TITLE, id)
                    values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        val saveUri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        if (saveUri != null) {
                            val out = cr.openOutputStream(saveUri)
                            val input = FileInputStream(path)
                            if (out != null) {
                                FileUtils.copy(input, out) //直接调用系统copy保存
                            }
                            out?.close()
                            input.close()
                        }
                    } else {
                        values.put(MediaStore.Images.Media.DATA, path)
                        cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    }
                }
            })
    }

    override fun setSize(size: String) {
        mSize = size
    }


    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    val thumbnailPath: String
        get() {
            if (BoxingFileHelper.isFileValid(mThumbnailPath)) {
                return mThumbnailPath
            } else if (BoxingFileHelper.isFileValid(compressPath)) {
                return compressPath
            }
            return path
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        other ?: return false
        if (javaClass != other.javaClass) {
            return false
        }
        if (other !is ImageMedia) return false
        return !(TextUtils.isEmpty(path) || TextUtils.isEmpty(other.path)) && path == other.path
    }

    class Builder(val mId: String?, val mImagePath: String?) {
        var mIsSelected = false
        var mThumbnailPath: String = ""
        var mSize: String = ""
        var mHeight = 0
        var mWidth = 0
        var mMimeType: String = ""

        fun setSelected(selected: Boolean): Builder {
            mIsSelected = selected
            return this
        }

        fun setThumbnailPath(thumbnailPath: String?): Builder {
            mThumbnailPath = thumbnailPath ?: ""
            return this
        }

        fun setHeight(height: Int): Builder {
            mHeight = height
            return this
        }

        fun setWidth(width: Int): Builder {
            mWidth = width
            return this
        }

        fun setMimeType(mimeType: String?): Builder {
            mMimeType = mimeType ?: ""
            return this
        }

        fun setSize(size: String?): Builder {
            mSize = size ?: ""
            return this
        }

        fun build(): ImageMedia {
            return ImageMedia(this)
        }

    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeByte(if (isSelected) 1.toByte() else 0.toByte())
        dest.writeString(mThumbnailPath)
        dest.writeString(compressPath)
        dest.writeInt(height)
        dest.writeInt(width)
        dest.writeInt(imageType.ordinal)
        dest.writeString(mMimeType)
    }

    override fun toString(): String {
        return "ImageMedia(id='$id',mThumbnailPath='$mThumbnailPath', compressPath='$compressPath', path='$path', imageType='$imageType', uri='$uri',sandboxPath='$sandboxPath')"
    }

    protected constructor(`in`: Parcel) : super(`in`) {
        isSelected = `in`.readByte().toInt() != 0
        mThumbnailPath = `in`.readString() ?: ""
        compressPath = `in`.readString() ?: ""
        height = `in`.readInt()
        width = `in`.readInt()
        imageType = IMAGE_TYPE.values()[`in`.readInt()]
        mMimeType = `in`.readString()
    }

    companion object {
        private const val MAX_GIF_SIZE = 1024 * 1024L
        private const val MAX_IMAGE_SIZE = 1024 * 1024L

        @JvmField
        val CREATOR: Parcelable.Creator<ImageMedia> =
            object : Parcelable.Creator<ImageMedia> {
                override fun createFromParcel(source: Parcel): ImageMedia? {
                    return ImageMedia(source)
                }

                override fun newArray(size: Int): Array<ImageMedia?> {
                    return arrayOfNulls(size)
                }
            }
    }
}