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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.ReentrantLock

internal class StorageImageCache(directory: File, compressMode: CompressMode, private val mMaxSize: Long) : ImageCache {
    private val mLock = ReentrantLock()
    private val mFiles = LinkedHashMap<String, File>(0, 0.75f, true)
    private val mFileFilter = CacheFileFilter()
    private val mFileComparator = FileComparator()
    private val mCompressMode: CompressMode
    private val mDirectory: File
    @Volatile
    private var mInitialized: Boolean = false
    @Volatile
    private var mSize: Long = 0

    constructor(context: Context) : this(getDefaultDirectory(context)) {}

    constructor(context: Context, maxSize: Long) : this(getDefaultDirectory(context), maxSize) {}

    constructor(context: Context, compressMode: CompressMode, maxSize: Long) : this(getDefaultDirectory(context),
            compressMode, maxSize) {
    }

    @JvmOverloads constructor(directory: File, maxSize: Long = DEFAULT_MAX_SIZE) : this(directory,
            CompressMode.LOSSLESS, maxSize) {
    }

    init {
        mDirectory = InternalUtils.requireNonNull(directory)
        mCompressMode = InternalUtils.requireNonNull(compressMode)
        if (mMaxSize < 0L) {
            throw IllegalArgumentException("Cache size should be greater than or equal to zero")
        }
    }

    override fun get(key: String): Bitmap? {
        val file: File?
        mLock.lock()
        try {
            initialize()
            file = mFiles[key]
        } finally {
            mLock.unlock()
        }
        if (file == null || !file.exists()) {
            return null
        }
        var bitmap: Bitmap? = null
        var inputStream: FileInputStream? = null
        try {
            inputStream = FileInputStream(file)
            val outputBuffer = ByteBuffer(BUFFER_SIZE)
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (true) {
                read = inputStream.read(buffer, 0, buffer.size)
                if (read == -1) {
                    break
                }
                outputBuffer.write(buffer, 0, read)
            }
            bitmap = BitmapFactory.decodeByteArray(outputBuffer.array, 0, outputBuffer.size)
        } catch (ignored: IOException) {
        } finally {
            InternalUtils.close(inputStream)
        }
        if (bitmap != null) {
            file.setLastModified(System.currentTimeMillis())
            return bitmap
        } else {
            file.delete()
            return null
        }
    }

    override fun put(key: String, value: Bitmap) {
        val file = File(mDirectory, key)
        if (file.exists()) {
            file.delete()
        }
        val outputBuffer = ByteBuffer(BUFFER_SIZE)
        if (value.compress(mCompressMode.format, mCompressMode.quality, outputBuffer)) {
            val array = outputBuffer.array
            val outputSize = outputBuffer.size
            var output: FileOutputStream? = null
            var success: Boolean
            try {
                output = FileOutputStream(file)
                var remaining = outputSize
                var write: Int
                while (remaining > 0) {
                    write = Math.min(remaining, BUFFER_SIZE)
                    output.write(array, outputSize - remaining, write)
                    remaining -= write
                }
                success = true
            } catch (e: IOException) {
                success = false
            } finally {
                InternalUtils.close(output)
            }
            if (success) {
                mLock.lock()
                try {
                    initialize()
                    mFiles[key] = file
                    var cacheSize = mSize
                    cacheSize += file.length()
                    val maxCacheSize = mMaxSize
                    if (cacheSize > maxCacheSize) {
                        val i = mFiles.entries.iterator()
                        while (i.hasNext()) {
                            val f = i.next().value
                            cacheSize -= f.length()
                            i.remove()
                            f.delete()
                            if (cacheSize <= maxCacheSize) {
                                break
                            }
                        }
                    }
                    mSize = cacheSize
                } finally {
                    mLock.unlock()
                }
            } else {
                file.delete()
            }
        }
    }

    override fun remove(key: String) {
        val files = mDirectory.listFiles(RemoveFileFilter(key))
        if (files == null || files.size == 0) {
            return
        }
        for (file in files) {
            mLock.lock()
            try {
                initialize()
                mFiles.remove(file.name)
                mSize -= file.length()
            } finally {
                mLock.unlock()
            }
            file.delete()
        }
    }

    override fun clear() {
        mLock.lock()
        try {
            initialize()
            mFiles.clear()
            mSize = 0L
        } finally {
            mLock.unlock()
        }
        val files = mDirectory.listFiles(mFileFilter)
        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private fun initialize() {
        if (!mInitialized) {
            val directory = mDirectory
            if (directory.exists()) {
                val files = directory.listFiles(mFileFilter)
                if (files != null && files.size != 0) {
                    Arrays.sort(files, mFileComparator)
                    var size: Long = 0
                    for (file in files) {
                        mFiles[file.name] = file
                        size += file.length()
                    }
                    var i = files.size - 1
                    while (i >= 0 && size > mMaxSize) {
                        val file = files[i]
                        mFiles.remove(file.name)
                        size -= file.length()
                        file.delete()
                        i--
                    }
                    mSize = size
                }
            } else {
                directory.mkdirs()
            }
            mInitialized = true
        }
    }

    private class CacheFileFilter : FileFilter {
        override fun accept(pathname: File): Boolean {
            return pathname.isFile
        }
    }

    private class RemoveFileFilter(name: String) : FileFilter {
        private val mName: String = name.toLowerCase()

        override fun accept(pathname: File): Boolean {
            return pathname.isFile && pathname.name.toLowerCase().startsWith(mName)
        }
    }

    private class FileComparator : Comparator<File> {
        override fun compare(lhs: File, rhs: File): Int {
            return java.lang.Long.signum(rhs.lastModified() - lhs.lastModified())
        }
    }

    companion object {
        val DEFAULT_DIRECTORY = "image_loader_cache"
        val DEFAULT_MAX_SIZE = 268435456L
        private val BUFFER_SIZE = 16384

        private fun getDefaultDirectory(context: Context): File {
            var directory = context.externalCacheDir
            if (directory == null) {
                directory = context.cacheDir
            }
            return File(directory, DEFAULT_DIRECTORY)
        }
    }
}
