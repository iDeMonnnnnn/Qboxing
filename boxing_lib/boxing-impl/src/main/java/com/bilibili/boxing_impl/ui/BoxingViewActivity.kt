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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.bilibili.boxing.AbsBoxingViewActivity
import com.bilibili.boxing.Boxing
import com.bilibili.boxing.model.BoxingManager
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.model.task.IMediaTask
import com.bilibili.boxing.utils.asType
import com.bilibili.boxing.utils.asTypeNull
import com.bilibili.boxing.utils.toGone
import com.bilibili.boxing.utils.toVisible
import com.bilibili.boxing_impl.BoxingResHelper.mediaCheckedRes
import com.bilibili.boxing_impl.BoxingResHelper.mediaUncheckedRes
import com.bilibili.boxing_impl.R
import com.bilibili.boxing_impl.view.HackyViewPager
import kotlinx.android.synthetic.main.activity_boxing_view.*

/**
 * An Activity to show raw image by holding [BoxingViewFragment].
 *
 * @author ChenSL
 */
class BoxingViewActivity : AbsBoxingViewActivity() {
    lateinit var mGallery: HackyViewPager
    lateinit var mProgressBar: ProgressBar
    private var mNeedEdit = false
    private var mNeedLoading = false
    private var mFinishLoading = false
    private var mNeedAllCount = true
    private var mCurrentPage = 0
    private var mTotalCount = 0
    private var mStartPos = 0
    private var mPos = 0
    private var mMaxCount = 0
    private var mAlbumId: String = ""
    private lateinit var mAdapter: ImagesAdapter
    private var mCurrentImageItem: ImageMedia? = null
    private var mImages: MutableList<BaseMedia> = mutableListOf()
    private var mSelectedMenuItem: MenuItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boxing_view)
        createToolbar()
        initData()
        initView()
        startLoading()
    }

    private fun createToolbar() {
        setSupportActionBar(nav_top_bar)
        nav_top_bar.setNavigationOnClickListener { onBackPressed() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initData() {
        mAlbumId = albumId
        mStartPos = startPos
        mNeedLoading = BoxingManager.getInstance().boxingConfig.isNeedLoading
        mNeedEdit = BoxingManager.getInstance().boxingConfig.isNeedEdit
        mMaxCount = maxCount
        if (!mNeedLoading) {
            mImages.addAll(mSelectedImages)
        }
    }

    private fun initView() {
        mAdapter = ImagesAdapter(supportFragmentManager)
        mProgressBar = findViewById(R.id.loading)
        mGallery = findViewById(R.id.pager)
        mGallery.adapter = mAdapter
        mGallery.addOnPageChangeListener(OnPagerChangeListener())
        if (!mNeedEdit) {
            findViewById<View>(R.id.item_choose_layout).toGone
        } else {
            setOkTextNumber()
            image_items_ok.setOnClickListener { finishByBackPressed(false) }
        }
    }

    private fun setOkTextNumber() {
        if (mNeedEdit) {
            val selectedSize = mSelectedImages.size
            val size = selectedSize.coerceAtLeast(mMaxCount)
            image_items_ok.text = getString(
                R.string.boxing_image_preview_ok_fmt,
                selectedSize.toString(),
                size.toString()
            )
            image_items_ok.isEnabled = selectedSize > 0
        }
    }

    private fun finishByBackPressed(value: Boolean) {
        val intent = Intent()
        intent.putParcelableArrayListExtra(
            Boxing.EXTRA_SELECTED_MEDIA,
            mSelectedImages
        )
        intent.putExtra(
            EXTRA_TYPE_BACK,
            value
        )
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (mNeedEdit) {
            menuInflater.inflate(
                R.menu.activity_boxing_image_viewer,
                menu
            )
            mSelectedMenuItem =
                menu.findItem(R.id.menu_image_item_selected)
            setMenuIcon(mCurrentImageItem?.isSelected ?: false)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_image_item_selected) {
            val currentImageItem = mCurrentImageItem ?: return false
            if (mSelectedImages.size >= mMaxCount && !currentImageItem.isSelected) {
                val warning = getString(
                    R.string.boxing_max_image_over_fmt,
                    mMaxCount
                )
                Toast.makeText(this, warning, Toast.LENGTH_SHORT).show()
                return true
            }
            when {
                currentImageItem.isSelected -> {
                    cancelImage()
                }
                !mSelectedImages.contains(currentImageItem) -> {
                    if (currentImageItem.isGifOverSize) {
                        Toast.makeText(
                            applicationContext,
                            R.string.boxing_gif_too_big,
                            Toast.LENGTH_SHORT
                        ).show()
                        return true
                    }
                    currentImageItem.isSelected = true
                    mSelectedImages.add(currentImageItem)
                }
            }

            setOkTextNumber()
            setMenuIcon(currentImageItem.isSelected)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun cancelImage() {
        val currentImageItem = mCurrentImageItem ?: return
        if (mSelectedImages.contains(currentImageItem)) {
            mSelectedImages.remove(currentImageItem)
        }
        currentImageItem.isSelected = false
    }

    private fun setMenuIcon(isSelected: Boolean) {
        if (mNeedEdit) {
            mSelectedMenuItem?.setIcon(if (isSelected) mediaCheckedRes else mediaUncheckedRes)
        }
    }

    override fun startLoading() {
        if (!mNeedLoading) {
            mCurrentImageItem = mSelectedImages[mStartPos].asTypeNull<ImageMedia>()
            nav_top_bar.title = getString(
                R.string.boxing_image_preview_title_fmt,
                (mStartPos + 1).toString(),
                mSelectedImages.size.toString()
            )
            mProgressBar.toGone
            mGallery.toVisible
            mAdapter.setMedias(mImages)
            if (mStartPos > 0 && mStartPos < mSelectedImages.size) {
                mGallery.setCurrentItem(mStartPos, false)
            }
        } else {
            loadMedia(mAlbumId, mStartPos, mCurrentPage)
            mAdapter.setMedias(mImages)
        }
    }

    private fun loadMedia(albumId: String?, startPos: Int, page: Int) {
        mPos = startPos
        loadMedias(page, albumId)
    }

    override fun showMedia(
        medias: List<BaseMedia>?,
        allCount: Int
    ) {
        if (medias == null || allCount <= 0) {
            return
        }
        mImages.addAll(medias)
        mAdapter.notifyDataSetChanged()
        checkSelectedMedia(mImages, mSelectedImages)
        setupGallery()
        if (mNeedAllCount) {
            nav_top_bar.title = getString(
                R.string.boxing_image_preview_title_fmt,
                (++mPos).toString(),
                allCount.toString()
            )
            mNeedAllCount = false
        }
        loadOtherPagesInAlbum(allCount)
    }

    private fun setupGallery() {
        val startPos = mStartPos
        if (startPos < 0) {
            return
        }
        if (startPos < mImages.size && !mFinishLoading) {
            mGallery.setCurrentItem(mStartPos, false)
            mCurrentImageItem = mImages[startPos].asTypeNull<ImageMedia>()
            mProgressBar.toGone
            mGallery.toVisible
            mFinishLoading = true
            invalidateOptionsMenu()
        } else if (startPos >= mImages.size) {
            mProgressBar.toVisible
            mGallery.toGone
        }
    }

    private fun loadOtherPagesInAlbum(totalCount: Int) {
        mTotalCount = totalCount
        if (mCurrentPage <= mTotalCount / IMediaTask.PAGE_LIMIT) {
            mCurrentPage++
            loadMedia(mAlbumId, mStartPos, mCurrentPage)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(
            Boxing.EXTRA_SELECTED_MEDIA,
            mSelectedImages
        )
        outState.putString(Boxing.EXTRA_ALBUM_ID, mAlbumId)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        finishByBackPressed(true)
    }

    @Suppress("DEPRECATION")
    private inner class ImagesAdapter internal constructor(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        private var mMedias: MutableList<BaseMedia> = mutableListOf()
        override fun getItem(i: Int): Fragment {
            return BoxingRawImageFragment.newInstance(mMedias[i].asType<ImageMedia>())
        }

        override fun getCount(): Int {
            return mMedias.size
        }

        fun setMedias(medias: MutableList<BaseMedia>?) {
            mMedias = medias ?: mutableListOf()
            notifyDataSetChanged()
        }
    }

    private inner class OnPagerChangeListener : SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            if (position < mImages.size) {
                nav_top_bar.title = getString(
                    R.string.boxing_image_preview_title_fmt,
                    (position + 1).toString(),
                    if (mNeedLoading) mTotalCount.toString() else mImages.size.toString()
                )
                mCurrentImageItem = mImages[position].asTypeNull<ImageMedia>()
                invalidateOptionsMenu()
            }
        }
    }

    companion object {
        const val EXTRA_TYPE_BACK =
            "com.bilibili.boxing_impl.ui.BoxingViewActivity.type_back"
    }
}