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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.support.annotation.NonNull;

final class LoaderCache {
    private static final Lock LOCK = new ReentrantLock();
    private static volatile UriBitmapLoader sUriBitmapLoader;
    private static volatile UrlBitmapLoader sUrlBitmapLoader;
    private static volatile FileBitmapLoader sFileBitmapLoader;
    private static volatile FileDescriptorBitmapLoader sFileDescriptorBitmapLoader;
    private static volatile ResourceBitmapLoader sResourceBitmapLoader;
    private static volatile ByteArrayBitmapLoader sByteArrayBitmapLoader;

    private LoaderCache() {
    }

    @NonNull
    public static UriBitmapLoader getUriBitmapLoader() {
        UriBitmapLoader loader = sUriBitmapLoader;
        if (loader == null) {
            LOCK.lock();
            try {
                loader = sUriBitmapLoader;
                if (loader == null) {
                    loader = new UriBitmapLoader();
                    sUriBitmapLoader = loader;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }

    @NonNull
    public static UrlBitmapLoader getUrlBitmapLoader() {
        UrlBitmapLoader loader = sUrlBitmapLoader;
        if (loader == null) {
            LOCK.lock();
            try {
                loader = sUrlBitmapLoader;
                if (loader == null) {
                    loader = new UrlBitmapLoader();
                    sUrlBitmapLoader = loader;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }

    @NonNull
    public static FileBitmapLoader getFileBitmapLoader() {
        FileBitmapLoader loader = sFileBitmapLoader;
        if (loader == null) {
            LOCK.lock();
            try {
                loader = sFileBitmapLoader;
                if (loader == null) {
                    loader = new FileBitmapLoader();
                    sFileBitmapLoader = loader;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }

    @NonNull
    public static FileDescriptorBitmapLoader getFileDescriptorBitmapLoader() {
        FileDescriptorBitmapLoader loader = sFileDescriptorBitmapLoader;
        if (loader == null) {
            LOCK.lock();
            try {
                loader = sFileDescriptorBitmapLoader;
                if (loader == null) {
                    loader = new FileDescriptorBitmapLoader();
                    sFileDescriptorBitmapLoader = loader;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }

    @NonNull
    public static ResourceBitmapLoader getResourceBitmapLoader() {
        ResourceBitmapLoader loader = sResourceBitmapLoader;
        if (loader == null) {
            LOCK.lock();
            try {
                loader = sResourceBitmapLoader;
                if (loader == null) {
                    loader = new ResourceBitmapLoader();
                    sResourceBitmapLoader = loader;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }

    @NonNull
    public static ByteArrayBitmapLoader getByteArrayBitmapLoader() {
        ByteArrayBitmapLoader loader = sByteArrayBitmapLoader;
        if (loader == null) {
            LOCK.lock();
            try {
                loader = sByteArrayBitmapLoader;
                if (loader == null) {
                    loader = new ByteArrayBitmapLoader();
                    sByteArrayBitmapLoader = loader;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }
}
