@file:JvmName("BoxingUCrop")

package com.demon.qxing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bilibili.boxing.loader.IBoxingCrop
import com.bilibili.boxing.model.config.BoxingCropOption
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.utils.BoxingFileHelper
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options

/**
 * use Ucrop(https://github.com/Yalantis/uCrop) as the implement for [IBoxingCrop]
 *
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
        val ucropUri = BoxingFileHelper.getFileUri(context, media)
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
            UCrop.of(ucropUri, it)
                .withOptions(crop)
                .start(context, fragment, requestCode)
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