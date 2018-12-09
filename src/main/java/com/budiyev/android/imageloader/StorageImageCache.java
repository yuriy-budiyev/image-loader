/*
 * MIT License
 *
 * Copyright (c) 2018 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class StorageImageCache implements ImageCache {
    public static final String DEFAULT_DIRECTORY = "image_loader_cache";
    public static final long DEFAULT_MAX_SIZE = 268435456L;
    private static final int BUFFER_SIZE = 16384;
    private final Lock mLock = new ReentrantLock();
    private final LinkedHashMap<String, File> mFiles = new LinkedHashMap<>(0, 0.75f, true);
    private final FileFilter mFileFilter = new CacheFileFilter();
    private final Comparator<File> mFileComparator = new FileComparator();
    private final CompressMode mCompressMode;
    private final File mDirectory;
    private final long mMaxSize;
    private volatile boolean mInitialized;
    private volatile long mSize;

    public StorageImageCache(@NonNull final Context context) {
        this(getDefaultDirectory(context));
    }

    public StorageImageCache(@NonNull final Context context, final long maxSize) {
        this(getDefaultDirectory(context), maxSize);
    }

    public StorageImageCache(@NonNull final Context context,
            @NonNull final CompressMode compressMode, final long maxSize) {
        this(getDefaultDirectory(context), compressMode, maxSize);
    }

    public StorageImageCache(@NonNull final File directory) {
        this(directory, DEFAULT_MAX_SIZE);
    }

    public StorageImageCache(@NonNull final File directory, final long maxSize) {
        this(directory, CompressMode.LOSSLESS, maxSize);
    }

    public StorageImageCache(@NonNull final File directory,
            @NonNull final CompressMode compressMode, final long maxSize) {
        mDirectory = InternalUtils.requireNonNull(directory);
        mCompressMode = InternalUtils.requireNonNull(compressMode);
        if (maxSize < 0L) {
            throw new IllegalArgumentException(
                    "Cache size should be greater than or equal to zero");
        }
        mMaxSize = maxSize;
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull final String key) {
        File file;
        mLock.lock();
        try {
            initialize();
            file = mFiles.get(key);
        } finally {
            mLock.unlock();
        }
        if (file == null || !file.exists()) {
            return null;
        }
        Bitmap bitmap = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            final ByteBuffer outputBuffer = new ByteBuffer(BUFFER_SIZE);
            final byte[] buffer = new byte[BUFFER_SIZE];
            for (int read; ; ) {
                read = inputStream.read(buffer, 0, buffer.length);
                if (read == -1) {
                    break;
                }
                outputBuffer.write(buffer, 0, read);
            }
            bitmap = BitmapFactory
                    .decodeByteArray(outputBuffer.getArray(), 0, outputBuffer.getSize());
        } catch (final IOException ignored) {
        } finally {
            InternalUtils.close(inputStream);
        }
        if (bitmap != null) {
            file.setLastModified(System.currentTimeMillis());
            return bitmap;
        } else {
            file.delete();
            return null;
        }
    }

    @Override
    public void put(@NonNull final String key, @NonNull final Bitmap value) {
        final File file = new File(mDirectory, key);
        if (file.exists()) {
            file.delete();
        }
        final ByteBuffer outputBuffer = new ByteBuffer(BUFFER_SIZE);
        if (value.compress(mCompressMode.getFormat(), mCompressMode.getQuality(), outputBuffer)) {
            final byte[] array = outputBuffer.getArray();
            final int outputSize = outputBuffer.getSize();
            FileOutputStream output = null;
            boolean success;
            try {
                output = new FileOutputStream(file);
                int remaining = outputSize;
                for (int write; remaining > 0; ) {
                    write = Math.min(remaining, BUFFER_SIZE);
                    output.write(array, (outputSize - remaining), write);
                    remaining -= write;
                }
                success = true;
            } catch (final IOException e) {
                success = false;
            } finally {
                InternalUtils.close(output);
            }
            if (success) {
                mLock.lock();
                try {
                    initialize();
                    mFiles.put(key, file);
                    long cacheSize = mSize;
                    cacheSize += file.length();
                    final long maxCacheSize = mMaxSize;
                    if (cacheSize > maxCacheSize) {
                        final Iterator<Map.Entry<String, File>> i = mFiles.entrySet().iterator();
                        while (i.hasNext()) {
                            final File f = i.next().getValue();
                            cacheSize -= f.length();
                            i.remove();
                            f.delete();
                            if (cacheSize <= maxCacheSize) {
                                break;
                            }
                        }
                    }
                    mSize = cacheSize;
                } finally {
                    mLock.unlock();
                }
            } else {
                file.delete();
            }
        }
    }

    @Override
    public void remove(@NonNull final String key) {
        final File[] files = mDirectory.listFiles(new RemoveFileFilter(key));
        if (files == null || files.length == 0) {
            return;
        }
        for (final File file : files) {
            mLock.lock();
            try {
                initialize();
                mFiles.remove(file.getName());
                mSize -= file.length();
            } finally {
                mLock.unlock();
            }
            file.delete();
        }
    }

    @Override
    public void clear() {
        mLock.lock();
        try {
            initialize();
            mFiles.clear();
            mSize = 0L;
        } finally {
            mLock.unlock();
        }
        final File[] files = mDirectory.listFiles(mFileFilter);
        if (files != null) {
            for (final File file : files) {
                file.delete();
            }
        }
    }

    private void initialize() {
        if (!mInitialized) {
            final File directory = mDirectory;
            if (directory.exists()) {
                final File[] files = directory.listFiles(mFileFilter);
                if (files != null && files.length != 0) {
                    Arrays.sort(files, mFileComparator);
                    long size = 0;
                    for (final File file : files) {
                        mFiles.put(file.getName(), file);
                        size += file.length();
                    }
                    for (int i = files.length - 1; i >= 0 && size > mMaxSize; i--) {
                        final File file = files[i];
                        mFiles.remove(file.getName());
                        size -= file.length();
                        file.delete();
                    }
                    mSize = size;
                }
            } else {
                directory.mkdirs();
            }
            mInitialized = true;
        }
    }

    @NonNull
    private static File getDefaultDirectory(@NonNull final Context context) {
        File directory = context.getExternalCacheDir();
        if (directory == null) {
            directory = context.getCacheDir();
        }
        return new File(directory, DEFAULT_DIRECTORY);
    }

    private static final class CacheFileFilter implements FileFilter {
        @Override
        public boolean accept(final File pathname) {
            return pathname.isFile();
        }
    }

    private static final class RemoveFileFilter implements FileFilter {
        private final String mName;

        private RemoveFileFilter(@NonNull final String name) {
            mName = name.toLowerCase();
        }

        @Override
        public boolean accept(final File pathname) {
            return pathname.isFile() && pathname.getName().toLowerCase().startsWith(mName);
        }
    }

    private static final class FileComparator implements Comparator<File> {
        @Override
        public int compare(@NonNull final File lhs, @NonNull final File rhs) {
            return Long.signum(rhs.lastModified() - lhs.lastModified());
        }
    }
}
