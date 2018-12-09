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

import java.util.concurrent.ExecutorService;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

abstract class LoadImageAction<T> extends BaseAction {
    private final DataDescriptor<T> mDescriptor;
    private final BitmapLoader<T> mBitmapLoader;
    private final Size mRequiredSize;
    private final BitmapTransformation mTransformation;
    private final PauseLock mPauseLock;
    private final MemoryImageCache mMemoryCache;
    private final StorageImageCache mStorageCache;
    private final ExecutorService mCacheExecutor;
    private final LoadCallback mLoadCallback;
    private final ErrorCallback mErrorCallback;
    private volatile ImageRequestDelegate mCacheDelegate;

    protected LoadImageAction(@NonNull final DataDescriptor<T> descriptor,
            @NonNull final BitmapLoader<T> bitmapLoader, @Nullable final Size requiredSize,
            @Nullable final BitmapTransformation transformation,
            @Nullable final MemoryImageCache memoryCache,
            @Nullable final StorageImageCache storageCache,
            @Nullable final ExecutorService cacheExecutor,
            @Nullable final LoadCallback loadCallback, @Nullable final ErrorCallback errorCallback,
            @NonNull final PauseLock pauseLock) {
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

    @Override
    protected void onCancelled() {
        final ImageRequestDelegate delegate = mCacheDelegate;
        if (delegate != null) {
            delegate.cancel();
        }
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
    protected final MemoryImageCache getMemoryCache() {
        return mMemoryCache;
    }

    @Nullable
    protected final StorageImageCache getStorageCache() {
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

    @Override
    @WorkerThread
    protected final void execute() {
        while (!isCancelled() && !mPauseLock.shouldInterruptEarly() && mPauseLock.isPaused()) {
            try {
                mPauseLock.await();
            } catch (final InterruptedException e) {
                return;
            }
        }
        if (isCancelled() || mPauseLock.shouldInterruptEarly()) {
            return;
        }
        final DataDescriptor<T> descriptor = mDescriptor;
        final String key = getKey();
        final T data = descriptor.getData();
        Bitmap image;
        // Memory cache
        final MemoryImageCache memoryCache = mMemoryCache;
        if (key != null && memoryCache != null) {
            image = memoryCache.get(key);
            if (image != null) {
                processImage(image);
                return;
            }
        }
        if (isCancelled()) {
            return;
        }
        // Storage cache
        final StorageImageCache storageCache = mStorageCache;
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
        if (isCancelled()) {
            return;
        }
        // Load new image
        final Size requiredSize = mRequiredSize;
        try {
            image = mBitmapLoader.load(data, requiredSize);
        } catch (final Throwable error) {
            processError(error);
            return;
        }
        if (image == null) {
            processError(new ImageNotLoadedException());
            return;
        }
        if (isCancelled()) {
            return;
        }
        // Transform image
        final BitmapTransformation transformation = mTransformation;
        if (transformation != null) {
            try {
                final Bitmap transformed = transformation.transform(image);
                if (image != transformed && !image.isRecycled()) {
                    image.recycle();
                }
                image = transformed;
            } catch (final Throwable error) {
                processError(error);
                return;
            }
        }
        if (isCancelled()) {
            return;
        }
        processImage(image);
        if (key != null) {
            if (memoryCache != null) {
                memoryCache.put(key, image);
            }
            if (storageCache != null && (requiredSize != null || transformation != null ||
                    descriptor.getLocation() != DataLocation.LOCAL)) {
                final ExecutorService cacheExecutor = mCacheExecutor;
                if (cacheExecutor != null) {
                    mCacheDelegate = new CacheImageOnStorageAction(key, image, storageCache)
                            .submit(cacheExecutor);
                } else {
                    storageCache.put(key, image);
                }
            }
        }
    }

    @WorkerThread
    private void processImage(@NonNull final Bitmap image) {
        final LoadCallback loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(image);
        }
        onImageLoaded(image);
    }

    @WorkerThread
    private void processError(@NonNull final Throwable error) {
        final ErrorCallback errorCallback = mErrorCallback;
        if (errorCallback != null) {
            errorCallback.onError(error);
        }
        onError(error);
    }
}
