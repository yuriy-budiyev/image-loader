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
import java.util.concurrent.ExecutorService;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
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
     * Load image into view from specified {@link DataDescriptor}
     *
     * @param descriptor      Source data descriptor
     * @param view            Image view
     * @param transformation  Bitmap transformation
     * @param loadCallback    Load callback
     * @param errorCallback   Error callback
     * @param displayCallback Display callback
     * @param fadeEnabled     Whether to enable or disable fade effect
     * @param fadeDuration    Duration of fade effect if it's enabled
     * @see DataDescriptor
     * @see DataUtils#descriptor(Object)
     */
    /*@MainThread
    public void load(@NonNull DataDescriptor<T> descriptor, @NonNull ImageView view,
            @Nullable BitmapTransformation transformation, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback, @Nullable DisplayCallback<T> displayCallback,
            boolean fadeEnabled, long fadeDuration, float cornerRadius) {
        Bitmap image = null;
        String key = descriptor.getKey();
        ImageCache memoryCache = mMemoryCache;
        if (memoryCache != null) {
            if (transformation != null) {
                image = memoryCache.get(key + transformation.getKey());
            } else {
                image = memoryCache.get(key);
            }
        }
        T data = descriptor.getData();
        Context context = mContext;
        if (image != null) {
            if (loadCallback != null) {
                loadCallback.onLoaded(context, data, image);
            }
            if (cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS) {
                view.setImageDrawable(
                        new RoundedDrawable(context.getResources(), image, cornerRadius));
            } else {
                view.setImageBitmap(image);
            }
            if (displayCallback != null) {
                displayCallback.onDisplayed(context, data, image, view);
            }
            return;
        }
        DisplayImageAction<?> currentAction = InternalUtils.getDisplayImageAction(view);
        if (currentAction != null) {
            if (currentAction.hasSameDescriptor(key)) {
                return;
            }
            currentAction.cancel();
        }
        Drawable placeholder;
        if (placeholderProvider != null) {
            placeholder = placeholderProvider.getPlaceholder(context, data);
        } else {
            placeholder = new ColorDrawable(Color.TRANSPARENT);
        }
        Drawable errorDrawable;
        if (errorDrawableProvider != null) {
            errorDrawable = errorDrawableProvider.getErrorDrawable(context, data);
        } else {
            errorDrawable = null;
        }
        DisplayImageAction<T> action =
                new DisplayImageAction<>(context, descriptor, mBitmapLoader, mPauseLock,
                        mStorageCache, loadCallback, errorCallback, mMainThreadHandler,
                        transformation, memoryCache, displayCallback, view, placeholder,
                        errorDrawable, fadeEnabled, fadeDuration, cornerRadius);
        view.setImageDrawable(new PlaceholderDrawable(placeholder, action));
        action.execute(mExecutor);
    }*/

    /**
     * Load image from specified {@link DataDescriptor},
     * override callbacks, specified in builder
     *
     * @param descriptor    Source data descriptor
     * @param loadCallback  Load callback
     * @param errorCallback Error callback
     * @see DataDescriptor
     * @see DataUtils#descriptor(Object)
     */
/*    @AnyThread
    public void load(@NonNull DataDescriptor<T> descriptor, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback) {
        new LoadImageAction<>(mContext, descriptor, mBitmapLoader, mPauseLock, mMemoryCache,
                mStorageCache, loadCallback, errorCallback).execute(mExecutor);
    }*/

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

    void runOnMainThread(@NonNull Runnable action) {
        mMainThreadHandler.post(action);
    }

    /**
     * Create new image loader builder instance
     */
    @NonNull
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
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
