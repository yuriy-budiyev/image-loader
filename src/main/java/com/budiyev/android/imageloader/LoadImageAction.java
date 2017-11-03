package com.budiyev.android.imageloader;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

final class LoadImageAction<T> {
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final BitmapLoader<T> mBitmapLoader;
    private final ImageCache mMemoryImageCache;
    private final ImageCache mStorageImageCache;
    private final LoadCallback<T> mLoadCallback;
    private final ErrorCallback<T> mErrorCallback;
    private final DataDescriptor<T> mDescriptor;

    public LoadImageAction(@NonNull Context context, @NonNull ExecutorService executor,
            @NonNull PauseLock pauseLock, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable ImageCache memoryImageCache, @Nullable ImageCache storageImageCache,
            @Nullable LoadCallback<T> loadCallback, @Nullable ErrorCallback<T> errorCallback,
            @NonNull DataDescriptor<T> descriptor) {
        mContext = context;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mBitmapLoader = bitmapLoader;
        mMemoryImageCache = memoryImageCache;
        mStorageImageCache = storageImageCache;
        mLoadCallback = loadCallback;
        mErrorCallback = errorCallback;
        mDescriptor = descriptor;
    }

    public void execute() {
        mExecutor.submit(new LoadImageTask());
    }

    @WorkerThread
    private void loadImage() {
        while (mPauseLock.isPaused()) {
            if (mPauseLock.await()) {
                return;
            }
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
        ImageCache memoryImageCache = mMemoryImageCache;
        if (memoryImageCache != null) {
            memoryImageCache.put(key, image);
        }
        LoadCallback<T> loadCallback = mLoadCallback;
        if (loadCallback != null) {
            loadCallback.onLoaded(mContext, data, image);
        }
    }

    private final class LoadImageTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            loadImage();
            return null;
        }
    }
}
