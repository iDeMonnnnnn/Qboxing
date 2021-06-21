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
package com.bilibili.boxing.model.config

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.bilibili.boxing.R
import com.bilibili.boxing.model.config.BoxingConfig.Mode

/**
 * The pick config.<br></br>
 * 1.[Mode] is necessary. <br></br>
 * 2.specify functions: camera, gif, paging. <br></br>
 * calling [.needCamera] to displayThumbnail a camera icon. <br></br>
 * calling [.needGif] to displayThumbnail gif photos. <br></br>
 * calling [.needPaging] to create load medias page by page, by default is true.
 *
 * @author ChenSL
 */
class BoxingConfig : Parcelable {
    var mode: Mode =
        Mode.SINGLE_IMG
        private set
    private var viewMode: ViewMode =
        ViewMode.PREVIEW
    var cropOption: BoxingCropOption? = null
        private set
    /**
     * get the image drawable resource by [BoxingConfig.withMediaPlaceHolderRes].
     * @return >0, set a valid drawable resource; otherwise without a placeholder.
     */
    @get:DrawableRes
    var mediaPlaceHolderRes = 0
        private set
    /**
     * get the media checked drawable resource by [BoxingConfig.withMediaCheckedRes].
     * @return >0, set a valid drawable resource; otherwise without a placeholder.
     */
    @get:DrawableRes
    var mediaCheckedRes = 0
        private set
    /**
     * get the media unchecked drawable resource by [BoxingConfig.withMediaUncheckedRes] (int)}.
     * @return >0, set a valid drawable resource; otherwise without a placeholder.
     */
    @get:DrawableRes
    var mediaUnCheckedRes = 0
        private set
    /**
     * get the album drawable resource by [BoxingConfig.withAlbumPlaceHolderRes].
     * @return >0, set a valid drawable resource; otherwise without a placeholder.
     */
    @get:DrawableRes
    var albumPlaceHolderRes = 0
        private set
    /**
     * get the video drawable resource by [BoxingConfig.withVideoDurationRes].
     * @return >0, set a valid drawable resource; otherwise without a placeholder.
     */
    @get:DrawableRes
    var videoDurationRes = 0
        private set
    /**
     * get the media unchecked drawable resource by [BoxingConfig.withMediaPlaceHolderRes].
     * @return >0, set a valid drawable resource; otherwise without a placeholder.
     */
    @get:DrawableRes
    var cameraRes = 0
    var isNeedCamera = false
    var isNeedGif = false
    var isNeedPaging = true
    private var mMaxCount =
        DEFAULT_SELECTED_COUNT

    enum class Mode {
        SINGLE_IMG, MULTI_IMG, VIDEO
    }

    enum class ViewMode {
        PREVIEW, EDIT, PRE_EDIT
    }

    constructor(mode: Mode) {
        this.mode = mode
        this.cameraRes = R.drawable.ic_boxing_camera
    }

    /**
     * get the max count set by [.withMaxCount], otherwise return 9.
     */
    val maxCount: Int
        get() = if (mMaxCount > 0) {
            mMaxCount
        } else DEFAULT_SELECTED_COUNT

    val isNeedLoading: Boolean
        get() = viewMode == ViewMode.EDIT

    val isNeedEdit: Boolean
        get() = viewMode != ViewMode.PREVIEW

    val isVideoMode: Boolean
        get() = mode == Mode.VIDEO

    val isMultiImageMode: Boolean
        get() = mode == Mode.MULTI_IMG

    val isSingleImageMode: Boolean
        get() = mode == Mode.SINGLE_IMG

    /**
     * call this means gif is needed.
     */
    fun needGif() = apply {
        isNeedGif = true
    }

    fun needCamera() = apply {
        isNeedCamera = true
    }

    /**
     * set the camera res.
     */
    fun needCamera(@DrawableRes _cameraRes: Int)= apply {
        cameraRes = _cameraRes
        isNeedCamera = true
    }

