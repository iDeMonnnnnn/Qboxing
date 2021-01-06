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
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bilibili.boxing.AbsBoxingViewFragment
import com.bilibili.boxing.model.BoxingManager.Companion.getInstance
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.utils.BoxingFileHelper
import com.bilibili.boxing.utils.asType
import com.bilibili.boxing.utils.toGone
import com.bilibili.boxing.utils.toVisible
import com.bilibili.boxing_impl.R
import com.bilibili.boxing_impl.adapter.BoxingMediaAdapter
import com.bilibili.boxing_impl.view.HackyGridLayoutManager
import com.bilibili.boxing_impl.view.SpacesItemDecoration
import kotlinx.android.synthetic.main.fragment_boxing_bottom_sheet.*
import java.util.*

/**
 * the most easy to implement [com.bilibili.boxing.presenter.PickerContract.View] to show medias with google's Bottom Sheet
 * for simplest purpose, it only support SINGLE_IMG and VIDEO Mode.
 * for MULTI_IMG mode, use [BoxingViewFragment] instead.
 *
 * @author ChenSL
 */
class BoxingBottomSheetFragment : AbsBoxingViewFragment() {
    private var mIsCamera = false
    private lateinit var mMediaAdapter: BoxingMediaAdapter
    private var mDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMediaAdapter = BoxingMediaAdapter(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_boxing_bottom_sheet,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        media_recycleview.setHasFixedSize(true)
        val gridLayoutManager: GridLayoutManager =
            HackyGridLayoutManager(
                requireActivity(),
                GRID_COUNT
            )
        gridLayoutManager.isSmoothScrollbarEnabled = true
        media_recycleview.layoutManager = gridLayoutManager
        media_recycleview.addItemDecoration(
            SpacesItemDecoration(
                resources.getDimensionPixelOffset(R.dimen.boxing_media_margin),
                GRID_COUNT
            )
        )
        media_recycleview.adapter = mMediaAdapter
        media_recycleview.addOnScrollListener(ScrollListener())
        mMediaAdapter.setOnMediaClickListener(OnMediaClickListener())
        mMediaAdapter.setOnCameraClickListener(OnCameraClickListener())
        finish_txt.setOnClickListener { onFinish(null) }
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
        if (mDialog?.isShowing == false) {
            mDialog?.hide()
            mDialog?.dismiss()
        }
    }

    override fun showMedia(
        medias: List<BaseMedia>?,
        allCount: Int
    ) {
        if (medias == null || isEmptyData(medias)
            && isEmptyData(mMediaAdapter.allMedias)
        ) {
            showEmptyData()
            return
        }
        showData()
        mMediaAdapter.addAllData(medias)
    }

    private fun isEmptyData(medias: List<BaseMedia>): Boolean {
        return medias.isEmpty() && !getInstance().boxingConfig.isNeedCamera
    }

    private fun showEmptyData() {
        empty_txt.toVisible
        media_recycleview.toGone
        loading.toGone
    }

    private fun showData() {
        loading.toGone
        empty_txt.toGone
        media_recycleview.toVisible
    }

    override fun onCameraFinish(media: BaseMedia) {
        dismissProgressDialog()
        mIsCamera = false
        val selectedMedias = mMediaAdapter.selectedMedias
        selectedMedias.add(media)
        onFinish(selectedMedias)
    }

    override fun onCameraError() {
        mIsCamera = false
        dismissProgressDialog()
    }

    override fun startLoading() {
        loadMedias()
    }

    override fun onRequestPermissionError(
        permissions: Array<String>,
        e: Exception?
    ) {
        if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            showEmptyData()
            Toast.makeText(
                context,
                R.string.boxing_storage_permission_deny,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionSuc(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (permissions[0] == STORAGE_PERMISSIONS[0]
        ) {
            startLoading()
        }
    }

    override fun clearMedia() {
        mMediaAdapter.clearData()
    }

    private inner class OnMediaClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val iMedias = ArrayList<BaseMedia>()
            val media = v.tag.asType<BaseMedia>()
            iMedias.add(media)
            onFinish(iMedias)
        }
    }

    private inner class OnCameraClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            if (!mIsCamera) {
                mIsCamera = true
                startCamera(
                    requireActivity(),
                    this@BoxingBottomSheetFragment,
                    BoxingFileHelper.DEFAULT_SUB_DIR
                )
            }
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

    companion object {
        const val TAG = "com.bilibili.boxing_impl.ui.BoxingBottomSheetFragment"
        private const val GRID_COUNT = 3
        fun newInstance(): BoxingBottomSheetFragment {
            return BoxingBottomSheetFragment()
        }
    }
}