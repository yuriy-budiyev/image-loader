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
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.widget.ImageView;

public final class LoadImageRequest {
    private final Context mContext;
    private Uri mSource;
    private Drawable mPlaceholder;
    private Drawable mErrorDrawable;
    private LoadCallback<Uri> mLoadCallback;
    private DisplayCallback<Uri> mDisplayCallback;
    private ErrorCallback<Uri> mErrorCallback;
    private ImageView mView;
    private List<BitmapTransformation<Uri>> mTransformations;
    private int mRequiredWidth = -1;
    private int mRequiredHeight = -1;
    private boolean mFadeEnabled = true;
    private long mFadeDuration = 200L;
    private float mCornerRadius;

    LoadImageRequest(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Source data, if not set, {@link #load()} method will do nothing
     */
    @NonNull
    public LoadImageRequest from(@Nullable Uri uri) {
        mSource = uri;
        return this;
    }

    /**
     * Source data, if not set, {@link #load()} method will do nothing
     */
    @NonNull
    public LoadImageRequest from(@NonNull String uri) {
        mSource = Uri.parse(uri);
        return this;
    }

    /**
     * Source data, if not set, {@link #load()} method will do nothing
     */
    @NonNull
    public LoadImageRequest from(@NonNull File file) {
        mSource = Uri.fromFile(file);
        return this;
    }

    /**
     * Required size of image
     */
    @NonNull
    public LoadImageRequest size(@Px int requiredWidth, @Px int requiredHeight) {
        mRequiredWidth = requiredWidth;
        mRequiredHeight = requiredHeight;
        return this;
    }

    /**
     * Display image with rounded corners using maximum corner radius,
     * for square image, will lead to circle result
     */
    @NonNull
    public LoadImageRequest roundCorners() {
        mCornerRadius = RoundedDrawable.MAX_RADIUS;
        return this;
    }

    /**
     * Display image with rounded corners using specified corner radius
     */
    @NonNull
    public LoadImageRequest roundCorners(float cornerRadius) {
        mCornerRadius = cornerRadius;
        return this;
    }

    /**
     * Placeholder
     */
    @NonNull
    public LoadImageRequest placeholder(@Nullable Drawable placeholder) {
        mPlaceholder = placeholder;
        return this;
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    @NonNull
    public LoadImageRequest errorDrawable(@Nullable Drawable errorDrawable) {
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
    public LoadImageRequest transform(@NonNull BitmapTransformation<Uri> transformation) {
        List<BitmapTransformation<Uri>> transformations = mTransformations;
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
    public LoadImageRequest fade() {
        mFadeEnabled = true;
        return this;
    }

    /**
     * Disable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    @NonNull
    public LoadImageRequest noFade() {
        mFadeEnabled = true;
        return this;
    }

    /**
     * Whether to enable fade effect for images that isn't cached in memory,
     * allows to specify fade effect duration,
     * supported on API 19+
     */
    @NonNull
    public LoadImageRequest fade(boolean enabled, long duration) {
        mFadeEnabled = enabled;
        mFadeDuration = duration;
        return this;
    }

    /**
     * Load callback
     */
    @NonNull
    public LoadImageRequest onLoaded(@Nullable LoadCallback<Uri> callback) {
        mLoadCallback = callback;
        return this;
    }

    /**
     * Error callback
     */
    @NonNull
    public LoadImageRequest onError(@Nullable ErrorCallback<Uri> callback) {
        mErrorCallback = callback;
        return this;
    }

    /**
     * Display callback
     */
    @NonNull
    public LoadImageRequest onDisplayed(@Nullable DisplayCallback<Uri> callback) {
        mDisplayCallback = callback;
        return this;
    }

    /**
     * Target view in which image will be loaded
     */
    @NonNull
    public LoadImageRequest into(@Nullable ImageView view) {
        mView = view;
        return this;
    }

    /**
     * Load image
     */
    @AnyThread
    public void load() {
        Uri source = mSource;
        if (source == null) {
            return;
        }
        new LoadImageRequestInternal(mContext, source, mRequiredWidth, mRequiredHeight, mView,
                mPlaceholder, mErrorDrawable, mTransformations, mLoadCallback, mDisplayCallback,
                mErrorCallback, mFadeEnabled, mFadeDuration, mCornerRadius).execute();
    }
}
