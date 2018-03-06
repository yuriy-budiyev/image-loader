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

import java.io.File;
import java.io.FileDescriptor;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Image loader builder
 */
public final class ImageLoaderBuilder {
    private final Context mContext;
    private ImageCache mMemoryCache;
    private ImageCache mStorageCache;
    private ExecutorService mLoadExecutor;
    private ExecutorService mCacheExecutor;

    ImageLoaderBuilder(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Default memory cache
     */
    @NonNull
    public ImageLoaderBuilder memoryCache() {
        mMemoryCache = new MemoryImageCache();
        return this;
    }

    /**
     * Memory cache with specified maximum size
     */
    @NonNull
    public ImageLoaderBuilder memoryCache(@IntRange(from = 0) int maxSize) {
        mMemoryCache = new MemoryImageCache(maxSize);
        return this;
    }

    /**
     * Custom memory cache
     */
    @NonNull
    public ImageLoaderBuilder memoryCache(@Nullable ImageCache memoryCache) {
        mMemoryCache = memoryCache;
        return this;
    }

    /**
     * Default storage cache,
     * located in subdirectory of {@link Context#getExternalCacheDir}
     */
    @NonNull
    public ImageLoaderBuilder storageCache() {
        mStorageCache = new StorageImageCache(mContext);
        return this;
    }

    /**
     * Default storage cache with specified maximum size,
     * located in subdirectory of {@link Context#getExternalCacheDir}
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@IntRange(from = 0L) long maxSize) {
        mStorageCache = new StorageImageCache(mContext, maxSize);
        return this;
    }

    /**
     * Default storage cache with specified maximum size and compress mode,
     * located in subdirectory of {@link Context#getExternalCacheDir}
     *
     * @see CompressMode
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull CompressMode compressMode, @IntRange(from = 0L) long maxSize) {
        mStorageCache = new StorageImageCache(mContext, compressMode, maxSize);
        return this;
    }

    /**
     * Storage cache with specified directory
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull File directory) {
        mStorageCache = new StorageImageCache(directory);
        return this;
    }

    /**
     * Storage cache with specified directory and maximum size
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull File directory, @IntRange(from = 0L) long maxSize) {
        mStorageCache = new StorageImageCache(directory, maxSize);
        return this;
    }

    /**
     * Storage cache with specified directory, maximum size and compress mode
     *
     * @see CompressMode
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull File directory, @NonNull CompressMode compressMode, long maxSize) {
        mStorageCache = new StorageImageCache(directory, compressMode, maxSize);
        return this;
    }

    /**
     * Custom storage cache
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@Nullable ImageCache storageCache) {
        mStorageCache = storageCache;
        return this;
    }

    /**
     * Custom load executor
     */
    @NonNull
    public ImageLoaderBuilder loadExecutor(@Nullable ExecutorService executor) {
        mLoadExecutor = executor;
        return this;
    }

    /**
     * Custom storage cache executor
     */
    @NonNull
    public ImageLoaderBuilder cacheExecutor(@Nullable ExecutorService executor) {
        mCacheExecutor = executor;
        return this;
    }

    /**
     * Create new image loader instance with specified parameters
     */
    @NonNull
    public ImageLoader build() {
        ExecutorService loadExecutor = mLoadExecutor;
        if (loadExecutor == null) {
            loadExecutor = new ImageLoaderExecutor(InternalUtils.getLoadPoolSize());
        }
        ExecutorService cacheExecutor = mCacheExecutor;
        if (cacheExecutor == null) {
            cacheExecutor = new ImageLoaderExecutor(InternalUtils.getCachePoolSize());
        }
        Context context = mContext;
        ImageLoader loader = new ImageLoader(context, loadExecutor, cacheExecutor, mMemoryCache, mStorageCache);
        loader.registerDataType(Uri.class, new UriDataDescriptorFactory(), new UriBitmapLoader(context));
        loader.registerDataType(File.class, new FileDataDescriptorFactory(), new FileBitmapLoader());
        loader.registerDataType(String.class, new StringUriDataDescriptorFactory(), new StringUriBitmapLoader(context));
        loader.registerDataType(Integer.class, new ResourceDataDescriptorFactory(), new ResourceBitmapLoader(context));
        loader.registerDataType(FileDescriptor.class, new FileDescriptorDataDescriptorFactory(),
                new FileDescriptorBitmapLoader());
        loader.registerDataType(byte[].class, new ByteArrayDataDescriptorFactory(), new ByteArrayBitmapLoader());
        return loader;
    }
}
