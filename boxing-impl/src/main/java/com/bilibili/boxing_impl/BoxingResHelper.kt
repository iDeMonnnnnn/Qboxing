package com.bilibili.boxing_impl

import androidx.annotation.DrawableRes
import com.bilibili.boxing.model.BoxingManager

/**
 * Help getting the resource in config.
 *
 * @author ChenSL
 */
object BoxingResHelper {
    @JvmStatic
    @get:DrawableRes
    val mediaCheckedRes: Int
        get() {
            val result =
                BoxingManager.getInstance().boxingConfig.mediaCheckedRes
            return if (result > 0) result else R.drawable.ic_boxing_checked
        }

    @JvmStatic
    @get:DrawableRes
    val mediaUncheckedRes: Int
        get() {
            val result =
                BoxingManager.getInstance().boxingConfig.mediaUnCheckedRes
            return if (result > 0) result else R.drawable.shape_boxing_unchecked
        }

    @get:DrawableRes
    val cameraRes: Int
        get() = BoxingManager.getInstance().boxingConfig.cameraRes
}