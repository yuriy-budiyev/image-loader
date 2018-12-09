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
import android.view.View;

import androidx.annotation.AnyThread;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.WorkerThread;

/**
 * Image request
 * <br>
 * Note that all methods of this class should be called on the same thread as {@link ImageLoader#from} method
 * that created this request, each request can be executed only once
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
    private boolean mExecuted;

    ImageRequest(@NonNull final Resources resources, @NonNull final ExecutorService loadExecutor,
            @NonNull final ExecutorService cacheExecutor, @NonNull final PauseLock pauseLock,
            @NonNull final Handler mainThreadHandler, @Nullable final ImageCache memoryCache,
            @Nullable final ImageCache storageCache, @NonNull final BitmapLoader<T> bitmapLoader,
            @NonNull final DataDescriptor<T> descriptor) {
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
    public ImageRequest<T> size(@Nullable final Size requiredSize) {
        if (requiredSize != null) {
            checkSize(requiredSize.getWidth(), requiredSize.getHeight());
        }
        mRequiredSize = requiredSize;
        return this;
    }

    /**
     * Required image size
     */
    @NonNull
    public ImageRequest<T> size(@Px final int requiredWidth, @Px final int requiredHeight) {
        checkSize(requiredWidth, requiredHeight);
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
    public ImageRequest<T> roundCorners(
            @FloatRange(from = 0f, to = Float.MAX_VALUE) final float radius) {
        if (radius < 0f) {
            throw new IllegalArgumentException(
                    "Corner radius should be greater than or equal to zero");
        }
        mCornerRadius = radius;
        return this;
    }

    /**
     * Placeholder
     */
    @NonNull
    public ImageRequest<T> placeholder(@Nullable final Drawable placeholder) {
        mPlaceholder = placeholder;
        return this;
    }

    /**
     * Placeholder
     */
    @NonNull
    public ImageRequest<T> placeholder(@DrawableRes final int resId) {
        mPlaceholder = mResources.getDrawable(resId);
        return this;
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    @NonNull
    public ImageRequest<T> errorDrawable(@Nullable final Drawable errorDrawable) {
        mErrorDrawable = errorDrawable;
        return this;
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    @NonNull
    public ImageRequest<T> errorDrawable(@DrawableRes final int resId) {
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
    public ImageRequest<T> transform(@NonNull final BitmapTransformation transformation) {
        transformations().add(InternalUtils.requireNonNull(transformation));
        return this;
    }

    /**
     * Add bitmap transformations
     *
     * @see ImageUtils
     * @see BitmapTransformation
     */
    @NonNull
    public ImageRequest<T> transform(
            @NonNull final Collection<BitmapTransformation> transformations) {
        transformations().addAll(InternalUtils.requireNonNull(transformations));
        return this;
    }

    /**
     * Add bitmap transformations
     *
     * @see ImageUtils
     * @see BitmapTransformation
     */
    @NonNull
    public ImageRequest<T> transform(@NonNull final BitmapTransformation... transformations) {
        Collections.addAll(transformations(), InternalUtils.requireNonNull(transformations));
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
    public ImageRequest<T> fade(@IntRange(from = 0L) final long duration) {
        if (duration < 0L) {
            throw new IllegalArgumentException(
                    "Fade duration should be greater than or equal to zero");
        }
        mFadeEnabled = true;
        mFadeDuration = duration;
        return this;
    }

    /**
     * Load callback
     */
    @NonNull
    public ImageRequest<T> onLoaded(@Nullable final LoadCallback callback) {
        mLoadCallback = callback;
        return this;
    }

    /**
     * Error callback
     */
    @NonNull
    public ImageRequest<T> onError(@Nullable final ErrorCallback callback) {
        mErrorCallback = callback;
        return this;
    }

    /**
     * Display callback
     */
    @NonNull
    public ImageRequest<T> onDisplayed(@Nullable final DisplayCallback callback) {
        mDisplayCallback = callback;
        return this;
    }

    /**
     * Don't use memory cache in this request
     */
    @NonNull
    public ImageRequest<T> noMemoryCache() {
        mMemoryCacheEnabled = false;
        return this;
    }

    /**
     * Don't use storage cache in this request
     */
    @NonNull
    public ImageRequest<T> noStorageCache() {
        mStorageCacheEnabled = false;
        return this;
    }

    /**
     * Load image synchronously (on current thread)
     *
     * @return Loaded image or {@code null} if image could not be loaded
     * @throws IllegalStateException if request has already been executed
     */
    @Nullable
    @WorkerThread
    public Bitmap loadSync() {
        checkAndSetExecutedState();
        return new SyncLoadImageAction<>(mDescriptor, mBitmapLoader, mRequiredSize,
                getTransformation(), getMemoryCache(), getStorageCache(), mLoadCallback,
                mErrorCallback, mPauseLock).load();
    }

    /**
     * Load image asynchronously
     *
     * @return {@link ImageRequestDelegate} object, associated with execution of the request
     * @throws IllegalStateException if request has already been executed
     * @see #onLoaded
     * @see LoadCallback
     */
    @NonNull
    @AnyThread
    public ImageRequestDelegate load() {
        checkAndSetExecutedState();
        return new AsyncLoadImageAction<>(mDescriptor, mBitmapLoader, mRequiredSize,
                getTransformation(), getMemoryCache(), getStorageCache(), mCacheExecutor,
                mLoadCallback, mErrorCallback, mPauseLock).submit(mLoadExecutor);
    }

    /**
     * Load image asynchronously and display it into the specified {@code view}
     *
     * @return {@link ImageRequestDelegate} object, associated with execution of the request
     * @throws IllegalStateException if request has already been executed
     */
    @NonNull
    @MainThread
    public ImageRequestDelegate load(@NonNull final View view) {
        checkAndSetExecutedState();
        final Resources resources = mResources;
        final DataDescriptor<T> descriptor = mDescriptor;
        final Size requiredSize = mRequiredSize;
        final BitmapTransformation transformation = getTransformation();
        final LoadCallback loadCallback = mLoadCallback;
        final DisplayCallback displayCallback = mDisplayCallback;
        final ImageCache memoryCache = getMemoryCache();
        final float cornerRadius = mCornerRadius;
        Bitmap image = null;
        final String key =
                InternalUtils.buildFullKey(descriptor.getKey(), requiredSize, transformation);
        if (key != null && memoryCache != null) {
            image = memoryCache.get(key);
        }
        final DisplayImageAction<?> currentAction = InternalUtils.getDisplayImageAction(view);
        if (image != null) {
            if (currentAction != null) {
                currentAction.cancel();
            }
            if (loadCallback != null) {
                loadCallback.onLoaded(image);
            }
            if (cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS) {
                InternalUtils
                        .setDrawable(new RoundedDrawable(resources, image, cornerRadius), view);
            } else {
                InternalUtils.setBitmap(resources, image, view);
            }
            if (displayCallback != null) {
                displayCallback.onDisplayed(image, view);
            }
            return EmptyImageRequestDelegate.INSTANCE;
        }
        if (currentAction != null) {
            if (currentAction.hasSameKey(key) && !currentAction.isCancelled()) {
                return currentAction;
            }
            currentAction.cancel();
        }
        Drawable placeholder = mPlaceholder;
        if (placeholder == null) {
            placeholder = new ColorDrawable(Color.TRANSPARENT);
        }
        final DisplayImageAction<T> action =
                new DisplayImageAction<>(resources, view, descriptor, mBitmapLoader, requiredSize,
                        transformation, placeholder, mErrorDrawable, memoryCache, getStorageCache(),
                        mCacheExecutor, loadCallback, mErrorCallback, displayCallback, mPauseLock,
                        mMainThreadHandler, mFadeEnabled, mFadeDuration, cornerRadius);
        InternalUtils.setDrawable(new PlaceholderDrawable(placeholder, action), view);
        return action.submit(mLoadExecutor);
    }

    /**
     * Delete all cached images for specified data asynchronously
     *
     * @return {@link ImageRequestDelegate} object, associated with execution of the request
     * @throws IllegalStateException if request has already been executed
     */
    @NonNull
    @AnyThread
    public ImageRequestDelegate invalidate() {
        checkAndSetExecutedState();
        return new InvalidateAction(mDescriptor, getMemoryCache(), getStorageCache())
                .submit(mCacheExecutor);
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
        final List<BitmapTransformation> t = mTransformations;
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

    private void checkAndSetExecutedState() {
        if (mExecuted) {
            throw new IllegalStateException("Request can be executed only once");
        }
        mExecuted = true;
    }

    private void checkSize(final int width, final int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Width and height should be greater than zero");
        }
    }
}
