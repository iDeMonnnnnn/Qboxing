@file:Suppress("DEPRECATION")

package com.bilibili.boxing_impl.ui

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Created by ChenSL on 2017/4/5.
 */
open class BoxingBaseFragment : Fragment() {
    private var mNeedPendingUserVisibleHint = false
    private var mLastUserVisibleHint = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (mNeedPendingUserVisibleHint) {
            setUserVisibleCompat(mLastUserVisibleHint)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (activity == null) {
            mNeedPendingUserVisibleHint = true
            mLastUserVisibleHint = isVisibleToUser
        } else {
            setUserVisibleCompat(isVisibleToUser)
        }
    }

    open fun setUserVisibleCompat(userVisibleCompat: Boolean) {}
}