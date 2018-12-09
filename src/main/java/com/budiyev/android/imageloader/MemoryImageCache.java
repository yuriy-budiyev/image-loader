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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class MemoryImageCache implements ImageCache {
    private static final float DEFAULT_MEMORY_FRACTION = 0.25f;
    private final LinkedHashMap<String, Bitmap> mImages;
    private final Lock mLock;
    private final int mMaxSize;
    private volatile int mSize;

    public MemoryImageCache() {
        this(Math.round(Runtime.getRuntime().maxMemory() * DEFAULT_MEMORY_FRACTION));
    }

    public MemoryImageCache(final int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException(
                    "Cache size should be greater than or equal to zero");
        }
        mImages = new LinkedHashMap<>(0, 0.75f, true);
        mLock = new ReentrantLock();
        mMaxSize = maxSize;
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull final String key) {
        mLock.lock();
        try {
            return mImages.get(key);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void put(@NonNull final String key, @NonNull final Bitmap value) {
        mLock.lock();
        try {
            int size = mSize;
            size += getBitmapSize(value);
            mImages.put(key, value);
            final int maxSize = mMaxSize;
            if (size > maxSize) {
                final Iterator<Map.Entry<String, Bitmap>> i = mImages.entrySet().iterator();
                while (i.hasNext()) {
                    size -= getBitmapSize(i.next().getValue());
                    i.remove();
                    if (size <= maxSize) {
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
    public void remove(@NonNull final String key) {
        mLock.lock();
        try {
            final Iterator<Map.Entry<String, Bitmap>> i = mImages.entrySet().iterator();
            int size = mSize;
            while (i.hasNext()) {
                final Map.Entry<String, Bitmap> entry = i.next();
                if (entry.getKey().startsWith(key)) {
                    size -= getBitmapSize(entry.getValue());
                    i.remove();
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
            mImages.clear();
            mSize = 0;
        } finally {
            mLock.unlock();
        }
    }

    private static int getBitmapSize(@NonNull final Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else {
            return bitmap.getByteCount();
        }
    }
}
