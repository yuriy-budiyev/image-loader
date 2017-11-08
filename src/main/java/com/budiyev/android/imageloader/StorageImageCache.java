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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class StorageImageCache implements ImageCache {
    public static final String DEFAULT_DIRECTORY = "image_loader_cache";
    public static final long DEFAULT_MAX_SIZE = 52428800L;
    private final Lock mTrimLock = new ReentrantLock();
    private final Runnable mTrimAction = new TrimAction();
    private final FileFilter mFileFilter = new CacheFileFilter();
    private final Comparator<File> mFileComparator = new FileComparator();
    private final CompressMode mCompressMode;
    private final File mDirectory;
    private final long mMaxSize;
    private volatile Executor mExecutor;
    private volatile boolean mTrimming;
    private volatile boolean mTrimRequested;

    public StorageImageCache(@NonNull Context context) {
        this(getDefaultDirectory(context));
    }

    public StorageImageCache(@NonNull Context context, long maxSize) {
        this(getDefaultDirectory(context), maxSize);
    }

    public StorageImageCache(@NonNull Context context, @NonNull CompressMode compressMode,
            long maxSize) {
        this(getDefaultDirectory(context), compressMode, maxSize);
    }

    public StorageImageCache(@NonNull File directory) {
        this(directory, DEFAULT_MAX_SIZE);
    }

    public StorageImageCache(@NonNull File directory, long maxSize) {
        this(directory, CompressMode.LOSSLESS, maxSize);
    }

    public StorageImageCache(@NonNull File directory, @NonNull CompressMode compressMode,
            long maxSize) {
        mDirectory = directory;
        mCompressMode = compressMode;
        mMaxSize = maxSize;
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public void setExecutor(@Nullable Executor executor) {
        if (mExecutor != null) {
            return;
        }
        mExecutor = executor;
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        File file = getFile(key);
        if (!file.exists()) {
            return null;
        }
        Bitmap bitmap = InternalUtils.decodeBitmap(file);
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
        File file = getFile(key);
        if (file.exists()) {
            file.delete();
        }
        ByteBuffer outputBuffer = InternalUtils.byteBuffer();
        if (value.compress(mCompressMode.getFormat(), mCompressMode.getQuality(), outputBuffer)) {
            if (InternalUtils.writeBytes(file, outputBuffer.getArray(), outputBuffer.getSize())) {
                trim();
            } else {
                file.delete();
            }
        }
    }

    @Override
    public void remove(@NonNull String key) {
        File file = getFile(key);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void clear() {
        File[] files = getFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    @NonNull
    private File getFile(@NonNull String key) {
        return new File(mDirectory, key);
    }

    @Nullable
    private File[] getFiles() {
        return mDirectory.listFiles(mFileFilter);
    }

    private void trim() {
        Executor executor = mExecutor;
        if (executor == null) {
            return;
        }
        mTrimLock.lock();
        try {
            if (mTrimming) {
                mTrimRequested = true;
            } else {
                mTrimming = true;
                executor.execute(mTrimAction);
            }
        } finally {
            mTrimLock.unlock();
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

    private final class TrimAction implements Runnable {
        @Override
        public void run() {
            for (; ; ) {
                try {
                    File[] files = getFiles();
                    if (files != null && files.length != 0) {
                        Arrays.sort(files, mFileComparator);
                        long size = 0L;
                        boolean removing = false;
                        for (File cached : files) {
                            if (removing) {
                                cached.delete();
                            } else {
                                size += cached.length();
                                if (size >= mMaxSize) {
                                    removing = true;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                mTrimLock.lock();
                try {
                    if (!mTrimRequested) {
                        mTrimming = false;
                        break;
                    }
                    mTrimRequested = false;
                } finally {
                    mTrimLock.unlock();
                }
            }
        }
    }

    private static final class CacheFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    }

    private static final class FileComparator implements Comparator<File> {
        @Override
        public int compare(@NonNull File lhs, @NonNull File rhs) {
            return Long.signum(rhs.lastModified() - lhs.lastModified());
        }
    }
}
