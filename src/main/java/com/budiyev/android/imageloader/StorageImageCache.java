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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class StorageImageCache implements ImageCache {
    public static final String DEFAULT_DIRECTORY = "image_loader_cache";
    public static final long DEFAULT_MAX_SIZE = 52428800L;
    private final KeyLock mLock = new KeyLock();
    private final Comparator<File> mFileComparator = new FileComparator();
    private final FileFilter mFileFilter = new CacheFileFilter();
    private final Runnable mFitCacheSizeTask = new FitCacheSizeTask();
    private final CompressMode mCompressMode;
    private final File mDirectory;
    private final long mMaxSize;
    private volatile ExecutorService mExecutor;
    private volatile boolean mCacheFitting;
    private volatile boolean mCacheFitRequested;

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

    public void setExecutor(@Nullable ExecutorService executor) {
        if (mExecutor != null) {
            return;
        }
        mExecutor = executor;
    }

    @Override
    public void put(@NonNull String key, @NonNull Bitmap value) {
        mLock.lock(key);
        try {
            File file = getFile(key);
            if (file.exists()) {
                file.delete();
            }
            OutputStream stream = null;
            try {
                stream = InternalUtils.buffer(new FileOutputStream(file));
                value.compress(mCompressMode.getFormat(), mCompressMode.getQuality(), stream);
            } catch (IOException ignored) {
            } finally {
                InternalUtils.close(stream);
            }
        } finally {
            mLock.unlock(key);
        }
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        mLock.lock(key);
        try {
            File file = getFile(key);
            if (!file.exists()) {
                return null;
            }
            InputStream stream = null;
            try {
                stream = InternalUtils.buffer(new FileInputStream(file));
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                return null;
            } finally {
                InternalUtils.close(stream);
            }
        } finally {
            mLock.unlock(key);
        }
    }

    @Override
    public void remove(@NonNull String key) {
        mLock.lock(key);
        try {
            File file = getFile(key);
            if (file.exists()) {
                file.delete();
            }
        } finally {
            mLock.unlock(key);
        }
    }

    @Override
    public void clear() {
        mLock.lock();
        try {
            File[] files = getFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        } finally {
            mLock.unlock();
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

    @NonNull
    private static File getDefaultDirectory(@NonNull Context context) {
        File directory = context.getExternalCacheDir();
        if (directory == null) {
            directory = context.getCacheDir();
        }
        return new File(directory, DEFAULT_DIRECTORY);
    }

    private void fitCache() {
        ExecutorService executor = mExecutor;
        if (executor == null) {
            return;
        }
        mLock.lock();
        try {
            if (mCacheFitting) {
                mCacheFitRequested = true;
            } else {
                mCacheFitting = true;
                executor.execute(mFitCacheSizeTask);
            }
        } finally {
            mLock.unlock();
        }
    }

    private final class FitCacheSizeTask implements Runnable {
        @Override
        public void run() {
            for (; ; ) {
                try {
                    File[] files;
                    mLock.lock();
                    try {
                        files = getFiles();
                        if (files != null) {
                            Arrays.sort(files, mFileComparator);
                        }
                    } finally {
                        mLock.unlock();
                    }
                    if (files != null && files.length != 0) {
                        long size = 0L;
                        boolean removing = false;
                        for (File file : files) {
                            String key = file.getName();
                            mLock.lock(key);
                            try {
                                if (removing) {
                                    file.delete();
                                } else {
                                    size += file.length();
                                    if (size >= mMaxSize) {
                                        removing = true;
                                    }
                                }
                            } finally {
                                mLock.unlock(key);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                mLock.lock();
                try {
                    if (mCacheFitRequested) {
                        mCacheFitRequested = false;
                    } else {
                        mCacheFitting = false;
                        break;
                    }
                } finally {
                    mLock.unlock();
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
