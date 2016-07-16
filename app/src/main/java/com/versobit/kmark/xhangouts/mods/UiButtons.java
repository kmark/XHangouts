/*
 * Copyright (C) 2016 Kevin Mark
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

import android.content.Context;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Setting;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiButtons {
    private static final String HANGOUTS_CONVERSATION_EMOJI = "ccu";
    private static final String HANGOUTS_CONVERSATION_GALLERY = "bzt";
    private static final String HANGOUTS_CONVERSATION_CAMERA = "bys";
    private static final String HANGOUTS_CONVERSATION_VIDEO = "cdg";
    private static final String HANGOUTS_CONVERSATION_STICKER = "cbz";
    private static final String HANGOUTS_CONVERSATION_LOCATION = "cbc";

    private static final String HANGOUTS_REQUIRED_CLASS_1 = "bkc";
    private static final String HANGOUTS_REQUIRED_CLASS_2 = "bwb";

    private static final String HANGOUTS_A = "a";
    private static final String HANGOUTS_CONVERSATION_CONTEXT_FIELD = "b";
    private static final String HANGOUTS_CONVERSATION_DETECT_KEYBOARD = "a";

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        Class cEmojiPicker = findClass(HANGOUTS_CONVERSATION_EMOJI, loader);
        Class cGalleryPicker = findClass(HANGOUTS_CONVERSATION_GALLERY, loader);
        Class cCameraPicker = findClass(HANGOUTS_CONVERSATION_CAMERA, loader);
        Class cVideoPicker = findClass(HANGOUTS_CONVERSATION_VIDEO, loader);
        Class cStickerPicker = findClass(HANGOUTS_CONVERSATION_STICKER, loader);
        Class cLocationPicker = findClass(HANGOUTS_CONVERSATION_LOCATION, loader);
        Class cFirstParam = findClass(HANGOUTS_REQUIRED_CLASS_1, loader);
        Class cSecondParam = findClass(HANGOUTS_REQUIRED_CLASS_2, loader);

        findAndHookMethod(cEmojiPicker, HANGOUTS_CONVERSATION_DETECT_KEYBOARD, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_CONTEXT_FIELD);
                config.reload(context);

                if (!config.modEnabled) {
                    return;
                }

                if (config.emoji == Setting.UiEmoji.HIDE) {
                    param.setResult(true);
                } else if (config.emoji == Setting.UiEmoji.SHOW) {
                    param.setResult(false);
                }
            }
        });

        if (!config.modEnabled) {
            return;
        }

        findAndHookMethod(cGalleryPicker, HANGOUTS_A, cFirstParam, cSecondParam, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.gallery == Setting.UiButtons.HIDE) {
                    param.setResult(false);
                }
            }
        });

        findAndHookMethod(cCameraPicker, HANGOUTS_A, cFirstParam, cSecondParam, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.camera == Setting.UiButtons.HIDE) {
                    param.setResult(false);
                }
            }
        });

        findAndHookMethod(cVideoPicker, HANGOUTS_A, cFirstParam, cSecondParam, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.video == Setting.UiButtons.HIDE) {
                    param.setResult(false);
                }
            }
        });

        findAndHookMethod(cStickerPicker, HANGOUTS_A, cFirstParam, cSecondParam, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.stickers == Setting.UiButtons.HIDE) {
                    param.setResult(false);
                }
            }
        });

        findAndHookMethod(cLocationPicker, HANGOUTS_A, cFirstParam, cSecondParam, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.location == Setting.UiButtons.HIDE) {
                    param.setResult(false);
                }
            }
        });

    }
}
