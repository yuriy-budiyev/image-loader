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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.support.annotation.ColorInt

object ImageUtils {

    /**
     * Invert image colors
     *
     * @see BitmapTransformation
     */
    fun invertColors(): BitmapTransformation {
        return InvertColorsTransformation()
    }

    /**
     * Convert image colors to gray-scale
     *
     * @see BitmapTransformation
     */
    fun convertToGrayScale(): BitmapTransformation {
        return GrayScaleTransformation()
    }

    /**
     * Apply color tint
     *
     * @param color Color
     * @see BitmapTransformation
     */
    fun tint(@ColorInt color: Int): BitmapTransformation {
        return TintTransformation(color, PorterDuff.Mode.SRC_ATOP)
    }

    /**
     * Apply color tint
     *
     * @param color Color
     * @param mode  Mode
     * @see BitmapTransformation
     */
    fun tint(@ColorInt color: Int, mode: PorterDuff.Mode): BitmapTransformation {
        return TintTransformation(color, mode)
    }

    /**
     * Mirror image horizontally
     *
     * @see BitmapTransformation
     */
    fun mirrorHorizontally(): BitmapTransformation {
        return MirrorHorizontallyTransformation()
    }

    /**
     * Mirror image vertically
     *
     * @see BitmapTransformation
     */
    fun mirrorVertically(): BitmapTransformation {
        return MirrorVerticallyTransformation()
    }

    /**
     * Rotate image by specified amount of degrees
     *
     * @param rotationAngle Amount of degrees
     * @see BitmapTransformation
     */
    fun rotate(rotationAngle: Float): BitmapTransformation {
        return RotateTransformation(rotationAngle)
    }

    /**
     * Round image corners with maximum corner radius,
     * for square image, will lead to circle result
     *
     * @see BitmapTransformation
     */
    fun roundCorners(): BitmapTransformation {
        return RoundCornersTransformation()
    }

    /**
     * Round image corners with specified corner radius
     *
     * @param cornerRadius Corner radius
     * @return Image with rounded corners
     * @see BitmapTransformation
     */
    fun roundCorners(cornerRadius: Float): BitmapTransformation {
        return RoundCornersTransformation(cornerRadius)
    }

    /**
     * Crop center of image in square proportions (1:1), no resize
     *
     * @see BitmapTransformation
     */
    fun cropCenter(): BitmapTransformation {
        return CropCenterTransformation()
    }

    /**
     * Crop center of image in proportions of `resultWidth` and `resultHeight`
     * and, if needed, resize it to `resultWidth` x `resultHeight` size
     *
     * @see BitmapTransformation
     */
    fun cropCenter(resultWidth: Int, resultHeight: Int): BitmapTransformation {
        return CropCenterTransformation(resultWidth, resultHeight)
    }

    /**
     * Fit image into frame with size of maximum image dimension
     *
     * @see BitmapTransformation
     */
    fun fitCenter(): BitmapTransformation {
        return FitCenterTransformation()
    }

    /**
     * Fit image to specified frame (`resultWidth` x `resultHeight`,
     * image will be scaled if needed
     *
     * @see BitmapTransformation
     */
    fun fitCenter(resultWidth: Int, resultHeight: Int): BitmapTransformation {
        return FitCenterTransformation(resultWidth, resultHeight)
    }

    /**
     * Scale image to fit specified frame (`resultWidth` x `resultHeight`)
     *
     * @see BitmapTransformation
     */
    fun scaleToFit(resultWidth: Int, resultHeight: Int): BitmapTransformation {
        return ScaleToFitTransformation(resultWidth, resultHeight, false)
    }

    /**
     * Scale image to fit specified frame (`resultWidth` x `resultHeight`),
     * upscale image if needed if `upscale` set to true
     *
     * @see BitmapTransformation
     */
    fun scaleToFit(resultWidth: Int, resultHeight: Int, upscale: Boolean): BitmapTransformation {
        return ScaleToFitTransformation(resultWidth, resultHeight, upscale)
    }

