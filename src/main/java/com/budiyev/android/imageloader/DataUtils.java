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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

@SuppressWarnings("SameParameterValue")
public final class DataUtils {
    private static final String HASH_ALGORITHM_SHA256 = "SHA-256";

    private DataUtils() {
    }

    /**
     * Default {@link DataDescriptor}, {@code data}'s toString method will be used
     * for key generation, any characters are allowed
     */
    @NonNull
    public static <T> DataDescriptor<T> descriptor(@NonNull T data, @Nullable Size size) {
        //return new StringDataDescriptor<>(data, size);
    }

    /**
     * Default {@link DataDescriptor}, {@code data}'s toString method will be used
     * for key generation, any characters are allowed
     */
    @NonNull
    public static <T> DataDescriptor<T> descriptor(@NonNull T data) {
        //return new StringDataDescriptor<>(data, null);
    }

    /**
     * Generate SHA-256 hash string with {@link Character#MAX_RADIX} radix
     * for specified {@link String}; usable for keys of {@link DataDescriptor} implementations
     *
     * @param string Source string
     * @return SHA-256 hash string
     * @see DataDescriptor#getKey
     */
    @NonNull
    public static String generateSHA256(@NonNull String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM_SHA256);
            messageDigest.update(string.getBytes());
            return new BigInteger(1, messageDigest.digest()).toString(Character.MAX_RADIX);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load sampled bitmap from uri
     *
     * @param context        Context
     * @param uri            URI
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromUri(@NonNull Context context, @NonNull Uri uri, int requiredWidth,
            int requiredHeight) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = null;
        try {
            inputStream = InternalUtils.getDataStreamFromUri(context, uri);
            if (inputStream == null) {
                return null;
            }
            BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        inputStream = null;
        try {
            inputStream = InternalUtils.getDataStreamFromUri(context, uri);
            if (inputStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
    }

    /**
     * Load sampled bitmap from url
     *
     * @param url            URL
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromUrl(@NonNull String url, int requiredWidth, int requiredHeight)
            throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = null;
        try {
            inputStream = InternalUtils.getDataStreamFromUrl(url);
            if (inputStream == null) {
                return null;
            }
            BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        inputStream = null;
        try {
            inputStream = InternalUtils.getDataStreamFromUrl(url);
            if (inputStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
    }

    /**
     * Load sampled bitmap from file
     *
     * @param file           File
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromFile(@NonNull File file, int requiredWidth, int requiredHeight)
            throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }

    }

    /**
     * Load sampled bitmap from file descriptor
     *
     * @param fileDescriptor File descriptor
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromFileDescriptor(@NonNull FileDescriptor fileDescriptor, int requiredWidth,
            int requiredHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileDescriptor);
            BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        inputStream = null;
        try {
            inputStream = new FileInputStream(fileDescriptor);
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            InternalUtils.close(inputStream);
        }
    }

    /**
     * Load sampled bitmap from resource
     *
     * @param resources      Resources
     * @param resourceId     Resource id
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromResource(@NonNull Resources resources, int resourceId, int requiredWidth,
            int requiredHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resourceId, options);
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resourceId, options);
    }

    /**
     * Load sampled bitmap from byte array
     *
     * @param byteArray      Byte array
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromByteArray(@NonNull byte[] byteArray, int requiredWidth,
            int requiredHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
    }

    private static void calculateSampleSize(@NonNull BitmapFactory.Options options, int requiredWidth,
            int requiredHeight) {
        int sampleSize = 1;
        int width = options.outWidth;
        int height = options.outHeight;
        int threshold = Math.max(requiredWidth, requiredHeight) / 4;
        while (Math.abs(width - requiredWidth) > threshold && Math.abs(height - requiredHeight) > threshold) {
            width /= 2;
            height /= 2;
            sampleSize *= 2;
        }
        options.inSampleSize = sampleSize;
    }
}
