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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.WorkerThread;
import android.view.View;

/**
 * Image request
 * <br>
 * Note that all methods of this class should be called on the same thread as {@link ImageLoader#from} method
 * that created this request
 */
public final class ImageRequest<T> {
    private static final long DEFAULT_FADE_DURATION = 200L;
    private static final int TRANSFORMATIONS_CAPACITY = 4;
    private final Resources mResources;
    private final ExecutorService mLoadExecutor;
    private final ExecutorService mCacheExecutor;
    private final PauseLock mPauseLock;
    private final Handler mMainThreadHandler;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final BitmapLoader<T> mBitmapLoader;
    private final DataDescriptor<T> mDescriptor;
    private Size mRequiredSize;
    private LoadCallback mLoadCallback;
    private ErrorCallback mErrorCallback;
    private DisplayCallback mDisplayCallback;
    private List<BitmapTransformation> mTransformations;
    private Drawable mPlaceholder;
    private Drawable mErrorDrawable;
    private boolean mFadeEnabled = true;
    private long mFadeDuration = DEFAULT_FADE_DURATION;
    private float mCornerRadius;
    private boolean mMemoryCacheEnabled = true;
    private boolean mStorageCacheEnabled = true;

    ImageRequest(@NonNull Resources resources, @NonNull ExecutorService loadExecutor,
            @NonNull ExecutorService cacheExecutor, @NonNull PauseLock pauseLock, @NonNull Handler mainThreadHandler,
            @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache, @NonNull BitmapLoader<T> bitmapLoader,
            @NonNull DataDescriptor<T> descriptor) {
        mResources = resources;
        mLoadExecutor = loadExecutor;
        mCacheExecutor = cacheExecutor;
        mPauseLock = pauseLock;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        mBitmapLoader = bitmapLoader;
        mMainThreadHandler = mainThreadHandler;
        mDescriptor = descriptor;
    }

    /**
     * Required image size
     */
    @NonNull
    public ImageRequest<T> size(@Nullable Size requiredSize) {
        mRequiredSize = requiredSize;
        return this;
    }

    /**
     * Required image size
     */
    @NonNull
    public ImageRequest<T> size(@Px int requiredWidth, @Px int requiredHeight) {
        mRequiredSize = new Size(requiredWidth, requiredHeight);
        return this;
    }

    /**
     * Display image with rounded corners using maximum corner radius,
     * for square image, will lead to circle result
     */
    @NonNull
    public ImageRequest<T> roundCorners() {
        mCornerRadius = RoundedDrawable.MAX_RADIUS;
        return this;
    }

    /**
     * Display image with rounded corners using specified corner radius (in pixels),
     * zero means that rounding is disabled; note that visible rounding depends on image size
     * and image view scale type
     */
    @NonNull
    public ImageRequest<T> roundCorners(@FloatRange(from = 0f, to = Float.MAX_VALUE) float radius) {
        mCornerRadius = radius;
        return this;
    }

    /**
     * Placeholder
     */
    @NonNull
    public ImageRequest<T> placeholder(@Nullable Drawable placeholder) {
        mPlaceholder = placeholder;
        return this;
    }

