package com.budiyev.android.imageloader;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.KITKAT)
final class FadeDrawable extends LayerDrawable {
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_DONE = 3;
    private static final int START_DRAWABLE = 0;
    private static final int END_DRAWABLE = 1;
    private final Handler mMainThreadHandler;
    private final FadeCallback mFadeCallback;
    private final long mFadeDuration;
    private final int mStartAlpha;
    private final int mEndAlpha;
    private long mStartTime;
    private int mFadeState = STATE_IDLE;

    public FadeDrawable(@NonNull Drawable startDrawable, @NonNull Drawable endDrawable,
            long fadeDuration, @NonNull Handler mainThreadHandler,
            @Nullable FadeCallback fadeCallback) {
        super(new Drawable[] {startDrawable, endDrawable});
        mMainThreadHandler = mainThreadHandler;
        mFadeDuration = fadeDuration;
        mFadeCallback = fadeCallback;
        mStartAlpha = startDrawable.getAlpha();
        mEndAlpha = endDrawable.getAlpha();
    }

    @Override
    public void draw(Canvas canvas) {
        switch (mFadeState) {
            case STATE_IDLE: {
                getDrawable(START_DRAWABLE).draw(canvas);
                mStartTime = SystemClock.uptimeMillis();
                mFadeState = STATE_RUNNING;
                invalidateSelf();
                break;
            }
            case STATE_RUNNING: {
                long elapsed = SystemClock.uptimeMillis() - mStartTime;
                int startAlpha = mStartAlpha - (int) (mStartAlpha * elapsed / mFadeDuration);
                int endAlpha = (int) (mEndAlpha * elapsed / mFadeDuration);
                Drawable startDrawable = getDrawable(START_DRAWABLE);
                Drawable endDrawable = getDrawable(END_DRAWABLE);
                if (startAlpha <= 0 || endAlpha >= mEndAlpha) {
                    mFadeState = STATE_DONE;
                    endDrawable.setAlpha(mEndAlpha);
                    endDrawable.draw(canvas);
                    FadeCallback fadeCallback = mFadeCallback;
                    if (fadeCallback != null) {
                        mMainThreadHandler.post(new CallbackAction(fadeCallback));
                    }
                } else {
                    startDrawable.setAlpha(startAlpha);
                    startDrawable.draw(canvas);
                    endDrawable.setAlpha(endAlpha);
                    endDrawable.draw(canvas);
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
