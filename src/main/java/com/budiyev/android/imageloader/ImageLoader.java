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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.DrawableRes;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

/**
 * Image loader is a universal tool for loading bitmaps efficiently in Android
 *
 * @see #with(Context)
 * @see Builder
 */
public final class ImageLoader<T> {
    private final PauseLock mPauseLock = new PauseLock();
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final BitmapLoader<T> mBitmapLoader;
    private final BitmapTransformation<T> mBitmapTransformation;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final PlaceholderProvider<T> mPlaceholderProvider;
    private final ErrorDrawableProvider<T> mErrorDrawableProvider;
    private final ExecutorService mExecutor;
    private final LoadCallback<T> mLoadCallback;
    private final DisplayCallback<T> mDisplayCallback;
    private final ErrorCallback<T> mErrorCallback;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;

    /**
     * @see #with(Context)
     * @see Builder
     */
    private ImageLoader(@NonNull Context context, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable BitmapTransformation<T> bitmapTransformation,
            @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache,
            @Nullable PlaceholderProvider<T> placeholderProvider,
            @Nullable ErrorDrawableProvider<T> errorDrawableProvider,
            @Nullable ExecutorService executor, @Nullable LoadCallback<T> loadCallback,
            @Nullable DisplayCallback<T> displayCallback, @Nullable ErrorCallback<T> errorCallback,
            boolean fadeEnabled, long fadeDuration) {
        mContext = context;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mBitmapLoader = bitmapLoader;
        mBitmapTransformation = bitmapTransformation;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        mErrorDrawableProvider = errorDrawableProvider;
        mLoadCallback = loadCallback;
        mDisplayCallback = displayCallback;
        mErrorCallback = errorCallback;
        mPlaceholderProvider = placeholderProvider;
        if (executor != null) {
            mExecutor = executor;
        } else {
            mExecutor = new ImageLoaderExecutor(InternalUtils.getPoolSize());
        }
        if (mStorageCache instanceof StorageImageCache) {
            ((StorageImageCache) mStorageCache).setExecutor(mExecutor);
        }
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
    }

    /**
     * Load image into view from specified {@link DataDescriptor}
     *
     * @param descriptor Source data descriptor
     * @param view       Image view
     */
    @MainThread
    public void load(@NonNull DataDescriptor<T> descriptor, @NonNull ImageView view) {
        load(descriptor, view, mBitmapTransformation, mLoadCallback, mErrorCallback,
                mDisplayCallback, mFadeEnabled, mFadeDuration);
    }

    /**
     * Load image into view from specified {@link DataDescriptor}
     *
     * @param descriptor     Source data descriptor
     * @param view           Image view
     * @param transformation Bitmap transformation
     */
    @MainThread
    public void load(@NonNull DataDescriptor<T> descriptor, @NonNull ImageView view,
            @Nullable BitmapTransformation<T> transformation) {
        load(descriptor, view, transformation, mLoadCallback, mErrorCallback, mDisplayCallback,
                mFadeEnabled, mFadeDuration);
    }

