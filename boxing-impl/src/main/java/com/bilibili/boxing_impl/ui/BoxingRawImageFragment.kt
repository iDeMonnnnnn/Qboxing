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
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import com.bilibili.boxing.AbsBoxingViewActivity
import com.bilibili.boxing.loader.IBoxingCallback
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.utils.BoxingLog.d
import com.bilibili.boxing.utils.asTypeNull
import com.bilibili.boxing.utils.toGone
import com.bilibili.boxing.utils.toVisible
import com.bilibili.boxing_impl.R
import uk.co.senab.photoview.PhotoView
import uk.co.senab.photoview.PhotoViewAttacher
import java.lang.ref.WeakReference

/**
 * show raw image with the control of finger gesture.
 *
 * @author ChenSL
 */
class BoxingRawImageFragment : BoxingBaseFragment() {
    private var mMedia: ImageMedia? = null
    private lateinit var mAttacher: PhotoViewAttacher
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMedia = arguments?.getParcelable(BUNDLE_IMAGE)
    }

    private lateinit var rootView: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(
            R.layout.fragment_boxing_raw_image,
            container,
            false
        )
        return rootView
    }

    private lateinit var photo_view: PhotoView
    private lateinit var loading: ProgressBar
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        photo_view = rootView.findViewById(R.id.photo_view)
        loading = rootView.findViewById(R.id.loading)
        mAttacher = PhotoViewAttacher(photo_view)
        mAttacher.setRotatable(true)
        mAttacher.setToRightAngle(true)
    }

    override fun setUserVisibleCompat(userVisibleCompat: Boolean) {
        if (userVisibleCompat) {
            val point = getResizePointer(mMedia?.size ?: 0)
            activity.asTypeNull<AbsBoxingViewActivity>()?.loadRawImage(
                photo_view,
                mMedia?.uri,
                point.x,
                point.y,
                BoxingCallback(this)
            )
        }
    }

    /**
     * resize the image or not according to size.
     *
     * @param size the size of image
     */
    private fun getResizePointer(size: Long): Point {
        val metrics = resources.displayMetrics
        val point =
            Point(metrics.widthPixels, metrics.heightPixels)
        when {
            size >= MAX_IMAGE2 -> {
                point.x = point.x shr 2
                point.y = point.y shr 2
            }
            size >= MAX_IMAGE1 -> {
                point.x = point.x shr 1
                point.y = point.y shr 1
            }
            size > 0 -> { // avoid some images do not have a size.
                point.x = 0
                point.y = 0
            }
        }
        return point
    }

    private fun dismissProgressDialog() {
        loading.toGone
        thisActivity?.mProgressBar?.toGone
    }

    private val thisActivity: BoxingViewActivity?
        get() {
            val activity: Activity? = activity
            return if (activity is BoxingViewActivity) {
                activity
            } else null
        }

    override fun onDestroyView() {
        super.onDestroyView()
        mAttacher.cleanup()
    }

    private class BoxingCallback internal constructor(fragment: BoxingRawImageFragment?) :
        IBoxingCallback {
        private val mWr: WeakReference<BoxingRawImageFragment?> = WeakReference(fragment)

        override fun onSuccess() {
            val get = mWr.get() ?: return
            //get.photo_view must not be null
            if (get.photo_view == null) return
            get.dismissProgressDialog()
            val drawable = get.photo_view.drawable
            if (drawable.intrinsicHeight > drawable.intrinsicWidth shl 2) { // handle the super height image.
                var scale = drawable.intrinsicHeight / drawable.intrinsicWidth
                scale = MAX_SCALE.coerceAtMost(scale)
                get.mAttacher.maximumScale = scale.toFloat()
                get.mAttacher.setScale(scale.toFloat(), true)
            }
            get.mAttacher.update()
            val activity = get.thisActivity
            activity?.mGallery?.toVisible
        }

        override fun onFail(t: Throwable?) {
            val get = mWr.get() ?: return
            d(if (t != null) t.message else "load raw image error.")
            get.dismissProgressDialog()
            get.photo_view.setImageResource(R.drawable.ic_boxing_broken_image)
            get.mAttacher.update()
        }

    }

    companion object {
        private const val BUNDLE_IMAGE =
            "com.bilibili.boxing_impl.ui.BoxingRawImageFragment.image"
        private const val MAX_SCALE = 15
        private const val MAX_IMAGE1 = 1024 * 1024L
        private const val MAX_IMAGE2 =
            4 * MAX_IMAGE1

        @JvmStatic
        fun newInstance(image: ImageMedia): BoxingRawImageFragment {
            return BoxingRawImageFragment().apply {
                arguments = bundleOf(BUNDLE_IMAGE to image)
            }
        }
    }
}