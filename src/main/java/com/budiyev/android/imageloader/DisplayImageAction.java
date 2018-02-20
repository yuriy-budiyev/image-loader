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

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

final class DisplayImageAction<T> extends BaseLoadImageAction<T> {
    private final WeakReference<Resources> mResources;
    private final WeakReference<View> mView;
    private final Handler mMainThreadHandler;
    private final DisplayCallback mDisplayCallback;
    private final Drawable mPlaceholder;
    private final Drawable mErrorDrawable;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;
    private final float mCornerRadius;

    public DisplayImageAction(@NonNull Resources resources, @NonNull View view, @NonNull DataDescriptor<T> descriptor,
            @NonNull BitmapLoader<T> bitmapLoader, @Nullable Size requiredSize,
            @Nullable BitmapTransformation transformation, @NonNull Drawable placeholder,
            @Nullable Drawable errorDrawable, @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache,
            @Nullable ExecutorService cacheExecutor, @Nullable LoadCallback loadCallback,
            @Nullable ErrorCallback errorCallback, @Nullable DisplayCallback displayCallback,
            @NonNull PauseLock pauseLock, @NonNull Handler mainThreadHandler, boolean fadeEnabled, long fadeDuration,
            float cornerRadius) {
        super(descriptor, bitmapLoader, requiredSize, transformation, memoryCache, storageCache, cacheExecutor,
                loadCallback, errorCallback, pauseLock);
        mResources = new WeakReference<>(resources);
        mView = new WeakReference<>(view);
        mDisplayCallback = displayCallback;
        mPlaceholder = placeholder;
        mErrorDrawable = errorDrawable;
        mMainThreadHandler = mainThreadHandler;
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
        mCornerRadius = cornerRadius;
    }

    public boolean hasSameKey(@Nullable String key) {
        String k = getKey();
        return k != null && key != null && k.equals(key);
    }

    @Override
    protected void onImageLoaded(@NonNull Bitmap image) {
        if (isCancelled() || mView.get() == null || mResources.get() == null) {
            return;
        }
        mMainThreadHandler.post(new SetImageAction(image));
    }

    @Override
    protected void onError(@NonNull Throwable error) {
        if (mErrorDrawable != null || !isCancelled()) {
            mMainThreadHandler.post(new SetErrorDrawableAction());
        }
    }

    @Override
    protected void onCancelled() {
        mView.clear();
        mResources.clear();
    }

    private final class SetErrorDrawableAction implements Runnable {
        @Override
        public void run() {
            Drawable errorDrawable = mErrorDrawable;
            View view = mView.get();
            if (isCancelled() || errorDrawable == null || view == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                InternalUtils.setDrawable(
                        new FadeDrawable(mPlaceholder, errorDrawable, mFadeDuration, mMainThreadHandler, null), view);
            } else {
                InternalUtils.setDrawable(errorDrawable, view);
            }
        }
    }

    private final class SetImageAction implements Runnable {
        private final Bitmap mImage;

        public SetImageAction(@NonNull Bitmap image) {
            mImage = image;
        }

        @Override
        @MainThread
        public void run() {
            if (isCancelled()) {
                return;
            }
            View view = mView.get();
            Resources resources = mResources.get();
            if (view == null || resources == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            Bitmap image = mImage;
            DisplayCallback displayCallback = mDisplayCallback;
            float cornerRadius = mCornerRadius;
            boolean roundCorners = cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS;
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                InternalUtils.setDrawable(new FadeDrawable(mPlaceholder,
                        roundCorners ? new RoundedDrawable(resources, image, cornerRadius) :
                                new BitmapDrawable(resources, image), mFadeDuration, mMainThreadHandler,
                        displayCallback != null ? new FadeCallback<>(displayCallback, image, view) : null), view);
            } else {
                if (roundCorners) {
                    InternalUtils.setDrawable(new RoundedDrawable(resources, image, cornerRadius), view);
                } else {
                    InternalUtils.setBitmap(resources, image, view);
                }
                if (displayCallback != null) {
                    displayCallback.onDisplayed(image, view);
                }
            }
        }
    }

    private static final class FadeCallback<T> implements FadeDrawable.FadeCallback {
        private final DisplayCallback mDisplayCallback;
        private final Bitmap mImage;
        private final View mView;

        private FadeCallback(@NonNull DisplayCallback displayCallback, @NonNull Bitmap image, @NonNull View view) {
            mDisplayCallback = displayCallback;
            mImage = image;
            mView = view;
        }

        @Override
        public void onDone() {
            mDisplayCallback.onDisplayed(mImage, mView);
        }
    }
}
