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
package com.bilibili.boxing_impl.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bilibili.boxing.utils.asType
import com.bilibili.boxing.utils.asTypeNull

/**
 * @author ChenSL
 */
class SpacesItemDecoration @JvmOverloads constructor(
    private val mSpace: Int,
    private val mSpanCount: Int = 1
) : ItemDecoration() {
    private val mRadixX: Int = mSpace / mSpanCount
    private var mItemCountInLastLine = 0
    private var mOldItemCount = -1

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val params = view.layoutParams.asType<RecyclerView.LayoutParams>()
        val sumCount = state.itemCount
        val position = params.viewLayoutPosition
        val spanSize: Int
        val index: Int
        when (params) {
            is GridLayoutManager.LayoutParams -> {
                spanSize = params.spanSize
                index = params.spanIndex
                if ((position == 0 || mOldItemCount != sumCount) && mSpanCount > 1) {
                    var countInLine = 0
                    var spanIndex: Int
                    for (tempPosition in sumCount - mSpanCount until sumCount) {
                        spanIndex = parent.layoutManager
                            .asTypeNull<GridLayoutManager>()
                            ?.spanSizeLookup
                            ?.getSpanIndex(tempPosition, mSpanCount)
                            ?: 0
                        countInLine = if (spanIndex == 0) 1 else countInLine + 1
                    }
                    mItemCountInLastLine = countInLine
                    if (mOldItemCount != sumCount) {
                        mOldItemCount = sumCount
                        if (position != 0) {
                            parent.post { parent.invalidateItemDecorations() }
                        }
                    }
                }
            }
            is StaggeredGridLayoutManager.LayoutParams -> {
                spanSize = if (params.isFullSpan) mSpanCount else 1
                index = params.spanIndex
            }
            else -> {
                spanSize = 1
                index = 0
            }
        }
        if (spanSize < 1 || index < 0 || spanSize > mSpanCount) {
            return
        }
        outRect.left = mSpace - mRadixX * index
        outRect.right = mRadixX + mRadixX * (index + spanSize - 1)
        when {
            mSpanCount == 1 && position == sumCount - 1 -> {
                outRect.bottom = mSpace
            }
            position >= sumCount - mItemCountInLastLine && position < sumCount -> {
                outRect.bottom = mSpace
            }
        }
        outRect.top = mSpace
    }

}