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

import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

final class ResourceBitmapLoader implements BitmapLoader<Integer> {
    @Nullable
    @Override
    public Bitmap load(@NonNull Context context, @NonNull Integer data, @Nullable Size size) throws Throwable {
        Resources resources = context.getResources();
        if (size != null) {
            return DataUtils.loadSampledBitmapFromResource(resources, data, size.getWidth(), size.getHeight());
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            TypedValue typedValue = new TypedValue();
            options.inTargetDensity = resources.getDisplayMetrics().densityDpi;
            if (typedValue.density == TypedValue.DENSITY_DEFAULT) {
                options.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            } else if (typedValue.density != TypedValue.DENSITY_NONE) {
                options.inDensity = typedValue.density;
            }
            InputStream inputStream = null;
            try {
                inputStream = resources.openRawResource(data, typedValue);
                return BitmapFactory.decodeStream(inputStream, null, options);
            } finally {
                InternalUtils.close(inputStream);
            }
        }
    }
}
