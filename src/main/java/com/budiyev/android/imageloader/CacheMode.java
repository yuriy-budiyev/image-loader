/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
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

/**
 * Cache mode
 */
public enum CacheMode {
    /**
     * Both memory and storage caches will be used in loading process
     */
    FULL(true, true),

    /**
     * Only memory cache will be used in loading process
     */
    MEMORY(true, false),

    /**
     * Only storage caches will be used in loading process
     */
    STORAGE(false, true),

    /**
     * No caching
     */
    NONE(false, false);

    private final boolean mMemoryCacheEnabled;
    private final boolean mStorageCacheEnabled;

    CacheMode(boolean memoryCacheEnabled, boolean storageCacheEnabled) {
        mMemoryCacheEnabled = memoryCacheEnabled;
        mStorageCacheEnabled = storageCacheEnabled;
    }

    boolean isMemoryCacheEnabled() {
        return mMemoryCacheEnabled;
    }

    boolean isStorageCacheEnabled() {
        return mStorageCacheEnabled;
    }
}


