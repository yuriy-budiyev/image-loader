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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link ImageCache} for {@link ImageLoader}
 */
final class StorageImageCache implements ImageCache {
    public static final String DEFAULT_DIRECTORY = "image_loader_cache";
    public static final long DEFAULT_MAX_SIZE = 52428800L;
    private final Lock mFitLock = new ReentrantLock();
    private final Comparator<File> mFileComparator;
    private final FileFilter mFileFilter;
    private final Runnable mFitCacheSizeTask;
    private final File mDirectory;
    private final CompressMode mCompressMode;
    private final long mMaxSize;
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
        mFileComparator = new FileComparator();
        mFileFilter = new CacheFileFilter();
        mFitCacheSizeTask = new FitCacheSizeTask();
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
        fitCache();
    }

    @Override
    public void put(@NonNull String key, @NonNull Bitmap value) {
        File file = new File(mDirectory, key);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            mCompressMode.compress(value, outputStream);
        } catch (IOException e) {
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        fitCache();
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        File file = new File(mDirectory, key);
        //noinspection ResultOfMethodCallIgnored
        file.setLastModified(System.currentTimeMillis());
        try (InputStream inputStream = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void remove(@NonNull String key) {
        //noinspection ResultOfMethodCallIgnored
        new File(mDirectory, key).delete();
    }

    @Override
    public void clear() {
        File[] files = getCacheFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Nullable
    private File[] getCacheFiles() {
        return mDirectory.listFiles(mFileFilter);
    }

    private void fitCache() {
        mFitLock.lock();
        try {
            if (mCacheFitting) {
                mCacheFitRequested = true;
            } else {
                mCacheFitting = true;
                InternalUtils.getStorageCacheExecutor().execute(mFitCacheSizeTask);
            }
        } finally {
            mFitLock.unlock();
        }
    }

    private final class FitCacheSizeTask implements Runnable {
        @Override
        public void run() {
            for (; ; ) {
                try {
                    File[] files = getCacheFiles();
                    if (files != null && files.length != 0) {
                        Arrays.sort(files, mFileComparator);
                        long size = 0;
                        for (File file : files) {
                            size += file.length();
                        }
                        for (int i = files.length - 1; size > mMaxSize && i >= 0; i--) {
                            File removing = files[i];
                            size -= removing.length();
                            //noinspection ResultOfMethodCallIgnored
                            removing.delete();
                        }
                    }
                } catch (Exception ignored) {
                }
                mFitLock.lock();
                try {
                    if (mCacheFitRequested) {
                        mCacheFitRequested = false;
                    } else {
                        mCacheFitting = false;
                        break;
                    }
                } finally {
                    mFitLock.unlock();
                }
            }
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

    private static final class FileComparator implements Comparator<File> {
        @Override
        public int compare(@NonNull File lhs, @NonNull File rhs) {
            return Long.signum(rhs.lastModified() - lhs.lastModified());
        }
    }

    private static final class CacheFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    }
}
