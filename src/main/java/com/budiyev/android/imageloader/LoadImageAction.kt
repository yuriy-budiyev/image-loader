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

import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import java.util.concurrent.ExecutorService

internal abstract class LoadImageAction<T> protected constructor(protected val descriptor: DataDescriptor<T>,
        protected val bitmapLoader: BitmapLoader<T>, protected val requiredSize: Size?,
        private val mTransformation: BitmapTransformation?, protected val memoryCache: ImageCache?,
        protected val storageCache: ImageCache?, private val mCacheExecutor: ExecutorService?,
        protected val loadCallback: LoadCallback?, protected val errorCallback: ErrorCallback?,
        protected val pauseLock: PauseLock) : ImageRequestAction() {
    @Volatile
    private var mCacheDelegate: ImageRequestDelegate? = null

    protected val key: String?
        get() = InternalUtils.buildFullKey(descriptor.key, requiredSize, mTransformation)

    @WorkerThread
    protected abstract fun onImageLoaded(image: Bitmap)

    @WorkerThread
    protected abstract fun onError(error: Throwable)

    override fun onCancelled() {
        val delegate = mCacheDelegate
        delegate?.cancel()
    }

    @WorkerThread
    override fun execute() {
        while (!isCancelled && !pauseLock.shouldInterruptEarly() && pauseLock.isPaused) {
            try {
                pauseLock.await()
            } catch (e: InterruptedException) {
                return
            }

        }
        if (isCancelled || pauseLock.shouldInterruptEarly()) {
            return
        }
        val descriptor = this.descriptor
        val key = key
        val data = descriptor.data
        var image: Bitmap?
        // Memory cache
        val memoryCache = this.memoryCache
        if (key != null && memoryCache != null) {
            image = memoryCache.get(key)
            if (image != null) {
                processImage(image)
                return
            }
        }
        if (isCancelled) {
            return
        }
        // Storage cache
        val storageCache = this.storageCache
        if (key != null && storageCache != null) {
            image = storageCache.get(key)
            if (image != null) {
                processImage(image)
                memoryCache?.put(key, image)
                return
            }
        }
        if (isCancelled) {
            return
        }
        // Load new image
        val requiredSize = this.requiredSize
        try {
            image = bitmapLoader.load(data, requiredSize)
        } catch (error: Throwable) {
            processError(error)
            return
        }

        if (image == null) {
            processError(ImageNotLoadedException())
            return
        }
        if (isCancelled) {
            return
        }
        // Transform image
        val transformation = mTransformation
        if (transformation != null) {
            try {
                val transformed = transformation.transform(image)
                if (image != transformed && !image.isRecycled) {
                    image.recycle()
                }
                image = transformed
            } catch (error: Throwable) {
                processError(error)
                return
            }

        }
        if (isCancelled) {
            return
        }
        processImage(image)
        if (key != null) {
            memoryCache?.put(key, image)
            if (storageCache != null && (requiredSize != null || transformation != null || descriptor.location != DataLocation.LOCAL)) {
                val cacheExecutor = mCacheExecutor
                if (cacheExecutor != null) {
                    mCacheDelegate = CacheImageAction(key, image, storageCache).submit(cacheExecutor)
                } else {
                    storageCache.put(key, image)
                }
            }
        }
    }

    @WorkerThread
    private fun processImage(image: Bitmap) {
        val loadCallback = this.loadCallback
        loadCallback?.onLoaded(image)
        onImageLoaded(image)
    }

    @WorkerThread
    private fun processError(error: Throwable) {
        val errorCallback = this.errorCallback
        errorCallback?.onError(error)
        onError(error)
    }
}
