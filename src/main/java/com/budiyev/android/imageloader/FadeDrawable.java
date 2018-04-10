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

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.KITKAT)
final class FadeDrawable extends LayerDrawable {
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_DONE = 3;
    private static final int START_DRAWABLE = 0;
    private static final int END_DRAWABLE = 1;
    private final long mFadeDuration;
    private final int mStartAlpha;
    private final int mEndAlpha;
    private long mStartTime;
    private int mFadeState = STATE_IDLE;
    private boolean mIgnoreInvalidation;

    public FadeDrawable(@NonNull final Drawable startDrawable, @NonNull final Drawable endDrawable,
            final long fadeDuration) {
        super(new Drawable[] {startDrawable, endDrawable});
        mFadeDuration = fadeDuration;
        mStartAlpha = startDrawable.getAlpha();
        mEndAlpha = endDrawable.getAlpha();
    }

    @Override
    public void draw(final Canvas canvas) {
        switch (mFadeState) {
            case STATE_IDLE: {
                getDrawable(START_DRAWABLE).draw(canvas);
                mStartTime = SystemClock.uptimeMillis();
                mFadeState = STATE_RUNNING;
                invalidateSelf();
                break;
            }
            case STATE_RUNNING: {
                mIgnoreInvalidation = true;
                final long elapsed = SystemClock.uptimeMillis() - mStartTime;
                final int endAlpha = (int) (mEndAlpha * elapsed / mFadeDuration);
                final Drawable startDrawable = getDrawable(START_DRAWABLE);
                final Drawable endDrawable = getDrawable(END_DRAWABLE);
                final boolean done = endAlpha >= mEndAlpha;
                if (done) {
                    mFadeState = STATE_DONE;
                    endDrawable.setAlpha(mEndAlpha);
                    endDrawable.draw(canvas);
                } else {
                    startDrawable.setAlpha(Math.max(mStartAlpha - (int) (mStartAlpha * elapsed / mFadeDuration), 0));
                    startDrawable.draw(canvas);
                    endDrawable.setAlpha(endAlpha);
                    endDrawable.draw(canvas);
                }
                mIgnoreInvalidation = false;
                if (!done) {
                    invalidateSelf();
                }
                break;
            }
            case STATE_DONE: {
                getDrawable(END_DRAWABLE).draw(canvas);
                break;
            }
        }
    }

    @Override
    public void invalidateSelf() {
        if (!mIgnoreInvalidation) {
            super.invalidateSelf();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull final Drawable drawable) {
        if (!mIgnoreInvalidation) {
            super.invalidateDrawable(drawable);
        }
    }
}
