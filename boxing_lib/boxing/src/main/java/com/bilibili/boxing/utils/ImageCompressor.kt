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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.bilibili.boxing.utils.BoxingExifHelper.getRotateDegree
import com.bilibili.boxing.utils.BoxingLog.d
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and
import kotlin.math.ceil

/**
 * A compress for image.
 *
 * @author ChenSL
 */
class ImageCompressor {
    private var mOutFileFile: File? = null

    constructor(cachedRootDir: File) {
        mOutFileFile = File("${cachedRootDir.absolutePath}${File.separator}.compress${File.separator}")
    }

    constructor(context: Context) {
        val rootDir = BoxingFileHelper.getCacheDir(context)
        check(!TextUtils.isEmpty(rootDir)) { "the cache dir is null" }
        mOutFileFile = File("$rootDir${File.separator}.compress${File.separator}")
    }

    /**
     * @param file file to compress.
     * @param maxsize the proximate max size for compression, not for the image with large ratio.
     * @return may be a little bigger than expected for performance.
     */
    @WorkerThread
    @JvmOverloads
    @Throws(
        IOException::class,
        NullPointerException::class,
        IllegalArgumentException::class
    )
    fun compress(
        file: File,
        maxsize: Long = MAX_LIMIT_SIZE
    ): File {
        require(file.exists()) { "file not found : ${file.absolutePath}" }
        require(isLegalFile(file)) { "file is not a legal file : ${file.absolutePath}" }
        mOutFileFile ?: throw NullPointerException("the external cache dir is null")
        val checkOptions = BitmapFactory.Options()
        checkOptions.inJustDecodeBounds = true
        val absPath = file.absolutePath
        val angle = getRotateDegree(absPath)
        BitmapFactory.decodeFile(absPath, checkOptions)
        require(!(checkOptions.outWidth <= 0 || checkOptions.outHeight <= 0)) { "file is not a legal bitmap with 0 with or 0 height : " + file.absolutePath }
        val width = checkOptions.outWidth
        val height = checkOptions.outHeight
        val outFile = createCompressFile(file)
            ?: throw NullPointerException("the compressed file create fail, the compressed path is null.")
        if (!isLargeRatio(width, height)) {
            val display = getCompressDisplay(width, height)
            val bitmap = compressDisplay(absPath, display[0], display[1])
            val rotatedBitmap = rotatingImage(angle, bitmap)
            if (bitmap != rotatedBitmap) {
                bitmap.recycle()
            }
            saveBitmap(rotatedBitmap, outFile)
            rotatedBitmap.recycle()
            compressQuality(outFile, maxsize, 20)
        } else {
            if (checkOptions.outHeight >= MAX_HEIGHT && checkOptions.outWidth >= MAX_WIDTH) {
                checkOptions.inSampleSize = 2
            }
            checkOptions.inJustDecodeBounds = false
            val originBitmap = BitmapFactory.decodeFile(absPath, checkOptions)
            val rotatedBitmap = rotatingImage(angle, originBitmap)
            if (originBitmap != rotatedBitmap) {
                originBitmap.recycle()
            }
            saveBitmap(originBitmap, outFile)
            rotatedBitmap.recycle()
            compressQuality(
                outFile,
                MAX_LIMIT_SIZE_LONG,
                50
            )
        }
        d("compress suc: " + outFile.absolutePath)
        return outFile
    }

