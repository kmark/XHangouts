/*
 * Copyright (C) 2015-2016 Kevin Mark
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

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.XHangouts;

import java.io.ByteArrayOutputStream;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class ImageCompression {

    private static final String HANGOUTS_PROCESS_IMG_CLASS = "frq";
    // public static byte[] a(Bitmap paramBitmap, int paramInt)
    private static final String HANGOUTS_PROCESS_IMG_METHOD = "a";

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        if (!config.modEnabled || !config.resizing) {
            return;
        }

        Class cProcessImg = findClass(HANGOUTS_PROCESS_IMG_CLASS, loader);

        findAndHookMethod(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD, Bitmap.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try {
                    Bitmap paramBitmap = (Bitmap) param.args[0];
                    final int paramInt = (int) param.args[1]; // Original compression level

                    final int compressQ;
                    final String compressLog;
                    if (config.imageFormat == Setting.ImageFormat.PNG) {
                        compressQ = 0;
                        compressLog = "Lossless";
                    } else {
                        compressQ = config.imageQuality;
                        compressLog = String.valueOf(config.imageQuality);
                    }
                    XHangouts.debug(String.format("Old compression level: %d / New: %s", paramInt, compressLog));

                    paramBitmap.compress(config.imageFormat.toCompressFormat(), compressQ, output);
                    param.setResult(output.toByteArray());
                } catch (Throwable t) {
                    // Potential OutOfMemoryError
                    XHangouts.log(t);
                } finally {
                    output.close();
                }

            }
        });

    }
}
