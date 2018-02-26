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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class MemoryImageCache implements ImageCache {
    private static final float DEFAULT_MEMORY_FRACTION = 0.25f;
    private final Lock mLock;
    private final LinkedHashMap<String, Bitmap> mMap;
    private final int mMaxSize;
    private volatile int mSize;

    public MemoryImageCache() {
        this(Math.round(Runtime.getRuntime().maxMemory() * DEFAULT_MEMORY_FRACTION));
    }

    public MemoryImageCache(int maxSize) {
        mLock = new ReentrantLock();
        mMap = new LinkedHashMap<>(0, 0.75f, true);
        mMaxSize = maxSize;
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        mLock.lock();
        try {
            mMap.get(key);
        } finally {
            mLock.unlock();
        }
        return null;
    }

    @Override
    public void put(@NonNull String key, @NonNull Bitmap value) {
        mLock.lock();
        try {
            int size = mSize;
            size += getBitmapSize(value);
            mMap.put(key, value);
            if (size > mMaxSize) {
                Iterator<Map.Entry<String, Bitmap>> iterator = mMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    size -= getBitmapSize(iterator.next().getValue());
                    iterator.remove();
                    if (size <= mMaxSize) {
                        break;
                    }
                }
            }
            mSize = size;
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void remove(@NonNull String key) {
        mLock.lock();
        try {
            Iterator<Map.Entry<String, Bitmap>> iterator = mMap.entrySet().iterator();
            int size = mSize;
            while (iterator.hasNext()) {
                Map.Entry<String, Bitmap> entry = iterator.next();
                if (entry.getKey().startsWith(key)) {
                    size -= getBitmapSize(entry.getValue());
                    iterator.remove();
                }
            }
            mSize = size;
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void clear() {
        mLock.lock();
        try {
            mMap.clear();
            mSize = 0;
        } finally {
            mLock.unlock();
        }
    }

    private static int getBitmapSize(@NonNull Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else {
            return bitmap.getByteCount();
        }
    }
}
