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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

public final class LoadImageRequest {
    private static final Lock LOADER_LOCK = new ReentrantLock();
    private static volatile ImageLoader<RequestImpl> sLoader;
    private final Context mContext;
    private Uri mSource;
    private ImageView mView;

    LoadImageRequest(@NonNull Context context) {
        mContext = context;
    }

    @NonNull
    public LoadImageRequest from(@Nullable Uri source) {
        mSource = source;
        return this;
    }

    @NonNull
    public LoadImageRequest into(@Nullable ImageView view) {
        mView = view;
        return this;
    }

    @NonNull
    private static ImageLoader<RequestImpl> getLoader(@NonNull Context context) {
        ImageLoader<RequestImpl> loader = sLoader;
        if (loader == null) {
            LOADER_LOCK.lock();
            try {
                loader = sLoader;
                if (loader == null) {
                    loader = ImageLoader.builder(context).custom(new BitmapLoaderImpl())
                            .memoryCache().storageCache().placeholder(new PlaceholderProviderImpl())
                            .errorDrawable(new ErrorDrawableProviderImpl())
                            .processor(new BitmapProcessorImpl()).build();
                    sLoader = loader;
                }
            } finally {
                LOADER_LOCK.unlock();
            }
        }
        return loader;
    }

    private static final class RequestImpl implements BitmapLoader<RequestImpl> {
        @Nullable
        @Override
        public Bitmap load(@NonNull Context context, @NonNull RequestImpl data) throws Throwable {

            return null;
        }
    }

    private static final class BitmapLoaderImpl implements BitmapLoader<RequestImpl> {
        @Nullable
        @Override
        public Bitmap load(@NonNull Context context, @NonNull RequestImpl data) throws Throwable {
            return null;
        }
    }

    private static final class PlaceholderProviderImpl implements PlaceholderProvider<RequestImpl> {
        @NonNull
        @Override
        public Drawable getPlaceholder(@NonNull Context context, @NonNull RequestImpl data) {
            return new ColorDrawable(Color.TRANSPARENT);
        }
    }

    private static final class ErrorDrawableProviderImpl
            implements ErrorDrawableProvider<RequestImpl> {
        @NonNull
        @Override
        public Drawable getErrorDrawable(@NonNull Context context, @NonNull RequestImpl data) {
            return new ColorDrawable(Color.TRANSPARENT);
        }
    }

    private static final class BitmapProcessorImpl implements BitmapProcessor<RequestImpl> {
        @NonNull
        @Override
        public Bitmap process(@NonNull Context context, @NonNull RequestImpl data,
                @NonNull Bitmap bitmap) throws Throwable {
            return bitmap;
        }
    }
}
