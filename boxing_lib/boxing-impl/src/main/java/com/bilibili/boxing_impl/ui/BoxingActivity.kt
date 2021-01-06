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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.bilibili.boxing.AbsBoxingActivity
import com.bilibili.boxing.AbsBoxingViewFragment
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.utils.asType
import com.bilibili.boxing.utils.asTypeNull
import com.bilibili.boxing_impl.R
import kotlinx.android.synthetic.main.activity_boxing.*
import java.util.*

/**
 * Default UI Activity for simplest usage.
 * A simple subclass of [AbsBoxingActivity]. Holding a [AbsBoxingViewFragment] to display medias.
 */
class BoxingActivity : AbsBoxingActivity() {
    private var mPickerFragment: BoxingViewFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boxing)
        createToolbar()
        setTitleTxt(boxingConfig)
    }

    override fun onCreateBoxingView(medias: ArrayList<BaseMedia>?): AbsBoxingViewFragment {
        return supportFragmentManager
            .findFragmentByTag(BoxingViewFragment.TAG)
            .asTypeNull<BoxingViewFragment>()
            ?: BoxingViewFragment.newInstance()
                .setSelectedBundle(medias)
                .asType<BoxingViewFragment>()
                .also {
                    mPickerFragment = it
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_layout, it, BoxingViewFragment.TAG)
                        .commit()
                }
    }

    private fun createToolbar() {
        setSupportActionBar(nav_top_bar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        nav_top_bar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setTitleTxt(config: BoxingConfig) {
        if (config.mode === BoxingConfig.Mode.VIDEO) {
            pick_album_txt.setText(R.string.boxing_video_title)
            pick_album_txt.setCompoundDrawables(null, null, null, null)
            return
        }
        mPickerFragment?.setTitleTxt(pick_album_txt)
    }

    override fun onBoxingFinish(intent: Intent?, medias: List<BaseMedia>?) {
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}