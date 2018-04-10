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
package com.budiyev.android.imageloader

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.annotation.MainThread
import android.support.media.ExifInterface
import android.view.View
import android.widget.ImageView
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

internal object InternalUtils {
    private val CONNECT_TIMEOUT = 10000
    private val MAX_POOL_SIZE = 4
    private val MIN_POOL_SIZE = 1
    private val URI_SCHEME_HTTP = "http"
    private val URI_SCHEME_HTTPS = "https"
    private val URI_SCHEME_FTP = "ftp"

    val loadPoolSize: Int
        get() = Math.min(Runtime.getRuntime().availableProcessors(), MAX_POOL_SIZE)

    val cachePoolSize: Int
        get() {
            val size = loadPoolSize / 2
            return if (size < MIN_POOL_SIZE) {
                MIN_POOL_SIZE
            } else {
                size
            }
        }

    fun invalidate(memoryCache: ImageCache?, storageCache: ImageCache?, descriptor: DataDescriptor<*>) {
        val key = descriptor.key ?: return
        memoryCache?.remove(key)
        storageCache?.remove(key)
    }

    fun buildFullKey(base: String?, requiredSize: Size?, transformation: BitmapTransformation?): String? {
        if (base == null) {
            return null
        }
        if (requiredSize == null && transformation == null) {
            return base
        }
        val sb = StringBuilder(base)
        if (requiredSize != null) {
            sb.append("_required_size_").append(requiredSize.width).append("x").append(requiredSize.height)
        }
        if (transformation != null) {
            sb.append(transformation.key)
        }
        return sb.toString()
    }

    fun close(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (ignored: IOException) {
        }

    }

    @Throws(IOException::class)
    fun getDataStreamFromUri(context: Context, uri: Uri): InputStream? {
        val scheme = uri.scheme
        if (URI_SCHEME_HTTP.equals(scheme, ignoreCase = true) || URI_SCHEME_HTTPS.equals(scheme,
                        ignoreCase = true) || URI_SCHEME_FTP.equals(scheme, ignoreCase = true)) {
            val connection = URL(uri.toString()).openConnection()
            connection.connectTimeout = CONNECT_TIMEOUT
            return connection.getInputStream()
        } else {
            return context.contentResolver.openInputStream(uri)
        }
    }

    @Throws(IOException::class)
    fun getDataStreamFromUrl(url: String): InputStream? {
        val connection = URL(url).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT
        return connection.getInputStream()
    }

    @MainThread
    fun getDisplayImageAction(view: View?): DisplayImageAction<*>? {
        if (view != null) {
            val drawable = getDrawable(view)
            if (drawable is PlaceholderDrawable) {
                return drawable.action
            }
        }
        return null
    }

    fun setDrawable(drawable: Drawable, view: View) {
        if (view is ImageView) {
            view.setImageDrawable(drawable)
        } else {
            view.background = drawable
        }
    }

    fun setBitmap(resources: Resources, bitmap: Bitmap, view: View) {
        if (view is ImageView) {
            view.setImageBitmap(bitmap)
        } else {
            view.background = BitmapDrawable(resources, bitmap)
        }
    }

    private fun getDrawable(view: View): Drawable? {
        return if (view is ImageView) {
            view.drawable
        } else {
            view.background
        }
    }

    fun isUriLocal(uri: Uri): Boolean {
        return isUriSchemeLocal(uri.scheme)
    }

    fun isUriLocal(uri: String): Boolean {
        val ssi = uri.indexOf(':')
        return ssi != -1 && isUriSchemeLocal(uri.substring(0, ssi))

    }

    private fun isUriSchemeLocal(scheme: String): Boolean {
        return ContentResolver.SCHEME_FILE == scheme || ContentResolver.SCHEME_CONTENT == scheme || ContentResolver.SCHEME_ANDROID_RESOURCE == scheme
    }

    fun getExifRotation(context: Context, uri: Uri): Int {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            return if (inputStream != null) {
                getExifRotation(ExifInterface(inputStream))
            } else {
                0
            }
        } catch (e: IOException) {
            return 0
        } finally {
            close(inputStream)
        }
    }

    fun getExifRotation(file: File): Int {
        try {
            return getExifRotation(ExifInterface(file.absolutePath))
        } catch (e: IOException) {
            return 0
        }

    }

    fun getExifRotation(bytes: ByteArray): Int {
        try {
            return getExifRotation(ExifInterface(ByteArrayInputStream(bytes)))
        } catch (e: IOException) {
            return 0
        }

    }

    fun getExifRotation(exifInterface: ExifInterface): Int {
        when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> return 90
            ExifInterface.ORIENTATION_ROTATE_180 -> return 180
            ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            else -> return 0
        }
    }

    fun rotateAndRecycle(bitmap: Bitmap, rotation: Int): Bitmap {
        val rotated = ImageUtils.rotate(bitmap, rotation.toFloat())
        if (bitmap != rotated) {
            bitmap.recycle()
        }
        return rotated
    }

    fun <T> requireNonNull(value: T?): T {
        if (value == null) {
            throw NullPointerException()
        }
        return value
    }
}
