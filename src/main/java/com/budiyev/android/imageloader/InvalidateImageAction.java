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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class InvalidateImageAction extends BaseAction {
    private final String mKey;
    private final MemoryImageCache mMemoryCache;
    private final StorageImageCache mStorageCache;

    public InvalidateImageAction(@NonNull final String key,
            @Nullable final MemoryImageCache memoryCache,
            @Nullable final StorageImageCache storageCache) {
        mKey = key;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
    }

    @Override
    protected void execute() {
        if (!isCancelled()) {
            final String key = mKey;
            final MemoryImageCache memoryCache = mMemoryCache;
            if (memoryCache != null) {
                memoryCache.remove(key);
            }
            final StorageImageCache storageCache = mStorageCache;
            if (storageCache != null) {
                storageCache.remove(key);
            }
        }
    }

    @Override
    protected void onCancelled() {
        // Do noting
    }
}
