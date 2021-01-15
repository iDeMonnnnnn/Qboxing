@file:JvmName("BoxingUCrop")

package com.demon.qboxing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import com.bilibili.boxing.loader.IBoxingCrop
import com.bilibili.boxing.model.config.BoxingCropOption
import com.bilibili.boxing.model.entity.BaseMedia

/**
 * @author DeMon
 * Created on 2021/1/14.
 * E-mail 757454343@qq.com
 * Desc: 使用系统原生裁剪
 * 系统裁剪在不同手机的表现都有差异，建议使用UCrop
 */
class BoxingSystemCrop : IBoxingCrop {
    var cropUri: Uri? = null
    override fun onStartCrop(
        context: Context,
        fragment: Fragment,
        cropConfig: BoxingCropOption,
        media: BaseMedia,
        requestCode: Int
    ) {
        cropUri = cropConfig.destination
        media.uri?.run {
            val intentCrop = Intent("com.android.camera.action.CROP")
            intentCrop.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intentCrop.setDataAndType(this, "image/*")
            //下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
            intentCrop.putExtra("crop", "true")
            //裁剪时是否保留图片的比例
            intentCrop.putExtra("scale", true)
            if (!cropConfig.isFreeStyle) {
                // aspectX aspectY 是宽高的比例
                intentCrop.putExtra("aspectX", cropConfig.maxWidth)
                intentCrop.putExtra("aspectY", cropConfig.maxHeight)
                // outputX outputY 是裁剪图片宽高
                intentCrop.putExtra("outputX", cropConfig.maxWidth)
                intentCrop.putExtra("outputY", cropConfig.maxHeight)
            }
            //是否将数据保留在Bitmap中返回
            //注意：这里设置为true的话，而不设置outputX&outputY，华为手机无法正常自由裁剪
            intentCrop.putExtra("return-data", false)
            //关闭人脸识别
            intentCrop.putExtra("noFaceDetection", true)
            //设置输出的格式
            intentCrop.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString())
            intentCrop.putExtra(MediaStore.EXTRA_OUTPUT, cropConfig.destination)
            fragment.startActivityForResult(intentCrop, requestCode)
        }
    }

    override fun onCropFinish(
        resultCode: Int,
        data: Intent?
    ): Uri? = cropUri
}