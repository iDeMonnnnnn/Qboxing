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
package com.bilibili.boxing.model.task.impl

import android.content.ContentResolver
import android.provider.MediaStore.Images
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.bilibili.boxing.model.BoxingManager
import com.bilibili.boxing.model.callback.IAlbumTaskCallback
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.entity.AlbumEntity
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.utils.BoxingExecutor

/**
 * A task to load albums.
 *
 * @author ChenSL
 */
@WorkerThread
class AlbumTask {
    private var mUnknownAlbumNumber = 1
    private val mBucketMap: MutableMap<String, AlbumEntity>?
    private val mDefaultAlbum: AlbumEntity
    private val mPickerConfig: BoxingConfig?
    fun start(
        cr: ContentResolver,
        callback: IAlbumTaskCallback
    ) {
        buildAlbumInfo(cr)
        getAlbumList(callback)
    }

    private fun buildAlbumInfo(cr: ContentResolver) {
        val distinctBucketColumns =
            arrayOf(Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME)
        //android Q之后查询的where条件会出现(())两层的括号，没法使用group by
        val bucketCursor = cr.query(
            Images.Media.EXTERNAL_CONTENT_URI,
            distinctBucketColumns,
            null,
            null,
            "${Images.Media.DATE_MODIFIED} desc"
        )
        bucketCursor.use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                //由于查出来的数据中有多个buckId和name重复的数据，需要把它过滤掉
                val hashSet = mutableSetOf<MutableMap<String, String>>()
                do {
                    val buckId =
                        cursor.getString(cursor.getColumnIndex(Images.Media.BUCKET_ID)) ?: ""
                    val name =
                        cursor.getString(cursor.getColumnIndex(Images.Media.BUCKET_DISPLAY_NAME)) ?: ""
                    if (buckId.isNotEmpty()) {
                        hashSet.add(mutableMapOf(
                            Images.Media.BUCKET_ID to buckId,
                            Images.Media.BUCKET_DISPLAY_NAME to name
                        ))
                    }
                } while (cursor.moveToNext())
                hashSet.forEach {  map ->
                    val album = buildAlbumInfo(
                        map[Images.Media.BUCKET_DISPLAY_NAME],
                        map[Images.Media.BUCKET_ID]
                    )
                    if (!TextUtils.isEmpty(map[Images.Media.BUCKET_ID])) {
                        buildAlbumCover(cr, map[Images.Media.BUCKET_ID] ?: "", album)
                    }
                }
            }
        }
    }

    /**
     * get the cover and count
     *
     * @param buckId album id
     */
    private fun buildAlbumCover(
        cr: ContentResolver,
        buckId: String,
        album: AlbumEntity
    ) {
        val photoColumn =
            arrayOf(Images.Media._ID, Images.Media.DATA)
        val isNeedGif = mPickerConfig != null && mPickerConfig.isNeedGif
        val selectionId =
            if (isNeedGif) SELECTION_ID else SELECTION_ID_WITHOUT_GIF
        val args =
            if (isNeedGif) SELECTION_ARGS_IMAGE_MIME_TYPE else SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF
        val selectionArgs = arrayOfNulls<String>(args.size + 1)
        selectionArgs[0] = buckId
        for (i in 1 until selectionArgs.size) {
            selectionArgs[i] = args[i - 1]
        }
        val coverCursor = cr.query(
            Images.Media.EXTERNAL_CONTENT_URI, photoColumn, selectionId,
            selectionArgs, "${Images.Media.DATE_MODIFIED} desc"
        )
        coverCursor.use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val picPath =
                    cursor.getString(cursor.getColumnIndex(Images.Media.DATA)) ?: ""
                val id =
                    cursor.getString(cursor.getColumnIndex(Images.Media._ID)) ?: ""
                album.mCount = cursor.count
                album.mImageList.add(ImageMedia(id, picPath))
                if (album.mImageList.size > 0) {
                    mBucketMap?.put(buckId, album)
                }
            }
        }
    }

    private fun getAlbumList(callback: IAlbumTaskCallback) {
        mDefaultAlbum.mCount = 0
        val tmpList: MutableList<AlbumEntity> = mutableListOf()
        if (mBucketMap == null) {
            postAlbums(callback, tmpList)
            return
        }
        for ((_, value) in mBucketMap) {
            tmpList.add(value)
            mDefaultAlbum.mCount += value.mCount
        }
        if (tmpList.size > 0) {
            tmpList[0].mImageList.let {
                mDefaultAlbum.mImageList = it
            }
            tmpList.add(0, mDefaultAlbum)
        }
        postAlbums(callback, tmpList)
        clear()
    }

    private fun postAlbums(
        callback: IAlbumTaskCallback,
        result: MutableList<AlbumEntity>
    ) {
        BoxingExecutor.getInstance().runUI { callback.postAlbumList(result) }
    }

    private fun buildAlbumInfo(
        bucketName: String?,
        bucketId: String?
    ): AlbumEntity {
        return if (!bucketId.isNullOrEmpty()) {
            mBucketMap?.get(bucketId) ?: AlbumEntity()
        } else {
            AlbumEntity()
        }.apply {
            when {
                !bucketId.isNullOrEmpty() -> {
                    mBucketId = bucketId
                }
                else -> {
                    mBucketId = mUnknownAlbumNumber.toString()
                    mUnknownAlbumNumber++
                }
            }
            when {
                !bucketName.isNullOrEmpty() -> {
                    mBucketName = bucketName
                }
                else -> {
                    mBucketName = UNKNOWN_ALBUM_NAME
                    mUnknownAlbumNumber++
                }
            }
            if (mImageList.size > 0) {
                mBucketMap?.put(bucketId ?: "", this)
            }
        }
    }

    private fun clear() {
        mBucketMap?.clear()
    }

    companion object {
        private const val UNKNOWN_ALBUM_NAME = "unknow"
        private const val SELECTION_IMAGE_MIME_TYPE =
            "${Images.Media.MIME_TYPE}=? or ${Images.Media.MIME_TYPE}=? or ${Images.Media.MIME_TYPE}=? or ${Images.Media.MIME_TYPE}=?"
        private const val SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF =
            "${Images.Media.MIME_TYPE}=? or ${Images.Media.MIME_TYPE}=? or ${Images.Media.MIME_TYPE}=?"
        private const val SELECTION_ID =
            "${Images.Media.BUCKET_ID}=? and ($SELECTION_IMAGE_MIME_TYPE )"
        private const val SELECTION_ID_WITHOUT_GIF =
            "${Images.Media.BUCKET_ID}=? and ($SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF )"
        private val SELECTION_ARGS_IMAGE_MIME_TYPE =
            arrayOf(
                "image/jpeg",
                "image/png",
                "image/jpg",
                "image/gif"
            )
        private val SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF =
            arrayOf(
                "image/jpeg",
                "image/png",
                "image/jpg"
            )
    }

    init {
        mBucketMap = mutableMapOf()
        mDefaultAlbum = AlbumEntity.createDefaultAlbum()
        mPickerConfig = BoxingManager.getInstance().boxingConfig
    }
}