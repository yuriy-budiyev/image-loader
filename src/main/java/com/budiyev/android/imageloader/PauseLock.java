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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class PauseLock {
    private final Lock mLock = new ReentrantLock();
    private final Condition mCondition = mLock.newCondition();
    private volatile boolean mPaused;
    private volatile boolean mInterruptEarly;

    public boolean isPaused() {
        return mPaused;
    }

    public void setPaused(boolean paused) {
        mLock.lock();
        try {
            mPaused = paused;
            if (!paused) {
                mCondition.signalAll();
            }
        } finally {
            mLock.unlock();
        }
    }

    public boolean shouldInterruptEarly() {
        return mInterruptEarly;
    }

    public void setInterruptEarly(boolean interrupt) {
        mInterruptEarly = interrupt;
        if (interrupt) {
            setPaused(false);
        }
    }

    public void await() throws InterruptedException {
        mLock.lock();
        try {
            if (mPaused) {
                mCondition.await();
            }
        } finally {
            mLock.unlock();
        }
    }
}
