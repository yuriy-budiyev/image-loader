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

import java.io.OutputStream;
import java.util.Arrays;

import android.support.annotation.NonNull;

final class ByteBuffer extends OutputStream {
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private byte[] mArray;
    private int mSize;

    public ByteBuffer(int size) {
        mArray = new byte[size];
    }

    @Override
    public void write(int b) {
        grow(mSize + 1);
        mArray[mSize] = (byte) b;
        mSize++;
    }

    @Override
    public void write(@NonNull byte b[], int offset, int length) {
        grow(mSize + length);
        System.arraycopy(b, offset, mArray, mSize, length);
        mSize += length;
    }

    private void grow(int capacity) {
        int length = mArray.length;
        if (capacity > length) {
            int c = length * 2;
            if (c < capacity) {
                c = capacity;
            }
            if (c > MAX_ARRAY_SIZE) {
                c = (capacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
            }
            mArray = Arrays.copyOf(mArray, c);
        }
    }

    @NonNull
    public byte[] getArray() {
        return mArray;
    }

    public int getSize() {
        return mSize;
    }
}
