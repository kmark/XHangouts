/*
 * Copyright (C) 2014 Kevin Mark
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
 * along with Weather Doge.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.xhangouts;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class XHangouts implements IXposedHookLoadPackage {
    private static final String TAG = "XHangouts";

    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";
    private static final String ACTIVITY_THREAD_CURRENTACTHREAD = "currentActivityThread";
    private static final String ACTIVITY_THREAD_GETSYSCTX = "getSystemContext";

    private static final String HANGOUTS_PKG_NAME = "com.google.android.talk";

    private static final String HANGOUTS_ESAPP_CLASS = "com.google.android.apps.hangouts.phone.EsApplication";
    private static final String HANGOUTS_ESAPP_ONCREATE = "onCreate";

    private static final String HANGOUTS_PROCESS_MMS_IMG_CLASS = "bvp";
    // private static a(IIIILandroid/net/Uri;)[B
    private static final String HANGOUTS_PROCESS_MMS_IMG_METHOD = "a";

    private static final String HANGOUTS_ESPROVIDER_CLASS = "com.google.android.apps.hangouts.content.EsProvider";
    // private static d(Ljava/lang/String;)Ljava/lang/String
    private static final String HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE = "d";

    private static final boolean LOG = true;
    private static final boolean ROTATE = true;

    private static final String TESTED_VERSION_STR = "2.3.75731955";
    private static final int TESTED_VERSION_INT = 22037769;

    // Not certain if I need a WeakReference here. Without it could prevent the Context from being closed?
    WeakReference<Context> hangoutsCtx;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(!loadPackageParam.packageName.equals(HANGOUTS_PKG_NAME)) {
            return;
        }

        Object activityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass(ACTIVITY_THREAD_CLASS, null), ACTIVITY_THREAD_CURRENTACTHREAD);
        final Context systemCtx = (Context)XposedHelpers.callMethod(activityThread, ACTIVITY_THREAD_GETSYSCTX);

        log("--- LOADING XHANGOUTS ---", false);
        log(String.format("XHangouts v%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), false);

        PackageInfo pi = systemCtx.getPackageManager().getPackageInfo(HANGOUTS_PKG_NAME, 0);
        log(String.format("Google Hangouts v%s (%d)", pi.versionName, pi.versionCode), false);
        // TODO: replace this with something more robust?
        if(pi.versionCode != TESTED_VERSION_INT) {
            log(String.format("Warning: Your Hangouts version differs from the version XHangouts was built against: v%s (%d)", TESTED_VERSION_STR, TESTED_VERSION_INT), false);
        }

        // Get application context to use later
        XposedHelpers.findAndHookMethod(HANGOUTS_ESAPP_CLASS, loadPackageParam.classLoader, HANGOUTS_ESAPP_ONCREATE, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("Context set.");
                hangoutsCtx = new WeakReference<Context>((Context)param.thisObject);
            }
        });


        // This is called when the user hits the send button on an image MMS
        // TODO: there seem to be a few instances where this is not called, find alternate code paths
        XposedHelpers.findAndHookMethod(HANGOUTS_PROCESS_MMS_IMG_CLASS, loadPackageParam.classLoader, HANGOUTS_PROCESS_MMS_IMG_METHOD, int.class, int.class, int.class, int.class, Uri.class, new XC_MethodHook() {
            // int1 = ? (usually zero, it seems)
            // int2 = max scaled width, appears to be 640 if landscape or square, 480 if portrait
            // int3 = max scaled height, appears to be 640 if portrait, 480 if landscape or square
            // int4 ?, seems to be width * height - 1024 = 306176
            // Uri1 content:// path that references the input image

            // We're not replacing the method so that even if we fail, which is conceivable, we
            // safely fall back to the original Hangouts result. This also means that if the Hangout
            // function call does something weird that needs to be done (that we don't do) it still
            // gets done. Downside is that we're running code that may never be used.

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // Thanks to cottonBallPaws @ http://stackoverflow.com/a/4250279/238374

                final int maxWidth = (Integer)param.args[1];
                final int maxHeight = (Integer)param.args[2];
                final Uri imgUri = (Uri)param.args[4];

                log(String.format("New MMS image! Max dimensions: %dx%d, Uri: %s, Other: %s %s", maxWidth, maxHeight, imgUri, param.args[0], param.args[3]));

                ContentResolver esAppResolver = hangoutsCtx.get().getContentResolver();
                InputStream imgStream = esAppResolver.openInputStream(imgUri);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(imgStream, null, options);
                imgStream.close();

                int srcW = options.outWidth;
                int srcH = options.outHeight;

                log(String.format("Original: %dx%d", srcW, srcH));

                int rotation = 0;
                if(ROTATE) {
                    // Find the rotated "real" dimensions to determine proper final scaling
                    // ExifInterface requires a real file path so we ask Hangouts to tell us where the cached file is located
                    String scratchId = imgUri.getPathSegments().get(1);
                    String filePath = (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass(HANGOUTS_ESPROVIDER_CLASS, loadPackageParam.classLoader), HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE, scratchId);
                    log(String.format("Cache file located: %s", filePath));
                    ExifInterface exif = new ExifInterface(filePath);
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
                    }
                    if (rotation != 0) {
                        // Technically we could just swap width and height if rotation = 90 or 270 but
                        // this is a much more fun reference implementation.
                        // TODO: apply rotation to max values as well? Rotated images are scaled more than non
                        Matrix imgMatrix = new Matrix();
                        imgMatrix.postRotate(rotation);
                        RectF imgRect = new RectF();
                        imgMatrix.mapRect(imgRect, new RectF(0, 0, srcW, srcH));
                        srcW = Math.round(imgRect.width());
                        srcH = Math.round(imgRect.height());
                        log(String.format("Rotated: %dx%d, Rotation: %d°", srcW, srcH, rotation));
                    }
                }

                // Find the highest possible sample size divisor that is still larger than our maxes
                int inSS = 1;
                while((srcW / 2 > maxWidth) || (srcH / 2 > maxHeight)) {
                    srcW /= 2;
                    srcH /= 2;
                    inSS *= 2;
                }

                // Use the longest side to determine scale, this should always be <= 1
                float scale = ((float)(srcW > srcH ? maxWidth : maxHeight)) / (srcW > srcH ? srcW : srcH);

                log(String.format("Estimated: %dx%d, Sample Size: 1/%d, Scale: %f", srcW, srcH, inSS, scale));

                // Load the sampled image into memory
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inSampleSize = inSS;
                options.inScaled = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                imgStream = esAppResolver.openInputStream(imgUri);
                Bitmap sampled = BitmapFactory.decodeStream(imgStream, null, options);
                imgStream.close();
                log(String.format("Sampled: %dx%d", sampled.getWidth(), sampled.getHeight()));

                // Load our scale and rotation changes into a matrix and use it to create the final bitmap
                Matrix m = new Matrix();
                m.postScale(scale, scale);
                m.postRotate(rotation);
                Bitmap scaled = Bitmap.createBitmap(sampled, 0, 0, sampled.getWidth(), sampled.getHeight(), m, true);
                sampled.recycle();
                log(String.format("Scaled: %dx%d", scaled.getWidth(), scaled.getHeight()));

                
                // In the Hangouts implementation it converts to JPEG here, and then compresses it again later
                // not certain as to why that is.
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 0, output);

                scaled.recycle();

                param.setResult(output.toByteArray());
                output.close();
                log("MMS image processing complete.");
            }
        });

        XposedBridge.hookMethod(XHangouts.class.getDeclaredMethod("isActive"), new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return true;
            }
        });

        log("--- LOAD COMPLETE ---", false);
    }

    static boolean isActive() {
        return false;
    }

    private static void log(String msg) {
        log(msg, true);
    }

    private static void log(String msg, boolean tag) {
        if(LOG) {
            XposedBridge.log((tag ? TAG + ": " : "") + msg);
        }
    }
}
