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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.content.Context;
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
    private List<BitmapProcessor<Uri>> mProcessors;
    private boolean mFadeEnabled = true;
    private long mFadeDuration = 200L;

    LoadImageRequest(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Source data, if not set, {@link #load()} method will do nothing
     */
    @NonNull
    public LoadImageRequest from(@Nullable Uri source) {
        mSource = source;
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
     * Add bitmap processor
     *
     * @see BitmapProcessor
     */
    @NonNull
    public LoadImageRequest process(@NonNull BitmapProcessor<Uri> processor) {
        List<BitmapProcessor<Uri>> processors = mProcessors;
        if (processors == null) {
            processors = new ArrayList<>();
            mProcessors = processors;
        }
        processors.add(processor);
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
     * Lad image
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
                        mDisplayCallback, mErrorCallback, mProcessors));
        if (view == null) {
            loader.load(descriptor);
        } else {
            loader.runOnMainThread(
                    new LoadAction(loader, descriptor, view, mFadeEnabled, mFadeDuration));
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
                            .processor(new BitmapProcessorImpl()).onLoaded(new LoadCallbackImpl())
                            .onError(new ErrorCallbackImpl()).onDisplayed(new DisplayCallbackImpl())
                            .build();
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
        private final ImageView mView;
        private final boolean mFadeEnabled;
        private final long mFadeDuration;

        private LoadAction(@NonNull ImageLoader<RequestImpl> loader,
                @NonNull DataDescriptor<RequestImpl> descriptor, @NonNull ImageView view,
                boolean fadeEnabled, long fadeDuration) {
            mLoader = loader;
            mDescriptor = descriptor;
            mView = view;
            mFadeEnabled = fadeEnabled;
            mFadeDuration = fadeDuration;
        }

        @Override
        @MainThread
        public void run() {
            mLoader.load(mDescriptor, mView, mFadeEnabled, mFadeDuration);
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
        private final List<BitmapProcessor<Uri>> mProcessors;


        private RequestImpl(@NonNull Uri source, @Nullable Drawable placeholder,
                @Nullable Drawable errorDrawable, @Nullable LoadCallback<Uri> loadCallback,
                @Nullable DisplayCallback<Uri> displayCallback,
                @Nullable ErrorCallback<Uri> errorCallback,
                @Nullable List<BitmapProcessor<Uri>> processors) {
            mSource = source;
            mKey = DataUtils.generateSHA256(source.toString());
            mPlaceholder = placeholder;
            mErrorDrawable = errorDrawable;
            mLoadCallback = loadCallback;
            mDisplayCallback = displayCallback;
            mErrorCallback = errorCallback;
            mProcessors = processors;
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

        @NonNull
        private Bitmap process(@NonNull Context context, @NonNull Bitmap bitmap) throws Throwable {
            List<BitmapProcessor<Uri>> processors = mProcessors;
            if (processors == null) {
                return bitmap;
            }
            for (BitmapProcessor<Uri> processor : processors) {
                Bitmap processed = processor.process(context, mSource, bitmap);
                if (bitmap != processed && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                bitmap = processed;
            }
            return bitmap;
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

    private static final class BitmapProcessorImpl implements BitmapProcessor<RequestImpl> {
        @NonNull
        @Override
        public Bitmap process(@NonNull Context context, @NonNull RequestImpl data,
                @NonNull Bitmap bitmap) throws Throwable {
            return data.process(context, bitmap);
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
