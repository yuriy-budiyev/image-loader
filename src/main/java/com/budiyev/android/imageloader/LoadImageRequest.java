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

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

public final class LoadImageRequest<T> {
    private final BitmapLoader<T> mBitmapLoader;
    private final Handler mMainThreadHandler;
    private DataDescriptor<T> mDescriptor;
    private LoadCallback<T> mLoadCallback;
    private ErrorCallback<T> mErrorCallback;
    private DisplayCallback<T> mDisplayCallback;
    private List<BitmapTransformation> mTransformations;
    private ImageView mView;
    private Drawable mPlaceholder;
    private Drawable mErrorDrawable;
    private boolean mFadeEnabled;
    private long mFadeDuration;
    private float mCornerRadius;

    LoadImageRequest(@NonNull BitmapLoader<T> bitmapLoader, @NonNull Handler mainThreadHandler) {
        mBitmapLoader = bitmapLoader;
        mMainThreadHandler = mainThreadHandler;
    }

    @NonNull
    public LoadImageRequest<T> from(@Nullable DataDescriptor<T> descriptor) {
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
     * Target view in which image will be loaded
     */
    @NonNull
    public LoadImageRequest<T> into(@Nullable ImageView view) {
        mView = view;
        return this;
    }

    @AnyThread
    public void load() {

    }

    private static final class LoadAction implements Runnable {
        @Override
        public void run() {

        }
    }

}
