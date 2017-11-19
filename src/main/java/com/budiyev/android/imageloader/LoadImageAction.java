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

class LoadImageAction<T, R extends LoadRequestInternal<T>> {
    private final Context mContext;
    private final R mRequest;
    private final PauseLock mPauseLock;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private volatile Future<?> mFuture;
    private volatile boolean mCancelled;
    private volatile boolean mCalled;

    public LoadImageAction(@NonNull Context context, @NonNull R request,
            @NonNull PauseLock pauseLock, @Nullable ImageCache memoryCache,
            @Nullable ImageCache storageCache) {
        mContext = context;
        mRequest = request;
        mPauseLock = pauseLock;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
    }

    @WorkerThread
    protected void onImageLoaded(@NonNull Bitmap image) {
        // Default implementation
    }

    @WorkerThread
    protected void onError(@NonNull Throwable error) {
        // Default implementation
    }

    @AnyThread
    protected void onCancelled() {
        // Default implementation
    }

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
    public R getRequest() {
        return mRequest;
    }

    @NonNull
    protected final PauseLock getPauseLock() {
        return mPauseLock;
    }

    @Nullable
    protected final ImageCache getMemoryCache() {
        return mMemoryCache;
    }

    @Nullable
    protected final ImageCache getStorageCache() {
        return mStorageCache;
    }

    protected final boolean isCancelled() {
        return mCancelled;
    }

    protected void processImage(@NonNull Context context, @NonNull T data, @NonNull Bitmap image) {
        if (mCancelled) {
            return;
        }
        LoadCallback<T> loadCallback = mRequest.getLoadCallback();
        if (loadCallback != null) {
            loadCallback.onLoaded(context, data, image);
        }
        onImageLoaded(image);
    }

    protected void processError(@NonNull Context context, @NonNull T data,
            @NonNull Throwable error) {
        if (mCancelled) {
            return;
        }
        ErrorCallback<T> errorCallback = mRequest.getErrorCallback();
        if (errorCallback != null) {
            errorCallback.onError(context, data, error);
        }
        onError(error);
    }

    @WorkerThread
    private void loadImage() {
        while (!mCancelled && !mPauseLock.shouldInterruptEarly() && mPauseLock.isPaused()) {
            if (mPauseLock.await()) {
                return;
            }
        }
        if (mCancelled || mPauseLock.shouldInterruptEarly()) {
            return;
        }
        Context context = mContext;
        String key = mRequest.getDescriptor().getKey();
        T data = mRequest.getDescriptor().getData();
        Bitmap image;
        // Memory cache
        ImageCache memoryCache = mMemoryCache;
        if (memoryCache != null) {
            image = memoryCache.get(key);
            if (image != null) {
                processImage(context, data, image);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        // Storage cache
        ImageCache storageCache = mStorageCache;
        if (storageCache != null) {
            image = storageCache.get(key);
            if (image != null) {
                if (memoryCache != null) {
                    memoryCache.put(key, image);
                }
                processImage(context, data, image);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        // Load new image
        try {
            image = mRequest.getBitmapLoader().load(context, data);
        } catch (Throwable error) {
            processError(context, data, error);
            return;
        }
        if (image == null) {
            processError(context, data, new ImageNotLoadedException());
            return;
        }
        if (memoryCache != null) {
            memoryCache.put(key, image);
        }
        processImage(context, data, image);
        if (mCancelled) {
            return;
        }
        if (storageCache != null) {
            storageCache.put(key, image);
        }
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