    private fun rotatingImage(angle: Int, bitmap: Bitmap): Bitmap {
        if (angle == 0) {
            return bitmap
        }
        //rotate image
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        //create a new image
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    @Throws(IOException::class)
    private fun saveBitmap(bitmap: Bitmap, outFile: File) {
        val fos = FileOutputStream(outFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        try {
            fos.flush()
        } finally {
            try {
                fos.close()
            } catch (e: IOException) {
                d("IOException when saving a bitmap")
            }
        }
    }

    /**
     * @param _width  must > 0
     * @param _height must > 0
     */
    private fun getCompressDisplay(_width: Int, _height: Int): IntArray {
        var width = _width
        var height = _height
        var thumbWidth = if (width % 2 == 1) width + 1 else width
        var thumbHeight = if (height % 2 == 1) height + 1 else height
        val results = intArrayOf(thumbWidth, thumbHeight)
        width = if (thumbWidth > thumbHeight) thumbHeight else thumbWidth
        height = if (thumbWidth > thumbHeight) thumbWidth else thumbHeight
        val scale = width.toFloat() / height
        when {
            scale <= 1 && scale >= 0.5625 -> {
                when {
                    height < 1664 -> {
                        thumbWidth = width
                        thumbHeight = height
                    }
                    height >= 1664 && height < 4990 -> {
                        thumbWidth = width / 2
                        thumbHeight = height / 2
                    }
                    height >= 4990 && height < 10240 -> {
                        thumbWidth = width / 4
                        thumbHeight = height / 4
                    }
                    else -> {
                        val multiple = if (height / 1280 == 0) 1 else height / 1280
                        thumbWidth = width / multiple
                        thumbHeight = height / multiple
                    }
                }
            }
            scale <= 0.5625 && scale > 0.5 -> {
                when {
                    height < 1280 -> {
                        thumbWidth = width
                        thumbHeight = height
                    }
                    else -> {
                        val multiple = if (height / 1280 == 0) 1 else height / 1280
                        thumbWidth = width / multiple
                        thumbHeight = height / multiple
                    }
                }
            }
            else -> {
                val multiple = Math.ceil(height / (1280.0 / scale)).toInt()
                thumbWidth = width / multiple
                thumbHeight = height / multiple
            }
        }
        results[0] = thumbWidth
        results[1] = thumbHeight
        return results
    }

    /**
     * @param width  must > 0
     * @param height must > 0
     */
    private fun compressDisplay(imagePath: String, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        val outH = options.outHeight
        val outW = options.outWidth
        var inSampleSize = 1
        if (outH > height || outW > width) {
            val halfH = outH / 2
            val halfW = outW / 2
            while (halfH / inSampleSize > height && halfW / inSampleSize > width) {
                inSampleSize *= 2
            }
        }
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        val heightRatio = ceil((options.outHeight / height).toDouble()).toInt()
        val widthRatio = ceil((options.outWidth / width).toDouble()).toInt()
        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio > widthRatio) {
                options.inSampleSize = heightRatio
            } else {
                options.inSampleSize = widthRatio
            }
        }
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imagePath, options)
    }

    @Throws(IOException::class)
    private fun compressQuality(
        outFile: File,
        maxSize: Long,
        maxQuality: Int
    ) {
        val length = outFile.length()
        var quality = 90
        if (length > maxSize) {
            val bos = ByteArrayOutputStream()
            d("source file size : ${outFile.length()},path : $outFile")
            while (true) {
                compressPhotoByQuality(outFile, bos, quality)
                val size = bos.size().toLong()
                d("compressed file size : $size")
                if (quality <= maxQuality) {
                    break
                }
                if (size < maxSize) {
                    break
                } else {
                    quality -= 10
                    bos.reset()
                }
            }
            val fos: OutputStream = FileOutputStream(outFile)
            bos.writeTo(fos)
            bos.flush()
            fos.close()
            bos.close()
        }
    }

    @Throws(IOException::class, OutOfMemoryError::class)
    private fun compressPhotoByQuality(
        file: File,
        os: OutputStream,
        quality: Int
    ) {
        d("start compress quality... ")
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)
            bitmap.recycle()
        } else {
            throw NullPointerException("bitmap is null when compress by quality")
        }
    }

    @Throws(IOException::class)
    private fun createCompressFile(file: File): File? {
        val outFile = getCompressOutFile(file)
        mOutFileFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        d("compress out file : $outFile")
        outFile?.createNewFile()
        return outFile
    }

    fun getCompressOutFile(file: File): File? {
        val path = getCompressOutFilePath(file)
        return if (TextUtils.isEmpty(path)) null else File(path)
    }

    fun getCompressOutFile(filePth: String): File? {
        val path = getCompressOutFilePath(filePth)
        return if (TextUtils.isEmpty(path)) null else File(path)
    }

    fun getCompressOutFilePath(file: File): String? {
        return getCompressOutFilePath(file.absolutePath)
    }

    fun getCompressOutFilePath(filePath: String): String? {
        return try {
            "${mOutFileFile.toString()}${File.separator}$COMPRESS_FILE_PREFIX${signMD5(
                filePath.toByteArray(charset("UTF-8")))}.jpg"
        } catch (e: UnsupportedEncodingException) {
            null
        }
    }

    fun signMD5(source: ByteArray): String? {
        try {
            val digest = MessageDigest.getInstance("MD5")
            return signDigest(source, digest)
        } catch (e: NoSuchAlgorithmException) {
            d("have no md5")
        }
        return null
    }

    private fun signDigest(
        source: ByteArray,
        digest: MessageDigest
    ): String {
        digest.update(source)
        val data = digest.digest()
        val j = data.size
        val str = CharArray(j * 2)
        var k = 0
        for (byte0 in data) {
            str[k++] = HEX_DIGITS[(byte0.toInt() ushr 4) and 0xf]
            str[k++] = HEX_DIGITS[(byte0 and 0xf).toInt()]
        }
        return String(str).toLowerCase(Locale.getDefault())
    }

    private fun isLargeRatio(width: Int, height: Int): Boolean {
        return width / height >= 3 || height / width >= 3
    }

    private fun isLegalFile(file: File?): Boolean {
        return file != null && file.exists() && file.isFile && file.length() > 0
    }

    companion object {
        const val MAX_LIMIT_SIZE_LONG = 1024 * 1024L
        private val HEX_DIGITS = charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F'
        )
        private const val MAX_WIDTH = 3024
        private const val MAX_HEIGHT = 4032
        private const val MAX_LIMIT_SIZE = 300 * 1024L
        private const val COMPRESS_FILE_PREFIX = "compress-"
    }
}