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

/**
 * Image request source data type selector
 */
public final class RequestDataTypeSelector {
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final PauseLock mPauseLock;
    private final Handler mMainThreadHandler;
    private final BitmapLoaders mBitmapLoaders;
    private final DataDescriptors mDataDescriptors;
    private final ImageCache mMemoryCache;
    private final ImageCache mStorageCache;

    RequestDataTypeSelector(@NonNull Context context, @NonNull ExecutorService executor, @NonNull PauseLock pauseLock,
            @NonNull Handler mainThreadHandler, @NonNull BitmapLoaders bitmapLoaders,
            @NonNull DataDescriptors dataDescriptors, @NonNull ImageCache memoryCache,
            @NonNull ImageCache storageCache) {
        mContext = context;
        mExecutor = executor;
        mPauseLock = pauseLock;
        mMainThreadHandler = mainThreadHandler;
        mBitmapLoaders = bitmapLoaders;
        mDataDescriptors = dataDescriptors;
        mMemoryCache = memoryCache;
        mStorageCache = storageCache;
    }

    /**
     * {@link Uri} image load request
     */
    @NonNull
    public ImageRequest<Uri> uri() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mBitmapLoaders.uri(), mDataDescriptors.uri());
    }

    /**
     * URL image load request
     */
    @NonNull
    public ImageRequest<String> url() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mBitmapLoaders.url(), mDataDescriptors.url());
    }

    /**
     * {@link File} image load request
     */
    @NonNull
    public ImageRequest<File> file() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mBitmapLoaders.file(), mDataDescriptors.file());
    }

    /**
     * {@link FileDescriptor} image load request
     */
    @NonNull
    public ImageRequest<FileDescriptor> fileDescriptor() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mBitmapLoaders.fileDescriptor(), mDataDescriptors.fileDescriptor());
    }

    /**
     * Resource image load request
     */
    @NonNull
    public ImageRequest<Integer> resource() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mBitmapLoaders.resource(), mDataDescriptors.resource());
    }

    /**
     * Byte array image load request
     */
    @NonNull
    public ImageRequest<byte[]> byteArray() {
        return new ImageRequest<>(mContext, mExecutor, mPauseLock, mMainThreadHandler, mMemoryCache, mStorageCache,
                mBitmapLoaders.byteArray(), mDataDescriptors.byteArray());
    }
}
