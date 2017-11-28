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

import java.util.ArrayList;
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
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.view.View;

/**
 * Load image request
 */
public final class LoadImageRequest<T> {
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;
    private final BitmapLoader<T> mBitmapLoader;
    private final Handler mMainThreadHandler;
    private T mData;
    private Size mRequiredSize;
    private DataDescriptor<T> mDescriptor;
    private LoadCallback<T> mLoadCallback;
    private ErrorCallback<T> mErrorCallback;
    private DisplayCallback<T> mDisplayCallback;
    private List<BitmapTransformation> mTransformations;
    private Drawable mPlaceholder;
    private Drawable mErrorDrawable;
    private boolean mFadeEnabled = true;
    private long mFadeDuration = 200L;
    private float mCornerRadius;
    private boolean mSynchronous;

    LoadImageRequest(@NonNull Context context, @NonNull ExecutorService executor, @NonNull PauseLock pauseLock,
            @NonNull Handler mainThreadHandler, @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache,
            @NonNull BitmapLoader<T> bitmapLoader) {
        mContext = context;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
        mBitmapLoader = bitmapLoader;
        mMainThreadHandler = mainThreadHandler;
    }

    /**
     * Source data,
     * default {@link DataDescriptor} will be used, {@code data}'s toString() method will be used
     * for key generation, any characters allowed
     */
    @NonNull
    public LoadImageRequest<T> from(@NonNull T data) {
        mDescriptor = null;
        mData = data;
        return this;
    }

    /**
     * Required image size, affects only when source data set through {@code from(T data)} method
     *
     * @see DataDescriptor
     */
    @NonNull
    public LoadImageRequest<T> size(@Px int requiredWidth, @Px int requiredHeight) {
        mDescriptor = null;
        mRequiredSize = new Size(requiredWidth, requiredHeight);
        return this;
    }

    /**
     * Source data descriptor
     *
     * @see DataDescriptor
     */
    @NonNull
    public LoadImageRequest<T> from(@Nullable DataDescriptor<T> descriptor) {
        mData = null;
        mRequiredSize = null;
        mDescriptor = descriptor;
        return this;
    }

    /**
     * Display image with rounded corners using maximum corner radius,
     * for square image, will lead to circle result
     */
    @NonNull
    public LoadImageRequest<T> roundCorners() {
        mCornerRadius = RoundedDrawable.MAX_RADIUS;
        return this;
    }

    /**
     * Display image with rounded corners using specified corner radius
     */
    @NonNull
    public LoadImageRequest<T> roundCorners(float cornerRadius) {
        mCornerRadius = cornerRadius;
        return this;
    }

    /**
     * Placeholder
     */
    @NonNull
    public LoadImageRequest<T> placeholder(@Nullable Drawable placeholder) {
        mPlaceholder = placeholder;
        return this;
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    @NonNull
    public LoadImageRequest<T> errorDrawable(@Nullable Drawable errorDrawable) {
        mErrorDrawable = errorDrawable;
        return this;
    }

    /**
     * Add bitmap transformation
     *
     * @see ImageUtils
     * @see BitmapTransformation
     */
    @NonNull
    public LoadImageRequest<T> transform(@NonNull BitmapTransformation transformation) {
        List<BitmapTransformation> transformations = mTransformations;
        if (transformations == null) {
            transformations = new ArrayList<>();
            mTransformations = transformations;
        }
        transformations.add(transformation);
        return this;
    }

    /**
     * Enable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    @NonNull
    public LoadImageRequest<T> fade() {
        mFadeEnabled = true;
        return this;
    }

    /**
     * Disable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    @NonNull
    public LoadImageRequest<T> noFade() {
        mFadeEnabled = false;
        return this;
    }

    /**
     * Enable fade effect for images that isn't cached in memory,
     * allows to specify fade effect duration,
     * supported on API 19+
     */
    @NonNull
    public LoadImageRequest<T> fade(long duration) {
        mFadeEnabled = true;
        mFadeDuration = duration;
        return this;
    }

    /**
     * Load callback
     */
    @NonNull
    public LoadImageRequest<T> onLoaded(@Nullable LoadCallback<T> callback) {
        mLoadCallback = callback;
        return this;
    }

    /**
     * Error callback
     */
    @NonNull
    public LoadImageRequest<T> onError(@Nullable ErrorCallback<T> callback) {
        mErrorCallback = callback;
        return this;
    }

    /**
     * Display callback
     */
    @NonNull
    public LoadImageRequest<T> onDisplayed(@Nullable DisplayCallback<T> callback) {
        mDisplayCallback = callback;
        return this;
    }

    /**
     * Execute request synchronously (on current thread)
     */
    @NonNull
    public LoadImageRequest<T> sync() {
        mSynchronous = true;
        return this;
    }

    /**
     * Execute request asynchronously (default),
     * for custom image loaders, request will be executed on executor,
     * specified in builder (if it was specified), generally executed on default image loader executor
     */
    @NonNull
    public LoadImageRequest<T> async() {
        mSynchronous = false;
        return this;
    }

    /**
     * Load image
     */
    @AnyThread
    public void load() {
        DataDescriptor<T> descriptor = getDescriptor();
        if (descriptor == null) {
            return;
        }
        new LoadImageAction<>(mContext, descriptor, mBitmapLoader, getTransformation(), mMemoryCache, mStorageCache,
                mLoadCallback, mErrorCallback, mPauseLock).execute(getExecutor());
    }

    /**
     * Load image and display it in the specified {@code view}
     */
    @MainThread
    public void load(@NonNull View view) {
        DataDescriptor<T> descriptor = getDescriptor();
        if (descriptor == null) {
            return;
        }
        Bitmap image = null;
        String key = descriptor.getKey();
        ImageCache memoryCache = mMemoryCache;
        BitmapTransformation transformation = getTransformation();
        if (memoryCache != null) {
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
                new DisplayImageAction<>(context, descriptor, mBitmapLoader, transformation, placeholder,
                        mErrorDrawable, view, memoryCache, mStorageCache, loadCallback, mErrorCallback, displayCallback,
                        mPauseLock, mMainThreadHandler, mFadeEnabled, mFadeDuration, cornerRadius);
        InternalUtils.setDrawable(new PlaceholderDrawable(placeholder, action), view);
        action.execute(getExecutor());
    }

    @NonNull
    private ExecutorService getExecutor() {
        return mSynchronous ? SynchronousExecutor.get() : mExecutor;
    }

    @Nullable
    private DataDescriptor<T> getDescriptor() {
        DataDescriptor<T> descriptor = mDescriptor;
        if (descriptor != null) {
            return descriptor;
        }
        T data = mData;
        if (data != null) {
            return new StringDataDescriptor<>(data, mRequiredSize);
        }
        return null;
    }

    @Nullable
    private BitmapTransformation getTransformation() {
        List<BitmapTransformation> transformations = mTransformations;
        if (transformations != null && !transformations.isEmpty()) {
            if (transformations.size() == 1) {
                return transformations.get(0);
            } else {
                return new BitmapTransformationGroup(transformations);
            }
        } else {
            return null;
        }
    }
}
