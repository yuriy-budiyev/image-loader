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

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

abstract class BaseLoadImageAction<T> {
    private final Context mContext;
    private final DataDescriptor<T> mDescriptor;
    private final Size mRequiredSize;
    private final CacheMode mCacheMode;
    private final BitmapLoader<T> mBitmapLoader;
    private final BitmapTransformation mTransformation;
    private final PauseLock mPauseLock;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final LoadCallback<T> mLoadCallback;
    private final ErrorCallback<T> mErrorCallback;
    private volatile Future<?> mFuture;
    private volatile boolean mCancelled;
    private volatile boolean mCalled;

    protected BaseLoadImageAction(@NonNull Context context, @NonNull DataDescriptor<T> descriptor,
            @Nullable Size requiredSize, @Nullable CacheMode cacheMode, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable BitmapTransformation transformation, @Nullable ImageCache memoryCache,
            @Nullable ImageCache storageCache, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback, @NonNull PauseLock pauseLock) {
        mContext = context;
        mDescriptor = descriptor;
        mRequiredSize = requiredSize;
        if (cacheMode == null) {
            cacheMode = descriptor.getCacheMode();
            if (cacheMode == null) {
                cacheMode = CacheMode.FULL;
            }
        }
        mCacheMode = cacheMode;
        mBitmapLoader = bitmapLoader;
        mTransformation = transformation;
        mPauseLock = pauseLock;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
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

    @Nullable
    protected final Size getRequiredSize() {
        return mRequiredSize;
    }

    @NonNull
    protected final CacheMode getCacheMode() {
        return mCacheMode;
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
        Context context = mContext;
        DataDescriptor<T> descriptor = mDescriptor;
        CacheMode cacheMode = mCacheMode;
        String key = descriptor.getKey();
        Size requiredSize = mRequiredSize;
        if (requiredSize != null) {
            key += "_sampled_" + requiredSize.getWidth() + "x" + requiredSize.getHeight();
        }
        T data = descriptor.getData();
        Bitmap image = null;
        // Memory cache
        ImageCache memoryCache = mMemoryCache;
        if (cacheMode.isMemoryCacheEnabled() && key != null && memoryCache != null) {
            BitmapTransformation transformation = mTransformation;
            if (transformation != null) {
                image = memoryCache.get(key + transformation.getKey());
            }
            if (image != null) {
                processImage(context, descriptor, image, true);
                return;
            }
            image = memoryCache.get(key);
            if (image != null) {
                processImage(context, descriptor, image, false);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        // Storage cache
        ImageCache storageCache = mStorageCache;
        boolean storageCachingEnabled = cacheMode.isStorageCacheEnabled();
        if (storageCachingEnabled && key != null && storageCache != null) {
            image = storageCache.get(key);
            if (image != null) {
                processImage(context, descriptor, image, false);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        // Load new image
        try {
            image = mBitmapLoader.load(context, data, requiredSize);
        } catch (Throwable error) {
            processError(context, data, error);
            return;
        }
        if (image == null) {
            processError(context, data, new ImageNotLoadedException());
            return;
        }
        processImage(context, descriptor, image, false);
        if (mCancelled) {
            return;
        }
        if (storageCachingEnabled && key != null && storageCache != null) {
            storageCache.put(key, image);
        }
    }

    @WorkerThread
    private void processImage(@NonNull Context context, @NonNull DataDescriptor<T> descriptor, @NonNull Bitmap image,
            boolean transformed) {
        if (mCancelled) {
            return;
        }
        T data = descriptor.getData();
        String key = descriptor.getKey();
        BitmapTransformation transformation = mTransformation;
        boolean memoryCachingEnabled = mCacheMode.isMemoryCacheEnabled();
        if (!transformed && transformation != null) {
            if (memoryCachingEnabled && key != null) {
                key += transformation.getKey();
            }
            try {
                image = transformation.transform(context, image);
            } catch (Throwable error) {
                processError(context, data, error);
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        if (!transformed) {
            // transformed == true also means that image was taken from memory cache
            ImageCache memoryCache = mMemoryCache;
            if (memoryCachingEnabled && key != null && memoryCache != null) {
                memoryCache.put(key, image);
            }
        }
        LoadCallback<T> loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(context, data, image);
        }
        onImageLoaded(image);
    }

    @WorkerThread
    private void processError(@NonNull Context context, @NonNull T data, @NonNull Throwable error) {
        if (mCancelled) {
            return;
        }
        ErrorCallback<T> errorCallback = mErrorCallback;
        if (errorCallback != null) {
            errorCallback.onError(context, data, error);
        }
        onError(error);
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
