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

class DisplayImageAction<T> extends LoadImageAction<T, DisplayRequestInternal<T>> {
    private final Handler mMainThreadHandler;

    public DisplayImageAction(@NonNull Context context, @NonNull DisplayRequestInternal<T> request,
            @NonNull PauseLock pauseLock, @NonNull Handler mainThreadHandler,
            @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache) {
        super(context, request, pauseLock, memoryCache, storageCache);
        mMainThreadHandler = mainThreadHandler;
    }

    public boolean hasSameDescriptor(@NonNull String descriptorKey) {
        return getRequest().getDescriptor().getKey().equals(descriptorKey);
    }

    @Override
    protected void onImageLoaded(@NonNull Bitmap image) {
        DisplayRequestInternal<T> request = getRequest();
        DataDescriptor<T> descriptor = request.getDescriptor();
        BitmapTransformation transformation = request.getTransformation();
        if (transformation != null) {
            Context context = getContext();
            T data = descriptor.getData();
            try {
                image = transformation.transform(context, image);
            } catch (Throwable error) {
                processError(context, data, error);
                return;
            }
            ImageCache memoryCache = getMemoryCache();
            if (memoryCache != null) {
                memoryCache.put(descriptor.getKey() + transformation.getKey(), image);
            }
        }
        if (isCancelled() || request.getView().get() == null) {
            return;
        }
        mMainThreadHandler.post(new SetImageAction(image));
    }

    @Override
    protected void onError(@NonNull Throwable error) {
        if (getRequest().getErrorDrawable() != null || !isCancelled()) {
            mMainThreadHandler.post(new SetErrorDrawableAction());
        }
    }

    @Override
    protected void onCancelled() {
        getRequest().getView().clear();
    }

    private final class SetErrorDrawableAction implements Runnable {
        @Override
        public void run() {
            DisplayRequestInternal<T> request = getRequest();
            Drawable errorDrawable = request.getErrorDrawable();
            ImageView view = request.getView().get();
            if (isCancelled() || errorDrawable == null || view == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            if (request.isFadeEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                view.setImageDrawable(new FadeDrawable(request.getPlaceholder(), errorDrawable,
                        request.getFadeDuration(), mMainThreadHandler, null));
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
            DisplayRequestInternal<T> request = getRequest();
            ImageView view = request.getView().get();
            if (view == null ||
                    InternalUtils.getDisplayImageAction(view) != DisplayImageAction.this) {
                return;
            }
            Bitmap image = mImage;
            Context context = getContext();
            T data = request.getDescriptor().getData();
            DisplayCallback<T> displayCallback = request.getDisplayCallback();
            float cornerRadius = request.getCornerRadius();
            boolean roundCorners = cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS;
            if (request.isFadeEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                view.setImageDrawable(new FadeDrawable(request.getPlaceholder(), roundCorners ?
                        new RoundedDrawable(context.getResources(), image, cornerRadius) :
                        new BitmapDrawable(context.getResources(), image),
                        request.getFadeDuration(), mMainThreadHandler,
                        displayCallback == null ? null :
                                new FadeCallback<>(context, displayCallback, data, image, view)));
            } else {
                if (roundCorners) {
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
