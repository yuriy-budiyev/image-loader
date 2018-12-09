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
import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class DisplayImageAction<T> extends LoadImageAction<T> {
    private final WeakReference<Resources> mResources;
    private final WeakReference<View> mView;
    private final Handler mMainThreadHandler;
    private final DisplayCallback mDisplayCallback;
    private final Drawable mPlaceholder;
    private final Drawable mErrorDrawable;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;
    private final float mCornerRadius;

    public DisplayImageAction(@NonNull final Resources resources, @NonNull final View view,
            @NonNull final DataDescriptor<T> descriptor,
            @NonNull final BitmapLoader<T> bitmapLoader, @Nullable final Size requiredSize,
            @Nullable final BitmapTransformation transformation,
            @NonNull final Drawable placeholder, @Nullable final Drawable errorDrawable,
            @Nullable final MemoryImageCache memoryCache,
            @Nullable final StorageImageCache storageCache,
            @Nullable final ExecutorService cacheExecutor,
            @Nullable final LoadCallback loadCallback, @Nullable final ErrorCallback errorCallback,
            @Nullable final DisplayCallback displayCallback, @NonNull final PauseLock pauseLock,
            @NonNull final Handler mainThreadHandler, final boolean fadeEnabled,
            final long fadeDuration, final float cornerRadius) {
        super(descriptor, bitmapLoader, requiredSize, transformation, memoryCache, storageCache,
                cacheExecutor, loadCallback, errorCallback, pauseLock);
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

    public boolean hasSameKey(@Nullable final String key) {
        final String k = getKey();
        return k != null && k.equals(key);
    }

    @Override
    protected void onImageLoaded(@NonNull final Bitmap image) {
        mMainThreadHandler.post(new SetImageAction(image));
    }

    @Override
    protected void onError(@NonNull final Throwable error) {
        if (mErrorDrawable != null) {
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
            if (isCancelled()) {
                return;
            }
            final Drawable errorDrawable = mErrorDrawable;
            final View view = mView.get();
            if (errorDrawable == null || view == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                InternalUtils
                        .setDrawable(new FadeDrawable(mPlaceholder, errorDrawable, mFadeDuration),
                                view);
            } else {
                InternalUtils.setDrawable(errorDrawable, view);
            }
        }
    }

    private final class SetImageAction implements Runnable {
        private final Bitmap mImage;

        public SetImageAction(@NonNull final Bitmap image) {
            mImage = image;
        }

        @Override
        @MainThread
        public void run() {
            if (isCancelled()) {
                return;
            }
            final View view = mView.get();
            final Resources resources = mResources.get();
            if (view == null || resources == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            final Bitmap image = mImage;
            final float cornerRadius = mCornerRadius;
            final boolean roundCorners =
                    cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS;
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                InternalUtils.setDrawable(new FadeDrawable(mPlaceholder,
                        roundCorners ? new RoundedDrawable(resources, image, cornerRadius) :
                                new BitmapDrawable(resources, image), mFadeDuration), view);
            } else {
                if (roundCorners) {
                    InternalUtils
                            .setDrawable(new RoundedDrawable(resources, image, cornerRadius), view);
                } else {
                    InternalUtils.setBitmap(resources, image, view);
                }
            }
            final DisplayCallback displayCallback = mDisplayCallback;
            if (displayCallback != null) {
                displayCallback.onDisplayed(image, view);
            }
        }
    }
}