    /**
     * call this means paging is needed,by default is true.
     */
    fun needPaging(needPaging: Boolean)= apply {
        isNeedPaging = needPaging
    }

    fun withViewer(_viewMode: ViewMode)= apply {
        viewMode = _viewMode
    }

    fun withCropOption(_cropOption: BoxingCropOption?)= apply {
        cropOption = _cropOption
    }

    /**
     * set the max count of selected medias in [Mode.MULTI_IMG]
     * @param count max count
     */
    fun withMaxCount(count: Int)= apply {
        if (count < 1) {
            return@apply
        }
        mMaxCount = count
    }

    /**
     * set the image placeholder, default 0
     */
    fun withMediaPlaceHolderRes(@DrawableRes _mediaPlaceHolderRes: Int)= apply {
        mediaPlaceHolderRes = _mediaPlaceHolderRes
    }

    /**
     * set the image placeholder, otherwise use default drawable.
     */
    fun withMediaCheckedRes(@DrawableRes mediaCheckedResRes: Int)= apply {
        mediaCheckedRes = mediaCheckedResRes
    }

    /**
     * set the image placeholder, otherwise use default drawable.
     */
    fun withMediaUncheckedRes(@DrawableRes mediaUncheckedRes: Int)= apply {
        mediaUnCheckedRes = mediaUncheckedRes
    }

    /**
     * set the album placeholder, default 0
     */
    fun withAlbumPlaceHolderRes(@DrawableRes _albumPlaceHolderRes: Int)= apply {
        albumPlaceHolderRes = _albumPlaceHolderRes
    }

    /**
     * set the video duration resource in video mode, default 0
     */
    fun withVideoDurationRes(@DrawableRes _videoDurationRes: Int)= apply {
        videoDurationRes = _videoDurationRes
    }

    override fun toString(): String {
        return "BoxingConfig{" +
                "mMode=" + mode +
                ", mViewMode=" + viewMode +
                '}'
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mode.ordinal)
        dest.writeInt(viewMode.ordinal)
        dest.writeParcelable(cropOption, flags)
        dest.writeInt(mediaPlaceHolderRes)
        dest.writeInt(mediaCheckedRes)
        dest.writeInt(mediaUnCheckedRes)
        dest.writeInt(albumPlaceHolderRes)
        dest.writeInt(videoDurationRes)
        dest.writeInt(cameraRes)
        dest.writeByte(if (isNeedCamera) 1.toByte() else 0.toByte())
        dest.writeByte(if (isNeedGif) 1.toByte() else 0.toByte())
        dest.writeByte(if (isNeedPaging) 1.toByte() else 0.toByte())
        dest.writeInt(mMaxCount)
    }

    protected constructor(`in`: Parcel) {
        mode = Mode.values()[`in`.readInt()]
        viewMode = ViewMode.values()[`in`.readInt()]
        cropOption =
            `in`.readParcelable(BoxingCropOption::class.java.classLoader)
        mediaPlaceHolderRes = `in`.readInt()
        mediaCheckedRes = `in`.readInt()
        mediaUnCheckedRes = `in`.readInt()
        albumPlaceHolderRes = `in`.readInt()
        videoDurationRes = `in`.readInt()
        cameraRes = `in`.readInt()
        isNeedCamera = `in`.readByte().toInt() != 0
        isNeedGif = `in`.readByte().toInt() != 0
        isNeedPaging = `in`.readByte().toInt() != 0
        mMaxCount = `in`.readInt()
    }

    companion object {
        const val DEFAULT_SELECTED_COUNT = 9
        @JvmField
        val CREATOR: Parcelable.Creator<BoxingConfig> =
            object : Parcelable.Creator<BoxingConfig> {
                override fun createFromParcel(source: Parcel): BoxingConfig? {
                    return BoxingConfig(source)
                }

                override fun newArray(size: Int): Array<BoxingConfig?> {
                    return arrayOfNulls(size)
                }
            }
    }
}