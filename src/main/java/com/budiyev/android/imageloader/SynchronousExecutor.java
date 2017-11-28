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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class SynchronousExecutor extends AbstractExecutorService {
    private static final Lock INSTANCE_LOCK = new ReentrantLock();
    private static volatile ExecutorService sInstance;

    private SynchronousExecutor() {
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("shutdown");
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException("shutdownNow");
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("awaitTermination");
    }

    @Override
    public void execute(@NonNull Runnable command) {
        Throwable thrown = null;
        try {
            command.run();
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            afterExecute(command, thrown);
        }
    }

    private void afterExecute(@NonNull Runnable r, @Nullable Throwable t) {
        if (t == null && r instanceof Future<?>) {
            Future<?> f = (Future<?>) r;
            if (f.isDone()) {
                try {
                    f.get();
                } catch (InterruptedException | CancellationException ignored) {
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    @NonNull
    public static ExecutorService get() {
        ExecutorService instance = sInstance;
        if (instance == null) {
            INSTANCE_LOCK.lock();
            try {
                instance = sInstance;
                if (instance == null) {
                    instance = new SynchronousExecutor();
                    sInstance = instance;
                }
            } finally {
                INSTANCE_LOCK.unlock();
            }
        }
        return instance;
    }
}
