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

    // TODO: This is awfully similar to MmsResizing. Move common functionality to util class?
    private final XC_MethodHook processImage = new XC_MethodHook() {
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
            if (!config.modEnabled || !config.resizing) {
                return;
            }

            try {
                // Get our params
                byte[] paramArrayOfByte = (byte[]) param.args[0];
                int paramHeight = (int) param.args[1];
                int paramWidth = (int) param.args[2];
                final int paramRotation = (int) param.args[3];

                // Stickers and other junk
                if (paramHeight <= 400 && paramWidth <= 400) {
                    return;
                }

                debug(String.format("Param Limits: %d×%d", paramWidth, paramHeight));

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
                BitmapFactory.decodeByteArray(paramArrayOfByte, 0, paramArrayOfByte.length, options);
                int srcW = options.outWidth;
                int srcH = options.outHeight;
                debug(String.format("Original: %d×%d", srcW, srcH));

                // Find the highest possible sample size divisor that is still larger than our maxes
                int inSS = 1;
                while ((srcW / 2 > config.imageWidth) || (srcH / 2 > config.imageHeight)) {
                    srcW /= 2;
                    srcH /= 2;
                    inSS *= 2;
                }
                debug(String.format("Estimated: %d×%d, Sample Size: 1/%d", srcW, srcH, inSS));

                // Load the sampled image into memory
                options.inJustDecodeBounds = false;
                options.inSampleSize = inSS;
                Bitmap sampled = BitmapFactory.decodeByteArray(paramArrayOfByte, 0, paramArrayOfByte.length, options);
                debug(String.format("Sampled: %d×%d", sampled.getWidth(), sampled.getHeight()));

                // A little performance tweak that helps to prevent some memory issues
                if ((paramWidth > config.imageWidth) || (paramHeight > config.imageHeight)) {
                    paramWidth = config.imageWidth;
                    paramHeight = config.imageHeight;
                }

                // If the image is already smaller than what Hangouts needs then don't scale the image
                Bitmap moddedBitmap = processBitmap(sampled, paramRotation,
                        sampled.getWidth() > paramWidth || sampled.getHeight() > paramHeight);
                debug(String.format("Final: %d×%d", moddedBitmap.getWidth(), moddedBitmap.getHeight()));
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
                log(t);
            }

        }

        // Uses a matrix to rotate and scale a Bitmap
        private Bitmap processBitmap(Bitmap bmp, float rotate, boolean doScale) {
            Matrix m = new Matrix();
            int w = bmp.getWidth(), h = bmp.getHeight();

            if(doScale) {
                // Use the longest side to determine scale
                float scale = ((float) (w > h ? config.imageWidth : config.imageHeight)) / (w > h ? w : h);
                m.postScale(scale, scale);
                debug(String.format("Scale factor: %s", scale));
            }

            if(rotate != 0f) {
                // TODO: Double check this as the decompiler had issues with this whole method
                // FIXME: Rotation should be performed before subsampling and scaling?
                m.postRotate(rotate, w / 2f, h / 2f);
                debug(String.format("Rotation: %s°", rotate));
            }

            return Bitmap.createBitmap(bmp, 0, 0, w, h, m, true);
        }

    };
}
