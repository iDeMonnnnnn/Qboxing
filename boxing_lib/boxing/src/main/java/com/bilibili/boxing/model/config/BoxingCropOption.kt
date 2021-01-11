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
@file:Suppress("unused")
@file:JvmName("BoxingCropOption")
package com.bilibili.boxing.model.config

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * The cropping config, a cropped photo uri is needed at least.
 *
 * @author ChenSL
 */
class BoxingCropOption : Parcelable {
    var destination: Uri?
        private set
    var aspectRatioX = 0f
        private set
    var aspectRatioY = 0f
        private set
    var maxWidth = 0
        private set
    var maxHeight = 0
        private set
    var isFreeStyle = false
        private set

    constructor(destination: Uri) {
        this.destination = destination
    }

    fun setFreeStyle(freeStyle: Boolean) = apply {
        isFreeStyle = freeStyle
    }

    fun aspectRatio(
        x: Float,
        y: Float
    )= apply {
        aspectRatioX = x
        aspectRatioY = y
    }

    fun useSourceImageAspectRatio()= apply {
        aspectRatioX = 0f
        aspectRatioY = 0f
    }

    fun withMaxResultSize(
        width: Int,
        height: Int
    )= apply {
        maxWidth = width
        maxHeight = height
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(destination, flags)
        dest.writeFloat(aspectRatioX)
        dest.writeFloat(aspectRatioY)
        dest.writeInt(maxWidth)
        dest.writeInt(maxHeight)
    }

    protected constructor(`in`: Parcel) {
        destination = `in`.readParcelable(Uri::class.java.classLoader)
        aspectRatioX = `in`.readFloat()
        aspectRatioY = `in`.readFloat()
        maxWidth = `in`.readInt()
        maxHeight = `in`.readInt()
    }

    companion object {
        const val CROP_IMAGE_ACTIVITY_REQUEST_CODE = 2333
        @JvmStatic
        fun with(destination: Uri): BoxingCropOption {
            return BoxingCropOption(destination)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<BoxingCropOption> =
            object : Parcelable.Creator<BoxingCropOption> {
                override fun createFromParcel(parcel: Parcel): BoxingCropOption {
                    return BoxingCropOption(parcel)
                }

                override fun newArray(size: Int): Array<BoxingCropOption?> {
                    return arrayOfNulls(size)
                }
            }
    }
}