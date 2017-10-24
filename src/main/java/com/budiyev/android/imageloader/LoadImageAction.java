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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Load image action for {@link ImageLoader}
 */
final class LoadImageAction<T> {
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final PauseLock mPauseLock;
    private final BitmapLoader<T> mBitmapLoader;
    private final BitmapProcessor<T> mBitmapProcessor;
    private final ImageCache mMemoryImageCache;
    private final ImageCache mStorageImageCache;
    private final Callbacks<T> mCallbacks;
    private final ImageSource<T> mSource;
    private final WeakReference<ImageView> mView;
    private final Drawable mPlaceholder;
    private final boolean mFadeEnabled;
    private final long mFadeDuration;
    private volatile Future<?> mFuture;
    private volatile boolean mCancelled;

    public LoadImageAction(@NonNull Context context, @NonNull Handler mainThreadHandler,
            @NonNull PauseLock pauseLock, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable BitmapProcessor<T> bitmapProcessor, @Nullable ImageCache memoryImageCache,
            @Nullable ImageCache storageImageCache, boolean fadeEnabled, long fadeDuration,
            @Nullable Callbacks<T> callbacks, @NonNull ImageSource<T> source,
            @NonNull ImageView view, @NonNull Drawable placeholder) {
        mContext = context;
        mMainThreadHandler = mainThreadHandler;
        mPauseLock = pauseLock;
        mBitmapLoader = bitmapLoader;
        mBitmapProcessor = bitmapProcessor;
        mMemoryImageCache = memoryImageCache;
        mStorageImageCache = storageImageCache;
        mCallbacks = callbacks;
        mFadeEnabled = fadeEnabled;
        mFadeDuration = fadeDuration;
        mSource = source;
        mView = new WeakReference<>(view);
        mPlaceholder = placeholder;
    }

    public void execute() {
        if (mCancelled) {
            return;
        }
        mFuture = InternalUtils.getImageLoaderExecutor().submit(new LoadImageTask());
    }

    @NonNull
    public ImageSource<T> getSource() {
        return mSource;
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
        Bitmap bitmap = null;
        ImageCache storageImageCache = mStorageImageCache;
        String key = mSource.getKey();
        T data = mSource.getData();
        if (storageImageCache != null) {
            bitmap = storageImageCache.get(key);
        }
        if (bitmap == null) {
            try {
                bitmap = mBitmapLoader.load(mContext, data);
            } catch (Throwable error) {
                Callbacks.notifyError(mCallbacks, data, error);
                return;
            }
            if (bitmap == null) {
                Callbacks.notifyError(mCallbacks, data, new Exception("Image is not loaded"));
                return;
            }
            if (storageImageCache != null) {
                storageImageCache.put(key, bitmap);
            }
        }
        Callbacks.notifyLoaded(mCallbacks, data, bitmap);
        if (mCancelled) {
            return;
        }
        ImageCache memoryImageCache = mMemoryImageCache;
        if (memoryImageCache != null) {
            memoryImageCache.put(key, bitmap);
        }
        BitmapProcessor<T> bitmapProcessor = mBitmapProcessor;
        if (bitmapProcessor != null) {
            bitmap = bitmapProcessor.process(mContext, data, bitmap);
        }
        if (mCancelled) {
            return;
        }
        mMainThreadHandler.post(new SetImageAction(bitmap));
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
            if (mFadeEnabled) {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                view.setImageDrawable(
                        new FadeBitmapDrawable(mContext.getResources(), mPlaceholder, mImage,
                                mFadeDuration));
            } else {
                view.setImageBitmap(mImage);
            }
        }
    }
}
