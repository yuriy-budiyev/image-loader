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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public final class ImageUtils {
    private ImageUtils() {
    }

    /**
     * Invert image colors
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation invertColors() {
        return new InvertColorsTransformation();
    }

    /**
     * Convert image colors to gray-scale
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation convertToGrayScale() {
        return new GrayScaleTransformation();
    }

    /**
     * Apply color tint
     *
     * @param color Color
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation tint(@ColorInt final int color) {
        return new TintTransformation(color, PorterDuff.Mode.SRC_ATOP);
    }

    /**
     * Apply color tint
     *
     * @param color Color
     * @param mode  Mode
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation tint(@ColorInt final int color,
            @NonNull final PorterDuff.Mode mode) {
        return new TintTransformation(color, mode);
    }

    /**
     * Mirror image horizontally
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation mirrorHorizontally() {
        return new MirrorHorizontallyTransformation();
    }

    /**
     * Mirror image vertically
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation mirrorVertically() {
        return new MirrorVerticallyTransformation();
    }

    /**
     * Rotate image by specified amount of degrees
     *
     * @param rotationAngle Amount of degrees
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation rotate(final float rotationAngle) {
        return new RotateTransformation(rotationAngle);
    }

    /**
     * Round image corners with maximum corner radius,
     * for square image, will lead to circle result
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation roundCorners() {
        return new RoundCornersTransformation();
    }

    /**
     * Round image corners with specified corner radius
     *
     * @param cornerRadius Corner radius
     * @return Image with rounded corners
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation roundCorners(final float cornerRadius) {
        return new RoundCornersTransformation(cornerRadius);
    }

    /**
     * Crop center of image in square proportions (1:1), no resize
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation cropCenter() {
        return new CropCenterTransformation();
    }

    /**
     * Crop center of image in proportions of {@code resultWidth} and {@code resultHeight}
     * and, if needed, resize it to {@code resultWidth} x {@code resultHeight} size
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation cropCenter(final int resultWidth, final int resultHeight) {
        return new CropCenterTransformation(resultWidth, resultHeight);
    }

    /**
     * Fit image into frame with size of maximum image dimension
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation fitCenter() {
        return new FitCenterTransformation();
    }

    /**
     * Fit image to specified frame ({@code resultWidth} x {@code resultHeight},
     * image will be scaled if needed
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation fitCenter(final int resultWidth, final int resultHeight) {
        return new FitCenterTransformation(resultWidth, resultHeight);
    }

    /**
     * Scale image to fit specified frame ({@code resultWidth} x {@code resultHeight})
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation scaleToFit(final int resultWidth, final int resultHeight) {
        return new ScaleToFitTransformation(resultWidth, resultHeight, false);
    }

    /**
     * Scale image to fit specified frame ({@code resultWidth} x {@code resultHeight}),
     * upscale image if needed if {@code upscale} set to true
     *
     * @see BitmapTransformation
     */
    @NonNull
    public static BitmapTransformation scaleToFit(final int resultWidth, final int resultHeight,
            final boolean upscale) {
        return new ScaleToFitTransformation(resultWidth, resultHeight, upscale);
    }

    /**
     * Invert image colors
     *
     * @param image Source image
     * @return Inverted image
     */
    @NonNull
    public static Bitmap invertColors(@NonNull final Bitmap image) {
        return applyColorFilter(image, new ColorMatrixColorFilter(
                new float[] {-1, 0, 0, 0, 255, 0, -1, 0, 0, 255, 0, 0, -1, 0, 255, 0, 0, 0, 1, 0}));
    }

    /**
     * Convert image colors to gray-scale
     *
     * @param image Source image
     * @return Converted image
     */
    @NonNull
    public static Bitmap convertToGrayScale(@NonNull final Bitmap image) {
        final ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        return applyColorFilter(image, new ColorMatrixColorFilter(colorMatrix));
    }

    /**
     * Convert image colors to gray-scale
     *
     * @param image Source image
     * @return Converted image
     */
    @NonNull
    public static Bitmap tint(@NonNull final Bitmap image, @ColorInt final int color) {
        return tint(image, color, PorterDuff.Mode.SRC_ATOP);
    }

    /**
     * Convert image colors to gray-scale
     *
     * @param image Source image
     * @return Converted image
     */
    @NonNull
    public static Bitmap tint(@NonNull final Bitmap image, @ColorInt final int color,
            @NonNull final PorterDuff.Mode mode) {
        final ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        return applyColorFilter(image, new PorterDuffColorFilter(color, mode));
    }

    /**
     * Apply color filter to the specified image
     *
     * @param image       Source image
     * @param colorFilter Color filter
     * @return Filtered image
     */
    @NonNull
    public static Bitmap applyColorFilter(@NonNull final Bitmap image,
            @NonNull final ColorFilter colorFilter) {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(colorFilter);
        final Bitmap bitmap =
                Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.setDensity(image.getDensity());
        new Canvas(bitmap).drawBitmap(image, 0f, 0f, paint);
        return bitmap;
    }

    /**
     * Mirror image horizontally
     *
     * @param image Source image
     * @return Mirrored image
     */
    @NonNull
    public static Bitmap mirrorHorizontally(@NonNull final Bitmap image) {
        final Matrix matrix = new Matrix();
        matrix.setScale(-1f, 1f);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Mirror image vertically
     *
     * @param image Source image
     * @return Mirrored image
     */
    @NonNull
    public static Bitmap mirrorVertically(@NonNull final Bitmap image) {
        final Matrix matrix = new Matrix();
        matrix.setScale(1f, -1f);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Rotate image by specified amount of degrees
     *
     * @param image         Source image
     * @param rotationAngle Amount of degrees
     * @return Rotated image
     */
    @NonNull
    public static Bitmap rotate(@NonNull final Bitmap image, final float rotationAngle) {
        final Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Round image corners with specified corner radius
     *
     * @param image        Source image
     * @param cornerRadius Corner radius
     * @return Image with rounded corners
     */
    @NonNull
    public static Bitmap roundCorners(@NonNull final Bitmap image, final float cornerRadius) {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColor(0xff424242);
        final int width = image.getWidth();
        final int height = image.getHeight();
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(image.getDensity());
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(image, rect, rect, paint);
        return bitmap;
    }

    /**
     * Crop center of image in square proportions, with size of minimum image dimension
     *
     * @param image Source image
     * @return Cropped image or source image
     */
    @NonNull
    public static Bitmap cropCenter(@NonNull final Bitmap image) {
        final int size = Math.min(image.getWidth(), image.getHeight());
        return cropCenter(image, size, size);
    }

    /**
     * Crop center of image in proportions of {@code resultWidth} and {@code resultHeight}
     * and, if needed, resize it to {@code resultWidth} x {@code resultHeight} size.
     * If specified {@code resultWidth} and {@code resultHeight} are the same as the current
     * width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @return Cropped (and/or resized) image or source image
     */
    @NonNull
    public static Bitmap cropCenter(@NonNull final Bitmap image, final int resultWidth,
            final int resultHeight) {
        final int sourceWidth = image.getWidth();
        final int sourceHeight = image.getHeight();
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image;
        }
        final int sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight);
        final int sourceRatioWidth = sourceWidth / sourceDivisor;
        final int sourceRatioHeight = sourceHeight / sourceDivisor;
        final int resultDivisor = greatestCommonDivisor(resultWidth, resultHeight);
        final int resultRatioWidth = resultWidth / resultDivisor;
        final int resultRatioHeight = resultHeight / resultDivisor;
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true);
        }
        final Bitmap cropped;
        final int cropWidth = resultRatioWidth * sourceHeight / resultRatioHeight;
        if (cropWidth > sourceWidth) {
            final int cropHeight = resultRatioHeight * sourceWidth / resultRatioWidth;
            cropped = Bitmap.createBitmap(image, 0, (sourceHeight - cropHeight) / 2, sourceWidth,
                    cropHeight);
            if (cropHeight == resultHeight && sourceWidth == resultWidth) {
                return cropped;
            }
        } else {
            cropped = Bitmap.createBitmap(image, (sourceWidth - cropWidth) / 2, 0, cropWidth,
                    sourceHeight);
            if (cropWidth == resultWidth && sourceHeight == resultHeight) {
                return cropped;
            }
        }
        final Bitmap scaled = Bitmap.createScaledBitmap(cropped, resultWidth, resultHeight, true);
        if (cropped != image && cropped != scaled) {
            cropped.recycle();
        }
        return scaled;
    }

    /**
     * Fit image into frame with size of maximum image dimension
     *
     * @param image Source image
     * @return Frame image with source image drawn in center or source image
     */
    @NonNull
    public static Bitmap fitCenter(@NonNull final Bitmap image) {
        final int size = Math.max(image.getWidth(), image.getHeight());
        return fitCenter(image, size, size);
    }

    /**
     * Fit image to specified frame ({@code resultWidth} x {@code resultHeight},
     * image will be scaled if needed.
     * If specified {@code resultWidth} and {@code resultHeight} are the same as the current
     * width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @return Frame image with source image drawn in center of it or original image
     * or scaled image
     */
    @NonNull
    public static Bitmap fitCenter(@NonNull final Bitmap image, final int resultWidth,
            final int resultHeight) {
        final int sourceWidth = image.getWidth();
        final int sourceHeight = image.getHeight();
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image;
        }
        final int sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight);
        final int sourceRatioWidth = sourceWidth / sourceDivisor;
        final int sourceRatioHeight = sourceHeight / sourceDivisor;
        final int resultDivisor = greatestCommonDivisor(resultWidth, resultHeight);
        final int resultRatioWidth = resultWidth / resultDivisor;
        final int resultRatioHeight = resultHeight / resultDivisor;
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true);
        }
        final Bitmap result =
                Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        result.setDensity(image.getDensity());
        final Canvas canvas = new Canvas(result);
        final int fitWidth = sourceRatioWidth * resultHeight / sourceRatioHeight;
        if (fitWidth > resultWidth) {
            final int fitHeight = sourceRatioHeight * resultWidth / sourceRatioWidth;
            final int top = (resultHeight - fitHeight) / 2;
            canvas.drawBitmap(image, null, new Rect(0, top, resultWidth, top + fitHeight),
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        } else {
            final int left = (resultWidth - fitWidth) / 2;
            canvas.drawBitmap(image, null, new Rect(left, 0, left + fitWidth, resultHeight),
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }
        return result;
    }

    /**
     * Scale image to fit specified frame ({@code resultWidth} x {@code resultHeight}).
     * If specified {@code resultWidth} and {@code resultHeight} are the same as or smaller than
     * the current width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @return Scaled image or original image
     */
    @NonNull
    public static Bitmap scaleToFit(@NonNull final Bitmap image, final int resultWidth,
            final int resultHeight) {
        return scaleToFit(image, resultWidth, resultHeight, false);
    }

    /**
     * Scale image to fit specified frame ({@code resultWidth} x {@code resultHeight}).
     * If specified {@code resultWidth} and {@code resultHeight} are the same as the current
     * width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @param upscale      Upscale image if it is smaller than the frame
     * @return Scaled image or original image
     */
    @NonNull
    public static Bitmap scaleToFit(@NonNull final Bitmap image, final int resultWidth,
            final int resultHeight, final boolean upscale) {
        final int sourceWidth = image.getWidth();
        final int sourceHeight = image.getHeight();
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image;
        }
        if (!upscale && sourceWidth < resultWidth && sourceHeight < resultHeight) {
            return image;
        }
        final int sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight);
        final int sourceRatioWidth = sourceWidth / sourceDivisor;
        final int sourceRatioHeight = sourceHeight / sourceDivisor;
        final int resultDivisor = greatestCommonDivisor(resultWidth, resultHeight);
        final int resultRatioWidth = resultWidth / resultDivisor;
        final int resultRatioHeight = resultHeight / resultDivisor;
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true);
        }
        final int fitWidth = sourceRatioWidth * resultHeight / sourceRatioHeight;
        if (fitWidth > resultWidth) {
            if (sourceWidth == resultWidth) {
                return image;
            } else {
                final int fitHeight = sourceRatioHeight * resultWidth / sourceRatioWidth;
                return Bitmap.createScaledBitmap(image, resultWidth, fitHeight, true);
            }
        } else {
            if (sourceHeight == resultHeight) {
                return image;
            } else {
                return Bitmap.createScaledBitmap(image, fitWidth, resultHeight, true);
            }
        }
    }

    private static int greatestCommonDivisor(int a, int b) {
        while (a > 0 && b > 0) {
            if (a > b) {
                a %= b;
            } else {
                b %= a;
            }
        }
        return a + b;
    }

    private static final class InvertColorsTransformation implements BitmapTransformation {
        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return invertColors(bitmap);
        }

        @NonNull
        @Override
        public String getKey() {
            return "_invert_colors";
        }
    }

    private static final class GrayScaleTransformation implements BitmapTransformation {
        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return convertToGrayScale(bitmap);
        }

        @NonNull
        @Override
        public String getKey() {
            return "_gray_scale";
        }
    }

    private static final class TintTransformation implements BitmapTransformation {
        private final int mColor;
        private final PorterDuff.Mode mMode;
        private final String mKey;

        private TintTransformation(@ColorInt final int color, @NonNull final PorterDuff.Mode mode) {
            mColor = color;
            mMode = mode;
            mKey = "_tint_" + color + "_" + mode;
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return tint(bitmap, mColor, mMode);
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class MirrorHorizontallyTransformation implements BitmapTransformation {
        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return mirrorHorizontally(bitmap);
        }

        @NonNull
        @Override
        public String getKey() {
            return "_mirror_horizontally";
        }
    }

    private static final class MirrorVerticallyTransformation implements BitmapTransformation {
        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return mirrorVertically(bitmap);
        }

        @NonNull
        @Override
        public String getKey() {
            return "_mirror_vertically";
        }
    }

    private static final class RotateTransformation implements BitmapTransformation {
        private final float mAngle;
        private final String mKey;

        public RotateTransformation(float angle) {
            angle = angle % 360f;
            mAngle = angle;
            mKey = "_rotate_" + angle;
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return rotate(bitmap, mAngle);
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class RoundCornersTransformation implements BitmapTransformation {
        private final float mRadius;
        private final String mKey;

        public RoundCornersTransformation(final float radius) {
            mRadius = radius;
            mKey = "_round_corners_" + radius;
        }

        public RoundCornersTransformation() {
            mRadius = -1f;
            mKey = "_round_corners_max";
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            float radius = mRadius;
            if (radius == -1f) {
                radius = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2f;
            }
            return roundCorners(bitmap, radius);
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class CropCenterTransformation implements BitmapTransformation {
        private final int mWidth;
        private final int mHeight;
        private final String mKey;

        public CropCenterTransformation(final int width, final int height) {
            mWidth = width;
            mHeight = height;
            mKey = "_crop_center_" + width + "x" + height;
        }

        public CropCenterTransformation() {
            mWidth = -1;
            mHeight = -1;
            mKey = "_crop_center_square";
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            if (mWidth > 0 && mHeight > 0) {
                return cropCenter(bitmap, mWidth, mHeight);
            } else {
                return cropCenter(bitmap);
            }
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class FitCenterTransformation implements BitmapTransformation {
        private final int mWidth;
        private final int mHeight;
        private final String mKey;

        public FitCenterTransformation() {
            mWidth = -1;
            mHeight = -1;
            mKey = "_fit_center_square";
        }

        public FitCenterTransformation(final int width, final int height) {
            mWidth = width;
            mHeight = height;
            mKey = "_fit_center_" + width + "x" + height;
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            if (mWidth > 0 && mHeight > 0) {
                return fitCenter(bitmap, mWidth, mHeight);
            } else {
                return fitCenter(bitmap);
            }
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }

    private static final class ScaleToFitTransformation implements BitmapTransformation {
        private final int mWidth;
        private final int mHeight;
        private final boolean mUpscale;
        private final String mKey;

        public ScaleToFitTransformation(final int width, final int height, final boolean upscale) {
            mWidth = width;
            mHeight = height;
            mUpscale = upscale;
            mKey = "_scale_to_fit_" + upscale + "_" + width + "x" + height;
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull final Bitmap bitmap) {
            return scaleToFit(bitmap, mWidth, mHeight, mUpscale);
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }
    }
}
