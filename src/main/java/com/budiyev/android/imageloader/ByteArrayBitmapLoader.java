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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class ByteArrayBitmapLoader implements BitmapLoader<byte[]> {
    @Nullable
    @Override
    public Bitmap load(@NonNull final byte[] data, @Nullable final Size requiredSize) {
        Bitmap bitmap;
        if (requiredSize != null) {
            bitmap = DataUtils.loadSampledBitmapFromByteArray(data, requiredSize.getWidth(),
                    requiredSize.getHeight());
        } else {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        if (bitmap != null) {
            final int rotation = InternalUtils.getExifRotation(data);
            if (rotation != 0) {
                bitmap = InternalUtils.rotateAndRecycle(bitmap, rotation);
            }
        }
        return bitmap;
    }
}
