/*
 * MIT License
 *
 * Copyright (c) 2018 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.budiyev.android.imageloader

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.support.annotation.MainThread
import android.view.View
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

internal class DisplayImageAction<T>(resources: Resources, view: View, descriptor: DataDescriptor<T>,
        bitmapLoader: BitmapLoader<T>, requiredSize: Size?, transformation: BitmapTransformation?,
        private val mPlaceholder: Drawable, private val mErrorDrawable: Drawable?, memoryCache: ImageCache?,
        storageCache: ImageCache?, cacheExecutor: ExecutorService?, loadCallback: LoadCallback?,
        errorCallback: ErrorCallback?, private val mDisplayCallback: DisplayCallback?, pauseLock: PauseLock,
        private val mMainThreadHandler: Handler, private val mFadeEnabled: Boolean, private val mFadeDuration: Long,
        private val mCornerRadius: Float) :
        LoadImageAction<T>(descriptor, bitmapLoader, requiredSize, transformation, memoryCache, storageCache,
                cacheExecutor, loadCallback, errorCallback, pauseLock) {
    private val mResources: WeakReference<Resources>
    private val mView: WeakReference<View>

    init {
        mResources = WeakReference(resources)
        mView = WeakReference(view)
    }

    fun hasSameKey(key: String?): Boolean {
        return key != null && key == key
    }

    override fun onImageLoaded(image: Bitmap) {
        mMainThreadHandler.post(SetImageAction(image))
    }

    override fun onError(error: Throwable) {
        if (mErrorDrawable != null) {
            mMainThreadHandler.post(SetErrorDrawableAction())
        }
    }

    override fun onCancelled() {
        mView.clear()
        mResources.clear()
    }

    private inner class SetErrorDrawableAction : Runnable {
        override fun run() {
            if (isCancelled) {
                return
            }
            val errorDrawable = mErrorDrawable
            val view = mView.get()
            if (errorDrawable == null || view == null || InternalUtils.getDisplayImageAction(
                            view) != this@DisplayImageAction) {
                return
            }
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                InternalUtils.setDrawable(FadeDrawable(mPlaceholder, errorDrawable, mFadeDuration), view)
            } else {
                InternalUtils.setDrawable(errorDrawable, view)
            }
        }
    }

    private inner class SetImageAction(private val mImage: Bitmap) : Runnable {

        @MainThread
        override fun run() {
            if (isCancelled) {
                return
            }
            val view = mView.get()
            val resources = mResources.get()
            if (view == null || resources == null || InternalUtils.getDisplayImageAction(
                            view) != this@DisplayImageAction) {
                return
            }
            val image = mImage
            val cornerRadius = mCornerRadius
            val roundCorners = cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                InternalUtils.setDrawable(
                        FadeDrawable(mPlaceholder, if (roundCorners) RoundedDrawable(resources, image, cornerRadius)
                        else BitmapDrawable(resources, image), mFadeDuration), view)
            } else {
                if (roundCorners) {
                    InternalUtils.setDrawable(RoundedDrawable(resources, image, cornerRadius), view)
                } else {
                    InternalUtils.setBitmap(resources, image, view)
                }
            }
            val displayCallback = mDisplayCallback
            displayCallback?.onDisplayed(image, view)
        }
    }
}
