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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

public final class ImageUtils {
    private ImageUtils() {
    }

    /**
     * Invert image colors
     *
     * @param image Source image
     * @return Inverted image
     */
    @NonNull
    public static Bitmap invertColors(@NonNull Bitmap image) {
        return applyColorFilter(image, new ColorMatrixColorFilter(
                new float[] {-1, 0, 0, 0, 255, 0, -1, 0, 0, 255, 0, 0, -1, 0, 255, 0, 0, 0, 1, 0}));
    }

    /**
     * Invert image colors
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> invertColors() {
        return (BitmapTransformation<T>) new InvertColorsTransformation();
    }

    /**
     * Convert image colors to gray-scale
     *
     * @param image Source image
     * @return Converted image
     */
    @NonNull
    public static Bitmap convertToGrayScale(@NonNull Bitmap image) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        return applyColorFilter(image, new ColorMatrixColorFilter(colorMatrix));
    }

    /**
     * Convert image colors to gray-scale
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> convertToGrayScale() {
        return (BitmapTransformation<T>) new GrayScaleTransformation();
    }

    /**
     * Apply color filter to the specified image
     *
     * @param image       Source image
     * @param colorFilter Color filter
     * @return Filtered image
     */
    @NonNull
    public static Bitmap applyColorFilter(@NonNull Bitmap image, @NonNull ColorFilter colorFilter) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(colorFilter);
        Bitmap bitmap =
                Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.setDensity(image.getDensity());
        new Canvas(bitmap).drawBitmap(image, 0, 0, paint);
        return bitmap;
    }

    /**
     * Color filter
     *
     * @param colorFilter Color filter
     * @param key         Identifier of this color filter transformation
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> applyColorFilter(@NonNull ColorFilter colorFilter,
            @NonNull String key) {
        return (BitmapTransformation<T>) new ColorFilterTransformation(colorFilter, key);
    }

    /**
     * Mirror image horizontally
     *
     * @param image Source image
     * @return Mirrored image
     */
    @NonNull
    public static Bitmap mirrorHorizontally(@NonNull Bitmap image) {
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Mirror image horizontally
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> mirrorHorizontally() {
        return (BitmapTransformation<T>) new MirrorHorizontallyTransformation();
    }

    /**
     * Mirror image vertically
     *
     * @param image Source image
     * @return Mirrored image
     */
    @NonNull
    public static Bitmap mirrorVertically(@NonNull Bitmap image) {
        Matrix matrix = new Matrix();
        matrix.setScale(1, -1);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Mirror image vertically
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> mirrorVertically() {
        return (BitmapTransformation<T>) new MirrorVerticallyTransformation();
    }

    /**
     * Rotate image by specified amount of degrees
     *
     * @param image         Source image
     * @param rotationAngle Amount of degrees
     * @return Rotated image
     */
    @NonNull
    public static Bitmap rotate(@NonNull Bitmap image, float rotationAngle) {
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Rotate image by specified amount of degrees
     *
     * @param rotationAngle Amount of degrees
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> rotate(float rotationAngle) {
        return (BitmapTransformation<T>) new RotateTransformation(rotationAngle);
    }

    /**
     * Round image corners with specified corner radius
     *
     * @param image        Source image
     * @param cornerRadius Corner radius
     * @return Image with rounded corners
     */
    @NonNull
    public static Bitmap roundCorners(@NonNull Bitmap image, float cornerRadius) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColor(0xff424242);
        int width = image.getWidth();
        int height = image.getHeight();
        Rect rect = new Rect(0, 0, width, height);
        RectF rectF = new RectF(rect);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(image.getDensity());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(image, rect, rect, paint);
        return bitmap;
    }

    /**
     * Round image corners with specified corner radius
     *
     * @param cornerRadius Corner radius
     * @return Image with rounded corners
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> roundCorners(float cornerRadius) {
        return (BitmapTransformation<T>) new RoundCornersTransformation(cornerRadius);
    }

    /**
     * Round image corners with maximum corner radius,
     * for square image will lead to circle result
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> roundCorners() {
        return (BitmapTransformation<T>) new RoundCornersTransformation();
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
    public static Bitmap cropCenter(@NonNull Bitmap image, int resultWidth, int resultHeight) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image;
        }
        int sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight);
        int sourceRatioWidth = sourceWidth / sourceDivisor;
        int sourceRatioHeight = sourceHeight / sourceDivisor;
        int resultDivisor = greatestCommonDivisor(resultWidth, resultHeight);
        int resultRatioWidth = resultWidth / resultDivisor;
        int resultRatioHeight = resultHeight / resultDivisor;
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true);
        }
        Bitmap cropped;
        int cropWidth = resultRatioWidth * sourceHeight / resultRatioHeight;
        if (cropWidth > sourceWidth) {
            int cropHeight = resultRatioHeight * sourceWidth / resultRatioWidth;
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
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, resultWidth, resultHeight, true);
        if (cropped != image && cropped != scaled) {
            cropped.recycle();
        }
        return scaled;
    }

    /**
     * Crop center of image in square proportions (1:1)
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> cropCenter() {
        return (BitmapTransformation<T>) new CropCenterTransformation();
    }

    /**
     * Crop center of image in proportions of {@code resultWidth} and {@code resultHeight}
     * and, if needed, resize it to {@code resultWidth} x {@code resultHeight} size
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> BitmapTransformation<T> cropCenter(int resultWidth, int resultHeight) {
        return (BitmapTransformation<T>) new CropCenterTransformation(resultWidth, resultHeight);
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
    public static Bitmap fitCenter(@NonNull Bitmap image, int resultWidth, int resultHeight) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image;
        }
        int sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight);
        int sourceRatioWidth = sourceWidth / sourceDivisor;
        int sourceRatioHeight = sourceHeight / sourceDivisor;
        int resultDivisor = greatestCommonDivisor(resultWidth, resultHeight);
        int resultRatioWidth = resultWidth / resultDivisor;
        int resultRatioHeight = resultHeight / resultDivisor;
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true);
        }
        Bitmap result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        result.setDensity(image.getDensity());
        Canvas canvas = new Canvas(result);
        int fitWidth = sourceRatioWidth * resultHeight / sourceRatioHeight;
        if (fitWidth > resultWidth) {
            int fitHeight = sourceRatioHeight * resultWidth / sourceRatioWidth;
            int top = (resultHeight - fitHeight) / 2;
            canvas.drawBitmap(image, null, new Rect(0, top, resultWidth, top + fitHeight),
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        } else {
            int left = (resultWidth - fitWidth) / 2;
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
    public static Bitmap scaleToFit(@NonNull Bitmap image, int resultWidth, int resultHeight) {
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
    public static Bitmap scaleToFit(@NonNull Bitmap image, int resultWidth, int resultHeight,
            boolean upscale) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image;
        }
        if (!upscale && sourceWidth < resultWidth && sourceHeight < resultHeight) {
            return image;
        }
        int sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight);
        int sourceRatioWidth = sourceWidth / sourceDivisor;
        int sourceRatioHeight = sourceHeight / sourceDivisor;
        int resultDivisor = greatestCommonDivisor(resultWidth, resultHeight);
        int resultRatioWidth = resultWidth / resultDivisor;
        int resultRatioHeight = resultHeight / resultDivisor;
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true);
        }
        int fitWidth = sourceRatioWidth * resultHeight / sourceRatioHeight;
        if (fitWidth > resultWidth) {
            if (sourceWidth == resultWidth) {
                return image;
            } else {
                int fitHeight = sourceRatioHeight * resultWidth / sourceRatioWidth;
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

    private static final class InvertColorsTransformation implements BitmapTransformation<Object> {
        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            return invertColors(bitmap);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return "_invert_colors";
        }
    }

    private static final class GrayScaleTransformation implements BitmapTransformation<Object> {
        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            return convertToGrayScale(bitmap);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return "_gray_scale";
        }
    }

    private static final class ColorFilterTransformation implements BitmapTransformation<Object> {
        private final ColorFilter mColorFilter;
        private final String mKey;

        private ColorFilterTransformation(@NonNull ColorFilter colorFilter, @NonNull String name) {
            mColorFilter = colorFilter;
            mKey = "_color_filter_" + name;
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            return applyColorFilter(bitmap, mColorFilter);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return mKey;
        }
    }

    private static final class MirrorHorizontallyTransformation
            implements BitmapTransformation<Object> {
        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            return mirrorHorizontally(bitmap);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return "_mirror_horizontally";
        }
    }

    private static final class MirrorVerticallyTransformation
            implements BitmapTransformation<Object> {
        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            return mirrorVertically(bitmap);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return "_mirror_vertically";
        }
    }

    private static final class RotateTransformation implements BitmapTransformation<Object> {
        private final float mAngle;
        private final String mKey;

        private RotateTransformation(float angle) {
            mAngle = angle;
            mKey = "_rotate_" + angle;
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            return rotate(bitmap, mAngle);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return mKey;
        }
    }

    private static final class RoundCornersTransformation implements BitmapTransformation<Object> {
        private final float mRadius;
        private final String mKey;

        private RoundCornersTransformation(float radius) {
            mRadius = radius;
            mKey = "_round_corners_" + radius;
        }

        private RoundCornersTransformation() {
            mRadius = -1f;
            mKey = "_round_corners_max";
        }

        @NonNull
        @Override
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            float radius = mRadius;
            if (radius == -1f) {
                radius = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2f;
            }
            return roundCorners(bitmap, radius);
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return mKey;
        }
    }

    private static final class CropCenterTransformation implements BitmapTransformation<Object> {
        private final int mWidth;
        private final int mHeight;
        private final String mKey;

        private CropCenterTransformation(int width, int height) {
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
        public Bitmap transform(@NonNull Context context, @NonNull Object data,
                @NonNull Bitmap bitmap) throws Throwable {
            if (mWidth > 0 && mHeight > 0) {
                return cropCenter(bitmap, mWidth, mHeight);
            } else {
                int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
                return cropCenter(bitmap, size, size);
            }
        }

        @NonNull
        @Override
        public String getKey(@NonNull Object data) {
            return mKey;
        }
    }
}
