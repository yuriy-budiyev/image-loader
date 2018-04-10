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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import android.support.annotation.NonNull;

abstract class ImageRequestAction implements ImageRequestDelegate, Callable<Void> {
    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private volatile Future<?> mFuture;

    protected abstract void execute();

    protected abstract void onCancelled();

    @Override
    public final Void call() throws Exception {
        execute();
        return null;
    }

    @NonNull
    public final ImageRequestDelegate submit(@NonNull final ExecutorService executor) {
        if (!mCancelled.get()) {
            mFuture = executor.submit(this);
        }
        return this;
    }

    @Override
    public final boolean cancel() {
        if (mCancelled.compareAndSet(false, true)) {
            final Future<?> future = mFuture;
            if (future != null) {
                future.cancel(false);
            }
            onCancelled();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final boolean isCancelled() {
        return mCancelled.get();
    }
}
