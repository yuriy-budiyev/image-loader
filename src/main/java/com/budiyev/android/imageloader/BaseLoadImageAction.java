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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import android.graphics.Bitmap;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

abstract class BaseLoadImageAction<T> {
    private final DataDescriptor<T> mDescriptor;
    private final BitmapLoader<T> mBitmapLoader;
    private final Size mRequiredSize;
    private final BitmapTransformation mTransformation;
    private final PauseLock mPauseLock;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final ExecutorService mCacheExecutor;
    private final LoadCallback mLoadCallback;
    private final ErrorCallback mErrorCallback;
    private volatile Future<?> mLoadFuture;
    private volatile CacheImageAction mCacheAction;
    private volatile boolean mCancelled;

    protected BaseLoadImageAction(@NonNull DataDescriptor<T> descriptor, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable Size requiredSize, @Nullable BitmapTransformation transformation,
            @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache,
            @Nullable ExecutorService cacheExecutor, @Nullable LoadCallback loadCallback,
            @Nullable ErrorCallback errorCallback, @NonNull PauseLock pauseLock) {
        mDescriptor = descriptor;
        mBitmapLoader = bitmapLoader;
        mRequiredSize = requiredSize;
        mTransformation = transformation;
        mPauseLock = pauseLock;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        mCacheExecutor = cacheExecutor;
        mLoadCallback = loadCallback;
        mErrorCallback = errorCallback;
    }

    @WorkerThread
    protected abstract void onImageLoaded(@NonNull Bitmap image);

    @WorkerThread
    protected abstract void onError(@NonNull Throwable error);

    @AnyThread
    protected abstract void onCancelled();

    @AnyThread
    public final void submit(@NonNull ExecutorService executor) {
        if (mCancelled) {
            return;
        }
        mLoadFuture = executor.submit(new LoadImageTask());
    }

    @AnyThread
    public final void cancel() {
        mCancelled = true;
        Future<?> loadFuture = mLoadFuture;
        if (loadFuture != null) {
            loadFuture.cancel(false);
        }
        CacheImageAction cacheAction = mCacheAction;
        if (cacheAction != null) {
            cacheAction.cancel();
        }
        onCancelled();
    }

    @NonNull
    protected final DataDescriptor<T> getDescriptor() {
        return mDescriptor;
    }

    @Nullable
    protected final String getKey() {
        return InternalUtils.buildFullKey(mDescriptor.getKey(), mRequiredSize, mTransformation);
    }

    @Nullable
    protected final Size getRequiredSize() {
        return mRequiredSize;
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
    protected final ImageCache getMemoryCache() {
        return mMemoryCache;
    }

    @Nullable
    protected final ImageCache getStorageCache() {
        return mStorageCache;
    }

    @Nullable
    protected final LoadCallback getLoadCallback() {
        return mLoadCallback;
    }

    @Nullable
    protected final ErrorCallback getErrorCallback() {
        return mErrorCallback;
    }

    protected final boolean isCancelled() {
        return mCancelled;
    }

    @WorkerThread
    protected final void loadImage() {
        while (!mCancelled && !mPauseLock.shouldInterruptEarly() && mPauseLock.isPaused()) {
            try {
                mPauseLock.await();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (mCancelled || mPauseLock.shouldInterruptEarly()) {
            return;
        }
        DataDescriptor<T> descriptor = mDescriptor;
        String key = getKey();
        T data = descriptor.getData();
        Bitmap image;
        // Memory cache
        ImageCache memoryCache = mMemoryCache;
        if (key != null && memoryCache != null) {
            image = memoryCache.get(key);
            if (image != null) {
                processImage(image);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        // Storage cache
        ImageCache storageCache = mStorageCache;
        if (key != null && storageCache != null) {
            image = storageCache.get(key);
            if (image != null) {
                processImage(image);
                if (memoryCache != null) {
                    memoryCache.put(key, image);
                }
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        // Load new image
        Size requiredSize = mRequiredSize;
        try {
            image = mBitmapLoader.load(data, requiredSize);
        } catch (Throwable error) {
            processError(error);
            return;
        }
        if (image == null) {
            processError(new ImageNotLoadedException());
            return;
        }
        // Transform image
        BitmapTransformation transformation = mTransformation;
        if (transformation != null) {
            try {
                Bitmap transformed = transformation.transform(image);
                if (image != transformed && !image.isRecycled()) {
                    image.recycle();
                }
                image = transformed;
            } catch (Throwable error) {
                processError(error);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        processImage(image);
        if (key != null) {
            if (memoryCache != null) {
                memoryCache.put(key, image);
            }
            if (storageCache != null && (requiredSize != null || transformation != null ||
                    descriptor.getLocation() != DataLocation.LOCAL)) {
                ExecutorService cacheExecutor = mCacheExecutor;
                if (cacheExecutor != null) {
                    mCacheAction = new CacheImageAction(key, image, storageCache).submit(cacheExecutor);
                } else {
                    storageCache.put(key, image);
                }
            }
        }
    }

    @WorkerThread
    private void processImage(@NonNull Bitmap image) {
        LoadCallback loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(image);
        }
        onImageLoaded(image);
    }

    @WorkerThread
    private void processError(@NonNull Throwable error) {
        if (mCancelled) {
            return;
        }
        ErrorCallback errorCallback = mErrorCallback;
        if (errorCallback != null) {
            errorCallback.onError(error);
        }
        onError(error);
    }

    private final class LoadImageTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            loadImage();
            mLoadFuture = null;
            return null;
        }
    }

}
