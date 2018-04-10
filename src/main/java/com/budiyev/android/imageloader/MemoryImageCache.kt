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
import android.os.Build
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class MemoryImageCache @JvmOverloads constructor(
        private val mMaxSize: Int = Math.round(Runtime.getRuntime().maxMemory() * DEFAULT_MEMORY_FRACTION)) :
        ImageCache {
    private val mImages: LinkedHashMap<String, Bitmap>
    private val mLock: Lock
    @Volatile
    private var mSize: Int = 0

    init {
        if (mMaxSize < 0) {
            throw IllegalArgumentException("Cache size should be greater than or equal to zero")
        }
        mImages = LinkedHashMap(0, 0.75f, true)
        mLock = ReentrantLock()
    }

    override fun get(key: String): Bitmap? {
        mLock.lock()
        try {
            return mImages[key]
        } finally {
            mLock.unlock()
        }
    }

    override fun put(key: String, value: Bitmap) {
        mLock.lock()
        try {
            var size = mSize
            size += getBitmapSize(value)
            mImages[key] = value
            val maxSize = mMaxSize
            if (size > maxSize) {
                val i = mImages.entries.iterator()
                while (i.hasNext()) {
                    size -= getBitmapSize(i.next().value)
                    i.remove()
                    if (size <= maxSize) {
                        break
                    }
                }
            }
            mSize = size
        } finally {
            mLock.unlock()
        }
    }

    override fun remove(key: String) {
        mLock.lock()
        try {
            val i = mImages.entries.iterator()
            var size = mSize
            while (i.hasNext()) {
                val entry = i.next()
                if (entry.key.startsWith(key)) {
                    size -= getBitmapSize(entry.value)
                    i.remove()
                }
            }
            mSize = size
        } finally {
            mLock.unlock()
        }
    }

    override fun clear() {
        mLock.lock()
        try {
            mImages.clear()
            mSize = 0
        } finally {
            mLock.unlock()
        }
    }

    companion object {
        private val DEFAULT_MEMORY_FRACTION = 0.25f

        private fun getBitmapSize(bitmap: Bitmap): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bitmap.allocationByteCount
            } else {
                bitmap.byteCount
            }
        }
    }
}
