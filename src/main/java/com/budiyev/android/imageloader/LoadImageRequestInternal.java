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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class LoadImageRequestInternal<T> {
    private final BitmapLoader<T> mBitmapLoader;
    private final DataDescriptor<T> mDescriptor;
    private final LoadCallback<T> mLoadCallback;
    private final ErrorCallback<T> mErrorCallback;

    public LoadImageRequestInternal(@NonNull BitmapLoader<T> bitmapLoader,
            @NonNull DataDescriptor<T> descriptor, @Nullable LoadCallback<T> loadCallback,
            @Nullable ErrorCallback<T> errorCallback) {
        mBitmapLoader = bitmapLoader;
        mDescriptor = descriptor;
        mLoadCallback = loadCallback;
        mErrorCallback = errorCallback;
    }

    @NonNull
    public BitmapLoader<T> getBitmapLoader() {
        return mBitmapLoader;
    }

    @NonNull
    public DataDescriptor<T> getDescriptor() {
        return mDescriptor;
    }

    @Nullable
    public LoadCallback<T> getLoadCallback() {
        return mLoadCallback;
    }

    @Nullable
    public ErrorCallback<T> getErrorCallback() {
        return mErrorCallback;
    }
}
