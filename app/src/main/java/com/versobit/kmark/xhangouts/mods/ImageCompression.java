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

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.Setting;

import java.io.ByteArrayOutputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class ImageCompression extends Module {

    private static final String HANGOUTS_PROCESS_IMG_CLASS = "edr";
    // public static byte[] a(Bitmap paramBitmap, int paramInt)
    private static final String HANGOUTS_PROCESS_IMG_METHOD = "a";

    public ImageCompression(Config config) {
        super(ImageCompression.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cProcessImg = findClass(HANGOUTS_PROCESS_IMG_CLASS, loader);

        return new IXUnhook[]{
                findAndHookMethod(cProcessImg, HANGOUTS_PROCESS_IMG_METHOD,
                        Bitmap.class, int.class, processImage)
        };
    }

    private final XC_MethodHook processImage = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (!config.modEnabled || !config.resizing) {
                return;
            }

            try {
                Bitmap paramBitmap = (Bitmap) param.args[0];
                final int paramInt = (Integer) param.args[1]; // Original compression level

                ByteArrayOutputStream output = new ByteArrayOutputStream();

                Bitmap.CompressFormat compressFormat = null;
                final int compressQ = config.imageFormat == Setting.ImageFormat.PNG ? 0 : config.imageQuality;
                switch (config.imageFormat) {
                    case PNG:
                        debug(String.format("Old compression level: %s / New: Lossless", paramInt));
                        compressFormat = Bitmap.CompressFormat.PNG;
                        break;
                    case JPEG:
                        debug(String.format("Old compression level: %s / New: %s", paramInt, config.imageQuality));
                        compressFormat = Bitmap.CompressFormat.JPEG;
                        break;
                }

                paramBitmap.compress(compressFormat, compressQ, output);
                param.setResult(output.toByteArray());
            } catch (Throwable t) {
                log(t.getMessage());
            }

        }
    };
}