    /**
     * Placeholder
     */
    @NonNull
    public ImageRequest<T> placeholder(@DrawableRes int resId) {
        mPlaceholder = mResources.getDrawable(resId);
        return this;
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    @NonNull
    public ImageRequest<T> errorDrawable(@Nullable Drawable errorDrawable) {
        mErrorDrawable = errorDrawable;
        return this;
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    @NonNull
    public ImageRequest<T> errorDrawable(@DrawableRes int resId) {
        mErrorDrawable = mResources.getDrawable(resId);
        return this;
    }

    /**
     * Add bitmap transformation
     *
     * @see ImageUtils
     * @see BitmapTransformation
     */
    @NonNull
    public ImageRequest<T> transform(@NonNull BitmapTransformation transformation) {
        transformations().add(transformation);
        return this;
    }

    /**
     * Add bitmap transformations
     *
     * @see ImageUtils
     * @see BitmapTransformation
     */
    @NonNull
    public ImageRequest<T> transform(@NonNull Collection<BitmapTransformation> transformations) {
        transformations().addAll(transformations);
        return this;
    }

    /**
     * Add bitmap transformations
     *
     * @see ImageUtils
     * @see BitmapTransformation
     */
    @NonNull
    public ImageRequest<T> transform(@NonNull BitmapTransformation... transformations) {
        Collections.addAll(transformations(), transformations);
        return this;
    }

    /**
     * Enable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    @NonNull
    public ImageRequest<T> fade() {
        mFadeEnabled = true;
        mFadeDuration = DEFAULT_FADE_DURATION;
        return this;
    }

    /**
     * Disable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    @NonNull
    public ImageRequest<T> noFade() {
        mFadeEnabled = false;
        return this;
    }

    /**
     * Enable fade effect for images that isn't cached in memory,
     * allows to specify fade effect duration,
     * supported on API 19+
     */
    @NonNull
    public ImageRequest<T> fade(long duration) {
        mFadeEnabled = true;
        mFadeDuration = duration;
        return this;
    }

    /**
     * Load callback
     */
    @NonNull
    public ImageRequest<T> onLoaded(@Nullable LoadCallback callback) {
        mLoadCallback = callback;
        return this;
    }

    /**
     * Error callback
     */
    @NonNull
    public ImageRequest<T> onError(@Nullable ErrorCallback callback) {
        mErrorCallback = callback;
        return this;
    }

    /**
     * Display callback
     */
    @NonNull
    public ImageRequest<T> onDisplayed(@Nullable DisplayCallback callback) {
        mDisplayCallback = callback;
        return this;
    }

    /**
     * Whether to use memory cache
     */
    @NonNull
    public ImageRequest<T> memoryCache(boolean enabled) {
        mMemoryCacheEnabled = enabled;
        return this;
    }

    /**
     * Whether to use storage cache
     */
    @NonNull
    public ImageRequest<T> storageCache(boolean enabled) {
        mStorageCacheEnabled = enabled;
        return this;
    }

    /**
     * Load image synchronously (on current thread)
     *
     * @return Loaded image or {@code null} if image could not be loaded
     */
    @Nullable
    @WorkerThread
    public Bitmap loadSync() {
        return new SyncLoadImageAction<>(mDescriptor, mBitmapLoader, mRequiredSize, getTransformation(),
                getMemoryCache(), getStorageCache(), mLoadCallback, mErrorCallback, mPauseLock).executeSync();
    }

    /**
     * Load image asynchronously
     *
     * @see #onLoaded
     * @see LoadCallback
     */
    @AnyThread
    public void load() {
        new LoadImageAction<>(mLoadExecutor, mCacheExecutor, mDescriptor, mBitmapLoader, mRequiredSize,
                getTransformation(), getMemoryCache(), getStorageCache(), mLoadCallback, mErrorCallback, mPauseLock)
                .execute();
    }

    /**
     * Load image asynchronously and display it in the specified {@code view}
     */
    @MainThread
    public void load(@NonNull View view) {
        Resources resources = mResources;
        DataDescriptor<T> descriptor = mDescriptor;
        Size requiredSize = mRequiredSize;
        BitmapTransformation transformation = getTransformation();
        LoadCallback loadCallback = mLoadCallback;
        DisplayCallback displayCallback = mDisplayCallback;
        ImageCache memoryCache = getMemoryCache();
        float cornerRadius = mCornerRadius;
        Bitmap image = null;
        String key = InternalUtils.buildFullKey(descriptor.getKey(), requiredSize, transformation);
        if (key != null && memoryCache != null) {
            image = memoryCache.get(key);
        }
        if (image != null) {
            if (loadCallback != null) {
                loadCallback.onLoaded(image);
            }
            if (cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS) {
                InternalUtils.setDrawable(new RoundedDrawable(resources, image, cornerRadius), view);
            } else {
                InternalUtils.setBitmap(resources, image, view);
            }
            if (displayCallback != null) {
                displayCallback.onDisplayed(image, view);
            }
            return;
        }
        DisplayImageAction<?> currentAction = InternalUtils.getDisplayImageAction(view);
        if (currentAction != null) {
            if (currentAction.hasSameKey(key)) {
                return;
            }
            currentAction.cancel();
        }
        Drawable placeholder = mPlaceholder;
        if (placeholder == null) {
            placeholder = new ColorDrawable(Color.TRANSPARENT);
        }
        DisplayImageAction<T> action =
                new DisplayImageAction<>(mLoadExecutor, mCacheExecutor, resources, view, descriptor, mBitmapLoader,
                        requiredSize, transformation, placeholder, mErrorDrawable, memoryCache, getStorageCache(),
                        loadCallback, mErrorCallback, displayCallback, mPauseLock, mMainThreadHandler, mFadeEnabled,
                        mFadeDuration, cornerRadius);
        InternalUtils.setDrawable(new PlaceholderDrawable(placeholder, action), view);
        action.execute();
    }

    /**
     * Delete all cached images for specified data asynchronously
     */
    @AnyThread
    public void invalidate() {
        mLoadExecutor.submit(new InvalidateAction(mDescriptor, getMemoryCache(), getStorageCache()));
    }

    @NonNull
    private List<BitmapTransformation> transformations() {
        List<BitmapTransformation> t = mTransformations;
        if (t == null) {
            t = new ArrayList<>(TRANSFORMATIONS_CAPACITY);
            mTransformations = t;
        }
        return t;
    }

    @Nullable
    private BitmapTransformation getTransformation() {
        List<BitmapTransformation> t = mTransformations;
        if (t != null && !t.isEmpty()) {
            if (t.size() == 1) {
                return t.get(0);
            } else {
                return new BitmapTransformationGroup(t);
            }
        } else {
            return null;
        }
    }

    @Nullable
    private ImageCache getMemoryCache() {
        return mMemoryCacheEnabled ? mMemoryCache : null;
    }

    @Nullable
    private ImageCache getStorageCache() {
        return mStorageCacheEnabled ? mStorageCache : null;
    }

}
