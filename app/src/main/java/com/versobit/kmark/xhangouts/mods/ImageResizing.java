/*
 * Copyright (C) 2014-2015 Kevin Mark
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
import android.graphics.Matrix;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.Setting;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;


public final class ImageResizing extends Module {

    private static final String HANGOUTS_PROCESS_IMG_CLASS = "ecp";
    // public Bitmap b(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3)
    private static final String HANGOUTS_PROCESS_IMG_METHOD = "b";
    // public void a(Bitmap paramBitmap)
    private static final String HANGOUTS_PROCESS_IMG_METHOD_CLEANUP = "a";

    public ImageResizing(Config config) {
        super(ImageResizing.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cProcessImg = findClass(HANGOUTS_PROCESS_IMG_CLASS, loader);

        return new IXUnhook[]{
                findAndHookMethod(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD,
                        byte[].class, int.class, int.class, int.class, processImage)
        };
    }

    private final XC_MethodHook processImage = new XC_MethodHook() {
        /*
         * byte[] = The image
         * int1 = Height
         * int2 = Width
         * int3 = Rotation
         *
         * We're using beforeHookedMethod to avoid a performance hit.
         */

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (!config.modEnabled || !config.resizing) {
                return;
            }

            try {
                // Get our params
                byte[] paramArrayOfByte = (byte[]) param.args[0];
                int paramHeight = (Integer) param.args[1];
                int paramWidth = (Integer) param.args[2];
                final int paramRotation = (Integer) param.args[3];

                // Stickers and other junk
                if (paramHeight <= 400 && paramWidth <= 400) {
                    return;
                }

                debug(String.format("Original limit is w=%d h=%d", paramWidth, paramHeight));

                // Setup some options to decode the bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                options.inDensity = 0;
                options.inTargetDensity = 0;
                options.inSampleSize = 1;
                options.inMutable = true;
                options.inJustDecodeBounds = true;

                // Get the real image size
                BitmapFactory.decodeByteArray(paramArrayOfByte, 0, paramArrayOfByte.length, options);
                options.inJustDecodeBounds = false;
                int srcW = options.outWidth;
                int srcH = options.outHeight;

                // Find the highest possible sample size divisor that is still larger than our maxes
                int inSS = 1;
                while ((srcW / 2 > config.imageWidth) || (srcH / 2 > config.imageHeight)) {
                    srcW /= 2;
                    srcH /= 2;
                    inSS *= 2;
                }
                debug(String.format("Unscaled bitmap is w=%d h=%d", srcW, srcH));

                // Subsample the image so that we don't crop it instead
                options.inSampleSize = inSS;
                Bitmap unscaledBitmap = BitmapFactory.decodeByteArray(paramArrayOfByte, 0, paramArrayOfByte.length, options);

                // A little performance tweak that helps to prevent some memory issues
                if ((paramWidth > config.imageWidth) || (paramHeight > config.imageHeight)) {
                    paramWidth = config.imageWidth;
                    paramHeight = config.imageHeight;
                }

                // If the image is already smaller than what Hangouts needs then don't scale the image
                Bitmap moddedBitmap;
                if ((srcW <= paramWidth && srcH <= paramHeight) || (config.imageFormat == Setting.ImageFormat.PNG)) {
                    moddedBitmap = RotateBitmap(unscaledBitmap, paramRotation, srcW, srcH, inSS, false);
                    debug("Returned unscaled bitmap");
                    /*if (unscaledBitmap != moddedBitmap) {
                        findMethodExact(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD_CLEANUP, Bitmap.class)
                                .invoke(param.thisObject, unscaledBitmap);
                        if (unscaledBitmap.isRecycled()) {
                            debug("Recycled unscaled bitmap");
                        }
                    }*/
                } else {
                    moddedBitmap = RotateBitmap(unscaledBitmap, paramRotation, srcW, srcH, inSS, true);
                    debug(String.format("Returned scaled bitmap w=%d h=%d", moddedBitmap.getWidth(), moddedBitmap.getHeight()));
                    /*if (unscaledBitmap != moddedBitmap) {
                        findMethodExact(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD_CLEANUP, Bitmap.class)
                                .invoke(param.thisObject, unscaledBitmap);
                        if (unscaledBitmap.isRecycled()) {
                            debug("Recycled unscaled bitmap");
                        }
                    }*/
                }

                // This is much faster than calling the method above, but what about the cache?
                if (unscaledBitmap != moddedBitmap) {
                    unscaledBitmap.recycle();
                }

                // Return the result
                param.setResult(moddedBitmap);
            } catch (Throwable t) {
                log(t.getMessage());
            }

        }

        private Bitmap RotateBitmap(Bitmap bitmap, int rotate, int srcWidth, int srcHeight, int sSize, Boolean scaleBitmap) {
            Matrix m = new Matrix();
            if (scaleBitmap) {
                // Use the longest side to determine scale
                float scale = ((float) (srcWidth > srcHeight ? config.imageWidth : config.imageHeight)) / (srcWidth > srcHeight ? srcWidth : srcHeight);
                debug(String.format("Sample Size: %d, Scale: %f", sSize, scale));
                m.postScale(scale, scale);
            }
            // TODO: Double check this as the decompiler had issues with this whole method
            m.postRotate(rotate, srcWidth / 2, srcHeight / 2); // This is how Hangouts 4+ seems to rotate all images
            return Bitmap.createBitmap(bitmap, 0, 0, srcWidth, srcHeight, m, true);
        }

    };
}