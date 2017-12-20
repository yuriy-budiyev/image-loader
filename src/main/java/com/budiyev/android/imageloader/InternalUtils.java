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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.view.View;
import android.widget.ImageView;

final class InternalUtils {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int MAX_POOL_SIZE = 4;
    private static final String URI_SCHEME_HTTP = "http";
    private static final String URI_SCHEME_HTTPS = "https";
    private static final String URI_SCHEME_FTP = "ftp";

    private InternalUtils() {
    }

    public static void close(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    @Nullable
    public static InputStream getDataStreamFromUri(@NonNull Context context, @NonNull Uri uri) throws IOException {
        String scheme = uri.getScheme();
        if (URI_SCHEME_HTTP.equalsIgnoreCase(scheme) || URI_SCHEME_HTTPS.equalsIgnoreCase(scheme) ||
                URI_SCHEME_FTP.equalsIgnoreCase(scheme)) {
            URLConnection connection = new URL(uri.toString()).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            return connection.getInputStream();
        } else {
            return context.getContentResolver().openInputStream(uri);
        }
    }

    @Nullable
    public static InputStream getDataStreamFromUrl(@NonNull String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        return connection.getInputStream();
    }

    @Nullable
    @MainThread
    public static DisplayImageAction<?> getDisplayImageAction(@Nullable View view) {
        if (view != null) {
            Drawable drawable = getDrawable(view);
            if (drawable instanceof PlaceholderDrawable) {
                return ((PlaceholderDrawable) drawable).getAction();
            }
        }
        return null;
    }

    public static void setDrawable(@NonNull Drawable drawable, @NonNull View view) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageDrawable(drawable);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(drawable);
            } else {
                view.setBackgroundDrawable(drawable);
            }
        }
    }

    public static void setBitmap(@NonNull Resources resources, @NonNull Bitmap bitmap, @NonNull View view) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageBitmap(bitmap);
        } else {
            Drawable drawable = new BitmapDrawable(resources, bitmap);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(drawable);
            } else {
                view.setBackgroundDrawable(drawable);
            }
        }
    }

    @Nullable
    public static Drawable getDrawable(@NonNull View view) {
        if (view instanceof ImageView) {
            return ((ImageView) view).getDrawable();
        } else {
            return view.getBackground();
        }
    }

    public static int getPoolSize() {
        return Math.min(Runtime.getRuntime().availableProcessors(), MAX_POOL_SIZE);
    }

    public static boolean isUriLocal(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        return ContentResolver.SCHEME_FILE.equals(scheme) || ContentResolver.SCHEME_CONTENT.equals(scheme) ||
                ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme);
    }

    public static int getExifRotation(@NonNull Context context, @NonNull Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                return getExifRotation(new ExifInterface(inputStream));
            } else {
                return 0;
            }
        } catch (IOException e) {
            return 0;
        } finally {
            close(inputStream);
        }
    }

    public static int getExifRotation(@NonNull File file) {
        try {
            return getExifRotation(new ExifInterface(file.getAbsolutePath()));
        } catch (IOException e) {
            return 0;
        }
    }

    public static int getExifRotation(@NonNull byte[] bytes) {
        try {
            return getExifRotation(new ExifInterface(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            return 0;
        }
    }

    public static int getExifRotation(@NonNull ExifInterface exifInterface) {
        switch (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    @NonNull
    public static Bitmap rotateAndRecycle(@NonNull Bitmap bitmap, int rotation) {
        Bitmap rotated = ImageUtils.rotate(bitmap, rotation);
        if (bitmap != rotated) {
            bitmap.recycle();
        }
        return rotated;
    }
}
