/*
 * Copyright (C) 2014-2016 Kevin Mark
 *
 * This file is part of XHangouts.
 *
 * XHangouts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XHangouts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XHangouts.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.xhangouts;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public final class ImageUtils {

    private ImageUtils() {
        //
    }

    public static Bitmap doMatrix(Bitmap input, int rotation) {
        return doMatrix(input, rotation, 0, 0);
    }

    public static Bitmap doMatrix(Bitmap input, int maxWidth, int maxHeight) {
        return doMatrix(input, 0, maxWidth, maxHeight);
    }

    public static Bitmap doMatrix(Bitmap input, int rotation, int maxWidth, int maxHeight) {
        Matrix m = new Matrix();
        int w = input.getWidth(), h = input.getHeight();

        if (maxWidth > 0 && maxHeight > 0) {
            // Use the longest side to determine scale
            float scale = ((float) (w > h ? maxWidth : maxHeight)) / (w > h ? w : h);
            // Never scale upwards
            if(scale < 1f) {
                m.postScale(scale, scale);
            }
        }

        if (rotation != 0f) {
            m.postRotate(rotation);
        }

        return Bitmap.createBitmap(input, 0, 0, w, h, m, true);
    }

    public static byte[] compress(Bitmap image, Setting.ImageFormat format, int quality, boolean recycle) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap.CompressFormat compressFormat;
        switch (format) {
            case PNG:
                compressFormat = Bitmap.CompressFormat.PNG;
                quality = 0;
                break;
            case JPEG:
                compressFormat = Bitmap.CompressFormat.JPEG;
                break;
            default:
                throw new IllegalArgumentException("Unknown format.");
        }
        image.compress(compressFormat, quality, baos);
        if (recycle) {
            image.recycle();
        }
        byte[] bytes = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException ex) {
            //
        }
        return bytes;
    }

    public static int getSampleSize(int imgW, int imgH, int maxW, int maxH) {
        int sampleSize = 1;
        while ((imgW / 2 > maxW) || (imgH / 2 > maxH)) {
            imgW /= 2;
            imgH /= 2;
            sampleSize *= 2;
        }
        return sampleSize;
    }

    public static int getExifRotation(String imgPath) {
        int rotation = 0;
        if (new File(imgPath).exists()) {
            try {
                ExifInterface exif = new ExifInterface(imgPath);
                // Let's pretend other orientation modes don't exist
                switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotation = 270;
                        break;
                    default:
                        rotation = 0;
                }
            } catch (IOException ex) {
                //
            }
        }
        return rotation;
    }

    public static int[] getRotatedDimens(int rotation, int width, int height) {
        Matrix m = new Matrix();
        m.postRotate(rotation);
        RectF dimens = new RectF();
        m.mapRect(dimens, new RectF(0, 0, width, height));
        return new int[] {
                Math.round(dimens.width()),
                Math.round(dimens.height()),
        };
    }

}
