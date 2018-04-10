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
import android.support.annotation.AnyThread

/**
 * Common image cache interface, implementations should be thread safe
 */
interface ImageCache {
    /**
     * Get [Bitmap] for the specified `key`, this method called on the main thread
     * if it's a memory cache, and on a worker thread otherwise
     *
     * @param key Unique key
     * @return Image [Bitmap] or `null`, if there are no entry
     * for the specified `key`
     */
    @AnyThread
    operator fun get(key: String): Bitmap?

    /**
     * Put [Bitmap] into cache
     *
     * @param key   Unique key
     * @param value Image bitmap
     */
    @AnyThread
    fun put(key: String, value: Bitmap)

    /**
     * Remove entry with specified `key` from cache
     *
     * @param key Unique key
     */
    @AnyThread
    fun remove(key: String)

    /**
     * Clear cache
     */
    @AnyThread
    fun clear()
}
