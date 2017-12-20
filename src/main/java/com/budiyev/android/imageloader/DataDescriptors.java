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

final class DataDescriptors {
    private final DataDescriptorFactory<Object> mCommonDataDescriptorFactory = new CommonDataDescriptorFactory();
    private final DataDescriptorFactory<Uri> mUriDataDescriptorFactory = new UriDataDescriptorFactory();
    private final DataDescriptorFactory<String> mUrlDataDescriptorFactory = new UrlDataDescriptorFactory();
    private final DataDescriptorFactory<File> mFileDataDescriptorFactory = new FileDataDescriptorFactory();
    private final DataDescriptorFactory<FileDescriptor> mFileDescriptorDataDescriptorFactory =
            new UnidentifiableDataDescriptorFactory<>();
    private final DataDescriptorFactory<Integer> mResourceDataDescriptorFactory = new ResourceDataDescriptorFactory();
    private final DataDescriptorFactory<byte[]> mByteArrayDataDescriptorFactory =
            new UnidentifiableDataDescriptorFactory<>();

    @NonNull
    @SuppressWarnings("unchecked")
    public <T> DataDescriptorFactory<T> common() {
        return (DataDescriptorFactory<T>) mCommonDataDescriptorFactory;
    }

    @NonNull
    public DataDescriptorFactory<Uri> uri() {
        return mUriDataDescriptorFactory;
    }

    @NonNull
    public DataDescriptorFactory<String> url() {
        return mUrlDataDescriptorFactory;
    }

    @NonNull
    public DataDescriptorFactory<File> file() {
        return mFileDataDescriptorFactory;
    }

    @NonNull
    public DataDescriptorFactory<FileDescriptor> fileDescriptor() {
        return mFileDescriptorDataDescriptorFactory;
    }

    @NonNull
    public DataDescriptorFactory<Integer> resource() {
        return mResourceDataDescriptorFactory;
    }

    @NonNull
    public DataDescriptorFactory<byte[]> byteArray() {
        return mByteArrayDataDescriptorFactory;
    }
}
