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
package com.bilibili.boxing.presenter

import android.text.TextUtils
import com.bilibili.boxing.model.BoxingManager
import com.bilibili.boxing.model.callback.IAlbumTaskCallback
import com.bilibili.boxing.model.callback.IMediaTaskCallback
import com.bilibili.boxing.model.entity.AlbumEntity
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.model.task.IMediaTask
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 * A presenter implement [com.bilibili.boxing.presenter.PickerContract.Presenter].
 *
 * @author ChenSL
 */
class PickerPresenter(private var mTasksView: PickerContract.View) :
    PickerContract.Presenter {
    private var mTotalPage = 0
    private var mCurrentPage = 0
    private var mIsLoadingNextPage = false
    private var mCurrentAlbumId: String = ""
    private val mLoadMediaCallback: LoadMediaCallback
    private val mLoadAlbumCallback: LoadAlbumCallback

    init {
        mTasksView.setPresenter(this)
        mLoadMediaCallback = LoadMediaCallback(this)
        mLoadAlbumCallback = LoadAlbumCallback(this)
    }

    override fun loadMedias(page: Int, albumId: String) {
        mCurrentAlbumId = albumId
        if (page == 0) {
            mTasksView.clearMedia()
            mCurrentPage = 0
        }
        BoxingManager.getInstance()
            .loadMedia(mTasksView.appCr, page, albumId, mLoadMediaCallback)
    }

    override fun loadAlbums() {
        BoxingManager.getInstance().loadAlbum(mTasksView.appCr, mLoadAlbumCallback)
    }

    override fun destroy() {
    }

    override fun hasNextPage(): Boolean {
        return mCurrentPage < mTotalPage
    }

    override fun canLoadNextPage(): Boolean {
        return !mIsLoadingNextPage
    }

    override fun onLoadNextPage() {
        mCurrentPage++
        mIsLoadingNextPage = true
        loadMedias(mCurrentPage, mCurrentAlbumId)
    }

    override fun checkSelectedMedia(
        allMedias: List<BaseMedia>,
        selectedMedias: List<BaseMedia>
    ) {
        if (allMedias.isEmpty()) {
            return
        }
        val map: MutableMap<String, ImageMedia> = HashMap(allMedias.size)
        for (allMedia in allMedias) {
            if (allMedia !is ImageMedia) {
                return
            }
            allMedia.isSelected = false
            map[allMedia.path] = allMedia
        }
        if (selectedMedias.size < 0) {
            return
        }
        selectedMedias
            .filter { map.containsKey(it.path) }
            .forEach { map[it.path]?.isSelected = true }
    }

    private class LoadMediaCallback internal constructor(presenter: PickerPresenter) :
        IMediaTaskCallback<BaseMedia> {
        private val mWr: WeakReference<PickerPresenter> = WeakReference(presenter)
        private val presenter: PickerPresenter?
            get() = mWr.get()

        override fun postMedia(
            medias: List<BaseMedia>?,
            count: Int
        ) {
            val presenter = presenter ?: return
            presenter.mTasksView.showMedia(medias, count)
            presenter.mTotalPage = count / IMediaTask.PAGE_LIMIT
            presenter.mIsLoadingNextPage = false
        }

        override fun needFilter(path: String?): Boolean {
            return TextUtils.isEmpty(path) || !File(path).exists()
        }

    }

    private class LoadAlbumCallback internal constructor(presenter: PickerPresenter) :
        IAlbumTaskCallback {
        private val mWr: WeakReference<PickerPresenter> = WeakReference(presenter)
        private val presenter: PickerPresenter?
            get() = mWr.get()

        override fun postAlbumList(list: MutableList<AlbumEntity>) {
            presenter?.mTasksView?.showAlbum(list)
        }

    }
}