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

internal class SyncLoadImageAction<T>(descriptor: DataDescriptor<T>, bitmapLoader: BitmapLoader<T>, requiredSize: Size?,
        transformation: BitmapTransformation?, memoryCache: ImageCache?, storageCache: ImageCache?,
        loadCallback: LoadCallback?, errorCallback: ErrorCallback?, pauseLock: PauseLock) :
        LoadImageAction<T>(descriptor, bitmapLoader, requiredSize, transformation, memoryCache, storageCache, null,
                loadCallback, errorCallback, pauseLock) {
    private var mImage: Bitmap? = null

    @WorkerThread
    fun load(): Bitmap? {
        execute()
        return mImage
    }

    override fun onImageLoaded(image: Bitmap) {
        mImage = image
    }

    override fun onError(error: Throwable) {
        // Do nothing
    }

    override fun onCancelled() {
        // Do nothing
    }
}
