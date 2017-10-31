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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
final class FadeBitmapDrawable extends BitmapDrawable {
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_DONE = 3;
    private static final int MAX_ALPHA = 255;
    private final Handler mMainThreadHandler;
    private final Drawable mPlaceholder;
    private final long mDuration;
    private final FadeCallback mCallback;
    private long mStartTime;
    private int mFadeState = STATE_IDLE;

    public FadeBitmapDrawable(@NonNull Handler mainThreadHandler, @NonNull Resources resources,
            @NonNull Bitmap bitmap, @NonNull Drawable placeholder, long duration,
            @Nullable FadeCallback callback) {
        super(resources, bitmap);
        mMainThreadHandler = mainThreadHandler;
        mPlaceholder = placeholder;
        mDuration = duration;
        mCallback = callback;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        switch (mFadeState) {
            case STATE_IDLE: {
                mPlaceholder.draw(canvas);
                mStartTime = SystemClock.uptimeMillis();
                mFadeState = STATE_RUNNING;
                invalidateSelf();
                break;
            }
            case STATE_RUNNING: {
                int alpha = Math.min(MAX_ALPHA, (int) (MAX_ALPHA *
                        ((SystemClock.uptimeMillis() - mStartTime) / (float) mDuration)));
                if (alpha == MAX_ALPHA) {
                    mFadeState = STATE_DONE;
                    super.draw(canvas);
                    FadeCallback callback = mCallback;
                    if (callback != null) {
                        mMainThreadHandler.post(new CallbackAction(callback));
                    }
                } else {
                    int placeholderAlpha = mPlaceholder.getAlpha();
                    mPlaceholder.setAlpha(MAX_ALPHA - alpha);
                    mPlaceholder.draw(canvas);
                    mPlaceholder.setAlpha(placeholderAlpha);
                    int imageAlpha = super.getAlpha();
                    super.setAlpha(alpha);
                    super.draw(canvas);
                    super.setAlpha(imageAlpha);
                    invalidateSelf();
                }
                break;
            }
            case STATE_DONE: {
                super.draw(canvas);
                break;
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPlaceholder.setAlpha(alpha);
        super.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPlaceholder.setColorFilter(colorFilter);
        super.setColorFilter(colorFilter);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        mPlaceholder.setBounds(bounds);
        super.onBoundsChange(bounds);
    }

    @Override
    public int getOpacity() {
        switch (mFadeState) {
            case STATE_IDLE: {
                return mPlaceholder.getOpacity();
            }
            case STATE_RUNNING: {
                return Math.min(mPlaceholder.getOpacity(), super.getOpacity());
            }
            case STATE_DONE:
            default: {
                return super.getOpacity();
            }
        }
    }

    public interface FadeCallback {
        void onDone();
    }

    private static final class CallbackAction implements Runnable {
        private final FadeCallback mCallback;

        private CallbackAction(@NonNull FadeCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            mCallback.onDone();
        }
    }
}