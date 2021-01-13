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

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore.Images
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.bilibili.boxing.model.BoxingManager
import com.bilibili.boxing.model.callback.IMediaTaskCallback
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.model.task.IMediaTask
import com.bilibili.boxing.utils.BoxingExecutor
import com.bilibili.boxing.utils.BoxingLog
import java.util.*

/**
 * A Task to load photos.
 *
 * @author ChenSL
 */
@WorkerThread
class ImageTask :
    IMediaTask<ImageMedia> {
    private val mPickerConfig: BoxingConfig = BoxingManager.getInstance().boxingConfig
    private val mThumbnailMap: MutableMap<String, String> = mutableMapOf()


    override fun load(
        cr: ContentResolver, page: Int, id: String,
        callback: IMediaTaskCallback<ImageMedia>
    ) {
        buildThumbnail(cr)
        buildAlbumList(cr, id, page, callback)
    }

    private fun buildThumbnail(cr: ContentResolver) {
        val projection =
            arrayOf(Images.Thumbnails.IMAGE_ID, Images.Thumbnails.DATA)
        queryThumbnails(cr, projection)
    }

    private fun queryThumbnails(
        cr: ContentResolver,
        projection: Array<String>
    ) {
        val cur= Images.Thumbnails.queryMiniThumbnails(
            cr, Images.Thumbnails.EXTERNAL_CONTENT_URI,
            Images.Thumbnails.MINI_KIND, projection
        )
        cur?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val imageId =
                        cursor.getString(cursor.getColumnIndex(Images.Thumbnails.IMAGE_ID)) ?: ""
                    val imagePath =
                        cursor.getString(cursor.getColumnIndex(Images.Thumbnails.DATA)) ?: ""
                    mThumbnailMap[imageId] = imagePath
                } while (cursor.moveToNext() && !cursor.isLast)
            }
        }
    }

    private fun buildAlbumList(
        cr: ContentResolver, bucketId: String, page: Int,
        callback: IMediaTaskCallback<ImageMedia>
    ): List<ImageMedia> {
        val result: MutableList<ImageMedia> =
            ArrayList()
        val columns = columns
        val isDefaultAlbum = TextUtils.isEmpty(bucketId)
        val isNeedPaging = mPickerConfig.isNeedPaging
        val isNeedGif = mPickerConfig.isNeedGif
        val totalCount = getTotalCount(cr, bucketId, columns, isDefaultAlbum, isNeedGif)
        val imageMimeType =
            if (isNeedGif) SELECTION_IMAGE_MIME_TYPE else SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF
        val args =
            if (isNeedGif) SELECTION_ARGS_IMAGE_MIME_TYPE else SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF
        val order = if (isNeedPaging) "${Images.Media.DATE_MODIFIED}$DESC LIMIT ${page * IMediaTask.PAGE_LIMIT} , ${IMediaTask.PAGE_LIMIT}" else "${Images.Media.DATE_MODIFIED}$DESC"
        val selectionId = if (isNeedGif) SELECTION_ID else SELECTION_ID_WITHOUT_GIF
        val cursor = query(
            cr,
            bucketId,
            columns,
            isDefaultAlbum,
            isNeedGif,
            imageMimeType,
            args,
            order,
            selectionId
        )
        cursor?.use {
            addItem(totalCount, result, it, callback)
        }
        return result
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun addItem(
        allCount: Int,
        result: MutableList<ImageMedia>,
        cursor: Cursor,
        callback: IMediaTaskCallback<ImageMedia>
    ) {
        if (cursor.moveToFirst()) {
            do {
                val picPath = cursor.getString(cursor.getColumnIndex(Images.Media.DATA)) ?: ""
                if (callback.needFilter(picPath)) {
                    BoxingLog.d("path:$picPath has been filter")
                } else {
                    val id = cursor.getString(cursor.getColumnIndex(Images.Media._ID)) ?: ""
                    val size = cursor.getString(cursor.getColumnIndex(Images.Media.SIZE)) ?: ""
                    val mimeType = cursor.getString(cursor.getColumnIndex(Images.Media.MIME_TYPE)) ?: ""
                    var width = 0
                    var height = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        width = cursor.getInt(cursor.getColumnIndex(Images.Media.WIDTH))
                        height = cursor.getInt(cursor.getColumnIndex(Images.Media.HEIGHT))
                    }
                    val imageItem = ImageMedia.Builder(id, picPath)
                            .setThumbnailPath(mThumbnailMap[id] ?: "")
                            .setSize(size).setMimeType(mimeType).setHeight(height).setWidth(width)
                            .build()
                    if (!result.contains(imageItem)) {
                        result.add(imageItem)
                    }
                }
            } while (!cursor.isLast && cursor.moveToNext())
            postMedias(result, allCount, callback)
        } else {
            postMedias(result, 0, callback)
        }
        clear()
    }

    private fun postMedias(
        result: List<ImageMedia>,
        count: Int,
        callback: IMediaTaskCallback<ImageMedia>
    ) {
        BoxingExecutor.getInstance().runUI { callback.postMedia(result, count) }
    }

    private fun query(
        cr: ContentResolver,
        bucketId: String,
        columns: Array<String>,
        isDefaultAlbum: Boolean,
        isNeedGif: Boolean,
        imageMimeType: String,
        args: Array<String>,
        order: String,
        selectionId: String
    ): Cursor? {
        return when {
            isDefaultAlbum -> cr.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                imageMimeType,
                args,
                order
            )
            isNeedGif -> cr.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                selectionId,
                arrayOf(
                    bucketId,
                    args[0],
                    args[1],
                    args[2],
                    args[3]
                ),
                order
            )
            else -> cr.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                selectionId,
                arrayOf(bucketId, args[0], args[1], args[2]),
                order
            )
        }
    }

    private val columns: Array<String>
        @SuppressLint("ObsoleteSdkInt")
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                arrayOf(
                    Images.Media._ID,
                    Images.Media.DATA,
                    Images.Media.SIZE,
                    Images.Media.MIME_TYPE,
                    Images.Media.WIDTH,
                    Images.Media.HEIGHT
                )
            } else {
                arrayOf(
                    Images.Media._ID,
                    Images.Media.DATA,
                    Images.Media.SIZE,
                    Images.Media.MIME_TYPE
                )
            }
        }

    private fun getTotalCount(
        cr: ContentResolver,
        bucketId: String,
        columns: Array<String>,
        isDefaultAlbum: Boolean,
        isNeedGif: Boolean
    ): Int {
        var result = 0
        val allCursor: Cursor? = when {
            isDefaultAlbum -> cr.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                SELECTION_IMAGE_MIME_TYPE,
                SELECTION_ARGS_IMAGE_MIME_TYPE,
                Images.Media.DATE_MODIFIED + DESC
            )
            isNeedGif -> cr.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                SELECTION_ID,
                arrayOf(
                    bucketId,
                    IMAGE_JPEG,
                    IMAGE_PNG,
                    IMAGE_JPG,
                    IMAGE_GIF
                ),
                Images.Media.DATE_MODIFIED + DESC
            )
            else -> cr.query(
                Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                SELECTION_ID_WITHOUT_GIF,
                arrayOf(
                    bucketId,
                    IMAGE_JPEG,
                    IMAGE_PNG,
                    IMAGE_JPG
                ),
                Images.Media.DATE_MODIFIED + DESC
            )
        }
        allCursor?.use {
            result = it.count
        }
        return result
    }

    private fun clear() {
        mThumbnailMap.clear()
    }

    companion object {
        private const val CONJUNCTION_SQL = "=? or"
        private const val SELECTION_IMAGE_MIME_TYPE =
            "${Images.Media.MIME_TYPE}$CONJUNCTION_SQL ${Images.Media.MIME_TYPE}$CONJUNCTION_SQL ${Images.Media.MIME_TYPE}$CONJUNCTION_SQL ${Images.Media.MIME_TYPE}=?"
        private const val SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF =
            "${Images.Media.MIME_TYPE}$CONJUNCTION_SQL ${Images.Media.MIME_TYPE}$CONJUNCTION_SQL ${Images.Media.MIME_TYPE}=?"
        private const val SELECTION_ID =
            Images.Media.BUCKET_ID + "=? and (" + SELECTION_IMAGE_MIME_TYPE + " )"
        private const val SELECTION_ID_WITHOUT_GIF =
            Images.Media.BUCKET_ID + "=? and (" + SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF + " )"
        private const val IMAGE_JPEG = "image/jpeg"
        private const val IMAGE_PNG = "image/png"
        private const val IMAGE_JPG = "image/jpg"
        private const val IMAGE_GIF = "image/gif"
        private val SELECTION_ARGS_IMAGE_MIME_TYPE =
            arrayOf(
                IMAGE_JPEG,
                IMAGE_PNG,
                IMAGE_JPG,
                IMAGE_GIF
            )
        private val SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF =
            arrayOf(
                IMAGE_JPEG,
                IMAGE_PNG,
                IMAGE_JPG
            )
        private const val DESC = " desc"
    }
}