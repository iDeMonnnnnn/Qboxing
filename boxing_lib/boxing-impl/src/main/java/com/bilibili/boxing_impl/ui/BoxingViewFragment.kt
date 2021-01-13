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

package com.bilibili.boxing_impl.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bilibili.boxing.AbsBoxingViewFragment
import com.bilibili.boxing.Boxing
import com.bilibili.boxing.Boxing.Companion.get
import com.bilibili.boxing.model.BoxingManager.Companion.getInstance
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.entity.AlbumEntity
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.utils.*
import com.bilibili.boxing_impl.R
import com.bilibili.boxing_impl.WindowManagerHelper.getScreenHeight
import com.bilibili.boxing_impl.WindowManagerHelper.getStatusBarHeight
import com.bilibili.boxing_impl.WindowManagerHelper.getToolbarHeight
import com.bilibili.boxing_impl.adapter.BoxingAlbumAdapter
import com.bilibili.boxing_impl.adapter.BoxingMediaAdapter
import com.bilibili.boxing_impl.view.HackyGridLayoutManager
import com.bilibili.boxing_impl.view.MediaItemLayout
import com.bilibili.boxing_impl.view.SpacesItemDecoration
import java.util.*

/**
 * A full implement for [com.bilibili.boxing.presenter.PickerContract.View] supporting all the mode
 * in [BoxingConfig.Mode].
 * use this to pick the picture.
 *
 * @author ChenSL
 */
