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

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.SystemClock
import android.support.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class FadeDrawable(startDrawable: Drawable, endDrawable: Drawable, private val mFadeDuration: Long) :
        LayerDrawable(arrayOf(startDrawable, endDrawable)) {
    private val mStartAlpha: Int
    private val mEndAlpha: Int
    private var mStartTime: Long = 0
    private var mFadeState = STATE_IDLE
    private var mIgnoreInvalidation: Boolean = false

    init {
        mStartAlpha = startDrawable.alpha
        mEndAlpha = endDrawable.alpha
    }

    override fun draw(canvas: Canvas) {
        when (mFadeState) {
            STATE_IDLE -> {
                getDrawable(START_DRAWABLE).draw(canvas)
                mStartTime = SystemClock.uptimeMillis()
                mFadeState = STATE_RUNNING
                invalidateSelf()
            }
            STATE_RUNNING -> {
                mIgnoreInvalidation = true
                val elapsed = SystemClock.uptimeMillis() - mStartTime
                val endAlpha = (mEndAlpha * elapsed / mFadeDuration).toInt()
                val startDrawable = getDrawable(START_DRAWABLE)
                val endDrawable = getDrawable(END_DRAWABLE)
                val done = endAlpha >= mEndAlpha
                if (done) {
                    mFadeState = STATE_DONE
                    endDrawable.alpha = mEndAlpha
                    endDrawable.draw(canvas)
                } else {
                    startDrawable.alpha = Math.max(mStartAlpha - (mStartAlpha * elapsed / mFadeDuration).toInt(), 0)
                    startDrawable.draw(canvas)
                    endDrawable.alpha = endAlpha
                    endDrawable.draw(canvas)
                }
                mIgnoreInvalidation = false
                if (!done) {
                    invalidateSelf()
                }
            }
            STATE_DONE -> {
                getDrawable(END_DRAWABLE).draw(canvas)
            }
        }
    }

    override fun invalidateSelf() {
        if (!mIgnoreInvalidation) {
            super.invalidateSelf()
        }
    }

    override fun invalidateDrawable(drawable: Drawable) {
        if (!mIgnoreInvalidation) {
            super.invalidateDrawable(drawable)
        }
    }

    companion object {
        private val STATE_IDLE = 0
        private val STATE_RUNNING = 1
        private val STATE_DONE = 3
        private val START_DRAWABLE = 0
        private val END_DRAWABLE = 1
    }
}
