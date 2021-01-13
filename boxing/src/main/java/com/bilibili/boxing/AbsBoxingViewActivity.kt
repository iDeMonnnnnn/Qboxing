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
package com.bilibili.boxing

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bilibili.boxing.loader.IBoxingCallback
import com.bilibili.boxing.model.BoxingManager
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.entity.AlbumEntity
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.presenter.PickerContract
import com.bilibili.boxing.presenter.PickerPresenter
import java.util.*

/**
 * A abstract class which implements [PickerContract.View] for custom media view.
 * For view big images.
 *
 * @author ChenSL
 */
abstract class AbsBoxingViewActivity : AppCompatActivity(),
    PickerContract.View {
    protected var mSelectedImages: ArrayList<BaseMedia> = ArrayList()
    var albumId: String = ""
    var startPos = 0
    private var mPresenter: PickerContract.Presenter? = null
    /**
     * start loading when the permission request is completed.
     * call [.loadMedias] or [.loadMedias].
     */
    abstract fun startLoading()

    /**
     * override this method to handle the medias.
     * make sure [.loadMedias] ()} being called first.
     *
     * @param medias the results of medias
     */
    override fun showMedia(medias: List<BaseMedia>?, allCount: Int) {}

    override fun showAlbum(albums: List<AlbumEntity>?) {}
    /**
     * to clear all medias the first time(the page number is 0). do some clean work.
     */
    override fun clearMedia() {}

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config: BoxingConfig? = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(Boxing.EXTRA_CONFIG)
        } else {
            BoxingManager.getInstance().boxingConfig
        }
        config?.let(this@AbsBoxingViewActivity::setPickerConfig)
        parseSelectedMedias(savedInstanceState, intent)
        setPresenter(PickerPresenter(this))
    }

    private fun parseSelectedMedias(savedInstanceState: Bundle?, intent: Intent?) {
        when {
            savedInstanceState != null -> {
                mSelectedImages = savedInstanceState.getParcelableArrayList(Boxing.EXTRA_SELECTED_MEDIA) ?: ArrayList()
                albumId = savedInstanceState.getString(Boxing.EXTRA_ALBUM_ID) ?: ""
                startPos = savedInstanceState.getInt(Boxing.EXTRA_START_POS, 0)
            }
            intent != null -> {
                startPos = intent.getIntExtra(Boxing.EXTRA_START_POS, 0)
                mSelectedImages = intent.getParcelableArrayListExtra(Boxing.EXTRA_SELECTED_MEDIA) ?: ArrayList()
                albumId = intent.getStringExtra(Boxing.EXTRA_ALBUM_ID) ?: ""
            }
        }
    }

    override fun setPresenter(presenter: PickerContract.Presenter) {
        mPresenter = presenter
    }

    /**
     * get the [ContentResolver]
     */
    override val appCr: ContentResolver
        get() = applicationContext.contentResolver

    fun loadRawImage(
        img: ImageView,
        path: Uri?,
        width: Int,
        height: Int,
        callback: IBoxingCallback?
    ) {
        BoxingMediaLoader.getInstance()
            .displayRaw(img, path, width, height, callback)
    }

    /**
     * called the job is done.Click the ok button, take a photo from camera, crop a photo.
     * most of the time, you do not have to override.
     *
     * @param medias the list of selection
     */
    override fun onFinish(medias: List<BaseMedia>?) {
        Intent().putParcelableArrayListExtra(
            Boxing.EXTRA_RESULT,
            medias as? ArrayList<BaseMedia>
        )
    }

    /**
     * need crop or not
     *
     * @return true, need it.
     */
    @Suppress("UNUSED")
    fun hasCropBehavior(): Boolean {
        val config =
            BoxingManager.getInstance().boxingConfig
        return config.isSingleImageMode && config.cropOption != null
    }

    /**
     * to start the crop behavior, call it when [.hasCropBehavior] return true.
     *
     * @param media       the media to be cropped.
     * @param requestCode The integer request code originally supplied to
     * startActivityForResult(), allowing you to identify who this
     * result came from.
     */
    override fun startCrop(media: BaseMedia, requestCode: Int) {}

    /**
     * set or update the config.most of the time, you do not have to call it.
     *
     * @param config [BoxingConfig]
     */
    override fun setPickerConfig(config: BoxingConfig) {
        BoxingManager.getInstance().boxingConfig = config
    }

    override fun onSaveInstanceState(
        outState: Bundle,
        outPersistentState: PersistableBundle
    ) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putParcelable(
            Boxing.EXTRA_CONFIG,
            BoxingManager.getInstance().boxingConfig
        )
    }

    /**
     * call this to clear resource.
     */
    public override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destroy()
    }

    /**
     * in [BoxingConfig.Mode.MULTI_IMG], call this to pick the selected medias in all medias.
     */
    fun checkSelectedMedia(
        allMedias: List<BaseMedia>,
        selectedMedias: List<BaseMedia>
    ) {
        mPresenter?.checkSelectedMedia(allMedias, selectedMedias)
    }

    /**
     * load first page of medias.
     * use [.showMedia] to get the result.
     */
    @Suppress("UNUSED")
    fun loadMedias() {
        mPresenter?.loadMedias(0, AlbumEntity.DEFAULT_NAME)
    }

    /**
     * load the medias for the specify page and album id.
     * use [.showMedia] to get the result.
     *
     * @param page    page numbers.
     * @param albumId the album id is [AlbumEntity.mBucketId].
     */
    fun loadMedias(page: Int, albumId: String?) {
        mPresenter?.loadMedias(page, albumId ?: "")
    }

    /**
     * get the max count set before
     */
    val maxCount: Int
        get() {
            return BoxingManager.getInstance().boxingConfig.maxCount
        }

}