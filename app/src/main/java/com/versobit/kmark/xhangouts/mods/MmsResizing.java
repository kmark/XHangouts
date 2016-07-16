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

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.ImageUtils;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.XHangouts;

import java.io.InputStream;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class MmsResizing {

    private static final String HANGOUTS_PROCESS_MMS_IMG_CLASS = "ffx";
    // private static a(IIIILandroid/net/Uri;Landroid/content/Context;)[B
    private static final String HANGOUTS_PROCESS_MMS_IMG_METHOD = "a";

    private static final String HANGOUTS_ESPROVIDER_CLASS = "com.google.android.apps.hangouts.content.EsProvider";
    // private static a(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
    private static final String HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE = "a";

    private static Class cEsProvider;

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        if (!config.modEnabled || !config.resizing) {
            return;
        }

        Class cProcessMmsImg = findClass(HANGOUTS_PROCESS_MMS_IMG_CLASS, loader);
        cEsProvider = findClass(HANGOUTS_ESPROVIDER_CLASS, loader);

        findAndHookMethod(cProcessMmsImg, HANGOUTS_PROCESS_MMS_IMG_METHOD, int.class, int.class,
                int.class, int.class, Uri.class, Context.class, new XC_MethodHook() {
            // This is called when the user hits the send button on an image MMS
            // FIXME: there seem to be a few instances where this is not called, find alternate code paths

            // int1 = ? (usually zero, it seems)
            // int2 = max scaled width, appears to be 640 if landscape or square, 480 if portrait
            // int3 = max scaled height, appears to be 640 if portrait, 480 if landscape or square
            // int4 ?, seems to be width * height - 1024 = 306176
            // Uri1 content:// path that references the input image
            // Context

            // At least one instance has been reported of int2, int3, and int4 being populated with
            // much larger values resulting in an image much too large to be sent via MMS

            // We're not replacing the method so that even if we fail, which is conceivable, we
            // safely fall back to the original Hangouts result. This also means that if the Hangout
            // function call does something weird that needs to be done (that we don't do) it still
            // gets done. Downside is that we're running code that may never be used.

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!config.modEnabled || !config.resizing) {
                    return;
                }

                // Thanks to cottonBallPaws @ http://stackoverflow.com/a/4250279/238374

                final int paramWidth = (Integer) param.args[1];
                final int paramHeight = (Integer) param.args[2];
                final Uri imgUri = (Uri) param.args[4];
                final Context paramContext = (Context) param.args[5];

                // Prevents leak of Hangouts account email to the debug log
                final String safeUri = imgUri.toString().substring(0, imgUri.toString().indexOf("?"));

                XHangouts.debug(String.format("New MMS image! %d, %d, %s, %s, %s", paramWidth, paramHeight, safeUri, param.args[0], param.args[3]));
                String quality = config.imageFormat == Setting.ImageFormat.PNG ? "lossless" : String.valueOf(config.imageQuality);
                XHangouts.debug(String.format("Configuration: %d×%d, %s at %s quality", config.imageWidth, config.imageHeight,
                        config.imageFormat.toString(), quality));

                ContentResolver esAppResolver = AndroidAppHelper.currentApplication().getContentResolver();
                InputStream imgStream = esAppResolver.openInputStream(imgUri);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(imgStream, null, options);
                imgStream.close();

                int srcW = options.outWidth;
                int srcH = options.outHeight;

                XHangouts.debug(String.format("Original: %d×%d", srcW, srcH));

                // We have to calculate the rotation now so that we may calculate the sample size
                // with the rotation applied to srcW / srcH
                int rotation = 0;
                if (config.rotation) {
                    rotation = config.rotateMode;
                    if (rotation == -1) {

                        // Find the rotated "real" dimensions to determine proper final scaling
                        // ExifInterface requires a real file path so we ask Hangouts to tell us where the cached file is located
                        String scratchId = imgUri.getPathSegments().get(1);
                        String filePath = (String) callStaticMethod(cEsProvider, HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE, paramContext, scratchId);
                        rotation = ImageUtils.getExifRotation(filePath);
                    }
                    if (rotation == 90 || rotation == 270) {
                        // If we need to support other rotation amounts we need to use getRotatedDimens
                        int tmp = srcW;
                        srcW = srcH;
                        srcH = tmp;
                        XHangouts.debug(String.format("Rotated: %d×%d, Rotation: %d°", srcW, srcH, rotation));
                    }
                }

                // Find the highest possible sample size divisor that is still larger than our maxes
                int sampleSize = ImageUtils.getSampleSize(srcW, srcH, config.imageWidth, config.imageHeight);

                XHangouts.debug(String.format("Estimated: %d×%d, Sample Size: 1/%d", srcW, srcH, sampleSize));

                // Load the sampled image into memory
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inSampleSize = sampleSize;
                options.inScaled = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                imgStream = esAppResolver.openInputStream(imgUri);
                Bitmap sampled = BitmapFactory.decodeStream(imgStream, null, options);
                imgStream.close();
                XHangouts.debug(String.format("Sampled: %d×%d", sampled.getWidth(), sampled.getHeight()));

                // Load our scale and rotation changes into a matrix and use it to create the final bitmap
                Bitmap scaled = ImageUtils.doMatrix(sampled, rotation, config.imageWidth, config.imageHeight);
                // There's a possibility that doMatrix may return its own input due to createBitmap
                if (sampled != scaled) {
                    sampled.recycle();
                }
                XHangouts.debug(String.format("Scaled: %d×%d", scaled.getWidth(), scaled.getHeight()));

                param.setResult(ImageUtils.compress(scaled, config.imageFormat, config.imageQuality, true));
                XHangouts.debug(String.format("MMS image processing complete."));
            }
        });

    }
}
