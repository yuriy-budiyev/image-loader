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

import android.net.Uri;
import android.support.annotation.NonNull;

final class BitmapLoaders {
    private final BitmapLoader<Uri> mUriBitmapLoader = new UriBitmapLoader();
    private final BitmapLoader<String> mUrlBitmapLoader = new UrlBitmapLoader();
    private final BitmapLoader<File> mFileBitmapLoader = new FileBitmapLoader();
    private final BitmapLoader<FileDescriptor> mFileDescriptorBitmapLoader = new FileDescriptorBitmapLoader();
    private final BitmapLoader<Integer> mResourceBitmapLoader = new ResourceBitmapLoader();
    private final BitmapLoader<byte[]> mByteArrayBitmapLoader = new ByteArrayBitmapLoader();

    @NonNull
    public BitmapLoader<Uri> uri() {
        return mUriBitmapLoader;
    }

    @NonNull
    public BitmapLoader<String> url() {
        return mUrlBitmapLoader;
    }

    @NonNull
    public BitmapLoader<File> file() {
        return mFileBitmapLoader;
    }

    @NonNull
    public BitmapLoader<FileDescriptor> fileDescriptor() {
        return mFileDescriptorBitmapLoader;
    }

    @NonNull
    public BitmapLoader<Integer> resource() {
        return mResourceBitmapLoader;
    }

    @NonNull
    public BitmapLoader<byte[]> byteArray() {
        return mByteArrayBitmapLoader;
    }
}
