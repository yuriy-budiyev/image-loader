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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.widget.ImageView;

/**
 * Load image action for {@link ImageLoader}
 */
final class LoadImageAction<T> {
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final BitmapLoader<T> mBitmapLoader;
    private final BitmapProcessor<T> mBitmapProcessor;
    private final ImageCache mMemoryImageCache;
    private final ImageCache mStorageImageCache;
    private final LoadCallback<T> mLoadCallback;
    private final DisplayCallback<T> mDisplayCallback;
    private final ErrorCallback<T> mErrorCallback;
    private final DataDescriptor<T> mDescriptor;
    private final WeakReference<ImageView> mView;
    private final Drawable mPlaceholder;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;
    private volatile Future<?> mFuture;
    private volatile boolean mCancelled;

    public LoadImageAction(@NonNull Context context, @NonNull Handler mainThreadHandler,
            @NonNull ExecutorService executor, @NonNull PauseLock pauseLock,
            @NonNull BitmapLoader<T> bitmapLoader, @Nullable BitmapProcessor<T> bitmapProcessor,
            @Nullable ImageCache memoryImageCache, @Nullable ImageCache storageImageCache,
            @Nullable LoadCallback<T> loadCallback, @Nullable DisplayCallback<T> displayCallback,
            @Nullable ErrorCallback<T> errorCallback, boolean fadeEnabled, long fadeDuration,
            @NonNull DataDescriptor<T> descriptor, @NonNull ImageView view,
            @NonNull Drawable placeholder) {
        mContext = context;
        mMainThreadHandler = mainThreadHandler;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mBitmapLoader = bitmapLoader;
        mBitmapProcessor = bitmapProcessor;
        mMemoryImageCache = memoryImageCache;
        mStorageImageCache = storageImageCache;
        mLoadCallback = loadCallback;
        mDisplayCallback = displayCallback;
        mErrorCallback = errorCallback;
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
        mDescriptor = descriptor;
        mView = new WeakReference<>(view);
        mPlaceholder = placeholder;
    }

    public void execute() {
        if (mCancelled) {
            return;
        }
        mFuture = mExecutor.submit(new LoadImageTask());
    }

    public boolean hasSameDescriptor(@NonNull String descriptorKey) {
        return mDescriptor.getKey().equals(descriptorKey);
    }

    public void cancel() {
        mCancelled = true;
        Future<?> future = mFuture;
        if (future != null) {
            future.cancel(false);
        }
    }

    @WorkerThread
    private void loadImage() {
        while (!mCancelled && mPauseLock.isPaused()) {
            if (mPauseLock.await()) {
                return;
            }
        }
        if (mCancelled) {
            return;
        }
        Bitmap image = null;
        ImageCache storageImageCache = mStorageImageCache;
        String key = mDescriptor.getKey();
        T data = mDescriptor.getData();
        if (storageImageCache != null) {
            image = storageImageCache.get(key);
        }
        if (image == null) {
            try {
                image = mBitmapLoader.load(mContext, data);
            } catch (Throwable error) {
                ErrorCallback<T> errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(mContext, data, error);
                }
                return;
            }
            if (image == null) {
                ErrorCallback<T> errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(mContext, data, new ImageNotLoadedException());
                }
                return;
            }
            if (storageImageCache != null) {
                storageImageCache.put(key, image);
            }
        }
        LoadCallback<T> loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(mContext, data, image);
        }
        if (mCancelled) {
            return;
        }
        ImageCache memoryImageCache = mMemoryImageCache;
        if (memoryImageCache != null) {
            memoryImageCache.put(key, image);
        }
        BitmapProcessor<T> bitmapProcessor = mBitmapProcessor;
        if (bitmapProcessor != null) {
            image = bitmapProcessor.process(mContext, data, image);
        }
        if (mCancelled) {
            return;
        }
        mMainThreadHandler.post(new SetImageAction(image));
    }

    private final class LoadImageTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            loadImage();
            mFuture = null;
            return null;
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
            if (mCancelled) {
                return;
            }
            ImageView view = mView.get();
            if (view == null || InternalUtils.getLoadImageAction(view) != LoadImageAction.this) {
                return;
            }
            DisplayCallback<T> displayCallback = mDisplayCallback;
            Context context = mContext;
            Bitmap image = mImage;
            if (mFadeEnabled) {
                view.setImageDrawable(
                        new FadeBitmapDrawable(mMainThreadHandler, context.getResources(), image,
                                mPlaceholder, mFadeDuration, displayCallback == null ? null :
                                new FadeCallback<>(context, displayCallback, mDescriptor.getData(),
                                        image, view)));
            } else {
                view.setImageBitmap(image);
                if (displayCallback != null) {
                    displayCallback.onDisplayed(context, mDescriptor.getData(), image, view);
                }
            }
        }
    }

    private static final class FadeCallback<T> implements FadeBitmapDrawable.FadeCallback {
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
