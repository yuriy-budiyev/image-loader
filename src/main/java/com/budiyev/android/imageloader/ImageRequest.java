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

import android.content.Context;
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
 */
public final class ImageRequest<T> {
    private static final long DEFAULT_FADE_DURATION = 200L;
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final Handler mMainThreadHandler;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final BitmapLoader<T> mBitmapLoader;
    private final DataDescriptorFactory<T> mDescriptorFactory;
    private final T mData;
    private Size mRequiredSize;
    private CacheMode mCacheMode;
    private LoadCallback<T> mLoadCallback;
    private ErrorCallback<T> mErrorCallback;
    private DisplayCallback<T> mDisplayCallback;
    private List<BitmapTransformation> mTransformations;
    private Drawable mPlaceholder;
    private Drawable mErrorDrawable;
    private boolean mFadeEnabled = true;
    private long mFadeDuration = DEFAULT_FADE_DURATION;
    private float mCornerRadius;

    ImageRequest(@NonNull Context context, @NonNull ExecutorService executor, @NonNull PauseLock pauseLock,
            @NonNull Handler mainThreadHandler, @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache,
            @NonNull BitmapLoader<T> bitmapLoader, @NonNull DataDescriptorFactory<T> descriptorFactory,
            @NonNull T data) {
        mContext = context;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        mBitmapLoader = bitmapLoader;
        mMainThreadHandler = mainThreadHandler;
        mDescriptorFactory = descriptorFactory;
        mData = data;
    }

    /**
     * Required image size
     */
    @NonNull
    public ImageRequest<T> size(@Px int requiredWidth, @Px int requiredHeight) {
        mRequiredSize = new Size(requiredWidth, requiredHeight);
        return this;
    }

    @NonNull
    public ImageRequest<T> cacheMode(@Nullable CacheMode mode) {
        mCacheMode = mode;
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
        mPlaceholder = mContext.getResources().getDrawable(resId);
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
        mErrorDrawable = mContext.getResources().getDrawable(resId);
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
        List<BitmapTransformation> t = mTransformations;
        if (t == null) {
            t = new ArrayList<>();
            mTransformations = t;
        }
        t.add(transformation);
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
        List<BitmapTransformation> t = mTransformations;
        if (t == null) {
            t = new ArrayList<>();
            mTransformations = t;
        }
        t.addAll(transformations);
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
        List<BitmapTransformation> t = mTransformations;
        if (t == null) {
            t = new ArrayList<>();
            mTransformations = t;
        }
        Collections.addAll(t, transformations);
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
    public ImageRequest<T> onLoaded(@Nullable LoadCallback<T> callback) {
        mLoadCallback = callback;
        return this;
    }

    /**
     * Error callback
     */
    @NonNull
    public ImageRequest<T> onError(@Nullable ErrorCallback<T> callback) {
        mErrorCallback = callback;
        return this;
    }

    /**
     * Display callback
     */
    @NonNull
    public ImageRequest<T> onDisplayed(@Nullable DisplayCallback<T> callback) {
        mDisplayCallback = callback;
        return this;
    }

    /**
     * Load image synchronously (on current thread)
     *
     * @return Loaded image or {@code null} if image could not be loaded or source data hasn't been specified
     */
    @Nullable
    @WorkerThread
    public Bitmap loadSync() {
        return new SyncLoadImageAction<>(mContext, getDescriptor(), mRequiredSize, mCacheMode, mBitmapLoader,
                getTransformation(), mMemoryCache, mStorageCache, mLoadCallback, mErrorCallback, mPauseLock).execute();
    }

    /**
     * Load image
     */
    @AnyThread
    public void load() {
        new LoadImageAction<>(mContext, getDescriptor(), mRequiredSize, mCacheMode, mBitmapLoader, getTransformation(),
                mMemoryCache, mStorageCache, mLoadCallback, mErrorCallback, mPauseLock).execute(mExecutor);
    }

    /**
     * Load image and display it in the specified {@code view}
     */
    @MainThread
    public void load(@NonNull View view) {
        DataDescriptor<T> descriptor = getDescriptor();
        Bitmap image = null;
        String key = descriptor.getKey();
        ImageCache memoryCache = mMemoryCache;
        BitmapTransformation transformation = getTransformation();
        CacheMode cacheMode = mCacheMode;
        if (cacheMode == null) {
            cacheMode = descriptor.getCacheMode();
            if (cacheMode == null) {
                cacheMode = CacheMode.FULL;
            }
        }
        if (cacheMode.isMemoryCacheEnabled() && key != null && memoryCache != null) {
            if (transformation != null) {
                image = memoryCache.get(key + transformation.getKey());
            } else {
                image = memoryCache.get(key);
            }
        }
        T data = descriptor.getData();
        Context context = mContext;
        LoadCallback<T> loadCallback = mLoadCallback;
        DisplayCallback<T> displayCallback = mDisplayCallback;
        float cornerRadius = mCornerRadius;
        if (image != null) {
            if (loadCallback != null) {
                loadCallback.onLoaded(context, data, image);
            }
            Resources resources = context.getResources();
            if (cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS) {
                InternalUtils.setDrawable(new RoundedDrawable(resources, image, cornerRadius), view);
            } else {
                InternalUtils.setBitmap(resources, image, view);
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
        Drawable placeholder = mPlaceholder;
        if (placeholder == null) {
            placeholder = new ColorDrawable(Color.TRANSPARENT);
        }
        DisplayImageAction<T> action =
                new DisplayImageAction<>(context, descriptor, mRequiredSize, cacheMode, mBitmapLoader, transformation,
                        placeholder, mErrorDrawable, view, memoryCache, mStorageCache, loadCallback, mErrorCallback,
                        displayCallback, mPauseLock, mMainThreadHandler, mFadeEnabled, mFadeDuration, cornerRadius);
        InternalUtils.setDrawable(new PlaceholderDrawable(placeholder, action), view);
        action.execute(mExecutor);
    }

    /**
     * Remove cached version of requested image asynchronously
     */
    public void invalidate() {
        mExecutor.submit(new InvalidateAction(getDescriptor(), mMemoryCache, mStorageCache));
    }

    @NonNull
    private DataDescriptor<T> getDescriptor() {
        return mDescriptorFactory.newDescriptor(mData);
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
}
