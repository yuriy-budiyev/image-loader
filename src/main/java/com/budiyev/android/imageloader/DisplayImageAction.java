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

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

final class DisplayImageAction<T> extends BaseLoadImageAction<T> {
    private final Handler mMainThreadHandler;
    private final BitmapTransformation<T> mBitmapTransformation;
    private final DisplayCallback<T> mDisplayCallback;
    private final WeakReference<ImageView> mView;
    private final Drawable mPlaceholder;
    private final Drawable mErrorDrawable;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;
    private final float mCornerRadius;

    protected DisplayImageAction(@NonNull Context context, @NonNull DataDescriptor<T> descriptor,
            @NonNull BitmapLoader<T> bitmapLoader, @NonNull PauseLock pauseLock,
            @Nullable ImageCache storageCache, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback, @NonNull Handler mainThreadHandler,
            @Nullable BitmapTransformation<T> bitmapTransformation,
            @Nullable ImageCache memoryCache, @Nullable DisplayCallback<T> displayCallback,
            @NonNull ImageView view, @NonNull Drawable placeholder,
            @Nullable Drawable errorDrawable, boolean fadeEnabled, long fadeDuration,
            float cornerRadius) {
        super(context, descriptor, bitmapLoader, pauseLock, memoryCache, storageCache, loadCallback,
                errorCallback);
        mMainThreadHandler = mainThreadHandler;
        mBitmapTransformation = bitmapTransformation;
        mDisplayCallback = displayCallback;
        mView = new WeakReference<>(view);
        mPlaceholder = placeholder;
        mErrorDrawable = errorDrawable;
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
        mCornerRadius = cornerRadius;
    }

    public boolean hasSameDescriptor(@NonNull String descriptorKey) {
        return getDescriptor().getKey().equals(descriptorKey);
    }

    @Override
    protected void onImageLoaded(@NonNull Bitmap image) {
        DataDescriptor<T> descriptor = getDescriptor();
        BitmapTransformation<T> bitmapTransformation = mBitmapTransformation;
        if (bitmapTransformation != null) {
            Context context = getContext();
            T data = descriptor.getData();
            try {
                image = bitmapTransformation.transform(context, data, image);
            } catch (Throwable error) {
                processError(context, data, error);
                return;
            }
            ImageCache memoryCache = getMemoryCache();
            if (memoryCache != null) {
                memoryCache.put(descriptor.getKey() + bitmapTransformation.getKey(), image);
            }
        }
        if (isCancelled() || mView.get() == null) {
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
    }

    private final class SetErrorDrawableAction implements Runnable {
        @Override
        public void run() {
            Drawable errorDrawable = mErrorDrawable;
            ImageView view = mView.get();
            if (isCancelled() || errorDrawable == null || view == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                view.setImageDrawable(new FadeDrawable(mPlaceholder, errorDrawable, mFadeDuration,
                        mMainThreadHandler, null));
            } else {
                view.setImageDrawable(errorDrawable);
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
            ImageView view = mView.get();
            if (view == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            Bitmap image = mImage;
            Context context = getContext();
            T data = getDescriptor().getData();
            DisplayCallback<T> displayCallback = mDisplayCallback;
            float cornerRadius = mCornerRadius;
            if (mFadeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                view.setImageDrawable(new FadeDrawable(mPlaceholder, cornerRadius > 0 ?
                        new RoundedDrawable(context.getResources(), image, cornerRadius) :
                        new BitmapDrawable(context.getResources(), image), mFadeDuration,
                        mMainThreadHandler, displayCallback == null ? null :
                        new FadeCallback<>(context, displayCallback, data, image, view)));
            } else {
                if (cornerRadius > 0) {
                    view.setImageDrawable(
                            new RoundedDrawable(context.getResources(), image, cornerRadius));
                } else {
                    view.setImageBitmap(image);
                }
                if (displayCallback != null) {
                    displayCallback.onDisplayed(context, data, image, view);
                }
            }
        }
    }

    private static final class FadeCallback<T> implements FadeDrawable.FadeCallback {
        private final Context mContext;
        private final DisplayCallback<T> mDisplayCallback;
        private final T mData;
        private final Bitmap mImage;
        private final ImageView mView;

        private FadeCallback(@NonNull Context context, @NonNull DisplayCallback<T> displayCallback,
                @NonNull T data, @NonNull Bitmap image, @NonNull ImageView view) {
            mContext = context;
            mDisplayCallback = displayCallback;
            mData = data;
            mImage = image;
            mView = view;
        }

        @Override
        public void onDone() {
            mDisplayCallback.onDisplayed(mContext, mData, mImage, mView);
        }
    }
}
