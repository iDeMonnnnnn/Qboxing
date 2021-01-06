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
package com.bilibili.boxing.utils

import android.content.Context
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.utils.BoxingFileHelper.isFileValid
import com.bilibili.boxing.utils.BoxingLog.d
import java.io.File
import java.util.concurrent.Callable

/**
 * A compress task for [ImageMedia]
 * @author ChenSL
 */
object CompressTask {
    fun compress(
        context: Context,
        image: ImageMedia
    ): Boolean {
        return compress(
            ImageCompressor(context),
            image,
            ImageCompressor.MAX_LIMIT_SIZE_LONG
        )
    }

    /**
     * @param imageCompressor see [ImageCompressor].
     * @param maxSize the proximate max size for compression
     * @return may be a little bigger than expected for performance.
     */
    fun compress(
        imageCompressor: ImageCompressor?,
        image: ImageMedia?,
        maxSize: Long
    ): Boolean {
        if (imageCompressor == null || image == null || maxSize <= 0) {
            return false
        }
        val task = BoxingExecutor.getInstance().runWorker(Callable {
            val path = image.sandboxPath ?: ""
            val compressSaveFile = imageCompressor.getCompressOutFile(path)
            val needCompressFile = File(path)
            if (isFileValid(compressSaveFile)) {
                image.compressPath = compressSaveFile?.absolutePath ?: ""
                return@Callable true
            }
            if (!isFileValid(needCompressFile)) {
                return@Callable false
            } else if (image.size < maxSize) {
                image.compressPath = path
                return@Callable true
            } else {
                try {
                    val result =
                        imageCompressor.compress(needCompressFile, maxSize)
                    val suc =
                        isFileValid(result)
                    image.compressPath = if (suc) result.absolutePath else ""
                    return@Callable suc
                } catch (_: Exception) {
                    image.compressPath = ""
                    d("image compress fail!")
                }
            }
            false
        }) ?: return false
        return try {
            task.get()
        } catch (_: Exception) {
            false
        }
    }
}