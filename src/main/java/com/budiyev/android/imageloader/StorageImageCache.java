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
package com.budiyev.android.imageloader;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Storage image cache interface, implementations should be thread safe
 */
public interface StorageImageCache {
    /**
     * Get {@link Bitmap} for the specified {@code key}, this method called on the main thread
     * if it's a memory cache, and on a worker thread otherwise
     *
     * @param key Unique key
     * @return Image {@link Bitmap} or {@code null}, if there are no entry
     * for the specified {@code key}
     */
    @Nullable
    @WorkerThread
    Bitmap get(@NonNull String key);

    /**
     * Put {@link Bitmap} into cache
     *
     * @param key   Unique key
     * @param value Image bitmap
     */
    @WorkerThread
    void put(@NonNull String key, @NonNull Bitmap value);

    /**
     * Remove entry with specified {@code key} from cache
     *
     * @param key Unique key
     */
    @WorkerThread
    void remove(@NonNull String key);

    /**
     * Clear cache
     */
    @WorkerThread
    void clear();
}
