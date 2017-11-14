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

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

final class LoadImageRequestInternal {
    private static final Lock LOADER_LOCK = new ReentrantLock();
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageLoader<Uri> sLoader;
    private final Context mContext;
    private final Uri mData;
    private final ImageView mView;
    private final PlaceholderProvider<Uri> mPlaceholder;
    private final ErrorDrawableProvider<Uri> mErrorDrawable;
    private final BitmapTransformation<Uri> mTransformation;
    private final LoadCallback<Uri> mLoadCallback;
    private final DisplayCallback<Uri> mDisplayCallback;
    private final ErrorCallback<Uri> mErrorCallback;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;

    LoadImageRequestInternal(@NonNull Context context, @NonNull Uri data, @Nullable ImageView view,
            @Nullable Drawable placeholder, @Nullable Drawable errorDrawable,
            @Nullable List<BitmapTransformation<Uri>> transformations,
            @Nullable LoadCallback<Uri> loadCallback,
            @Nullable DisplayCallback<Uri> displayCallback,
            @Nullable ErrorCallback<Uri> errorCallback, boolean fadeEnabled, long fadeDuration) {
        mContext = context.getApplicationContext();
        mData = data;
        mView = view;
        if (placeholder != null) {
            mPlaceholder = new PlaceholderWrapper(placeholder);
        } else {
            mPlaceholder = null;
        }
        if (errorDrawable != null) {
            mErrorDrawable = new ErrorDrawableWrapper(errorDrawable);
        } else {
            mErrorDrawable = null;
        }
        if (transformations != null && !transformations.isEmpty()) {
            if (transformations.size() == 1) {
                mTransformation = transformations.get(0);
            } else {
                mTransformation = new BitmapTransformationGroup<>(transformations);
            }
        } else {
            mTransformation = null;
        }
        mLoadCallback = loadCallback;
        mDisplayCallback = displayCallback;
        mErrorCallback = errorCallback;
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
    }

    @AnyThread
    public void execute() {
        DataDescriptor<Uri> descriptor = DataUtils.descriptor(mData);
        ImageLoader<Uri> loader = getLoader();
        if (mView == null) {
            loader.load(descriptor, mLoadCallback, mErrorCallback);
        } else {
            loader.runOnMainThread(
                    new LoadAction(loader, descriptor, mView, mPlaceholder, mErrorDrawable,
                            mTransformation, mLoadCallback, mDisplayCallback, mErrorCallback,
                            mFadeEnabled, mFadeDuration));
        }
    }

    private static final class LoadAction implements Runnable {
        private final ImageLoader<Uri> mLoader;
        private final DataDescriptor<Uri> mDescriptor;
        private final ImageView mView;
        private final PlaceholderProvider<Uri> mPlaceholder;
        private final ErrorDrawableProvider<Uri> mErrorDrawable;
        private final BitmapTransformation<Uri> mTransformation;
        private final LoadCallback<Uri> mLoadCallback;
        private final DisplayCallback<Uri> mDisplayCallback;
        private final ErrorCallback<Uri> mErrorCallback;
        private final boolean mFadeEnabled;
        private final long mFadeDuration;

        public LoadAction(@NonNull ImageLoader<Uri> loader, @NonNull DataDescriptor<Uri> descriptor,
                @NonNull ImageView view, @Nullable PlaceholderProvider<Uri> placeholder,
                @Nullable ErrorDrawableProvider<Uri> errorDrawable,
                @Nullable BitmapTransformation<Uri> transformation,
                @Nullable LoadCallback<Uri> loadCallback,
                @Nullable DisplayCallback<Uri> displayCallback,
                @Nullable ErrorCallback<Uri> errorCallback, boolean fadeEnabled,
                long fadeDuration) {
            mLoader = loader;
            mDescriptor = descriptor;
            mView = view;
            mPlaceholder = placeholder;
            mErrorDrawable = errorDrawable;
            mTransformation = transformation;
            mLoadCallback = loadCallback;
            mDisplayCallback = displayCallback;
            mErrorCallback = errorCallback;
            mFadeEnabled = fadeEnabled;
            mFadeDuration = fadeDuration;
        }

        @Override
        @MainThread
        public void run() {
            mLoader.load(mDescriptor, mView, mPlaceholder, mErrorDrawable, mTransformation,
                    mLoadCallback, mErrorCallback, mDisplayCallback, mFadeEnabled, mFadeDuration);
        }
    }

    @NonNull
    private ImageLoader<Uri> getLoader() {
        ImageLoader<Uri> loader = sLoader;
        if (loader == null) {
            LOADER_LOCK.lock();
            try {
                loader = sLoader;
                if (loader == null) {
                    loader = ImageLoader.builder(mContext).uri().memoryCache().storageCache()
                            .build();
                    mContext.registerComponentCallbacks(new ClearMemoryCacheCallbacks());
                    sLoader = loader;
                }
            } finally {
                LOADER_LOCK.unlock();
            }
        }
        return loader;
    }

    private static final class ClearMemoryCacheCallbacks implements ComponentCallbacks2 {
        @Override
        public void onTrimMemory(int level) {
            if (level >= TRIM_MEMORY_BACKGROUND) {
                ImageLoader<?> loader = sLoader;
                if (loader != null) {
                    loader.clearMemoryCache();
                }
            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
            ImageLoader<?> loader = sLoader;
            if (loader != null) {
                loader.clearMemoryCache();
            }
        }
    }

    private static final class PlaceholderWrapper implements PlaceholderProvider<Uri> {
        private final Drawable mDrawable;

        public PlaceholderWrapper(@NonNull Drawable drawable) {
            mDrawable = drawable;
        }

        @NonNull
        @Override
        public Drawable getPlaceholder(@NonNull Context context, @NonNull Uri data) {
            return mDrawable;
        }
    }

    private static final class ErrorDrawableWrapper implements ErrorDrawableProvider<Uri> {
        private final Drawable mDrawable;

        public ErrorDrawableWrapper(@NonNull Drawable drawable) {
            mDrawable = drawable;
        }

        @NonNull
        @Override
        public Drawable getErrorDrawable(@NonNull Context context, @NonNull Uri data) {
            return mDrawable;
        }
    }
}
