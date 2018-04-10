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

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.net.Uri
import android.os.Handler
import java.io.File
import java.io.FileDescriptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

/**
 * Image loader is a universal tool for loading bitmaps efficiently in Android
 *
 * @see .with
 *
 * @see .builder
 */
class ImageLoader
/**
 * Image Loader
 *
 * @see .with
 *
 * @see .builder
 */
internal constructor(private val mContext: Context, private val mLoadExecutor: ExecutorService,
        private val mCacheExecutor: ExecutorService, private val mMemoryCache: ImageCache?,
        private val mStorageCache: ImageCache?) {
    private val mMainThreadHandler: Handler
    private val mPauseLock = PauseLock()
    private val mDescriptorFactories = ConcurrentHashMap<String, DataDescriptorFactory<Any>>()
    private val mBitmapLoaders = ConcurrentHashMap<String, BitmapLoader<Any>>()

    /**
     * Whether loading is currently paused
     */
    val isLoadingPaused: Boolean
        get() = mPauseLock.isPaused

    init {
        mMainThreadHandler = Handler(mContext.mainLooper)
    }

    /**
     * Create new image request
     * <br></br><br></br>
     * **Data types, supported by default:**
     *
     *  * [Uri], [String] - URI (remote and local)
     *  * [File] - File
     *  * [Integer] - Android resource
     *  * [FileDescriptor] - File descriptor
     *  * `byte[]` - Byte array
     *
     *
     * @param data Source data, any registered data type
     * @return New image request for specified data
     * @throws IllegalArgumentException if specified data type is not registered
     * @see .registerDataType
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> from(data: T): ImageRequest<T> {
        val dataClassName = data.javaClass.name
        val descriptorFactory = mDescriptorFactories[dataClassName] as? DataDescriptorFactory<T>
        val bitmapLoader = mBitmapLoaders[dataClassName] as? BitmapLoader<T>
        if (descriptorFactory == null || bitmapLoader == null) {
            throw IllegalArgumentException("Unsupported data type: $dataClassName")
        }
        return ImageRequest(mContext.resources, mLoadExecutor, mCacheExecutor, mPauseLock, mMainThreadHandler,
                mMemoryCache, mStorageCache, bitmapLoader, descriptorFactory.newDescriptor(data))
    }

    /**
     * Delete all cached images for specified data
     *
     * @param data Data
     * @throws IllegalArgumentException if specified data type is not registered
     * @see .registerDataType
     */
    fun invalidate(data: Any) {
        val dataClassName = data.javaClass.name
        val descriptorFactory = mDescriptorFactories[dataClassName] ?: throw IllegalArgumentException(
                "Unsupported data type: $dataClassName")
        InternalUtils.invalidate(mMemoryCache, mStorageCache, descriptorFactory.newDescriptor(data))
    }

    /**
     * Register data type
     *
     * @param dataClass         Source data class
     * @param descriptorFactory Data descriptor factory for specified data class
     * @param bitmapLoader      Bitmap loader factory for specified data class
     * @see DataDescriptorFactory
     *
     * @see DataDescriptor
     *
     * @see BitmapLoader
     *
     * @see .unregisterDataType
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> registerDataType(dataClass: Class<T>, descriptorFactory: DataDescriptorFactory<T>,
            bitmapLoader: BitmapLoader<T>) {
        val dataClassName = dataClass.name
        mDescriptorFactories[dataClassName] =
                InternalUtils.requireNonNull(descriptorFactory) as DataDescriptorFactory<Any>
        mBitmapLoaders[dataClassName] = InternalUtils.requireNonNull(bitmapLoader) as BitmapLoader<Any>
    }

    /**
     * Unregister data type
     *
     * @param dataClass Source data class
     */
    fun unregisterDataType(dataClass: Class<*>) {
        val dataClassName = dataClass.name
        mDescriptorFactories.remove(dataClassName)
        mBitmapLoaders.remove(dataClassName)
    }

    /**
     * Whether to pause image loading. If this method is invoked with `true` parameter,
     * all loading actions will be paused until it will be invoked with `false`.
     */
    fun setPauseLoading(paused: Boolean) {
        mPauseLock.isPaused = paused
    }

    /**
     * Set all loading tasks to finish before any loading actions started
     */
    fun setInterruptLoadingEarly(interrupt: Boolean) {
        mPauseLock.setInterruptEarly(interrupt)
    }

    /**
     * Clear memory cache;
     * for better memory management when one singleton loader instance used across the app,
     * this method should be called in [)][Application.onTrimMemory] or [ComponentCallbacks2.onTrimMemory],
     * default instance ([.with]) automatically cares about it
     *
     * @see ComponentCallbacks2
     *
     * @see Context.registerComponentCallbacks
     */
    fun clearMemoryCache() {
        val memoryCache = mMemoryCache
        memoryCache?.clear()
    }

    /**
     * Clear storage cache
     */
    fun clearStorageCache() {
        val storageCache = mStorageCache
        storageCache?.clear()
    }

    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        clearMemoryCache()
        clearStorageCache()
    }

    companion object {

        /**
         * Get default image loader instance,
         * automatically cares about memory and storage caching
         */
        fun with(context: Context): ImageLoader {
            return ImageLoaderHolder.get(context)
        }

        /**
         * Create new image loader builder instance,
         * use application context to avoid memory leaks
         * if instance is static
         */
        fun builder(context: Context): ImageLoaderBuilder {
            return ImageLoaderBuilder(context)
        }
    }
}
