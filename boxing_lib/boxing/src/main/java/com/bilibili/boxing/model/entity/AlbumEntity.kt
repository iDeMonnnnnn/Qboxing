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

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * An entity for album.
 *
 * @author ChenSL
 */
class AlbumEntity : Parcelable {
    var mCount: Int
    var mIsSelected: Boolean
    var mBucketId: String = ""
    var mBucketName: String = DEFAULT_NAME
    var mImageList = mutableListOf<BaseMedia>()

    constructor() {
        mCount = 0
        mImageList = ArrayList()
        mIsSelected = false
    }

    fun hasImages() = mImageList.isNotEmpty()

    override fun toString(): String {
        return "AlbumEntity{" +
                "mCount=" + mCount +
                ", mBucketName='" + mBucketName + '\'' +
                ", mImageList=" + mImageList +
                '}'
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mBucketId)
        dest.writeInt(mCount)
        dest.writeString(mBucketName)
        dest.writeList(mImageList)
        dest.writeByte(if (mIsSelected) 1.toByte() else 0.toByte())
    }

    protected constructor(`in`: Parcel) {
        mBucketId = `in`.readString() ?: ""
        mCount = `in`.readInt()
        mBucketName = `in`.readString() ?: DEFAULT_NAME
        mImageList = mutableListOf()
        `in`.readList(
            mImageList,
            BaseMedia::class.java.classLoader
        )
        mIsSelected = `in`.readByte().toInt() != 0
    }

    companion object {
        const val DEFAULT_NAME = ""
        @JvmStatic
        fun createDefaultAlbum(): AlbumEntity {
            return AlbumEntity().apply {
                mBucketId = DEFAULT_NAME
                mIsSelected = true
            }
        }

        @JvmField
        val CREATOR: Parcelable.Creator<AlbumEntity> =
            object : Parcelable.Creator<AlbumEntity> {
                override fun createFromParcel(source: Parcel): AlbumEntity? {
                    return AlbumEntity(source)
                }

                override fun newArray(size: Int): Array<AlbumEntity?> {
                    return arrayOfNulls(size)
                }
            }
    }
}