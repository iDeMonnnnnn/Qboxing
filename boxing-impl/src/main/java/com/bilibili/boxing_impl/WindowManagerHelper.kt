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
package com.bilibili.boxing_impl

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import com.bilibili.boxing.utils.asTypeNull

/**
 * @author ChenSL
 */
object WindowManagerHelper {
    private fun getWindowManager(context: Context) =
        context.getSystemService(Context.WINDOW_SERVICE).asTypeNull<WindowManager>()

    private fun getDefaultDisplay(context: Context): Display? {
        return getWindowManager(context)?.defaultDisplay
    }

    @JvmStatic
    fun getScreenHeight(context: Context): Int {
        return getDisplayMetrics(context)?.heightPixels ?: 0
    }

    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        return getDisplayMetrics(context)?.widthPixels ?: 0
    }

    private fun getDisplayMetrics(context: Context): DisplayMetrics? {
        val display = getDefaultDisplay(context) ?: return null
        val result = DisplayMetrics()
        display.getMetrics(result)
        return result
    }

    @JvmStatic
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun getToolbarHeight(context: Context): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
            TypedValue.complexToDimensionPixelSize(
                tv.data,
                context.resources.displayMetrics
            )
        } else 0
    }
}