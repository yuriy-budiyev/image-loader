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

import android.support.annotation.AnyThread

/**
 * Data descriptor, provides data and key that identifies this data,
 * implementations should be immutable
 */
interface DataDescriptor<out T> {
    /**
     * Data, that will be transferred to [BitmapLoader],
     * [LoadCallback] and [ErrorCallback]
     *
     * @return Data
     */
    @get:AnyThread
    val data: T

    /**
     * Must be unique for each image. If you want to use storage caching, ensure that
     * returned value doesn't contain characters that can't be used in file name,
     * [DataDescriptor]s considered to be equal if their keys are equal and not `null`,
     * caching is not available if this method returns `null`
     *
     * @return Unique identifier or `null`
     * @see DataUtils.generateSHA256
     */
    @get:AnyThread
    val key: String?

    /**
     * Location of the data, affects caching policy
     *
     * @return Data location or `null`
     */
    @get:AnyThread
    val location: DataLocation?
}
