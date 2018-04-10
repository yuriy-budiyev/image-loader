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

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

internal class RoundedDrawable(resources: Resources, private val mBitmap: Bitmap, private val mCornerRadius: Float) :
        Drawable() {
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mDrawRect = RectF()
    private val mShaderMatrix = Matrix()
    private val mShader: BitmapShader
    private val mWidth: Int
    private val mHeight: Int

    init {
        mShader = BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        mPaint.shader = mShader
        val density = resources.displayMetrics.densityDpi
        mWidth = mBitmap.getScaledWidth(density)
        mHeight = mBitmap.getScaledHeight(density)
    }

    override fun draw(canvas: Canvas) {
        var cornerRadius = mCornerRadius
        if (cornerRadius > 0.5f) {
            canvas.drawRoundRect(mDrawRect, cornerRadius, cornerRadius, mPaint)
        } else if (cornerRadius == MAX_RADIUS) {
            cornerRadius = Math.min(mDrawRect.width(), mDrawRect.height()) / 2f
            canvas.drawRoundRect(mDrawRect, cornerRadius, cornerRadius, mPaint)
        } else {
            canvas.drawRect(mDrawRect, mPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return if (mBitmap.hasAlpha() || mPaint.alpha < 255 || mCornerRadius > 0.5f || mCornerRadius == MAX_RADIUS) PixelFormat.TRANSLUCENT
        else PixelFormat.OPAQUE
    }

    override fun getIntrinsicWidth(): Int {
        return mWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mHeight
    }

    override fun onBoundsChange(bounds: Rect) {
        mShaderMatrix.setScale(bounds.width().toFloat() / mBitmap.width.toFloat(),
                bounds.height().toFloat() / mBitmap.height.toFloat())
        mShader.setLocalMatrix(mShaderMatrix)
        mDrawRect.set(bounds)
        super.onBoundsChange(bounds)
    }

    companion object {
        val MAX_RADIUS = -1f
    }
}
