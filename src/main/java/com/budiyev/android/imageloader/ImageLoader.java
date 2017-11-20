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
import java.util.concurrent.ExecutorService;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Image loader is a universal tool for loading bitmaps efficiently in Android
 *
 * @see Builder
 */
public final class ImageLoader {
    private final PauseLock mPauseLock = new PauseLock();
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final ExecutorService mExecutor;

    /**
     * @see Builder
     */
    private ImageLoader(@NonNull Context context, @Nullable ImageCache memoryCache,
            @Nullable ImageCache storageCache, @Nullable ExecutorService executor) {
        mContext = context;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        if (executor != null) {
            mExecutor = executor;
        } else {
            mExecutor = new ImageLoaderExecutor(InternalUtils.getPoolSize());
        }
        if (mStorageCache instanceof StorageImageCache) {
            ((StorageImageCache) mStorageCache).setExecutor(mExecutor);
        }
    }

    /**
     * Create new load image request
     *
     * @param loader Bitmap loader for specified data type
     * @return New load image request
     */
    @NonNull
    @AnyThread
    public <T> LoadImageRequest<T> request(@NonNull BitmapLoader<T> loader) {
        return new LoadImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                mMemoryCache, mStorageCache, loader);
    }

    /**
     * Create new load image request
     *
     * @return Source data type selector
     */
    @NonNull
    @AnyThread
    public DataTypeSelector request() {
        return new DataTypeSelector(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                mMemoryCache, mStorageCache);
    }

    /**
     * Delete cached image for specified {@link DataDescriptor}
     */
    public void invalidate(@NonNull DataDescriptor<?> descriptor) {
        String key = descriptor.getKey();
        ImageCache memoryCache = mMemoryCache;
        if (memoryCache != null) {
            memoryCache.remove(key);
        }
        ImageCache storageCache = mStorageCache;
        if (storageCache != null) {
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
     * this method should be called in {@link Application#onTrimMemory(int)}
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
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    public static final class DataTypeSelector {
        private final Context mContext;
        private final ExecutorService mExecutor;
        private final PauseLock mPauseLock;
        private final Handler mMainThreadHandler;
        private final ImageCache mMemoryCache;
        private final ImageCache mStorageCache;

        private DataTypeSelector(@NonNull Context context, @NonNull ExecutorService executor,
                @NonNull PauseLock pauseLock, @NonNull Handler mainThreadHandler,
                @NonNull ImageCache memoryCache, @NonNull ImageCache storageCache) {
            mContext = context;
            mExecutor = executor;
            mPauseLock = pauseLock;
            mMainThreadHandler = mainThreadHandler;
            mMemoryCache = memoryCache;
            mStorageCache = storageCache;
        }

        /**
         * {@link Uri} image load request
         */
        @NonNull
        public LoadImageRequest<Uri> uri() {
            return new LoadImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                    mMemoryCache, mStorageCache, LoaderCache.getUriBitmapLoader());
        }

        /**
         * {@link File} image load request
         */
        @NonNull
        public LoadImageRequest<File> file() {
            return new LoadImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                    mMemoryCache, mStorageCache, LoaderCache.getFileBitmapLoader());
        }

        /**
         * {@link FileDescriptor} image load request
         */
        @NonNull
        public LoadImageRequest<FileDescriptor> fileDescriptor() {
            return new LoadImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                    mMemoryCache, mStorageCache, LoaderCache.getFileDescriptorBitmapLoader());
        }

        /**
         * Resource image load request
         */
        @NonNull
        public LoadImageRequest<Integer> resource() {
            return new LoadImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                    mMemoryCache, mStorageCache, LoaderCache.getResourceBitmapLoader());
        }

        /**
         * Byte array image load request
         */
        @NonNull
        public LoadImageRequest<byte[]> byteArray() {
            return new LoadImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler,
                    mMemoryCache, mStorageCache, LoaderCache.getByteArrayBitmapLoader());
        }
    }

    /**
     * Image loader builder
     */
    public static final class Builder {
        private final Context mContext;
        private ImageCache mMemoryCache;
        private ImageCache mStorageCache;
        private ExecutorService mExecutor;

        private Builder(@NonNull Context context) {
            mContext = context;
        }

        /**
         * Default memory cache
         */
        @NonNull
        public Builder memoryCache() {
            mMemoryCache = new MemoryImageCache();
            return this;
        }

        /**
         * Memory cache with specified maximum size
         */
        @NonNull
        public Builder memoryCache(int maxSize) {
            mMemoryCache = new MemoryImageCache(maxSize);
            return this;
        }

        /**
         * Custom memory cache
         */
        @NonNull
        public Builder memoryCache(@Nullable ImageCache memoryCache) {
            mMemoryCache = memoryCache;
            return this;
        }

        /**
         * Default storage cache,
         * located in subdirectory of {@link Context#getExternalCacheDir()}
         */
        @NonNull
        public Builder storageCache() {
            mStorageCache = new StorageImageCache(mContext);
            return this;
        }

        /**
         * Default storage cache with specified maximum size,
         * located in subdirectory of {@link Context#getExternalCacheDir()}
         */
        @NonNull
        public Builder storageCache(long maxSize) {
            mStorageCache = new StorageImageCache(mContext, maxSize);
            return this;
        }

        /**
         * Default storage cache with specified maximum size and compress mode,
         * located in subdirectory of {@link Context#getExternalCacheDir()}
         *
         * @see CompressMode
         */
        @NonNull
        public Builder storageCache(@NonNull CompressMode compressMode, long maxSize) {
            mStorageCache = new StorageImageCache(mContext, compressMode, maxSize);
            return this;
        }

        /**
         * Storage cache with specified directory
         */
        @NonNull
        public Builder storageCache(@NonNull File directory) {
            mStorageCache = new StorageImageCache(directory);
            return this;
        }

        /**
         * Storage cache with specified directory and maximum size
         */
        @NonNull
        public Builder storageCache(@NonNull File directory, long maxSize) {
            mStorageCache = new StorageImageCache(directory, maxSize);
            return this;
        }

        /**
         * Storage cache with specified directory, maximum size and compress mode
         *
         * @see CompressMode
         */
        @NonNull
        public Builder storageCache(@NonNull File directory, @NonNull CompressMode compressMode,
                long maxSize) {
            mStorageCache = new StorageImageCache(directory, compressMode, maxSize);
            return this;
        }

        /**
         * Custom storage cache
         */
        @NonNull
        public Builder storageCache(@Nullable ImageCache storageCache) {
            mStorageCache = storageCache;
            return this;
        }

        /**
         * Custom executor
         */
        @NonNull
        public Builder executor(@Nullable ExecutorService executor) {
            mExecutor = executor;
            return this;
        }

        /**
         * Create new image loader instance with specified parameters
         */
        @NonNull
        public ImageLoader build() {
            return new ImageLoader(mContext, mMemoryCache, mStorageCache, mExecutor);
        }
    }
}