class BoxingViewFragment : AbsBoxingViewFragment(),
    View.OnClickListener {
    private var mIsPreview = false
    private var mIsCamera = false
    lateinit var mediaAdapter: BoxingMediaAdapter
        private set
    private lateinit var mAlbumWindowAdapter: BoxingAlbumAdapter
    private var mDialog: ProgressDialog? = null
    private var mTitleTxt: TextView? = null
    private var mAlbumPopWindow: PopupWindow? = null
    private var mMaxCount = 0

    override fun onCreateWithSelectedMedias(
        bundle: Bundle?,
        selectedMedias: MutableList<BaseMedia>?
    ) {
        mAlbumWindowAdapter = BoxingAlbumAdapter(requireContext())
        mediaAdapter = BoxingMediaAdapter(requireContext())
        selectedMedias?.let { mediaAdapter.selectedMedias = it }
        mMaxCount = maxCount
    }

    override fun startLoading() {
        loadMedias()
        loadAlbum()
    }

    override fun onRequestPermissionError(
        permissions: Array<String>,
        e: Exception?
    ) {
        if (permissions.isEmpty()) return
        when {
            permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                Toast.makeText(
                    context,
                    R.string.boxing_storage_permission_deny,
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyData()
            }
            permissions[0] == Manifest.permission.CAMERA -> {
                Toast.makeText(
                    context,
                    R.string.boxing_camera_permission_deny,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionSuc(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when {
            permissions[0] == STORAGE_PERMISSIONS[0] -> {
                startLoading()
            }
            permissions[0] == CAMERA_PERMISSIONS[0] -> {
                startCamera(requireActivity(), this, null)
            }
        }
    }

    private lateinit var rootView: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(
            R.layout.fragmant_boxing_view,
            container,
            false
        )
        return rootView
    }

    private lateinit var media_recycleview: RecyclerView
    private lateinit var choose_preview_btn: Button
    private lateinit var choose_ok_btn: Button
    private lateinit var empty_txt: TextView
    private lateinit var loading: ProgressBar
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        media_recycleview = rootView.findViewById(R.id.media_recycleview)
        choose_preview_btn = rootView.findViewById(R.id.choose_preview_btn)
        choose_ok_btn = rootView.findViewById(R.id.choose_ok_btn)
        empty_txt = rootView.findViewById(R.id.empty_txt)
        loading = rootView.findViewById(R.id.loading)

        initViews(view)
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initViews(view: View) {
        media_recycleview.setHasFixedSize(true)
        initRecycleView()
        val isMultiImageMode =
            getInstance().boxingConfig.isMultiImageMode
        val multiImageLayout =
            view.findViewById<View>(R.id.multi_picker_layout)
        multiImageLayout.visibility = if (isMultiImageMode) View.VISIBLE else View.GONE
        if (isMultiImageMode) {
            choose_preview_btn.setOnClickListener(this)
            choose_ok_btn.setOnClickListener(this)
            updateMultiPickerLayoutState(mediaAdapter.selectedMedias)
        }
    }

    private fun initRecycleView() {
        val gridLayoutManager: GridLayoutManager = HackyGridLayoutManager(activity, GRID_COUNT)
        gridLayoutManager.isSmoothScrollbarEnabled = true
        media_recycleview.layoutManager = gridLayoutManager
        media_recycleview.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelOffset(R.dimen.boxing_media_margin), GRID_COUNT))
        mediaAdapter.setOnCameraClickListener(OnCameraClickListener())
        mediaAdapter.setOnCheckedListener(OnMediaCheckedListener())
        mediaAdapter.setOnMediaClickListener(OnMediaClickListener())
        media_recycleview.adapter = mediaAdapter
        media_recycleview.addOnScrollListener(ScrollListener())
    }

    override fun showMedia(
        medias: List<BaseMedia>?,
        allCount: Int
    ) {
        if (medias == null || isEmptyData(medias)
            && isEmptyData(mediaAdapter.allMedias)
        ) {
            showEmptyData()
            return
        }
        showData()
        mediaAdapter.addAllData(medias)
        checkSelectedMedia(medias, mediaAdapter.selectedMedias)
    }

    private fun isEmptyData(medias: List<BaseMedia>): Boolean {
        return medias.isEmpty() && !getInstance().boxingConfig.isNeedCamera
    }

    private fun showEmptyData() {
        loading.toGone
        empty_txt.toVisible
        media_recycleview.toGone
    }

    private fun showData() {
        loading.toGone
        empty_txt.toGone
        media_recycleview.toVisible
    }

    override fun showAlbum(albums: List<AlbumEntity>?) {
        if (albums.isNullOrEmpty()) {
            mTitleTxt?.setCompoundDrawables(null, null, null, null)
            mTitleTxt?.setOnClickListener(null)
            return
        }
        mAlbumWindowAdapter.addAllData(albums)
    }

    override fun clearMedia() {
        mediaAdapter.clearData()
    }

    private fun updateMultiPickerLayoutState(medias: List<BaseMedia>) {
        updateOkBtnState(medias)
        updatePreviewBtnState(medias)
    }

    private fun updatePreviewBtnState(medias: List<BaseMedia>?) {
        medias ?: return
        val enabled = medias.size > 0 && medias.size <= mMaxCount
        choose_preview_btn.isEnabled = enabled
    }

    private fun updateOkBtnState(medias: List<BaseMedia>?) {
        if (medias == null) {
            return
        }
        val enabled = medias.size > 0 && medias.size <= mMaxCount
        choose_ok_btn.isEnabled = enabled
        choose_ok_btn.text = if (enabled) getString(
            R.string.boxing_image_select_ok_fmt,
            medias.size.toString(),
            mMaxCount.toString()
        ) else getString(R.string.boxing_ok)
    }

    override fun onCameraFinish(media: BaseMedia) {
        dismissProgressDialog()
        mIsCamera = false
        if (hasCropBehavior()) {
            startCrop(media, IMAGE_CROP_REQUEST_CODE)
        } else {
            val selectedMedias = mediaAdapter.selectedMedias
            selectedMedias.add(media)
            onFinish(selectedMedias)
        }
    }

    override fun onCameraError() {
        mIsCamera = false
        dismissProgressDialog()
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.choose_ok_btn) {
            onFinish(mediaAdapter.selectedMedias)
        } else if (id == R.id.choose_preview_btn) {
            if (!mIsPreview) {
                mIsPreview = true
                val medias = mediaAdapter.selectedMedias.asType<ArrayList<BaseMedia>>()
                get().withIntent(
                    requireActivity(),
                    BoxingViewActivity::class.java,
                    medias
                ).start(
                    this,
                    IMAGE_PREVIEW_REQUEST_CODE,
                    BoxingConfig.ViewMode.PRE_EDIT
                )
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PREVIEW_REQUEST_CODE) {
            mIsPreview = false
            val isBackClick = data.getBooleanExtra(
                BoxingViewActivity.EXTRA_TYPE_BACK,
                false
            )
            val selectedMedias: List<BaseMedia> =
                data.getParcelableArrayListExtra(Boxing.EXTRA_SELECTED_MEDIA) ?: ArrayList()
            onViewActivityRequest(selectedMedias, mediaAdapter.allMedias, isBackClick)
            if (isBackClick) {
                mediaAdapter.selectedMedias = selectedMedias.toMutableList()
            }
            updateMultiPickerLayoutState(selectedMedias)
        }
    }

    private fun onViewActivityRequest(
        selectedMedias: List<BaseMedia>,
        allMedias: List<BaseMedia>,
        isBackClick: Boolean
    ) {
        if (isBackClick) {
            checkSelectedMedia(allMedias, selectedMedias)
        } else {
            onFinish(selectedMedias)
        }
    }

    override fun onCameraActivityResult(requestCode: Int, resultCode: Int) {
        showProgressDialog()
        super.onCameraActivityResult(requestCode, resultCode)
    }

    private fun showProgressDialog() {
        if (mDialog == null) {
            mDialog = ProgressDialog(activity)
            mDialog?.isIndeterminate = true
            mDialog?.setMessage(getString(R.string.boxing_handling))
        }
        if (mDialog?.isShowing == false) {
            mDialog?.show()
        }
    }

    private fun dismissProgressDialog() {
        if (mDialog != null && mDialog?.isShowing == true) {
            mDialog?.hide()
            mDialog?.dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val medias = mediaAdapter.selectedMedias.asTypeNull<ArrayList<BaseMedia?>>()
        onSaveMedias(outState, medias)
    }

    fun setTitleTxt(titleTxt: TextView) {
        mTitleTxt = titleTxt
        mTitleTxt?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (mAlbumPopWindow == null) {
                    val height =
                        getScreenHeight(v.context) -
                                (getToolbarHeight(v.context)
                                        + getStatusBarHeight(
                                    v.context
                                ))
                    val windowView = createWindowView()
                    mAlbumPopWindow = PopupWindow(
                        windowView, ViewGroup.LayoutParams.MATCH_PARENT,
                        height, true
                    ).apply {
                        animationStyle = R.style.Boxing_PopupAnimation
                        isOutsideTouchable = true
                        setBackgroundDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    v.context,
                                    R.color.boxing_colorPrimaryAlpha
                                )
                            )
                        )
                        contentView = windowView
                    }
                }
                mAlbumPopWindow?.showAsDropDown(v, 0, 0)
            }

            @SuppressLint("InflateParams")
            private fun createWindowView(): View {
                val view = LayoutInflater.from(activity)
                    .inflate(R.layout.layout_boxing_album, null)
                val recyclerView =
                    view.findViewById<RecyclerView>(R.id.album_recycleview)
                recyclerView.layoutManager = LinearLayoutManager(
                    view.context,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                recyclerView.addItemDecoration(
                    SpacesItemDecoration(
                        2,
                        1
                    )
                )
                val albumShadowLayout =
                    view.findViewById<View>(R.id.album_shadow)
                albumShadowLayout.setOnClickListener { dismissAlbumWindow() }
                mAlbumWindowAdapter.setAlbumOnClickListener(OnAlbumItemOnClickListener())
                recyclerView.adapter = mAlbumWindowAdapter
                return view
            }
        })
    }

    private fun dismissAlbumWindow() {
        if (mAlbumPopWindow != null && mAlbumPopWindow?.isShowing == true) {
            mAlbumPopWindow?.dismiss()
        }
    }

    private inner class ScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val childCount = recyclerView.childCount
            if (childCount > 0) {
                val lastChild = recyclerView.getChildAt(childCount - 1)
                val itemCount = recyclerView.adapter?.itemCount ?: 0
                val lastVisible = recyclerView.getChildAdapterPosition(lastChild)
                if (lastVisible == itemCount - 1 && hasNextPage() && canLoadNextPage()) {
                    onLoadNextPage()
                }
            }
        }
    }

    private inner class OnMediaClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val media = v.tag.asType<BaseMedia>()
            val pos = v.getTag(R.id.media_item_check) as Int
            val mode =
                getInstance().boxingConfig.mode
            when {
                mode === BoxingConfig.Mode.SINGLE_IMG -> {
                    singleImageClick(media)
                }
                mode === BoxingConfig.Mode.MULTI_IMG -> {
                    multiImageClick(pos)
                }
                mode === BoxingConfig.Mode.VIDEO -> {
                    videoClick(media)
                }
            }
        }

        private fun videoClick(media: BaseMedia) {
            val iMedias =
                ArrayList<BaseMedia>()
            iMedias.add(media)
            onFinish(iMedias)
        }

        private fun multiImageClick(pos: Int) {
            if (!mIsPreview) {
                val albumMedia =
                    mAlbumWindowAdapter.currentAlbum
                val albumId =
                    albumMedia?.mBucketId ?: AlbumEntity.DEFAULT_NAME
                mIsPreview = true
                val medias =
                    mediaAdapter.selectedMedias.asType<ArrayList<BaseMedia>>()
                get().withIntent(
                    requireContext(),
                    BoxingViewActivity::class.java,
                    medias,
                    pos,
                    albumId
                )
                    .start(
                        this@BoxingViewFragment,
                        IMAGE_PREVIEW_REQUEST_CODE,
                        BoxingConfig.ViewMode.EDIT
                    )
            }
        }

        private fun singleImageClick(media: BaseMedia) {
            val iMedias =
                ArrayList<BaseMedia>()
            iMedias.add(media)
            if (hasCropBehavior()) {
                startCrop(
                    media,
                    IMAGE_CROP_REQUEST_CODE
                )
            } else {
                onFinish(iMedias)
            }
        }
    }

    private inner class OnCameraClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            if (!mIsCamera) {
                mIsCamera = true
                startCamera(
                    requireActivity(),
                    this@BoxingViewFragment,
                    BoxingFileHelper.DEFAULT_SUB_DIR
                )
            }
        }
    }

    private inner class OnMediaCheckedListener :
        BoxingMediaAdapter.OnMediaCheckedListener {
        override fun onChecked(
            v: View?,
            iMedia: BaseMedia?
        ) {
            if (iMedia !is ImageMedia) {
                return
            }
            val isSelected = !iMedia.isSelected
            val layout = v.asTypeNull<MediaItemLayout>()
            val selectedMedias = mediaAdapter.selectedMedias
            if (isSelected) {
                if (selectedMedias.size >= mMaxCount) {
                    val warning = getString(
                        R.string.boxing_too_many_picture_fmt,
                        mMaxCount
                    )
                    Toast.makeText(activity, warning, Toast.LENGTH_SHORT).show()
                    return
                }
                if (!selectedMedias.contains(iMedia)) {
                    if (iMedia.isGifOverSize) {
                        Toast.makeText(
                            activity,
                            R.string.boxing_gif_too_big,
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    selectedMedias.add(iMedia)
                }
            } else {
                if (selectedMedias.size >= 1 && selectedMedias.contains(iMedia)) {
                    selectedMedias.remove(iMedia)
                }
            }
            iMedia.isSelected = isSelected
            layout?.setChecked(isSelected)
            updateMultiPickerLayoutState(selectedMedias)
        }
    }

    private inner class OnAlbumItemOnClickListener :
        BoxingAlbumAdapter.OnAlbumClickListener {
        override fun onClick(view: View?, pos: Int) {
            val adapter = mAlbumWindowAdapter
            if (adapter.currentAlbumPos != pos) {
                val albums = adapter.alums
                adapter.currentAlbumPos = pos
                val albumMedia = albums[pos]
                loadMedias(0, albumMedia.mBucketId)
                mTitleTxt?.text = albumMedia.mBucketName
                for (album in albums) {
                    album.mIsSelected = false
                }
                albumMedia.mIsSelected = true
                adapter.notifyDataSetChanged()
            }
            dismissAlbumWindow()
        }
    }

    companion object {
        const val TAG = "com.bilibili.boxing_impl.ui.BoxingViewFragment"
        private const val IMAGE_PREVIEW_REQUEST_CODE = 9086
        private const val IMAGE_CROP_REQUEST_CODE = 9087
        private const val GRID_COUNT = 3
        fun newInstance(): BoxingViewFragment {
            return BoxingViewFragment()
        }
    }
}