    /**
     * Load image into view from specified {@link DataDescriptor}
     *
     * @param descriptor     Source data descriptor
     * @param view           Image view
     * @param transformation Bitmap transformation
     */
    @MainThread
    public void load(@NonNull DataDescriptor<T> descriptor, @NonNull ImageView view,
            @Nullable BitmapTransformation<T> transformation,
            @Nullable LoadCallback<T> loadCallback, @Nullable ErrorCallback<T> errorCallback,
            @Nullable DisplayCallback<T> displayCallback) {
        load(descriptor, view, transformation, loadCallback, errorCallback, displayCallback,
                mFadeEnabled, mFadeDuration);
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
     */
    @MainThread
    public void load(@NonNull DataDescriptor<T> descriptor, @NonNull ImageView view,
            @Nullable BitmapTransformation<T> transformation,
            @Nullable LoadCallback<T> loadCallback, @Nullable ErrorCallback<T> errorCallback,
            @Nullable DisplayCallback<T> displayCallback, boolean fadeEnabled, long fadeDuration) {
        Bitmap image = null;
        String key = descriptor.getKey();
        ImageCache memoryCache = mMemoryCache;
        T data = descriptor.getData();
        if (memoryCache != null) {
            if (transformation != null) {
                image = memoryCache.get(key + transformation.getKey(data));
            } else {
                image = memoryCache.get(key);
            }
        }
        Context context = mContext;
        if (image != null) {
            if (loadCallback != null) {
                loadCallback.onLoaded(context, data, image);
            }
            view.setImageBitmap(image);
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
        PlaceholderProvider<T> placeholderProvider = mPlaceholderProvider;
        if (placeholderProvider != null) {
            placeholder = placeholderProvider.getPlaceholder(context, data);
        } else {
            placeholder = new ColorDrawable(Color.TRANSPARENT);
        }
        Drawable errorDrawable;
        ErrorDrawableProvider<T> errorDrawableProvider = mErrorDrawableProvider;
        if (errorDrawableProvider != null) {
            errorDrawable = errorDrawableProvider.getErrorDrawable(context, data);
        } else {
            errorDrawable = null;
        }
        DisplayImageAction<T> action =
                new DisplayImageAction<>(context, descriptor, mBitmapLoader, mPauseLock,
                        mStorageCache, loadCallback, errorCallback, mMainThreadHandler,
                        transformation, memoryCache, displayCallback, view, placeholder,
                        errorDrawable, fadeEnabled, fadeDuration);
        view.setImageDrawable(new PlaceholderDrawable(placeholder, action));
        action.execute(mExecutor);
    }

    /**
     * Load image from specified {@link DataDescriptor}
     *
     * @param descriptor Source data descriptor
     */
    @AnyThread
    public void load(@NonNull DataDescriptor<T> descriptor) {
        load(descriptor, mLoadCallback, mErrorCallback);
    }

    /**
     * Load image from specified {@link DataDescriptor},
     * override callbacks, specified in builder
     *
     * @param descriptor    Source data descriptor
     * @param loadCallback  Load callback
     * @param errorCallback Error callback
     */
    @AnyThread
    public void load(@NonNull DataDescriptor<T> descriptor, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback) {
        new LoadImageAction<>(mContext, descriptor, mBitmapLoader, mPauseLock, mMemoryCache,
                mStorageCache, loadCallback, errorCallback).execute(mExecutor);
    }

    /**
     * Delete cached image for specified {@link DataDescriptor}
     */
    public void invalidate(@NonNull DataDescriptor<T> descriptor) {
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
     * Bitmap transformation, specified in builder
     */
    @Nullable
    public BitmapTransformation<T> getBitmapTransformation() {
        return mBitmapTransformation;
    }

    /**
     * Placeholder provider, specified in builder
     */
    @Nullable
    public PlaceholderProvider<T> getPlaceholderProvider() {
        return mPlaceholderProvider;
    }

    /**
     * Error drawable provider, specified in builder
     */
    @Nullable
    public ErrorDrawableProvider<T> getErrorDrawableProvider() {
        return mErrorDrawableProvider;
    }

    /**
     * Load callback, specified in builder
     */
    @Nullable
    public LoadCallback<T> getLoadCallback() {
        return mLoadCallback;
    }

    /**
     * Display callback, specified in builder
     */
    @Nullable
    public DisplayCallback<T> getDisplayCallback() {
        return mDisplayCallback;
    }

    /**
     * Error callback, specified in builder
     */
    @Nullable
    public ErrorCallback<T> getErrorCallback() {
        return mErrorCallback;
    }

    /**
     * Whether if fade effect is enabled for images that isn't cached in memory
     */
    public boolean isFadeEnabled() {
        return mFadeEnabled;
    }

    /**
     * Fade effect duration in milliseconds
     */
    public long getFadeDuration() {
        return mFadeDuration;
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
     * Create new image load request
     *
     * @param context Context
     * @see LoadImageRequest
     */
    @NonNull
    @AnyThread
    public static LoadImageRequest with(@NonNull Context context) {
        return new LoadImageRequest(context);
    }

    /**
     * Create new image loader builder instance
     */
    @NonNull
    public static BuilderSelector builder(@NonNull Context context) {
        return new BuilderSelector(context);
    }

    /**
     * Image loader builder type selector
     */
    public static final class BuilderSelector {
        private final Context mContext;

        private BuilderSelector(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        /**
         * Common {@link Uri} image loader builder
         */
        @NonNull
        public Builder<Uri> uri() {
            return new Builder<>(mContext, new UriBitmapLoader());
        }

        /**
         * Common {@link File} image loader builder
         */
        @NonNull
        public Builder<File> file() {
            return new Builder<>(mContext, new FileBitmapLoader());
        }

        /**
         * Common {@link FileDescriptor} image loader builder
         */
        @NonNull
        public Builder<FileDescriptor> fileDescriptor() {
            return new Builder<>(mContext, new FileDescriptorBitmapLoader());
        }

        /**
         * Common resource image loader builder
         */
        @NonNull
        public Builder<Integer> resource() {
            return new Builder<>(mContext, new ResourceBitmapLoader());
        }

        /**
         * Common byte array image loader builder
         */
        @NonNull
        public Builder<byte[]> byteArray() {
            return new Builder<>(mContext, new ByteArrayBitmapLoader());
        }

        /**
         * Custom image loader builder
         *
         * @see DataDescriptor
         * @see BitmapLoader
         */
        @NonNull
        public <T> Builder<T> custom(@NonNull BitmapLoader<T> bitmapLoader) {
            return new Builder<>(mContext, bitmapLoader);
        }
    }

    /**
     * Image loader builder
     */
    public static final class Builder<T> {
        private final Context mContext;
        private final BitmapLoader<T> mBitmapLoader;
        private BitmapTransformation<T> mBitmapTransformation;
        private ImageCache mMemoryCache;
        private ImageCache mStorageCache;
        private PlaceholderProvider<T> mPlaceholderProvider;
        private ErrorDrawableProvider<T> mErrorDrawableProvider;
        private ExecutorService mExecutor;
        private LoadCallback<T> mLoadCallback;
        private DisplayCallback<T> mDisplayCallback;
        private ErrorCallback<T> mErrorCallback;
        private boolean mFadeEnabled = true;
        private long mFadeDuration = 200L;

        private Builder(@NonNull Context context, @NonNull BitmapLoader<T> bitmapLoader) {
            mContext = context;
            mBitmapLoader = bitmapLoader;
        }

        /**
         * Default memory cache
         */
        @NonNull
        public Builder<T> memoryCache() {
            mMemoryCache = new MemoryImageCache();
            return this;
        }

        /**
         * Memory cache with specified maximum size
         */
        @NonNull
        public Builder<T> memoryCache(int maxSize) {
            mMemoryCache = new MemoryImageCache(maxSize);
            return this;
        }

        /**
         * Custom memory cache
         */
        @NonNull
        public Builder<T> memoryCache(@Nullable ImageCache memoryCache) {
            mMemoryCache = memoryCache;
            return this;
        }

        /**
         * Default storage cache,
         * located in subdirectory of {@link Context#getExternalCacheDir()}
         */
        @NonNull
        public Builder<T> storageCache() {
            mStorageCache = new StorageImageCache(mContext);
            return this;
        }

        /**
         * Default storage cache with specified maximum size,
         * located in subdirectory of {@link Context#getExternalCacheDir()}
         */
        @NonNull
        public Builder<T> storageCache(long maxSize) {
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
        public Builder<T> storageCache(@NonNull CompressMode compressMode, long maxSize) {
            mStorageCache = new StorageImageCache(mContext, compressMode, maxSize);
            return this;
        }

        /**
         * Storage cache with specified directory
         */
        @NonNull
        public Builder<T> storageCache(@NonNull File directory) {
            mStorageCache = new StorageImageCache(directory);
            return this;
        }

        /**
         * Storage cache with specified directory and maximum size
         */
        @NonNull
        public Builder<T> storageCache(@NonNull File directory, long maxSize) {
            mStorageCache = new StorageImageCache(directory, maxSize);
            return this;
        }

        /**
         * Storage cache with specified directory, maximum size and compress mode
         *
         * @see CompressMode
         */
        @NonNull
        public Builder<T> storageCache(@NonNull File directory, @NonNull CompressMode compressMode,
                long maxSize) {
            mStorageCache = new StorageImageCache(directory, compressMode, maxSize);
            return this;
        }

        /**
         * Custom storage cache
         */
        @NonNull
        public Builder<T> storageCache(@Nullable ImageCache storageCache) {
            mStorageCache = storageCache;
            return this;
        }

        /**
         * Image placeholder
         */
        @NonNull
        public Builder<T> placeholder(@NonNull Bitmap image) {
            mPlaceholderProvider = new ImagePlaceholderProvider<>(mContext.getResources(), image);
            return this;
        }

        /**
         * Resource placeholder
         */
        @NonNull
        public Builder<T> placeholder(@DrawableRes int imageRes) {
            Resources resources = mContext.getResources();
            Bitmap image = BitmapFactory.decodeResource(resources, imageRes);
            if (image != null) {
                mPlaceholderProvider = new ImagePlaceholderProvider<>(resources, image);
            }
            return this;
        }

        /**
         * Custom placeholder
         */
        @NonNull
        public Builder<T> placeholder(@Nullable PlaceholderProvider<T> provider) {
            mPlaceholderProvider = provider;
            return this;
        }

        @NonNull
        public Builder<T> errorDrawable(@Nullable ErrorDrawableProvider<T> provider) {
            mErrorDrawableProvider = provider;
            return this;
        }

        /**
         * Bitmap transformation, processes bitmap before showing it
         *
         * @see BitmapTransformation
         */
        @NonNull
        public Builder<T> transform(@Nullable BitmapTransformation<T> processor) {
            mBitmapTransformation = processor;
            return this;
        }

        /**
         * Whether to enable fade effect for images that isn't cached in memory,
         * supported on API 19+
         */
        @NonNull
        public Builder<T> fade(boolean enabled) {
            mFadeEnabled = enabled;
            return this;
        }

        /**
         * Whether to enable fade effect for images that isn't cached in memory,
         * allows to specify fade effect duration,
         * supported on API 19+
         */
        @NonNull
        public Builder<T> fade(boolean enabled, long duration) {
            mFadeEnabled = enabled;
            mFadeDuration = duration;
            return this;
        }

        /**
         * Custom executor
         */
        @NonNull
        public Builder<T> executor(@Nullable ExecutorService executor) {
            mExecutor = executor;
            return this;
        }

        /**
         * Load callback
         */
        @NonNull
        public Builder<T> onLoaded(@Nullable LoadCallback<T> callback) {
            mLoadCallback = callback;
            return this;
        }

        /**
         * Error callback
         */
        @NonNull
        public Builder<T> onError(@Nullable ErrorCallback<T> callback) {
            mErrorCallback = callback;
            return this;
        }

        /**
         * Display callback
         */
        @NonNull
        public Builder<T> onDisplayed(@Nullable DisplayCallback<T> callback) {
            mDisplayCallback = callback;
            return this;
        }

        /**
         * Create new image loader instance with specified parameters
         */
        @NonNull
        public ImageLoader<T> build() {
            return new ImageLoader<>(mContext, mBitmapLoader, mBitmapTransformation, mMemoryCache,
                    mStorageCache, mPlaceholderProvider, mErrorDrawableProvider, mExecutor,
                    mLoadCallback, mDisplayCallback, mErrorCallback, mFadeEnabled, mFadeDuration);
        }
    }
}
