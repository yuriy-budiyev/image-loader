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
import android.net.Uri
import android.support.annotation.IntRange
import java.io.File
import java.io.FileDescriptor
import java.util.concurrent.ExecutorService

/**
 * Image loader builder
 */
class ImageLoaderBuilder internal constructor(private val mContext: Context) {
    private var mMemoryCache: ImageCache? = null
    private var mStorageCache: ImageCache? = null
    private var mLoadExecutor: ExecutorService? = null
    private var mCacheExecutor: ExecutorService? = null

    /**
     * Default memory cache
     */
    fun memoryCache(): ImageLoaderBuilder {
        mMemoryCache = MemoryImageCache()
        return this
    }

    /**
     * Memory cache with specified maximum size
     */
    fun memoryCache(@IntRange(from = 0) maxSize: Int): ImageLoaderBuilder {
        mMemoryCache = MemoryImageCache(maxSize)
        return this
    }

    /**
     * Custom memory cache
     */
    fun memoryCache(memoryCache: ImageCache?): ImageLoaderBuilder {
        mMemoryCache = memoryCache
        return this
    }

    /**
     * Default storage cache,
     * located in subdirectory of [Context.getExternalCacheDir]
     */
    fun storageCache(): ImageLoaderBuilder {
        mStorageCache = StorageImageCache(mContext)
        return this
    }

    /**
     * Default storage cache with specified maximum size,
     * located in subdirectory of [Context.getExternalCacheDir]
     */
    fun storageCache(@IntRange(from = 0L) maxSize: Long): ImageLoaderBuilder {
        mStorageCache = StorageImageCache(mContext, maxSize)
        return this
    }

    /**
     * Default storage cache with specified maximum size and compress mode,
     * located in subdirectory of [Context.getExternalCacheDir]
     *
     * @see CompressMode
     */
    fun storageCache(compressMode: CompressMode, @IntRange(from = 0L) maxSize: Long): ImageLoaderBuilder {
        mStorageCache = StorageImageCache(mContext, compressMode, maxSize)
        return this
    }

    /**
     * Storage cache with specified directory
     */
    fun storageCache(directory: File): ImageLoaderBuilder {
        mStorageCache = StorageImageCache(directory)
        return this
    }

    /**
     * Storage cache with specified directory and maximum size
     */
    fun storageCache(directory: File, @IntRange(from = 0L) maxSize: Long): ImageLoaderBuilder {
        mStorageCache = StorageImageCache(directory, maxSize)
        return this
    }

    /**
     * Storage cache with specified directory, maximum size and compress mode
     *
     * @see CompressMode
     */
    fun storageCache(directory: File, compressMode: CompressMode, maxSize: Long): ImageLoaderBuilder {
        mStorageCache = StorageImageCache(directory, compressMode, maxSize)
        return this
    }

    /**
     * Custom storage cache
     */
    fun storageCache(storageCache: ImageCache?): ImageLoaderBuilder {
        mStorageCache = storageCache
        return this
    }

    /**
     * Custom load executor
     */
    fun loadExecutor(executor: ExecutorService?): ImageLoaderBuilder {
        mLoadExecutor = executor
        return this
    }

    /**
     * Custom storage cache executor
     */
    fun cacheExecutor(executor: ExecutorService?): ImageLoaderBuilder {
        mCacheExecutor = executor
        return this
    }

    /**
     * Create new image loader instance with specified parameters
     */
    fun build(): ImageLoader {
        var loadExecutor = mLoadExecutor
        if (loadExecutor == null) {
            loadExecutor = ImageLoaderExecutor(InternalUtils.loadPoolSize)
        }
        var cacheExecutor = mCacheExecutor
        if (cacheExecutor == null) {
            cacheExecutor = ImageLoaderExecutor(InternalUtils.cachePoolSize)
        }
        val context = mContext
        val loader = ImageLoader(context, loadExecutor, cacheExecutor, mMemoryCache, mStorageCache)
        loader.registerDataType(Uri::class.java, UriDataDescriptorFactory(), UriBitmapLoader(context))
        loader.registerDataType(File::class.java, FileDataDescriptorFactory(), FileBitmapLoader())
        loader.registerDataType(String::class.java, StringUriDataDescriptorFactory(), StringUriBitmapLoader(context))
        loader.registerDataType(Int::class.java, ResourceDataDescriptorFactory(), ResourceBitmapLoader(context))
        loader.registerDataType(FileDescriptor::class.java, FileDescriptorDataDescriptorFactory(),
                FileDescriptorBitmapLoader())
        loader.registerDataType(ByteArray::class.java, ByteArrayDataDescriptorFactory(), ByteArrayBitmapLoader())
        return loader
    }
}
