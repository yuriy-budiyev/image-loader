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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class InternalUtils {
    private static final String URI_SCHEME_HTTP = "http";
    private static final String URI_SCHEME_HTTPS = "https";
    private static final String URI_SCHEME_FTP = "ftp";
    private static final Lock LOADER_EXECUTOR_LOCK = new ReentrantLock();
    private static final Lock CACHE_EXECUTOR_LOCK = new ReentrantLock();
    private static volatile ThreadPoolExecutor sImageLoaderExecutor;
    private static volatile ThreadPoolExecutor sStorageCacheExecutor;

    private InternalUtils() {
    }

    @Nullable
    @MainThread
    public static LoadImageAction<?> getLoadImageAction(@Nullable ImageView view) {
        if (view != null) {
            Drawable drawable = view.getDrawable();
            if (drawable instanceof PlaceholderDrawable) {
                return ((PlaceholderDrawable) drawable).getLoadImageAction();
            }
        }
        return null;
    }

    @Nullable
    public static InputStream getDataStreamFromUri(@NonNull Context context,
            @NonNull Uri uri) throws IOException {
        String scheme = uri.getScheme();
        if (URI_SCHEME_HTTP.equalsIgnoreCase(scheme) || URI_SCHEME_HTTPS.equalsIgnoreCase(scheme) ||
                URI_SCHEME_FTP.equalsIgnoreCase(scheme)) {
            return new URL(uri.toString()).openConnection().getInputStream();
        } else {
            return context.getContentResolver().openInputStream(uri);
        }
    }

    public static void close(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    @NonNull
    public static ThreadPoolExecutor getImageLoaderExecutor() {
        ThreadPoolExecutor executor = sImageLoaderExecutor;
        if (executor == null) {
            LOADER_EXECUTOR_LOCK.lock();
            try {
                executor = sImageLoaderExecutor;
                if (executor == null) {
                    executor = new ImageLoaderExecutor(Runtime.getRuntime().availableProcessors());
                    sImageLoaderExecutor = executor;
                }
            } finally {
                LOADER_EXECUTOR_LOCK.unlock();
            }
        }
        return executor;
    }

    @NonNull
    public static ThreadPoolExecutor getStorageCacheExecutor() {
        ThreadPoolExecutor executor = sStorageCacheExecutor;
        if (executor == null) {
            CACHE_EXECUTOR_LOCK.lock();
            try {
                executor = sStorageCacheExecutor;
                if (executor == null) {
                    executor = new ImageLoaderExecutor();
                    sStorageCacheExecutor = executor;
                }
            } finally {
                CACHE_EXECUTOR_LOCK.unlock();
            }
        }
        return executor;
    }
}
