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

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import java.util.concurrent.locks.ReentrantLock

internal object ImageLoaderHolder {
    private val lock = ReentrantLock()

    @Volatile
    @SuppressLint("StaticFieldLeak")
    private var sInstance: ImageLoader? = null

    operator fun get(context: Context): ImageLoader {
        var instance = sInstance
        if (instance == null) {
            lock.lock()
            try {
                instance = sInstance
                if (instance == null) {
                    val appContext = context.applicationContext
                    instance = ImageLoader.builder(appContext).storageCache().memoryCache().build()
                    appContext.registerComponentCallbacks(ClearMemoryCallbacks())
                    sInstance = instance
                }
            } finally {
                lock.unlock()
            }
        }
        return instance ?: throw AssertionError()
    }

    private class ClearMemoryCallbacks : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                val loader = sInstance
                loader?.clearMemoryCache()
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            // Do nothing
        }

        override fun onLowMemory() {
            val loader = sInstance
            loader?.clearMemoryCache()
        }
    }
}
