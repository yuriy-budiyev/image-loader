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

import java.io.File;
import java.io.FileDescriptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.net.Uri;
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
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final Handler mMainThreadHandler;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final PauseLock mPauseLock = new PauseLock();
    private final Map<String, DataDescriptorFactory<Object>> mDescriptorFactories = new ConcurrentHashMap<>();
    private final Map<String, BitmapLoader<Object>> mBitmapLoaders = new ConcurrentHashMap<>();

    /**
     * Image Loader
     *
     * @see #with
     * @see #builder
     */
    ImageLoader(@NonNull Context context, @NonNull ExecutorService executor, @Nullable ImageCache memoryCache,
            @Nullable ImageCache storageCache) {
        mContext = context;
        mExecutor = executor;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        registerDataType(Uri.class, new UriDataDescriptorFactory(), new UriBitmapLoader());
        registerDataType(File.class, new FileDataDescriptorFactory(), new FileBitmapLoader());
        registerDataType(String.class, new UrlDataDescriptorFactory(), new UrlBitmapLoader());
        registerDataType(FileDescriptor.class, new UnidentifiableDataDescriptorFactory<FileDescriptor>(),
                new FileDescriptorBitmapLoader());
        registerDataType(Integer.class, new ResourceDataDescriptorFactory(), new ResourceBitmapLoader());
        registerDataType(byte[].class, new UnidentifiableDataDescriptorFactory<byte[]>(), new ByteArrayBitmapLoader());
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public <T> ImageRequest<T> from(@NonNull T data) {
        String dataClassName = data.getClass().getName();
        DataDescriptorFactory<T> descriptorFactory = (DataDescriptorFactory<T>) mDescriptorFactories.get(dataClassName);
        BitmapLoader<T> bitmapLoader = (BitmapLoader<T>) mBitmapLoaders.get(dataClassName);
        if (descriptorFactory == null || bitmapLoader == null) {
            throw new IllegalArgumentException("Unsupported data type: " + dataClassName);
        }
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                bitmapLoader, descriptorFactory, data);
    }

    public void invalidate(@NonNull Object data) {
        invalidate(data, null);
    }

    public void invalidate(@NonNull Object data, @Nullable Size size) {
        String dataClassName = data.getClass().getName();
        DataDescriptorFactory<Object> descriptorFactory = mDescriptorFactories.get(dataClassName);
        if (descriptorFactory == null) {
            throw new IllegalArgumentException("Unsupported data type: " + dataClassName);
        }
        invalidate(descriptorFactory.newDescriptor(data));
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

    @SuppressWarnings("unchecked")
    public <T> void registerDataType(@NonNull Class<T> dataClass, @NonNull DataDescriptorFactory<T> descriptorFactory,
            @NonNull BitmapLoader<T> bitmapLoader) {
        String dataClassName = dataClass.getName();
        mDescriptorFactories.put(dataClassName, (DataDescriptorFactory<Object>) descriptorFactory);
        mBitmapLoaders.put(dataClassName, (BitmapLoader<Object>) bitmapLoader);
    }

    public void unregisterDataType(@NonNull Class<?> dataClass) {
        String dataClassName = dataClass.getName();
        mDescriptorFactories.remove(dataClassName);
        mBitmapLoaders.remove(dataClassName);
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
