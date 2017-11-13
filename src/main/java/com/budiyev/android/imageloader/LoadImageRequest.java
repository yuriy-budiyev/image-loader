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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

public final class LoadImageRequest {
    private static final Lock LOADER_LOCK = new ReentrantLock();
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageLoader<RequestImpl> sLoader;
    private final Context mContext;
    private Uri mSource;
    private Drawable mPlaceholder;
    private Drawable mErrorDrawable;
    private LoadCallback<Uri> mLoadCallback;
    private DisplayCallback<Uri> mDisplayCallback;
    private ErrorCallback<Uri> mErrorCallback;
    private ImageView mView;
    private List<BitmapTransformation<Uri>> mTransformations;
    private boolean mFadeEnabled = true;
    private long mFadeDuration = 200L;

    LoadImageRequest(@NonNull Context context) {
        mContext = context;
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
     * Whether to enable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    @NonNull
    public LoadImageRequest fade(boolean enabled) {
        mFadeEnabled = enabled;
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
        ImageView view = mView;
        ImageLoader<RequestImpl> loader = getLoader(mContext);
        DataDescriptorImpl descriptor = new DataDescriptorImpl(
                new RequestImpl(source, mPlaceholder, mErrorDrawable, mLoadCallback,
                        mDisplayCallback, mErrorCallback));
        if (view == null) {
            loader.load(descriptor);
        } else {
            BitmapTransformation<RequestImpl> transformation;
            if (mTransformations != null) {
                transformation = new BitmapTransformationImpl(mTransformations);
            } else {
                transformation = null;
            }
            loader.runOnMainThread(
                    new LoadAction(loader, descriptor, transformation, view, mFadeEnabled,
                            mFadeDuration));
        }
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
                            .onLoaded(new LoadCallbackImpl()).onError(new ErrorCallbackImpl())
                            .onDisplayed(new DisplayCallbackImpl()).build();
                    context.getApplicationContext()
                            .registerComponentCallbacks(new ComponentCallbacksImpl());
                    sLoader = loader;
                }
            } finally {
                LOADER_LOCK.unlock();
            }
        }
        return loader;
    }

    private static final class LoadAction implements Runnable {
        private final ImageLoader<RequestImpl> mLoader;
        private final DataDescriptor<RequestImpl> mDescriptor;
        private final BitmapTransformation<RequestImpl> mTransformation;
        private final ImageView mView;
        private final boolean mFadeEnabled;
        private final long mFadeDuration;

        private LoadAction(@NonNull ImageLoader<RequestImpl> loader,
                @NonNull DataDescriptor<RequestImpl> descriptor,
                @Nullable BitmapTransformation<RequestImpl> transformation, @NonNull ImageView view,
                boolean fadeEnabled, long fadeDuration) {
            mLoader = loader;
            mDescriptor = descriptor;
            mTransformation = transformation;
            mView = view;
            mFadeEnabled = fadeEnabled;
            mFadeDuration = fadeDuration;
        }

        @Override
        @MainThread
        public void run() {
            mLoader.load(mDescriptor, mView, mTransformation, mLoader.getLoadCallback(),
                    mLoader.getErrorCallback(), mLoader.getDisplayCallback(), mFadeEnabled,
                    mFadeDuration);
        }
    }

    private static final class ComponentCallbacksImpl implements ComponentCallbacks2 {
        @Override
        public void onTrimMemory(int level) {
            if (level >= TRIM_MEMORY_BACKGROUND) {
                ImageLoader<RequestImpl> loader = sLoader;
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
            ImageLoader<RequestImpl> loader = sLoader;
            if (loader != null) {
                loader.clearMemoryCache();
            }
        }
    }

    private static final class RequestImpl {
        private final Uri mSource;
        private final String mKey;
        private final Drawable mPlaceholder;
        private final Drawable mErrorDrawable;
        private final LoadCallback<Uri> mLoadCallback;
        private final DisplayCallback<Uri> mDisplayCallback;
        private final ErrorCallback<Uri> mErrorCallback;


        private RequestImpl(@NonNull Uri source, @Nullable Drawable placeholder,
                @Nullable Drawable errorDrawable, @Nullable LoadCallback<Uri> loadCallback,
                @Nullable DisplayCallback<Uri> displayCallback,
                @Nullable ErrorCallback<Uri> errorCallback) {
            mSource = source;
            mKey = DataUtils.generateSHA256(source.toString());
            mPlaceholder = placeholder;
            mErrorDrawable = errorDrawable;
            mLoadCallback = loadCallback;
            mDisplayCallback = displayCallback;
            mErrorCallback = errorCallback;
        }

        @NonNull
        public Uri getSource() {
            return mSource;
        }

        @NonNull
        private String getKey() {
            return mKey;
        }

        @Nullable
        private Bitmap load(@NonNull Context context) throws Throwable {
            InputStream inputStream = null;
            try {
                inputStream = InternalUtils.getDataStreamFromUri(context, mSource);
                if (inputStream == null) {
                    return null;
                }
                return BitmapFactory.decodeStream(inputStream);
            } finally {
                InternalUtils.close(inputStream);
            }
        }

        @NonNull
        private Drawable getPlaceholder() {
            Drawable placeholder = mPlaceholder;
            if (placeholder != null) {
                return placeholder;
            } else {
                return new ColorDrawable(Color.TRANSPARENT);
            }
        }

        @Nullable
        private Drawable getErrorDrawable() {
            return mErrorDrawable;
        }

        private void onLoaded(@NonNull Context context, @NonNull Bitmap image) {
            LoadCallback<Uri> loadCallback = mLoadCallback;
            if (loadCallback != null) {
                loadCallback.onLoaded(context, mSource, image);
            }
        }

        private void onError(@NonNull Context context, @NonNull Throwable error) {
            ErrorCallback<Uri> errorCallback = mErrorCallback;
            if (errorCallback != null) {
                errorCallback.onError(context, mSource, error);
            }
        }

        private void onDisplayed(@NonNull Context context, @NonNull Bitmap image,
                @NonNull ImageView view) {
            DisplayCallback<Uri> displayCallback = mDisplayCallback;
            if (displayCallback != null) {
                displayCallback.onDisplayed(context, mSource, image, view);
            }
        }
    }

    private static final class DataDescriptorImpl implements DataDescriptor<RequestImpl> {
        private final RequestImpl mRequest;

        public DataDescriptorImpl(@NonNull RequestImpl request) {
            mRequest = request;
        }

        @NonNull
        @Override
        public RequestImpl getData() {
            return mRequest;
        }

        @NonNull
        @Override
        public String getKey() {
            return mRequest.getKey();
        }
    }

    private static final class BitmapLoaderImpl implements BitmapLoader<RequestImpl> {
        @Nullable
        @Override
        public Bitmap load(@NonNull Context context, @NonNull RequestImpl data) throws Throwable {
            return data.load(context);
        }
    }

    private static final class PlaceholderProviderImpl implements PlaceholderProvider<RequestImpl> {
        @NonNull
        @Override
        public Drawable getPlaceholder(@NonNull Context context, @NonNull RequestImpl data) {
            return data.getPlaceholder();
        }
    }

    private static final class ErrorDrawableProviderImpl
            implements ErrorDrawableProvider<RequestImpl> {
        @Nullable
        @Override
        public Drawable getErrorDrawable(@NonNull Context context, @NonNull RequestImpl data) {
            return data.getErrorDrawable();
        }
    }

    private static final class BitmapTransformationImpl
            implements BitmapTransformation<RequestImpl> {
        private final List<BitmapTransformation<Uri>> mTransformations;
        private final String mKey;

        private BitmapTransformationImpl(@NonNull List<BitmapTransformation<Uri>> transformations) {
            mTransformations = transformations;
            StringBuilder sb = new StringBuilder();
            for (BitmapTransformation<Uri> transformation : transformations) {
                sb.append(transformation.getKey());
            }
            mKey = sb.toString();
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull RequestImpl data,
                @NonNull Bitmap bitmap) throws Throwable {
            List<BitmapTransformation<Uri>> transformations = mTransformations;
            if (transformations == null) {
                return bitmap;
            }
            Uri source = data.getSource();
            boolean first = true;
            for (BitmapTransformation<Uri> transformation : transformations) {
                Bitmap processed = transformation.transform(context, source, bitmap);
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

    private static final class LoadCallbackImpl implements LoadCallback<RequestImpl> {
        @Override
        public void onLoaded(@NonNull Context context, @NonNull RequestImpl data,
                @NonNull Bitmap image) {
            data.onLoaded(context, image);
        }
    }

    private static final class ErrorCallbackImpl implements ErrorCallback<RequestImpl> {
        @Override
        public void onError(@NonNull Context context, @NonNull RequestImpl data,
                @NonNull Throwable error) {
            data.onError(context, error);
        }
    }

    private static final class DisplayCallbackImpl implements DisplayCallback<RequestImpl> {
        @Override
        public void onDisplayed(@NonNull Context context, @NonNull RequestImpl data,
                @NonNull Bitmap image, @NonNull ImageView view) {
            data.onDisplayed(context, image, view);
        }
    }
}
