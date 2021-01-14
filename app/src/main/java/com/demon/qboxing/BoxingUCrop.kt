@file:JvmName("BoxingUCrop")

package com.demon.qboxing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bilibili.boxing.loader.IBoxingCrop
import com.bilibili.boxing.model.config.BoxingCropOption
import com.bilibili.boxing.model.entity.BaseMedia
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options

/**
 * use Ucrop(https://github.com/Yalantis/uCrop) as the implement for [IBoxingCrop]
 * 注意：Ucrop已适配AndroidX和AndroidQ可以直接使用
 * @author ChenSL
 */
class BoxingUCrop : IBoxingCrop {
    override fun onStartCrop(
        context: Context,
        fragment: Fragment,
        cropConfig: BoxingCropOption,
        media: BaseMedia,
        requestCode: Int
    ) {
        media.uri?.run {
            val crop = Options()
            // do not copy exif information to crop pictures
            // because png do not have exif and png is not Distinguishable
            crop.setCompressionFormat(CompressFormat.PNG)
            crop.setHideBottomControls(true)
            crop.setFreeStyleCropEnabled(cropConfig.isFreeStyle)
            crop.withMaxResultSize(cropConfig.maxWidth, cropConfig.maxHeight)
            crop.withAspectRatio(cropConfig.aspectRatioX, cropConfig.aspectRatioY)
            crop.setStatusBarColor(ActivityCompat.getColor(context, R.color.teal_200))
            crop.setToolbarColor(ActivityCompat.getColor(context, R.color.teal_200))
            crop.setShowCropGrid(false)
            cropConfig.destination?.let {
                UCrop.of(this, it)
                    .withOptions(crop)
                    .start(context, fragment, requestCode)
            }
        }
    }

    override fun onCropFinish(
        resultCode: Int,
        data: Intent?
    ): Uri? {
        data ?: return null
        val throwable = UCrop.getError(data)
        return if (throwable != null) {
            null
        } else UCrop.getOutput(data)
    }
}