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
@file:Suppress("DEPRECATION")

package com.bilibili.boxing.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bilibili.boxing.utils.BoxingExifHelper.getRotateDegree
import com.bilibili.boxing.utils.BoxingFileHelper.createFile
import com.bilibili.boxing.utils.BoxingFileHelper.getExternalDCIM
import com.bilibili.boxing.utils.BoxingLog.d
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

/**
 * A helper to start camera.<br></br>
 * used by [com.bilibili.boxing.AbsBoxingViewFragment]
 *
 * @author ChenSL
 */
class CameraPickerHelper(savedInstance: Bundle?) {
    var sourceFilePath: String = ""
    private var mOutputFile: File? = null
    private var mCallback: Callback? = null

    interface Callback {

        fun onFinish(helper: CameraPickerHelper)
        fun onError(helper: CameraPickerHelper)
    }

    init {
        val state: SavedState? = savedInstance?.getParcelable(STATE_SAVED_KEY)
        mOutputFile = state?.mOutputFile
        sourceFilePath = state?.mSourceFilePath ?: ""
    }

    fun setPickCallback(callback: Callback?) {
        mCallback = callback
    }

    fun onSaveInstanceState(out: Bundle) {
        val state = SavedState()
        state.mOutputFile = mOutputFile
        state.mSourceFilePath = sourceFilePath
        out.putParcelable(STATE_SAVED_KEY, state)
    }

    /**
     * start system camera to take a picture
     *
     * @param activity      not null if fragment is null.
     * @param fragment      not null if activity is null.
     * @param subFolderPath a folder in external DCIM,must start with "/".
     */
    fun startCamera(
        activity: Activity,
        fragment: Fragment?,
        subFolderPath: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || !takePhotoSecure(activity, fragment, subFolderPath)
        ) {
            val task = BoxingExecutor.getInstance()
                .runWorker(Callable {
                    try { // try...try...try
                        val camera = Camera.open()
                        camera.release()
                    } catch (e: Exception) {
                        d("camera is not available.")
                        return@Callable false
                    }
                    true
                }) ?: let {
                callbackError()
                return
            }
            try {
                if (task.get()) {
                    startCameraIntent(
                        activity,
                        fragment,
                        subFolderPath,
                        MediaStore.ACTION_IMAGE_CAPTURE,
                        REQ_CODE_CAMERA
                    )
                } else {
                    callbackError()
                }
            } catch (_: Exception) {
                callbackError()
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun takePhotoSecure(
        activity: Activity,
        fragment: Fragment?,
        subDir: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                startCameraIntent(
                    activity,
                    fragment,
                    subDir,
                    MediaStore.ACTION_IMAGE_CAPTURE,
                    REQ_CODE_CAMERA
                )
                true
            } catch (_: Exception) {
                false
            }
        } else false
    }

    private fun callbackFinish() {
        mCallback?.onFinish(this@CameraPickerHelper)
    }

    private fun callbackError() {
        mCallback?.onError(this@CameraPickerHelper)
    }

    @Throws(ActivityNotFoundException::class)
    private fun startActivityForResult(
        activity: Activity,
        fragment: Fragment?,
        intent: Intent,
        reqCodeCamera: Int
    ) {
        if (fragment == null) {
            activity.startActivityForResult(intent, reqCodeCamera)
        } else {
            fragment.startActivityForResult(intent, reqCodeCamera)
        }
    }

    private fun startCameraIntent(
        activity: Activity,
        fragment: Fragment?,
        subFolder: String,
        action: String,
        requestCode: Int
    ) {
        val cameraOutDir = getExternalDCIM(activity,subFolder)
        try {
            if (createFile(cameraOutDir)) {
                val mOutputFile = File(cameraOutDir, "${System.currentTimeMillis()}.jpg").also { mOutputFile = it }
                sourceFilePath = mOutputFile.path
                val intent = Intent(action)
                val uri = getFileUri(activity.applicationContext, mOutputFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    startActivityForResult(activity, fragment, intent, requestCode)
                } catch (ignore: ActivityNotFoundException) {
                    callbackError()
                }
            }
        } catch (e: ExecutionException) {
            d("create file$cameraOutDir error.")
        } catch (e: InterruptedException) {
            d("create file$cameraOutDir error.")
        }
    }

    private fun getFileUri(context: Context, file: File): Uri {
        val outputFile = mOutputFile
        return when {
            outputFile == null -> {
                Uri.fromFile(file)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                FileProvider.getUriForFile(context, "${context.applicationContext.packageName}.fileProvider", outputFile)
            }
            else -> {
                Uri.fromFile(file)
            }
        }
    }

    /**
     * deal with the system camera's shot.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQ_CODE_CAMERA) {
            return false
        }
        if (resultCode != Activity.RESULT_OK) {
            callbackError()
            return false
        }
        val task = BoxingExecutor.getInstance().runWorker(Callable { rotateImage(resultCode) })
                ?: let {
                    callbackError()
                    return false
                }
        try {
            if (task.get()) {
                callbackFinish()
            } else {
                callbackError()
            }
        } catch (_: Exception) {
            callbackError()
        }
        return true
    }

    @Throws(IOException::class)
    private fun rotateSourceFile(file: File?): Boolean {
        if (file == null || !file.exists()) {
            return false
        }
        var outputStream: FileOutputStream? = null
        var bitmap: Bitmap? = null
        var outBitmap: Bitmap? = null
        return try {
            val degree = getRotateDegree(file.absolutePath)
            if (degree == 0) {
                return true
            }
            val quality =
                if (file.length() >= MAX_CAMER_PHOTO_SIZE) 90 else 100
            val matrix = Matrix()
            matrix.postRotate(degree.toFloat())
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            val bitmapTemp =
                BitmapFactory.decodeFile(file.absolutePath, options).also { bitmap = it }
            outBitmap = Bitmap.createBitmap(
                bitmapTemp,
                0,
                0,
                bitmapTemp.width,
                bitmapTemp.height,
                matrix,
                false
            )
            outputStream = FileOutputStream(file)
            outBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            true
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    d("IOException when output stream closing!")
                }
            }
            bitmap?.recycle()
            outBitmap?.recycle()
        }
    }

    @Throws(IOException::class)
    private fun rotateImage(resultCode: Int): Boolean {
        return resultCode == Activity.RESULT_OK && rotateSourceFile(mOutputFile)
    }

    fun release() {
        mOutputFile = null
    }

    private class SavedState : Parcelable {
        var mOutputFile: File? = null
        var mSourceFilePath: String = ""

        internal constructor()

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeSerializable(mOutputFile)
            dest.writeString(mSourceFilePath)
        }

        internal constructor(`in`: Parcel) {
            mOutputFile = `in`.readSerializable().asType<File>()
            mSourceFilePath = `in`.readString() ?: ""
        }

        companion object {
            @Suppress("UNUSED")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object : Parcelable.Creator<SavedState> {
                    override fun createFromParcel(source: Parcel): SavedState? {
                        return SavedState(source)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(
                            size
                        )
                    }
                }
        }
    }

    companion object {
        private const val MAX_CAMER_PHOTO_SIZE = 4 * 1024 * 1024
        const val REQ_CODE_CAMERA = 0x2001
        private const val STATE_SAVED_KEY =
            "com.bilibili.boxing.utils.CameraPickerHelper.saved_state"
    }
}