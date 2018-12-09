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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Image loader builder
 */
public final class ImageLoaderBuilder {
    private final Context mContext;
    private MemoryImageCache mMemoryCache;
    private StorageImageCache mStorageCache;
    private ExecutorService mLoadExecutor;
    private ExecutorService mCacheExecutor;

    ImageLoaderBuilder(@NonNull final Context context) {
        mContext = context;
    }

    /**
     * Default memory cache
     */
    @NonNull
    public ImageLoaderBuilder memoryCache() {
        mMemoryCache = new MemoryImageCacheImpl();
        return this;
    }

    /**
     * Memory cache with specified maximum size
     */
    @NonNull
    public ImageLoaderBuilder memoryCache(@IntRange(from = 0) final int maxSize) {
        mMemoryCache = new MemoryImageCacheImpl(maxSize);
        return this;
    }

    /**
     * Custom memory cache
     */
    @NonNull
    public ImageLoaderBuilder memoryCache(@Nullable final MemoryImageCache memoryCache) {
        mMemoryCache = memoryCache;
        return this;
    }

    /**
     * Default storage cache,
     * located in subdirectory of {@link Context#getExternalCacheDir}
     */
    @NonNull
    public ImageLoaderBuilder storageCache() {
        mStorageCache = new StorageImageCacheImpl(mContext);
        return this;
    }

    /**
     * Default storage cache with specified maximum size,
     * located in subdirectory of {@link Context#getExternalCacheDir}
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@IntRange(from = 0L) final long maxSize) {
        mStorageCache = new StorageImageCacheImpl(mContext, maxSize);
        return this;
    }

    /**
     * Default storage cache with specified maximum size and compress mode,
     * located in subdirectory of {@link Context#getExternalCacheDir}
     *
     * @see CompressMode
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull final CompressMode compressMode,
            @IntRange(from = 0L) final long maxSize) {
        mStorageCache = new StorageImageCacheImpl(mContext, compressMode, maxSize);
        return this;
    }

    /**
     * Storage cache with specified directory
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull final File directory) {
        mStorageCache = new StorageImageCacheImpl(directory);
        return this;
    }

    /**
     * Storage cache with specified directory and maximum size
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull final File directory,
            @IntRange(from = 0L) final long maxSize) {
        mStorageCache = new StorageImageCacheImpl(directory, maxSize);
        return this;
    }

    /**
     * Storage cache with specified directory, maximum size and compress mode
     *
     * @see CompressMode
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@NonNull final File directory,
            @NonNull final CompressMode compressMode, final long maxSize) {
        mStorageCache = new StorageImageCacheImpl(directory, compressMode, maxSize);
        return this;
    }

    /**
     * Custom storage cache
     */
    @NonNull
    public ImageLoaderBuilder storageCache(@Nullable final StorageImageCache storageCache) {
        mStorageCache = storageCache;
        return this;
    }

    /**
     * Custom load executor
     */
    @NonNull
    public ImageLoaderBuilder loadExecutor(@Nullable final ExecutorService executor) {
        mLoadExecutor = executor;
        return this;
    }

    /**
     * Custom storage cache executor
     */
    @NonNull
    public ImageLoaderBuilder cacheExecutor(@Nullable final ExecutorService executor) {
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
        final Context context = mContext;
        final ImageLoader loader =
                new ImageLoader(context, loadExecutor, cacheExecutor, mMemoryCache, mStorageCache);
        loader.registerDataType(Uri.class, new UriDataDescriptorFactory(),
                new UriBitmapLoader(context));
        loader.registerDataType(File.class, new FileDataDescriptorFactory(),
                new FileBitmapLoader());
        loader.registerDataType(String.class, new StringUriDataDescriptorFactory(),
                new StringUriBitmapLoader(context));
        loader.registerDataType(Integer.class, new ResourceDataDescriptorFactory(),
                new ResourceBitmapLoader(context));
        loader.registerDataType(FileDescriptor.class, new FileDescriptorDataDescriptorFactory(),
                new FileDescriptorBitmapLoader());
        loader.registerDataType(byte[].class, new ByteArrayDataDescriptorFactory(),
                new ByteArrayBitmapLoader());
        return loader;
    }
}
