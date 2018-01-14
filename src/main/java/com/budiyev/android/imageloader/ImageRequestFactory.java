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
import java.io.FileDescriptor;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Image request factory
 */
public final class ImageRequestFactory {
    private final BitmapLoader<Uri> mUriBitmapLoader = new UriBitmapLoader();
    private final BitmapLoader<String> mUrlBitmapLoader = new UrlBitmapLoader();
    private final BitmapLoader<File> mFileBitmapLoader = new FileBitmapLoader();
    private final BitmapLoader<FileDescriptor> mFileDescriptorBitmapLoader = new FileDescriptorBitmapLoader();
    private final BitmapLoader<Integer> mResourceBitmapLoader = new ResourceBitmapLoader();
    private final BitmapLoader<byte[]> mByteArrayBitmapLoader = new ByteArrayBitmapLoader();
    private final DataDescriptorFactory<Uri> mUriDataDescriptorFactory = new UriDataDescriptorFactory();
    private final DataDescriptorFactory<String> mUrlDataDescriptorFactory = new UrlDataDescriptorFactory();
    private final DataDescriptorFactory<File> mFileDataDescriptorFactory = new FileDataDescriptorFactory();
    private final DataDescriptorFactory<FileDescriptor> mFileDescriptorDataDescriptorFactory =
            new UnidentifiableDataDescriptorFactory<>();
    private final DataDescriptorFactory<Integer> mResourceDataDescriptorFactory = new ResourceDataDescriptorFactory();
    private final DataDescriptorFactory<byte[]> mByteArrayDataDescriptorFactory =
            new UnidentifiableDataDescriptorFactory<>();
    private final DataDescriptorFactory<Object> mCommonDataDescriptorFactory = new CommonDataDescriptorFactory();
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final Handler mMainThreadHandler;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;

    ImageRequestFactory(@NonNull Context context, @NonNull ExecutorService executor, @NonNull PauseLock pauseLock,
            @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache) {
        mContext = context;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mMainThreadHandler = new Handler(context.getMainLooper());
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
    }

    /**
     * {@link Uri} image load request
     */
    @NonNull
    public ImageRequest<Uri> uri() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mUriBitmapLoader, mUriDataDescriptorFactory);
    }

    /**
     * URL image load request
     */
    @NonNull
    public ImageRequest<String> url() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mUrlBitmapLoader, mUrlDataDescriptorFactory);
    }

    /**
     * {@link File} image load request
     */
    @NonNull
    public ImageRequest<File> file() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mFileBitmapLoader, mFileDataDescriptorFactory);
    }

    /**
     * {@link FileDescriptor} image load request
     */
    @NonNull
    public ImageRequest<FileDescriptor> fileDescriptor() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mFileDescriptorBitmapLoader, mFileDescriptorDataDescriptorFactory);
    }

    /**
     * Resource image load request
     */
    @NonNull
    public ImageRequest<Integer> resource() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mResourceBitmapLoader, mResourceDataDescriptorFactory);
    }

    /**
     * Byte array image load request
     */
    @NonNull
    public ImageRequest<byte[]> byteArray() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mByteArrayBitmapLoader, mByteArrayDataDescriptorFactory);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    <T> ImageRequest<T> custom(@NonNull BitmapLoader<T> loader) {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                loader, (DataDescriptorFactory<T>) mCommonDataDescriptorFactory);
    }

    @NonNull
    <T> ImageRequest<T> custom(@NonNull BitmapLoader<T> loader, @NonNull DataDescriptorFactory<T> factory) {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                loader, factory);
    }
}
