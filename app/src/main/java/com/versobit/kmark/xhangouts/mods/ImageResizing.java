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

package com.versobit.kmark.xhangouts.mods;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.ImageUtils;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;


public final class ImageResizing {

    private static final String HANGOUTS_PROCESS_IMG_CLASS = "fql";
    // public Bitmap b(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3)
    private static final String HANGOUTS_PROCESS_IMG_METHOD = "b";
    // public void a(Bitmap paramBitmap)
    private static final String HANGOUTS_PROCESS_IMG_METHOD_CLEANUP = "a";

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        if (!config.modEnabled || !config.resizing) {
            return;
        }

        Class cProcessImg = findClass(HANGOUTS_PROCESS_IMG_CLASS, loader);

        findAndHookMethod(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD, byte[].class, int.class, int.class, int.class, new XC_MethodHook() {
            /*
             * byte[] = The compressed (PNG/JPEG/etc) image
             * int1 = Height
             * int2 = Width
             * int3 = Rotation
             *
             * We're using beforeHookedMethod to avoid a performance hit.
             */
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Get our params
                    byte[] paramImageBytes = (byte[]) param.args[0];
                    int targetHeight = (int) param.args[1];
                    int targetWidth = (int) param.args[2];
                    final int targetRotation = (int) param.args[3];

                    // Stickers and other junk
                    if (targetHeight <= 400 && targetWidth <= 400) {
                        return;
                    }

                    XHangouts.debug(String.format("Param Limits: %d×%d", targetWidth, targetHeight));

                    // Setup some options to decode the bitmap
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = false;
                    options.inDensity = 0;
                    options.inTargetDensity = 0;
                    options.inSampleSize = 1;
                    options.inMutable = true;
                    // We just want the bitmap info, do not allocate memory for the pixels (yet)
                    options.inJustDecodeBounds = true;

                    // Get the real image size
                    BitmapFactory.decodeByteArray(paramImageBytes, 0, paramImageBytes.length, options);
                    int srcW = options.outWidth;
                    int srcH = options.outHeight;
                    XHangouts.debug(String.format("Original: %d×%d", srcW, srcH));

                    // Determine the rotation's effect on srcW / srcH
                    int[] rotatedDimens = ImageUtils.getRotatedDimens(targetRotation, srcW, srcH);
                    srcW = rotatedDimens[0];
                    srcH = rotatedDimens[1];

                    // Find the highest possible sample size divisor that is still larger than our maxes
                    int sampleSize = ImageUtils.getSampleSize(srcW, srcH, config.imageWidth, config.imageHeight);
                    XHangouts.debug(String.format("Estimated: %d×%d, Sample Size: 1/%d", srcW, srcH, sampleSize));

                    // Load the sampled image into memory
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = sampleSize;
                    Bitmap sampled = BitmapFactory.decodeByteArray(paramImageBytes, 0, paramImageBytes.length, options);
                    XHangouts.debug(String.format("Sampled: %d×%d", sampled.getWidth(), sampled.getHeight()));

                    // A little performance tweak that helps to prevent some memory issues
                    if ((targetWidth > config.imageWidth) || (targetHeight > config.imageHeight)) {
                        targetWidth = config.imageWidth;
                        targetHeight = config.imageHeight;
                    }

                    // If the image is already smaller than what Hangouts needs then don't scale the image
                    Bitmap moddedBitmap;
                    if (sampled.getWidth() > targetWidth || sampled.getHeight() > targetHeight) {
                        moddedBitmap = ImageUtils.doMatrix(sampled, targetRotation, config.imageWidth, config.imageHeight);
                    } else {
                        moddedBitmap = ImageUtils.doMatrix(sampled, targetRotation);
                    }
                    XHangouts.debug(String.format("Final: %d×%d", moddedBitmap.getWidth(), moddedBitmap.getHeight()));

                    /*if (sampled != moddedBitmap) {
                        findMethodExact(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD_CLEANUP, Bitmap.class)
                                .invoke(param.thisObject, sampled);
                        if (sampled.isRecycled()) {
                            debug("Recycled unscaled bitmap");
                        }
                    }*/

                    // This is much faster than calling the method above, but what about the cache?
                    if (sampled != moddedBitmap) {
                        sampled.recycle();
                    }

                    // Return the result
                    param.setResult(moddedBitmap);
                } catch (Throwable t) {
                    XHangouts.log(t);
                }
            }
        });

    }
}
