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

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.annotation.WorkerThread
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object DataUtils {
    private const val HASH_ALGORITHM_SHA256 = "SHA-256"

    /**
     * Generate SHA-256 hash string with [Character.MAX_RADIX] radix
     * for specified [String]; usable for keys of [DataDescriptor] implementations
     *
     * @param string Source string
     * @return SHA-256 hash string
     * @see DataDescriptor.key
     */
    fun generateSHA256(string: String): String {
        try {
            val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM_SHA256)
            messageDigest.update(string.toByteArray())
            return BigInteger(1, messageDigest.digest()).toString(Character.MAX_RADIX)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Load sampled bitmap from uri
     *
     * @param context        Context
     * @param uri            URI
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or `null`
     */
    @WorkerThread
    @Throws(IOException::class)
    fun loadSampledBitmapFromUri(context: Context, uri: Uri, requiredWidth: Int, requiredHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var inputStream: InputStream? = null
        try {
            inputStream = InternalUtils.getDataStreamFromUri(context, uri)
            if (inputStream == null) {
                return null
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
        calculateSampleSize(options, requiredWidth, requiredHeight)
        options.inJustDecodeBounds = false
        inputStream = null
        try {
            inputStream = InternalUtils.getDataStreamFromUri(context, uri)
            return if (inputStream == null) {
                null
            } else BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
    }

    /**
     * Load sampled bitmap from url
     *
     * @param url            URL
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or `null`
     */
    @WorkerThread
    @Throws(IOException::class)
    fun loadSampledBitmapFromUrl(url: String, requiredWidth: Int, requiredHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var inputStream: InputStream? = null
        try {
            inputStream = InternalUtils.getDataStreamFromUrl(url)
            if (inputStream == null) {
                return null
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
        calculateSampleSize(options, requiredWidth, requiredHeight)
        options.inJustDecodeBounds = false
        inputStream = null
        try {
            inputStream = InternalUtils.getDataStreamFromUrl(url)
            return if (inputStream == null) {
                null
            } else BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
    }

    /**
     * Load sampled bitmap from file
     *
     * @param file           File
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or `null`
     */
    @WorkerThread
    @Throws(IOException::class)
    fun loadSampledBitmapFromFile(file: File, requiredWidth: Int, requiredHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
            BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
        calculateSampleSize(options, requiredWidth, requiredHeight)
        options.inJustDecodeBounds = false
        inputStream = null
        try {
            inputStream = FileInputStream(file)
            return BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }

    }

    /**
     * Load sampled bitmap from file descriptor
     *
     * @param fileDescriptor File descriptor
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or `null`
     */
    @WorkerThread
    fun loadSampledBitmapFromFileDescriptor(fileDescriptor: FileDescriptor, requiredWidth: Int,
            requiredHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(fileDescriptor)
            BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
        calculateSampleSize(options, requiredWidth, requiredHeight)
        options.inJustDecodeBounds = false
        inputStream = null
        try {
            inputStream = FileInputStream(fileDescriptor)
            return BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            InternalUtils.close(inputStream)
        }
    }

    /**
     * Load sampled bitmap from resource
     *
     * @param resources      Resources
     * @param resourceId     Resource id
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or `null`
     */
    @WorkerThread
    fun loadSampledBitmapFromResource(resources: Resources, resourceId: Int, requiredWidth: Int,
            requiredHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, resourceId, options)
        calculateSampleSize(options, requiredWidth, requiredHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(resources, resourceId, options)
    }

    /**
     * Load sampled bitmap from byte array
     *
     * @param byteArray      Byte array
     * @param requiredWidth  Required width
     * @param requiredHeight Required height
     * @return Loaded bitmap or `null`
     */
    @WorkerThread
    fun loadSampledBitmapFromByteArray(byteArray: ByteArray, requiredWidth: Int, requiredHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        calculateSampleSize(options, requiredWidth, requiredHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    }

    private fun calculateSampleSize(options: BitmapFactory.Options, requiredWidth: Int, requiredHeight: Int) {
        var width = options.outWidth
        var height = options.outHeight
        val threshold = Math.max(requiredWidth, requiredHeight) / 4
        if (width <= requiredWidth + threshold || height <= requiredHeight + threshold) {
            return
        }
        var sampleSize = 1
        while (width - requiredWidth > threshold && height - requiredHeight > threshold) {
            width /= 2
            height /= 2
            sampleSize *= 2
        }
        options.inSampleSize = sampleSize
    }
}
