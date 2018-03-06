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
import java.util.concurrent.atomic.AtomicInteger;

import android.support.annotation.NonNull;

abstract class ImageRequestAction implements ImageRequestDelegate, Callable<Void> {
    private static final int STATE_NEW = 0;
    private static final int STATE_PROCESSING = 1;
    private static final int STATE_DONE = 2;
    private static final int STATE_CANCELED = 3;
    private final AtomicInteger mState = new AtomicInteger(STATE_NEW);
    private volatile Future<?> mFuture;

    protected abstract void execute();

    protected abstract void onCancelled();

    @Override
    public final Void call() throws Exception {
        execute();
        mState.compareAndSet(STATE_PROCESSING, STATE_DONE);
        return null;
    }

    @NonNull
    public final ImageRequestDelegate submit(@NonNull ExecutorService executor) {
        if (mState.compareAndSet(STATE_NEW, STATE_PROCESSING)) {
            mFuture = executor.submit(this);
        }
        return this;
    }

    @Override
    public final void cancel() {
        if (mState.compareAndSet(STATE_NEW, STATE_CANCELED) || mState.compareAndSet(STATE_PROCESSING, STATE_CANCELED)) {
            Future<?> future = mFuture;
            if (future != null) {
                future.cancel(false);
            }
            onCancelled();
        }
    }

    @Override
    public final boolean isDone() {
        return mState.get() == STATE_DONE;
    }

    @Override
    public final boolean isCancelled() {
        return mState.get() == STATE_CANCELED;
    }
}
