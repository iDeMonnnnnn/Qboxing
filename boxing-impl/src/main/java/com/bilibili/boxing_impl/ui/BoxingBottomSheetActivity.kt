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
package com.bilibili.boxing_impl.ui

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import com.bilibili.boxing.AbsBoxingActivity
import com.bilibili.boxing.AbsBoxingViewFragment
import com.bilibili.boxing.BoxingMediaLoader
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.utils.asType
import com.bilibili.boxing.utils.asTypeNull
import com.bilibili.boxing_impl.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*

/**
 * Default UI Activity for simplest usage, containing layout achieve [BottomSheetBehavior].
 * Only support SINGLE_IMG and VIDEO Mode.
 *
 * @author ChenSL
 */
class BoxingBottomSheetActivity : AbsBoxingActivity() {
    private var mBehavior: BottomSheetBehavior<FrameLayout>? = null
    private lateinit var nav_top_bar: Toolbar
    private lateinit var content_layout: FrameLayout
    private lateinit var media_result: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boxing_bottom_sheet)
        nav_top_bar = findViewById(R.id.nav_top_bar)
        content_layout = findViewById(R.id.content_layout)
        media_result = findViewById(R.id.media_result)
        createToolbar()
        mBehavior = BottomSheetBehavior.from(content_layout)
        mBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        media_result.setOnClickListener { toggleBottomSheet() }
    }

    override fun onCreateBoxingView(medias: ArrayList<BaseMedia>?): AbsBoxingViewFragment {
        return supportFragmentManager
            .findFragmentByTag(BoxingBottomSheetFragment.TAG)
            .asTypeNull<BoxingBottomSheetFragment>()
            ?: BoxingBottomSheetFragment.newInstance()
                .also {
                    supportFragmentManager
                        .beginTransaction()
                        .add(R.id.content_layout, it, BoxingBottomSheetFragment.TAG)
                        .commit()
                }
    }

    private fun createToolbar() {
        setSupportActionBar(nav_top_bar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.boxing_default_album)
        nav_top_bar?.setNavigationOnClickListener { onBackPressed() }
    }

    private fun hideBottomSheet(): Boolean {
        if (mBehavior != null && mBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
            mBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            return true
        }
        return false
    }

    @Suppress("UNUSED")
    private fun collapseBottomSheet(): Boolean {
        if (mBehavior != null && mBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            mBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
    }

    private fun toggleBottomSheet() {
        mBehavior?.state = when (mBehavior?.state) {
            BottomSheetBehavior.STATE_HIDDEN -> {
                BottomSheetBehavior.STATE_COLLAPSED
            }
            else -> {
                BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    override fun onBackPressed() {
        if (hideBottomSheet()) {
            return
        }
        super.onBackPressed()
    }

    override fun onBoxingFinish(
        intent: Intent?,
        medias: List<BaseMedia>?
    ) {
        if (medias != null && medias.isNotEmpty()) {
            val imageMedia = medias[0].asType<ImageMedia>()
            BoxingMediaLoader.getInstance()
                .displayRaw(media_result, imageMedia.uri, 1080, 720, null)
        }
        hideBottomSheet()
    }
}