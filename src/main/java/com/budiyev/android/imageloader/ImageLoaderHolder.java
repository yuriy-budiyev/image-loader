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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;

final class ImageLoaderHolder {
    private static final Lock LOCK = new ReentrantLock();
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageLoader sInstance;

    private ImageLoaderHolder() {
    }

    @NonNull
    public static ImageLoader get(@NonNull Context context) {
        ImageLoader instance = sInstance;
        if (instance == null) {
            LOCK.lock();
            try {
                instance = sInstance;
                if (instance == null) {
                    instance = ImageLoader.builder(context).storageCache().memoryCache().build();
                    context.getApplicationContext().registerComponentCallbacks(new ClearMemoryCallbacks());
                    sInstance = instance;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return instance;
    }

    private static final class ClearMemoryCallbacks implements ComponentCallbacks2 {
        @Override
        public void onTrimMemory(int level) {
            if (level >= TRIM_MEMORY_BACKGROUND) {
                ImageLoader loader = sInstance;
                if (loader != null) {
                    loader.clearMemoryCache();
                }
            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            // Do nothing
        }

        @Override
        public void onLowMemory() {
            ImageLoader loader = sInstance;
            if (loader != null) {
                loader.clearMemoryCache();
            }
        }
    }
}
