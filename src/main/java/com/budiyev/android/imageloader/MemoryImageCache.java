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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

final class MemoryImageCache implements ImageCache {
    private static final float DEFAULT_MEMORY_FRACTION = 0.25f;
    private final LruCache<String, Bitmap> mCache;
    private final Set<String> mKeySet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public MemoryImageCache() {
        this(Math.round(Runtime.getRuntime().maxMemory() * DEFAULT_MEMORY_FRACTION));
    }

    public MemoryImageCache(int maxSize) {
        mCache = new CacheImpl(maxSize);
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        return mCache.get(key);
    }

    @Override
    public void put(@NonNull String key, @NonNull Bitmap value) {
        mCache.put(key, value);
        mKeySet.add(key);
    }

    @Override
    public void remove(@NonNull String key) {
        // Remove possible cached transformations too
        Iterator<String> i = mKeySet.iterator();
        while (i.hasNext()) {
            String k = i.next();
            if (k.startsWith(key)) {
                mCache.remove(k);
                i.remove();
            }
        }
    }

    @Override
    public void clear() {
        mCache.evictAll();
    }

    private final class CacheImpl extends LruCache<String, Bitmap> {
        public CacheImpl(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return value.getAllocationByteCount();
            } else {
                return value.getByteCount();
            }
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            if (evicted) {
                mKeySet.remove(key);
            }
        }
    }
}
