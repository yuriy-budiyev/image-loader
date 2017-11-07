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
import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

abstract class BaseLoadImageAction<T> {
    private final Context mContext;
    private final DataDescriptor<T> mDescriptor;
    private final BitmapLoader<T> mBitmapLoader;
    private final PauseLock mPauseLock;
    private final ImageCache mStorageCache;
    private final LoadCallback<T> mLoadCallback;
    private final ErrorCallback<T> mErrorCallback;
    private volatile Future<?> mFuture;
    private volatile boolean mCancelled;
    private volatile boolean mCalled;

    protected BaseLoadImageAction(@NonNull Context context, @NonNull DataDescriptor<T> descriptor,
            @NonNull BitmapLoader<T> bitmapLoader, @NonNull PauseLock pauseLock,
            @Nullable ImageCache storageCache, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback) {
        mContext = context;
        mDescriptor = descriptor;
        mBitmapLoader = bitmapLoader;
        mPauseLock = pauseLock;
        mStorageCache = storageCache;
        mLoadCallback = loadCallback;
        mErrorCallback = errorCallback;
    }

    @WorkerThread
    protected abstract void onImageLoaded(@NonNull Bitmap image);

    @AnyThread
    protected abstract void onCancelled();

    @AnyThread
    public final void execute(@NonNull ExecutorService executor) {
        if (mCalled) {
            throw new IllegalStateException("Action can be executed only once");
        }
        mCalled = true;
        if (mCancelled) {
            return;
        }
        mFuture = executor.submit(new LoadImageTask());
    }

    @AnyThread
    public final void cancel() {
        mCancelled = true;
        Future<?> future = mFuture;
        if (future != null) {
            future.cancel(false);
        }
        onCancelled();
    }

    @NonNull
    protected final Context getContext() {
        return mContext;
    }

    @NonNull
    protected final DataDescriptor<T> getDescriptor() {
        return mDescriptor;
    }

    @NonNull
    protected final BitmapLoader<T> getBitmapLoader() {
        return mBitmapLoader;
    }

    @NonNull
    protected final PauseLock getPauseLock() {
        return mPauseLock;
    }

    @Nullable
    protected final ImageCache getStorageCache() {
        return mStorageCache;
    }

    @Nullable
    protected final LoadCallback<T> getLoadCallback() {
        return mLoadCallback;
    }

    @Nullable
    protected final ErrorCallback<T> getErrorCallback() {
        return mErrorCallback;
    }

    protected final boolean isCancelled() {
        return mCancelled;
    }

    @WorkerThread
    private void loadImage() {
        while (!mCancelled && mPauseLock.isPaused()) {
            if (mPauseLock.await()) {
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        Bitmap image = null;
        ImageCache storageCache = mStorageCache;
        String key = mDescriptor.getKey();
        T data = mDescriptor.getData();
        if (storageCache != null) {
            image = storageCache.get(key);
        }
        Context context = mContext;
        if (image == null) {
            try {
                image = mBitmapLoader.load(context, data);
            } catch (Throwable error) {
                ErrorCallback<T> errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(context, data, error);
                }
                return;
            }
            if (image == null) {
                ErrorCallback<T> errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(context, data, new ImageNotLoadedException());
                }
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        if (storageCache != null) {
            storageCache.put(key, image);
        }
        if (mCancelled) {
            return;
        }
        LoadCallback<T> loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(context, data, image);
        }
        onImageLoaded(image);
    }

    private final class LoadImageTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            loadImage();
            mFuture = null;
            return null;
        }
    }
}
