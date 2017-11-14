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

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private static volatile ImageLoader<Data> sLoader;
    private final Context mContext;
    private final Data mData;
    private final ImageView mView;
    private final PlaceholderProvider<Data> mPlaceholder;
    private final ErrorDrawableProvider<Data> mErrorDrawable;
    private final BitmapTransformation<Data> mTransformation;
    private final LoadCallback<Data> mLoadCallback;
    private final DisplayCallback<Data> mDisplayCallback;
    private final ErrorCallback<Data> mErrorCallback;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;

    public LoadImageRequestInternal(@NonNull Context context, @NonNull Uri data, int requiredWidth,
            int requiredHeight, @Nullable ImageView view, @Nullable Drawable placeholder,
            @Nullable Drawable errorDrawable,
            @Nullable List<BitmapTransformation<Uri>> transformations,
            @Nullable LoadCallback<Uri> loadCallback,
            @Nullable DisplayCallback<Uri> displayCallback,
            @Nullable ErrorCallback<Uri> errorCallback, boolean fadeEnabled, long fadeDuration) {
        mContext = context.getApplicationContext();
        mData = new Data(data, requiredWidth, requiredHeight);
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
            mTransformation = new BitmapTransformationWrapper(transformations);
        } else {
            mTransformation = null;
        }
        if (loadCallback != null) {
            mLoadCallback = new LoadCallbackWrapper(loadCallback);
        } else {
            mLoadCallback = null;
        }
        if (displayCallback != null) {
            mDisplayCallback = new DisplayCallbackWrapper(displayCallback);
        } else {
            mDisplayCallback = null;
        }
        if (errorCallback != null) {
            mErrorCallback = new ErrorCallbackWrapper(errorCallback);
        } else {
            mErrorCallback = null;
        }
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
    }

    @AnyThread
    public void execute() {
        DataDescriptor<Data> descriptor = new RequestDataDescriptor(mData);
        ImageLoader<Data> loader = getLoader();
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
        private final ImageLoader<Data> mLoader;
        private final DataDescriptor<Data> mDescriptor;
        private final ImageView mView;
        private final PlaceholderProvider<Data> mPlaceholder;
        private final ErrorDrawableProvider<Data> mErrorDrawable;
        private final BitmapTransformation<Data> mTransformation;
        private final LoadCallback<Data> mLoadCallback;
        private final DisplayCallback<Data> mDisplayCallback;
        private final ErrorCallback<Data> mErrorCallback;
        private final boolean mFadeEnabled;
        private final long mFadeDuration;

        public LoadAction(@NonNull ImageLoader<Data> loader,
                @NonNull DataDescriptor<Data> descriptor, @NonNull ImageView view,
                @Nullable PlaceholderProvider<Data> placeholder,
                @Nullable ErrorDrawableProvider<Data> errorDrawable,
                @Nullable BitmapTransformation<Data> transformation,
                @Nullable LoadCallback<Data> loadCallback,
                @Nullable DisplayCallback<Data> displayCallback,
                @Nullable ErrorCallback<Data> errorCallback, boolean fadeEnabled,
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
    private ImageLoader<Data> getLoader() {
        ImageLoader<Data> loader = sLoader;
        if (loader == null) {
            LOADER_LOCK.lock();
            try {
                loader = sLoader;
                if (loader == null) {
                    loader = ImageLoader.builder(mContext).custom(new RequestBitmapLoader())
                            .memoryCache().storageCache().build();
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

    private static final class PlaceholderWrapper implements PlaceholderProvider<Data> {
        private final Drawable mDrawable;

        public PlaceholderWrapper(@NonNull Drawable drawable) {
            mDrawable = drawable;
        }

        @NonNull
        @Override
        public Drawable getPlaceholder(@NonNull Context context, @NonNull Data data) {
            return mDrawable;
        }
    }

    private static final class ErrorDrawableWrapper implements ErrorDrawableProvider<Data> {
        private final Drawable mDrawable;

        public ErrorDrawableWrapper(@NonNull Drawable drawable) {
            mDrawable = drawable;
        }

        @NonNull
        @Override
        public Drawable getErrorDrawable(@NonNull Context context, @NonNull Data data) {
            return mDrawable;
        }
    }

    private static final class Data {
        private final Uri mUri;
        private final int mRequiredWidth;
        private final int mRequiredHeight;

        public Data(@NonNull Uri uri, int requiredWidth, int requiredHeight) {
            mUri = uri;
            mRequiredWidth = requiredWidth;
            mRequiredHeight = requiredHeight;
        }

        @Nullable
        public Bitmap load(@NonNull Context context) throws Throwable {
            if (isSampled()) {
                return DataUtils
                        .loadSampledBitmapFromUri(context, mUri, mRequiredWidth, mRequiredHeight);
            } else {
                InputStream inputStream = null;
                try {
                    inputStream = InternalUtils.getDataStreamFromUri(context, mUri);
                    if (inputStream == null) {
                        return null;
                    }
                    return BitmapFactory.decodeStream(inputStream);
                } finally {
                    InternalUtils.close(inputStream);
                }
            }
        }

        public boolean isSampled() {
            return mRequiredWidth > 0 && mRequiredHeight > 0;
        }

        public int getRequiredWidth() {
            return mRequiredWidth;
        }

        public int getRequiredHeight() {
            return mRequiredHeight;
        }

        @NonNull
        public Uri getUri() {
            return mUri;
        }
    }

    private static final class RequestDataDescriptor implements DataDescriptor<Data> {
        private final Data mData;
        private final String mKey;

        private RequestDataDescriptor(@NonNull Data data) {
            mData = data;
            String key = DataUtils.generateSHA256(data.getUri().toString());
            if (data.isSampled()) {
                key += "_sampled" + data.getRequiredWidth() + "x" + data.getRequiredHeight();
            }
            mKey = key;
        }

        @NonNull
        @Override
        public Data getData() {
            return mData;
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class RequestBitmapLoader implements BitmapLoader<Data> {
        @Nullable
        @Override
        public Bitmap load(@NonNull Context context, @NonNull Data data) throws Throwable {
            return data.load(context);
        }
    }

    private static final class BitmapTransformationWrapper implements BitmapTransformation<Data> {
        private final List<BitmapTransformation<Uri>> mTransformations;
        private final String mKey;

        public BitmapTransformationWrapper(
                @NonNull List<BitmapTransformation<Uri>> transformations) {
            mTransformations = transformations;
            StringBuilder sb = new StringBuilder();
            for (BitmapTransformation<Uri> t : transformations) {
                sb.append(t.getKey());
            }
            mKey = sb.toString();
        }


        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Data data,
                @NonNull Bitmap bitmap) throws Throwable {
            Uri dataUri = data.getUri();
            boolean first = true;
            for (BitmapTransformation<Uri> transformation : mTransformations) {
                Bitmap processed = transformation.transform(context, dataUri, bitmap);
                if (bitmap != processed) {
                    if (!first && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    first = false;
                }
                bitmap = processed;
            }
            return bitmap;
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class LoadCallbackWrapper implements LoadCallback<Data> {
        private final LoadCallback<Uri> mCallback;

        public LoadCallbackWrapper(@NonNull LoadCallback<Uri> callback) {
            mCallback = callback;
        }

        @Override
        public void onLoaded(@NonNull Context context, @NonNull Data data, @NonNull Bitmap image) {
            mCallback.onLoaded(context, data.getUri(), image);
        }
    }

    private static final class ErrorCallbackWrapper implements ErrorCallback<Data> {
        private final ErrorCallback<Uri> mCallback;

        private ErrorCallbackWrapper(@NonNull ErrorCallback<Uri> callback) {
            mCallback = callback;
        }

        @Override
        public void onError(@NonNull Context context, @NonNull Data data,
                @NonNull Throwable error) {
            mCallback.onError(context, data.getUri(), error);
        }
    }

    private static final class DisplayCallbackWrapper implements DisplayCallback<Data> {
        private final DisplayCallback<Uri> mCallback;

        private DisplayCallbackWrapper(@NonNull DisplayCallback<Uri> callback) {
            mCallback = callback;
        }

        @Override
        public void onDisplayed(@NonNull Context context, @NonNull Data data, @NonNull Bitmap image,
                @NonNull ImageView view) {
            mCallback.onDisplayed(context, data.getUri(), image, view);
        }
    }
}
