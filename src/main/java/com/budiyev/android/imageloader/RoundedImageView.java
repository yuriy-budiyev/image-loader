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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Simple image view that will draw corners over an image/drawable;
 * Image size independent rounding. To make corners appear transparent,
 * set corner color same as color of image view background.
 */
public class RoundedImageView extends ImageView {
    private static final int NO_ROUNDING = 0;
    private static final int DEFAULT_CORNER_COLOR = Color.WHITE;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private int mCornerRadius;

    public RoundedImageView(@NonNull Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Corner radius in pixels
     */
    public void setCornerRadius(@Px int radius) {
        mCornerRadius = radius * 2;
        mPaint.setStrokeWidth(mCornerRadius);
    }

    /**
     * Corner color, translucency is not supported
     */
    public void setCornerColor(@ColorInt int color) {
        mPaint.setColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int radius = mCornerRadius;
        if (radius <= 0) {
            return;
        }
        int offset = radius / 2;
        mRect.set(0 - offset, 0 - offset, getWidth() + offset, getHeight() + offset);
        canvas.drawRoundRect(mRect, radius, radius, mPaint);
    }

    private void initialize(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mPaint.setStyle(Paint.Style.STROKE);
        if (attrs == null) {
            mCornerRadius = NO_ROUNDING;
            mPaint.setColor(DEFAULT_CORNER_COLOR);
        } else {
            TypedArray attributes = null;
            try {
                attributes = context.getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.RoundedImageView, defStyleAttr, defStyleRes);
                setCornerRadius(
                        attributes.getDimensionPixelOffset(R.styleable.RoundedImageView_cornerRadius, NO_ROUNDING) * 2);
                setCornerColor(attributes.getColor(R.styleable.RoundedImageView_cornerColor, DEFAULT_CORNER_COLOR));
            } finally {
                if (attributes != null) {
                    attributes.recycle();
                }
            }
        }
    }
}
