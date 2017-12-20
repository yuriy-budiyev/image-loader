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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class ImageLoaderExecutor extends ThreadPoolExecutor {
    public ImageLoaderExecutor(int poolSize) {
        super(poolSize, poolSize, 0L, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>(),
                new ImageLoaderThreadFactory(), new DiscardPolicy());
    }

    @Override
    protected void afterExecute(@NonNull Runnable r, @Nullable Throwable t) {
        if (t == null && r instanceof Future<?>) {
            Future<?> f = (Future<?>) r;
            if (f.isDone()) {
                try {
                    f.get();
                } catch (CancellationException ignored) {
                } catch (InterruptedException ignored) {
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }
}
