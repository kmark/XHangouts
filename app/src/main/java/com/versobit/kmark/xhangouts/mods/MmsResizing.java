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

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.Setting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class MmsResizing extends Module {

    private static final String HANGOUTS_PROCESS_MMS_IMG_CLASS = "due";
    // private static a(IIIILandroid/net/Uri;Landroid/content/Context;)[B
    private static final String HANGOUTS_PROCESS_MMS_IMG_METHOD = "a";

    private static final String HANGOUTS_ESPROVIDER_CLASS = "com.google.android.apps.hangouts.content.EsProvider";
    // private static a(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
    private static final String HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE = "a";

    private Class cEsProvider;

    public MmsResizing(Config config) {
        super(MmsResizing.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cProcessMmsImg = findClass(HANGOUTS_PROCESS_MMS_IMG_CLASS, loader);
        cEsProvider = findClass(HANGOUTS_ESPROVIDER_CLASS, loader);

        return new IXUnhook[]{
                findAndHookMethod(cProcessMmsImg, HANGOUTS_PROCESS_MMS_IMG_METHOD,
                        int.class, int.class, int.class, int.class, Uri.class, Context.class, processMmsImage)
        };
    }

    private final XC_MethodHook processMmsImage = new XC_MethodHook() {
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
            if(!config.modEnabled || !config.resizing) {
                return;
            }

            // Thanks to cottonBallPaws @ http://stackoverflow.com/a/4250279/238374

            final int paramWidth = (Integer)param.args[1];
            final int paramHeight = (Integer)param.args[2];
            final Uri imgUri = (Uri)param.args[4];
            final Context paramContext = (Context)param.args[5];

            // Prevents leak of Hangouts account email to the debug log
            final String safeUri = imgUri.toString().substring(0, imgUri.toString().indexOf("?"));

            debug(String.format("New MMS image! %d, %d, %s, %s, %s", paramWidth, paramHeight, safeUri, param.args[0], param.args[3]));
            String quality = config.imageFormat == Setting.ImageFormat.PNG ? "lossless" : String.valueOf(config.imageQuality);
            debug(String.format("Configuration: %d×%d, %s at %s quality", config.imageWidth, config.imageHeight,
                    config.imageFormat.toString(), quality));

            ContentResolver esAppResolver = AndroidAppHelper.currentApplication().getContentResolver();
            InputStream imgStream = esAppResolver.openInputStream(imgUri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(imgStream, null, options);
            imgStream.close();

            int srcW = options.outWidth;
            int srcH = options.outHeight;

            debug(String.format("Original: %d×%d", srcW, srcH));

            int rotation = 0;
            if(config.rotation) {
                rotation = config.rotateMode;
                if(rotation == -1) {
                    // Find the rotated "real" dimensions to determine proper final scaling
                    // ExifInterface requires a real file path so we ask Hangouts to tell us where the cached file is located
                    String scratchId = imgUri.getPathSegments().get(1);
                    String filePath = (String)callStaticMethod(cEsProvider, HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE, paramContext, scratchId);
                    if(new File(filePath).exists()) {
                        debug(String.format("Cache file located: %s", filePath));
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
                                break;
                            default:
                                rotation = 0;
                        }
                    } else {
                        rotation = 0;
                        log("Cache file does not exist! Skipping orientation correction.");
                        debug(String.format("Bad cache path: %s", filePath));
                        // We could work around this and write the image byte stream out and then
                        // read it back in but that would take a relatively long time. I've
                        // only seen the scratch pad method fail once (when I called the wrong
                        // function from EsProvider).
                    }
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
                    debug(String.format("Rotated: %d×%d, Rotation: %d°", srcW, srcH, rotation));
                }
            }

            // Find the highest possible sample size divisor that is still larger than our maxes
            int inSS = 1;
            while((srcW / 2 > config.imageWidth) || (srcH / 2 > config.imageHeight)) {
                srcW /= 2;
                srcH /= 2;
                inSS *= 2;
            }

            // Use the longest side to determine scale, this should always be <= 1
            float scale = ((float)(srcW > srcH ? config.imageWidth : config.imageHeight)) / (srcW > srcH ? srcW : srcH);

            debug(String.format("Estimated: %d×%d, Sample Size: 1/%d, Scale: %f", srcW, srcH, inSS, scale));

            // Load the sampled image into memory
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inSampleSize = inSS;
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            imgStream = esAppResolver.openInputStream(imgUri);
            Bitmap sampled = BitmapFactory.decodeStream(imgStream, null, options);
            imgStream.close();
            debug(String.format("Sampled: %d×%d", sampled.getWidth(), sampled.getHeight()));

            // Load our scale and rotation changes into a matrix and use it to create the final bitmap
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            Bitmap scaled = Bitmap.createBitmap(sampled, 0, 0, sampled.getWidth(), sampled.getHeight(), m, true);
            sampled.recycle();
            debug(String.format("Scaled: %d×%d", scaled.getWidth(), scaled.getHeight()));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = null;
            final int compressQ = config.imageFormat == Setting.ImageFormat.PNG ? 0 : config.imageQuality;
            switch (config.imageFormat) {
                case PNG:
                    compressFormat = Bitmap.CompressFormat.PNG;
                    break;
                case JPEG:
                    compressFormat = Bitmap.CompressFormat.JPEG;
                    break;
            }
            scaled.compress(compressFormat, compressQ, output);
            final int bytes = output.size();
            scaled.recycle();

            param.setResult(output.toByteArray());
            output.close();
            debug(String.format("MMS image processing complete. %d bytes", bytes));
        }
    };

}
