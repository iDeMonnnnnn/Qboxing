package com.bilibili.boxing.utils

import android.view.View

/**
 * @author Jowan
 * Created on 2020/5/8.
 */
/**
 * (a as? T)?.some() -> a.asType<T>()?.some()
 */
inline fun <reified T> Any?.asTypeNull(): T? {
    return this as? T
}

/**
 * (a as T).some() -> a.asType<T>().some()
 */
inline fun <reified T> Any?.asType(): T {
    return this as T
}

/**
 * 设置View的显隐为VISIBLE
 */
val View?.toVisible: Unit
    get() {
        this?.visibility = View.VISIBLE
    }

/**
 * 设置View的显隐为GONE
 */
val View?.toGone: Unit
    get() {
        this?.visibility = View.GONE
    }

/**
 * 设置View的显隐为INVISIBLE
 */
val View?.toInvisible: Unit
    get() {
        this?.visibility = View.INVISIBLE
    }