    /**
     * Invert image colors
     *
     * @param image Source image
     * @return Inverted image
     */
    fun invertColors(image: Bitmap): Bitmap {
        return applyColorFilter(image, ColorMatrixColorFilter(
                floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)))
    }

    /**
     * Convert image colors to gray-scale
     *
     * @param image Source image
     * @return Converted image
     */
    fun convertToGrayScale(image: Bitmap): Bitmap {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        return applyColorFilter(image, ColorMatrixColorFilter(colorMatrix))
    }

    /**
     * Convert image colors to gray-scale
     *
     * @param image Source image
     * @return Converted image
     */
    @JvmOverloads
    fun tint(image: Bitmap, @ColorInt color: Int, mode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP): Bitmap {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        return applyColorFilter(image, PorterDuffColorFilter(color, mode))
    }

    /**
     * Apply color filter to the specified image
     *
     * @param image       Source image
     * @param colorFilter Color filter
     * @return Filtered image
     */
    fun applyColorFilter(image: Bitmap, colorFilter: ColorFilter): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = colorFilter
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        bitmap.density = image.density
        Canvas(bitmap).drawBitmap(image, 0f, 0f, paint)
        return bitmap
    }

    /**
     * Mirror image horizontally
     *
     * @param image Source image
     * @return Mirrored image
     */
    fun mirrorHorizontally(image: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.setScale(-1f, 1f)
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }

    /**
     * Mirror image vertically
     *
     * @param image Source image
     * @return Mirrored image
     */
    fun mirrorVertically(image: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.setScale(1f, -1f)
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }

    /**
     * Rotate image by specified amount of degrees
     *
     * @param image         Source image
     * @param rotationAngle Amount of degrees
     * @return Rotated image
     */
    fun rotate(image: Bitmap, rotationAngle: Float): Bitmap {
        val matrix = Matrix()
        matrix.setRotate(rotationAngle)
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }

    /**
     * Round image corners with specified corner radius
     *
     * @param image        Source image
     * @param cornerRadius Corner radius
     * @return Image with rounded corners
     */
    fun roundCorners(image: Bitmap, cornerRadius: Float): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = -0xbdbdbe
        val width = image.width
        val height = image.height
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.density = image.density
        val canvas = Canvas(bitmap)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(image, rect, rect, paint)
        return bitmap
    }

    /**
     * Crop center of image in square proportions, with size of minimum image dimension
     *
     * @param image Source image
     * @return Cropped image or source image
     */
    fun cropCenter(image: Bitmap): Bitmap {
        val size = Math.min(image.width, image.height)
        return cropCenter(image, size, size)
    }

    /**
     * Crop center of image in proportions of `resultWidth` and `resultHeight`
     * and, if needed, resize it to `resultWidth` x `resultHeight` size.
     * If specified `resultWidth` and `resultHeight` are the same as the current
     * width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @return Cropped (and/or resized) image or source image
     */
    fun cropCenter(image: Bitmap, resultWidth: Int, resultHeight: Int): Bitmap {
        val sourceWidth = image.width
        val sourceHeight = image.height
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image
        }
        val sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight)
        val sourceRatioWidth = sourceWidth / sourceDivisor
        val sourceRatioHeight = sourceHeight / sourceDivisor
        val resultDivisor = greatestCommonDivisor(resultWidth, resultHeight)
        val resultRatioWidth = resultWidth / resultDivisor
        val resultRatioHeight = resultHeight / resultDivisor
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true)
        }
        val cropped: Bitmap
        val cropWidth = resultRatioWidth * sourceHeight / resultRatioHeight
        if (cropWidth > sourceWidth) {
            val cropHeight = resultRatioHeight * sourceWidth / resultRatioWidth
            cropped = Bitmap.createBitmap(image, 0, (sourceHeight - cropHeight) / 2, sourceWidth, cropHeight)
            if (cropHeight == resultHeight && sourceWidth == resultWidth) {
                return cropped
            }
        } else {
            cropped = Bitmap.createBitmap(image, (sourceWidth - cropWidth) / 2, 0, cropWidth, sourceHeight)
            if (cropWidth == resultWidth && sourceHeight == resultHeight) {
                return cropped
            }
        }
        val scaled = Bitmap.createScaledBitmap(cropped, resultWidth, resultHeight, true)
        if (cropped != image && cropped != scaled) {
            cropped.recycle()
        }
        return scaled
    }

    /**
     * Fit image into frame with size of maximum image dimension
     *
     * @param image Source image
     * @return Frame image with source image drawn in center or source image
     */
    fun fitCenter(image: Bitmap): Bitmap {
        val size = Math.max(image.width, image.height)
        return fitCenter(image, size, size)
    }

    /**
     * Fit image to specified frame (`resultWidth` x `resultHeight`,
     * image will be scaled if needed.
     * If specified `resultWidth` and `resultHeight` are the same as the current
     * width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @return Frame image with source image drawn in center of it or original image
     * or scaled image
     */
    fun fitCenter(image: Bitmap, resultWidth: Int, resultHeight: Int): Bitmap {
        val sourceWidth = image.width
        val sourceHeight = image.height
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image
        }
        val sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight)
        val sourceRatioWidth = sourceWidth / sourceDivisor
        val sourceRatioHeight = sourceHeight / sourceDivisor
        val resultDivisor = greatestCommonDivisor(resultWidth, resultHeight)
        val resultRatioWidth = resultWidth / resultDivisor
        val resultRatioHeight = resultHeight / resultDivisor
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true)
        }
        val result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
        result.density = image.density
        val canvas = Canvas(result)
        val fitWidth = sourceRatioWidth * resultHeight / sourceRatioHeight
        if (fitWidth > resultWidth) {
            val fitHeight = sourceRatioHeight * resultWidth / sourceRatioWidth
            val top = (resultHeight - fitHeight) / 2
            canvas.drawBitmap(image, null, Rect(0, top, resultWidth, top + fitHeight),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            val left = (resultWidth - fitWidth) / 2
            canvas.drawBitmap(image, null, Rect(left, 0, left + fitWidth, resultHeight),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return result
    }

    /**
     * Scale image to fit specified frame (`resultWidth` x `resultHeight`).
     * If specified `resultWidth` and `resultHeight` are the same as the current
     * width and height of the source image, the source image will be returned.
     *
     * @param image        Source image
     * @param resultWidth  Result width
     * @param resultHeight Result height
     * @param upscale      Upscale image if it is smaller than the frame
     * @return Scaled image or original image
     */
    @JvmOverloads
    fun scaleToFit(image: Bitmap, resultWidth: Int, resultHeight: Int, upscale: Boolean = false): Bitmap {
        val sourceWidth = image.width
        val sourceHeight = image.height
        if (sourceWidth == resultWidth && sourceHeight == resultHeight) {
            return image
        }
        if (!upscale && sourceWidth < resultWidth && sourceHeight < resultHeight) {
            return image
        }
        val sourceDivisor = greatestCommonDivisor(sourceWidth, sourceHeight)
        val sourceRatioWidth = sourceWidth / sourceDivisor
        val sourceRatioHeight = sourceHeight / sourceDivisor
        val resultDivisor = greatestCommonDivisor(resultWidth, resultHeight)
        val resultRatioWidth = resultWidth / resultDivisor
        val resultRatioHeight = resultHeight / resultDivisor
        if (sourceRatioWidth == resultRatioWidth && sourceRatioHeight == resultRatioHeight) {
            return Bitmap.createScaledBitmap(image, resultWidth, resultHeight, true)
        }
        val fitWidth = sourceRatioWidth * resultHeight / sourceRatioHeight
        if (fitWidth > resultWidth) {
            if (sourceWidth == resultWidth) {
                return image
            } else {
                val fitHeight = sourceRatioHeight * resultWidth / sourceRatioWidth
                return Bitmap.createScaledBitmap(image, resultWidth, fitHeight, true)
            }
        } else {
            return if (sourceHeight == resultHeight) {
                image
            } else {
                Bitmap.createScaledBitmap(image, fitWidth, resultHeight, true)
            }
        }
    }

    private fun greatestCommonDivisor(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (x > 0 && y > 0) {
            if (x > y) {
                x %= y
            } else {
                y %= x
            }
        }
        return x + y
    }

    private class InvertColorsTransformation : BitmapTransformation {

        override val key: String
            get() = "_invert_colors"

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return invertColors(bitmap)
        }
    }

    private class GrayScaleTransformation : BitmapTransformation {

        override val key: String
            get() = "_gray_scale"

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return convertToGrayScale(bitmap)
        }
    }

    private class TintTransformation(@param:ColorInt private val mColor: Int, private val mMode: PorterDuff.Mode) :
            BitmapTransformation {
        override val key: String

        init {
            key = "_tint_" + mColor + "_" + mMode
        }

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return tint(bitmap, mColor, mMode)
        }
    }

    private class MirrorHorizontallyTransformation : BitmapTransformation {

        override val key: String
            get() = "_mirror_horizontally"

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return mirrorHorizontally(bitmap)
        }
    }

    private class MirrorVerticallyTransformation : BitmapTransformation {

        override val key: String
            get() = "_mirror_vertically"

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return mirrorVertically(bitmap)
        }
    }

    private class RotateTransformation(angle: Float) : BitmapTransformation {
        private val mAngle: Float
        override val key: String

        init {
            val a = angle % 360f
            mAngle = a
            key = "_rotate_$a"
        }

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return rotate(bitmap, mAngle)
        }
    }

    private class RoundCornersTransformation : BitmapTransformation {
        private val mRadius: Float
        override val key: String

        constructor(radius: Float) {
            mRadius = radius
            key = "_round_corners_$radius"
        }

        constructor() {
            mRadius = -1f
            key = "_round_corners_max"
        }

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            var radius = mRadius
            if (radius == -1f) {
                radius = Math.min(bitmap.width, bitmap.height) / 2f
            }
            return roundCorners(bitmap, radius)
        }
    }

    private class CropCenterTransformation : BitmapTransformation {
        private val mWidth: Int
        private val mHeight: Int
        override val key: String

        constructor(width: Int, height: Int) {
            mWidth = width
            mHeight = height
            key = "_crop_center_" + width + "x" + height
        }

        constructor() {
            mWidth = -1
            mHeight = -1
            key = "_crop_center_square"
        }

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return if (mWidth > 0 && mHeight > 0) {
                cropCenter(bitmap, mWidth, mHeight)
            } else {
                cropCenter(bitmap)
            }
        }
    }

    private class FitCenterTransformation : BitmapTransformation {
        private val mWidth: Int
        private val mHeight: Int
        override val key: String

        constructor() {
            mWidth = -1
            mHeight = -1
            key = "_fit_center_square"
        }

        constructor(width: Int, height: Int) {
            mWidth = width
            mHeight = height
            key = "_fit_center_" + width + "x" + height
        }

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return if (mWidth > 0 && mHeight > 0) {
                fitCenter(bitmap, mWidth, mHeight)
            } else {
                fitCenter(bitmap)
            }
        }
    }

    private class ScaleToFitTransformation(private val mWidth: Int, private val mHeight: Int,
            private val mUpscale: Boolean) : BitmapTransformation {
        override val key: String

        init {
            key = "_scale_to_fit_" + mUpscale + "_" + mWidth + "x" + mHeight
        }

        @Throws(Throwable::class)
        override fun transform(bitmap: Bitmap): Bitmap {
            return scaleToFit(bitmap, mWidth, mHeight, mUpscale)
        }
    }
}
/**
 * Convert image colors to gray-scale
 *
 * @param image Source image
 * @return Converted image
 */
/**
 * Scale image to fit specified frame (`resultWidth` x `resultHeight`).
 * If specified `resultWidth` and `resultHeight` are the same as or smaller than
 * the current width and height of the source image, the source image will be returned.
 *
 * @param image        Source image
 * @param resultWidth  Result width
 * @param resultHeight Result height
 * @return Scaled image or original image
 */
