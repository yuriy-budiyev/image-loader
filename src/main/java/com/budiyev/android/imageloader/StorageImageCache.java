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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    public StorageImageCache(@NonNull Context context) {
        this(getDefaultDirectory(context));
    }

    public StorageImageCache(@NonNull Context context, long maxSize) {
        this(getDefaultDirectory(context), maxSize);
    }

    public StorageImageCache(@NonNull Context context, @NonNull CompressMode compressMode, long maxSize) {
        this(getDefaultDirectory(context), compressMode, maxSize);
    }

    public StorageImageCache(@NonNull File directory) {
        this(directory, DEFAULT_MAX_SIZE);
    }

    public StorageImageCache(@NonNull File directory, long maxSize) {
        this(directory, CompressMode.LOSSLESS, maxSize);
    }

    public StorageImageCache(@NonNull File directory, @NonNull CompressMode compressMode, long maxSize) {
        mDirectory = InternalUtils.requireNonNull(directory);
        mCompressMode = InternalUtils.requireNonNull(compressMode);
        if (maxSize < 0L) {
            throw new IllegalArgumentException("Cache size should be greater than or equal to zero");
        }
        mMaxSize = maxSize;
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
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
            ByteBuffer outputBuffer = new ByteBuffer(BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int read; ; ) {
                read = inputStream.read(buffer, 0, buffer.length);
                if (read == -1) {
                    break;
                }
                outputBuffer.write(buffer, 0, read);
            }
            bitmap = BitmapFactory.decodeByteArray(outputBuffer.getArray(), 0, outputBuffer.getSize());
        } catch (IOException ignored) {
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
    public void put(@NonNull String key, @NonNull Bitmap value) {
        File file = new File(mDirectory, key);
        if (file.exists()) {
            file.delete();
        }
        ByteBuffer outputBuffer = new ByteBuffer(BUFFER_SIZE);
        if (value.compress(mCompressMode.getFormat(), mCompressMode.getQuality(), outputBuffer)) {
            byte[] array = outputBuffer.getArray();
            int outputSize = outputBuffer.getSize();
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
            } catch (IOException e) {
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
                    long maxCacheSize = mMaxSize;
                    if (cacheSize > maxCacheSize) {
                        Iterator<Map.Entry<String, File>> i = mFiles.entrySet().iterator();
                        while (i.hasNext()) {
                            File f = i.next().getValue();
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
    public void remove(@NonNull String key) {
        File[] files = mDirectory.listFiles(new RemoveFileFilter(key));
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
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
        File[] files = mDirectory.listFiles(mFileFilter);
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private void initialize() {
        if (!mInitialized) {
            File directory = mDirectory;
            if (directory.exists()) {
                File[] files = directory.listFiles(mFileFilter);
                if (files != null && files.length != 0) {
                    Arrays.sort(files, mFileComparator);
                    long size = 0;
                    for (File file : files) {
                        mFiles.put(file.getName(), file);
                        size += file.length();
                    }
                    for (int i = files.length - 1; i >= 0 && size > mMaxSize; i--) {
                        File file = files[i];
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
    private static File getDefaultDirectory(@NonNull Context context) {
        File directory = context.getExternalCacheDir();
        if (directory == null) {
            directory = context.getCacheDir();
        }
        return new File(directory, DEFAULT_DIRECTORY);
    }

    private static final class CacheFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    }

    private static final class RemoveFileFilter implements FileFilter {
        private final String mName;

        private RemoveFileFilter(@NonNull String name) {
            mName = name.toLowerCase();
        }

        @Override
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().toLowerCase().startsWith(mName);
        }
    }

    private static final class FileComparator implements Comparator<File> {
        @Override
        public int compare(@NonNull File lhs, @NonNull File rhs) {
            return Long.signum(rhs.lastModified() - lhs.lastModified());
        }
    }
}
