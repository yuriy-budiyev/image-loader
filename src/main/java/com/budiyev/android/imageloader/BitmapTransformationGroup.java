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

import java.util.List;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

final class BitmapTransformationGroup implements BitmapTransformation {
    private final List<BitmapTransformation> mTransformations;
    private final String mKey;

    public BitmapTransformationGroup(@NonNull List<BitmapTransformation> transformations) {
        mTransformations = transformations;
        StringBuilder sb = new StringBuilder();
        for (BitmapTransformation t : transformations) {
            sb.append(t.getKey());
        }
        mKey = sb.toString();
    }

    @NonNull
    @Override
    public Bitmap transform(@NonNull Bitmap bitmap) throws Throwable {
        for (BitmapTransformation t : mTransformations) {
            Bitmap transformed = t.transform(bitmap);
            if (bitmap != transformed && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = transformed;
        }
        return bitmap;
    }

    @NonNull
    @Override
    public String getKey() {
        return mKey;
    }
}
