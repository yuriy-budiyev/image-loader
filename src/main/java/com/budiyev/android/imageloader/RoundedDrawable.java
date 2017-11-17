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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class RoundedDrawable extends Drawable {
    public static final float MAX_RADIUS = -1f;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF mDrawRect = new RectF();
    private final Matrix mShaderMatrix = new Matrix();
    private final BitmapShader mShader;
    private final Bitmap mBitmap;
    private final float mCornerRadius;
    private final int mWidth;
    private final int mHeight;

    public RoundedDrawable(@NonNull Resources resources, @NonNull Bitmap bitmap,
            float cornerRadius) {
        mShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mPaint.setShader(mShader);
        mBitmap = bitmap;
        int density = resources.getDisplayMetrics().densityDpi;
        mWidth = bitmap.getScaledWidth(density);
        mHeight = bitmap.getScaledHeight(density);
        mCornerRadius = cornerRadius;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float cornerRadius = mCornerRadius;
        if (cornerRadius > 0.5f) {
            canvas.drawRoundRect(mDrawRect, cornerRadius, cornerRadius, mPaint);
        } else if (cornerRadius == MAX_RADIUS) {
            cornerRadius = Math.min(mDrawRect.width(), mDrawRect.height()) / 2f;
            canvas.drawRoundRect(mDrawRect, cornerRadius, cornerRadius, mPaint);
        } else {
            canvas.drawRect(mDrawRect, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return mBitmap.hasAlpha() || mPaint.getAlpha() < 255 || mCornerRadius > 0.5f ||
                mCornerRadius == MAX_RADIUS ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        mShaderMatrix.setScale((float) bounds.width() / (float) mBitmap.getWidth(),
                (float) bounds.height() / (float) mBitmap.getHeight());
        mShader.setLocalMatrix(mShaderMatrix);
        mDrawRect.set(bounds);
        super.onBoundsChange(bounds);
    }
}
