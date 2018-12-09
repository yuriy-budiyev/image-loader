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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

@SuppressWarnings("SameParameterValue")
public final class DataUtils {
    private static final String HASH_ALGORITHM_SHA256 = "SHA-256";
    private static final char[] HEX_DIGITS =
            new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
                    'f'};

    private DataUtils() {
    }

    /**
     * Generate SHA-256 hash string for specified {@link String},
     * usable for keys of {@link DataDescriptor} implementations
     *
     * @param string Source string
     * @return SHA-256 hash string
     * @see DataDescriptor#getKey
     */
    @NonNull
    public static String generateSha256(@NonNull final String string) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM_SHA256);
            messageDigest.update(string.getBytes());
            final byte[] digest = messageDigest.digest();
            final StringBuilder hexBuilder = new StringBuilder(digest.length << 1);
            for (final byte b : digest) {
                hexBuilder.append(HEX_DIGITS[b >> 4 & 15]);
                hexBuilder.append(HEX_DIGITS[b & 15]);
            }
            return hexBuilder.toString();
        } catch (final NoSuchAlgorithmException e) {
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
    public static Bitmap loadSampledBitmapFromUri(@NonNull final Context context,
            @NonNull final Uri uri, final int requiredWidth, final int requiredHeight)
            throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
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
    public static Bitmap loadSampledBitmapFromUrl(@NonNull final String url,
            final int requiredWidth, final int requiredHeight) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
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
    public static Bitmap loadSampledBitmapFromFile(@NonNull final File file,
            final int requiredWidth, final int requiredHeight) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
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
    public static Bitmap loadSampledBitmapFromFileDescriptor(
            @NonNull final FileDescriptor fileDescriptor, final int requiredWidth,
            final int requiredHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
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
    public static Bitmap loadSampledBitmapFromResource(@NonNull final Resources resources,
            final int resourceId, final int requiredWidth, final int requiredHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
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
    public static Bitmap loadSampledBitmapFromByteArray(@NonNull final byte[] byteArray,
            final int requiredWidth, final int requiredHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
        calculateSampleSize(options, requiredWidth, requiredHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
    }

    private static void calculateSampleSize(@NonNull final BitmapFactory.Options options,
            final int requiredWidth, final int requiredHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        final int threshold = Math.max(requiredWidth, requiredHeight) / 4;
        if (width <= requiredWidth + threshold || height <= requiredHeight + threshold) {
            return;
        }
        int sampleSize = 1;
        while (width - requiredWidth > threshold && height - requiredHeight > threshold) {
            width /= 2;
            height /= 2;
            sampleSize *= 2;
        }
        options.inSampleSize = sampleSize;
    }
}
