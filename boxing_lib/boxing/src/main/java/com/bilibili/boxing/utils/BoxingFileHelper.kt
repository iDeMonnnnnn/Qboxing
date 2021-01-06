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

import android.app.Activity
import android.content.Context
import android.os.Environment
import java.io.File
import java.util.concurrent.ExecutionException

/**
 * A file helper to make thing easier.
 *
 * @author ChenSL
 */
object BoxingFileHelper {
    const val DEFAULT_SUB_DIR = "/boxing"

    @JvmStatic
    @Throws(ExecutionException::class, InterruptedException::class)
    fun createFile(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val file = File(path)
        return file.exists() || file.mkdirs()
    }

    @JvmStatic
    fun getCacheDir(_context: Context?): String? {
        val context = _context ?: return null
        val cacheDir = context.applicationContext.cacheDir
        if (cacheDir == null) {
            BoxingLog.d("cache dir do not exist.")
            return null
        }
        val result = "${cacheDir.absolutePath}/boxing"
        try {
            createFile(result)
        } catch (e: ExecutionException) {
            BoxingLog.d("cache dir $result not exist")
            return null
        } catch (e: InterruptedException) {
            BoxingLog.d("cache dir $result not exist")
            return null
        }
        BoxingLog.d("cache dir is: $result")
        return result
    }

    @JvmStatic
    fun getExternalDCIM(activity: Activity, subDir: String?): String? {
        val result = "${activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}$subDir"
        /*  activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
          if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
              val file =
                  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                      ?: return null
              var dir = "/bili/boxing"
              if (!subDir.isNullOrEmpty()) {
                  dir = subDir
              }
              val result = "${file.absolutePath}$dir"
              BoxingLog.d("external DCIM is: $result")
              return result
          }
          BoxingLog.d("external DCIM do not exist.")*/
        BoxingLog.d("external DCIM is: $result")
        return result
    }

    fun isFileValid(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val file = File(path)
        return isFileValid(file)
    }

    @JvmStatic
    fun isFileValid(file: File?): Boolean {
        return file != null && file.exists() && file.isFile && file.length() > 0 && file.canRead()
    }
}