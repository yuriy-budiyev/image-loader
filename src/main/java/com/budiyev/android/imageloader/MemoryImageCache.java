package com.budiyev.android.imageloader;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

final class MemoryImageCache implements ImageCache {
    private static final float DEFAULT_MEMORY_FRACTION = 0.25f;
    private final LruCache<String, Bitmap> mCache;
    private final Set<String> mKeySet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public MemoryImageCache() {
        this(Math.round(Runtime.getRuntime().maxMemory() * DEFAULT_MEMORY_FRACTION));
    }

    public MemoryImageCache(int maxSize) {
        mCache = new CacheImpl(maxSize);
    }

    @Nullable
    @Override
    public Bitmap get(@NonNull String key) {
        return mCache.get(key);
    }

    @Override
    public void put(@NonNull String key, @NonNull Bitmap value) {
        mCache.put(key, value);
        mKeySet.add(key);
    }

    @Override
    public void remove(@NonNull String key) {
        // Remove possible cached transformations too
        Iterator<String> i = mKeySet.iterator();
        while (i.hasNext()) {
            String k = i.next();
            if (k.startsWith(key)) {
                mCache.remove(k);
                i.remove();
            }
        }
    }

    @Override
    public void clear() {
        mCache.evictAll();
    }

    private final class CacheImpl extends LruCache<String, Bitmap> {
        public CacheImpl(int maxSize) {
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

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            if (evicted) {
                mKeySet.remove(key);
            }
        }
    }
}
