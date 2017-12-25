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

import java.util.concurrent.ExecutorService;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Image loader is a universal tool for loading bitmaps efficiently in Android
 *
 * @see #with
 * @see #builder
 */
public final class ImageLoader {
    private final BitmapLoaders mBitmapLoaders = new BitmapLoaders();
    private final DataDescriptors mDataDescriptors = new DataDescriptors();
    private final PauseLock mPauseLock = new PauseLock();
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final ExecutorService mExecutor;

    /**
     * Image Loader
     *
     * @see #with
     * @see #builder
     */
    ImageLoader(@NonNull Context context, @NonNull ExecutorService executor, @Nullable ImageCache memoryCache,
            @Nullable ImageCache storageCache) {
        mContext = context;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        mExecutor = executor;
        if (storageCache instanceof StorageImageCache) {
            ((StorageImageCache) storageCache).setExecutor(mExecutor);
        }
    }

    /**
     * Create new load image request
     *
     * @return Source data type selector
     */
    @NonNull
    public RequestDataTypeSelector request() {
        return new RequestDataTypeSelector(mContext, mExecutor, mPauseLock, mMainThreadHandler, mBitmapLoaders,
                mDataDescriptors, mMemoryCache, mStorageCache);
    }

    /**
     * Create new load image request
     *
     * @param loader Bitmap loader for specified data type
     * @return New load image request
     */
    @NonNull
    public <T> ImageRequest<T> request(@NonNull BitmapLoader<T> loader) {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                loader, mDataDescriptors.<T>common());
    }

    /**
     * Create new load image request
     *
     * @param loader  Bitmap loader
     * @param factory Data descriptor factory
     * @return New load image request
     */
    @NonNull
    public <T> ImageRequest<T> request(@NonNull BitmapLoader<T> loader, @NonNull DataDescriptorFactory<T> factory) {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                loader, factory);
    }

    /**
     * Delete cached image for specified {@link DataDescriptor}
     *
     * @see DataDescriptor
     */
    public void invalidate(@NonNull DataDescriptor<?> descriptor) {
        String key = descriptor.getKey();
        if (key == null) {
            return;
        }
        ImageCache memoryCache = mMemoryCache;
        if (memoryCache != null && descriptor.isMemoryCachingEnabled()) {
            memoryCache.remove(key);
        }
        ImageCache storageCache = mStorageCache;
        if (storageCache != null && descriptor.isStorageCachingEnabled()) {
            storageCache.remove(key);
        }
    }

    /**
     * Whether loading is currently paused
     */
    public boolean isLoadingPaused() {
        return mPauseLock.isPaused();
    }

    /**
     * Whether to pause image loading. If this method is invoked with {@code true} parameter,
     * all loading actions will be paused until it will be invoked with {@code false}.
     */
    public void setPauseLoading(boolean paused) {
        mPauseLock.setPaused(paused);
    }

    /**
     * Set all loading tasks to finish before any loading actions started
     */
    public void setInterruptLoadingEarly(boolean interrupt) {
        mPauseLock.setInterruptEarly(interrupt);
    }

    /**
     * Clear memory cache;
     * for better memory management when one singleton loader instance used across the app,
     * this method should be called in {@link Application#onTrimMemory)} or {@link ComponentCallbacks2#onTrimMemory},
     * default instance ({@link #with}) automatically cares about it
     *
     * @see ComponentCallbacks2
     * @see Context#registerComponentCallbacks
     */
    public void clearMemoryCache() {
        ImageCache memoryCache = mMemoryCache;
        if (memoryCache != null) {
            memoryCache.clear();
        }
    }

    /**
     * Clear storage cache
     */
    public void clearStorageCache() {
        ImageCache storageCache = mStorageCache;
        if (storageCache != null) {
            storageCache.clear();
        }
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        clearMemoryCache();
        clearStorageCache();
    }

    /**
     * Get default image loader instance,
     * automatically cares about memory and storage caching
     */
    @NonNull
    public static ImageLoader with(@NonNull Context context) {
        return ImageLoaderHolder.get(context);
    }

    /**
     * Create new image loader builder instance
     */
    @NonNull
    public static ImageLoaderBuilder builder(@NonNull Context context) {
        return new ImageLoaderBuilder(context);
    }
}
