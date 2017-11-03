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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

final class LoadImageAction<T> {
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final BitmapLoader<T> mBitmapLoader;
    private final ImageCache mStorageCache;
    private final LoadCallback<T> mLoadCallback;
    private final ErrorCallback<T> mErrorCallback;
    private final DataDescriptor<T> mDescriptor;

    public LoadImageAction(@NonNull Context context, @NonNull ExecutorService executor,
            @NonNull PauseLock pauseLock, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable ImageCache storageCache, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback, @NonNull DataDescriptor<T> descriptor) {
        mContext = context;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mBitmapLoader = bitmapLoader;
        mStorageCache = storageCache;
        mLoadCallback = loadCallback;
        mErrorCallback = errorCallback;
        mDescriptor = descriptor;
    }

    public void execute() {
        mExecutor.submit(new LoadImageTask());
    }

    @WorkerThread
    private void loadImage() {
        while (mPauseLock.isPaused()) {
            if (mPauseLock.await()) {
                return;
            }
        }
        Bitmap image = null;
        ImageCache storageCache = mStorageCache;
        String key = mDescriptor.getKey();
        T data = mDescriptor.getData();
        if (storageCache != null) {
            image = storageCache.get(key);
        }
        if (image == null) {
            try {
                image = mBitmapLoader.load(mContext, data);
            } catch (Throwable error) {
                ErrorCallback<T> errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(mContext, data, error);
                }
                return;
            }
            if (image == null) {
                ErrorCallback<T> errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(mContext, data, new ImageNotLoadedException());
                }
                return;
            }
            if (storageCache != null) {
                storageCache.put(key, image);
            }
        }
        LoadCallback<T> loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(mContext, data, image);
        }
    }

    private final class LoadImageTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            loadImage();
            return null;
        }
    }
}
