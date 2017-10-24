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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("SameParameterValue")
public final class DataUtils {
    private static final String HASH_ALGORITHM_SHA256 = "SHA-256";

    private DataUtils() {
    }

    /**
     * Generate SHA-256 hash string with {@link Character#MAX_RADIX} radix
     * for specified {@link String}; usable for keys of {@link DataDescriptor} implementations
     *
     * @param string Source string
     * @return SHA-256 hash string
     * @see DataDescriptor#getKey()
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
     * @param context                   Context
     * @param uri                       Uri
     * @param requiredWidth             Required width
     * @param requiredHeight            Required height
     * @param ignoreTotalNumberOfPixels Ignore total number of pixels
     *                                  (requiredWidth * requiredHeight)
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromUri(@NonNull Context context, @NonNull Uri uri,
            int requiredWidth, int requiredHeight, boolean ignoreTotalNumberOfPixels) throws
            IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream inputStream = InternalUtils.getDataStreamFromUri(context, uri)) {
            BitmapFactory.decodeStream(inputStream, null, options);
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize =
                calculateSampleSize(options.outWidth, options.outHeight, requiredWidth,
                        requiredHeight, ignoreTotalNumberOfPixels);
        try (InputStream inputStream = InternalUtils.getDataStreamFromUri(context, uri)) {
            return BitmapFactory.decodeStream(inputStream, null, options);
        }
    }

    /**
     * Load sampled bitmap from file
     *
     * @param file                      File
     * @param requiredWidth             Required width
     * @param requiredHeight            Required height
     * @param ignoreTotalNumberOfPixels Ignore total number of pixels
     *                                  (requiredWidth * requiredHeight)
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromFile(@NonNull File file, int requiredWidth,
            int requiredHeight, boolean ignoreTotalNumberOfPixels) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream inputStream = new FileInputStream(file)) {
            BitmapFactory.decodeStream(inputStream, null, options);
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize =
                calculateSampleSize(options.outWidth, options.outHeight, requiredWidth,
                        requiredHeight, ignoreTotalNumberOfPixels);
        try (InputStream inputStream = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(inputStream, null, options);
        }
    }

    /**
     * Load sampled bitmap from file descriptor
     *
     * @param fileDescriptor            File descriptor
     * @param requiredWidth             Required width
     * @param requiredHeight            Required height
     * @param ignoreTotalNumberOfPixels Ignore total number of pixels
     *                                  (requiredWidth * requiredHeight)
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromFileDescriptor(@NonNull FileDescriptor fileDescriptor,
            int requiredWidth, int requiredHeight, boolean ignoreTotalNumberOfPixels) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = new FileInputStream(fileDescriptor);
        BitmapFactory.decodeStream(inputStream, null, options);
        InternalUtils.close(inputStream);
        options.inJustDecodeBounds = false;
        options.inSampleSize =
                calculateSampleSize(options.outWidth, options.outHeight, requiredWidth,
                        requiredHeight, ignoreTotalNumberOfPixels);
        inputStream = new FileInputStream(fileDescriptor);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        InternalUtils.close(inputStream);
        return bitmap;
    }

    /**
     * Load sampled bitmap from resource
     *
     * @param resources                 Resources
     * @param resourceId                Resource id
     * @param requiredWidth             Required width
     * @param requiredHeight            Required height
     * @param ignoreTotalNumberOfPixels Ignore total number of pixels
     *                                  (requiredWidth * requiredHeight)
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromResource(@NonNull Resources resources, int resourceId,
            int requiredWidth, int requiredHeight, boolean ignoreTotalNumberOfPixels) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        TypedValue typedValue = new TypedValue();
        options.inJustDecodeBounds = true;
        options.inTargetDensity = resources.getDisplayMetrics().densityDpi;
        InputStream inputStream = resources.openRawResource(resourceId, typedValue);
        if (typedValue.density == TypedValue.DENSITY_DEFAULT) {
            options.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        } else if (typedValue.density != TypedValue.DENSITY_NONE) {
            options.inDensity = typedValue.density;
        }
        BitmapFactory.decodeStream(inputStream, null, options);
        InternalUtils.close(inputStream);
        options.inJustDecodeBounds = false;
        options.inSampleSize =
                calculateSampleSize(options.outWidth, options.outHeight, requiredWidth,
                        requiredHeight, ignoreTotalNumberOfPixels);
        inputStream = resources.openRawResource(resourceId, typedValue);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        InternalUtils.close(inputStream);
        return bitmap;

    }

    /**
     * Load sampled bitmap from byte array
     *
     * @param byteArray                 Byte array
     * @param requiredWidth             Required width
     * @param requiredHeight            Required height
     * @param ignoreTotalNumberOfPixels Ignore total number of pixels
     *                                  (requiredWidth * requiredHeight)
     * @return Loaded bitmap or {@code null}
     */
    @Nullable
    @WorkerThread
    public static Bitmap loadSampledBitmapFromByteArray(@NonNull byte[] byteArray,
            int requiredWidth, int requiredHeight, boolean ignoreTotalNumberOfPixels) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
        options.inSampleSize =
                calculateSampleSize(options.outWidth, options.outHeight, requiredWidth,
                        requiredHeight, ignoreTotalNumberOfPixels);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
    }

    private static int calculateSampleSize(int sourceWidth, int sourceHeight, int requiredWidth,
            int requiredHeight, boolean ignoreTotalNumberOfPixels) {
        int sampleSize = 1;
        if (sourceWidth > requiredWidth || sourceHeight > requiredHeight) {
            int halfWidth = sourceWidth / 2;
            int halfHeight = sourceHeight / 2;
            while ((halfWidth / sampleSize) > requiredWidth &&
                    (halfHeight / sampleSize) > requiredHeight) {
                sampleSize *= 2;
            }
            if (ignoreTotalNumberOfPixels) {
                return sampleSize;
            }
            long totalPixels = (sourceWidth * sourceHeight) / (sampleSize * sampleSize);
            long totalRequiredPixels = requiredWidth * requiredHeight;
            while (totalPixels > totalRequiredPixels) {
                sampleSize *= 2;
                totalPixels /= 4L;
            }
        }
        return sampleSize;
    }
}
