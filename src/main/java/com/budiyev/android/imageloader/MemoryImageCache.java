package com.budiyev.android.imageloader;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

final class MemoryImageCache implements ImageCache {
    private static final float DEFAULT_MEMORY_FRACTION = 0.25f;
    private final LruCache<String, Bitmap> mCache;

    public MemoryImageCache() {
        this(Math.round(Runtime.getRuntime().maxMemory() * DEFAULT_MEMORY_FRACTION));
    }

    public MemoryImageCache(int maxSize) {
        mCache = new InternalCacheImpl(maxSize);
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        return mCache.get(key);
    }

    @Override
    public void put(@NonNull String key, @NonNull Bitmap value) {
        mCache.put(key, value);
    }

    @Override
    public void remove(@NonNull String key) {
        mCache.remove(key);
    }

    @Override
    public void clear() {
        mCache.evictAll();
    }

    private static final class InternalCacheImpl extends LruCache<String, Bitmap> {
        public InternalCacheImpl(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return value.getAllocationByteCount();
            } else {
                return value.getByteCount();
            }
        }
    }
}
