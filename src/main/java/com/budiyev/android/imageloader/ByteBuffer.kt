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

import java.io.OutputStream
import java.util.*

internal class ByteBuffer(initialCapacity: Int) : OutputStream() {
    var array: ByteArray = ByteArray(initialCapacity)
        private set
    var size: Int = 0
        private set

    override fun write(b: Int) {
        grow(size + 1)
        array[size] = b.toByte()
        size++
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        grow(size + length)
        System.arraycopy(bytes, offset, array, size, length)
        size += length
    }

    private fun grow(capacity: Int) {
        var length = array.size
        if (capacity > length) {
            length *= 2
            if (length < capacity) {
                length = capacity
            }
            if (length > MAX_ARRAY_SIZE) {
                length = if (capacity > MAX_ARRAY_SIZE) Integer.MAX_VALUE else MAX_ARRAY_SIZE
            }
            array = Arrays.copyOf(array, length)
        }
    }

    companion object {
        private const val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8
    }
}
