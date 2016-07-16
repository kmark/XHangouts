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

public final class UiEmoji {
    private static final String HANGOUTS_CONVERSATION_EMOJI = "ccu";

    private static final String HANGOUTS_CONVERSATION_CONTEXT_FIELD = "b";
    private static final String HANGOUTS_CONVERSATION_DETECT_KEYBOARD = "a";

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        Class cConversationKeyboard = findClass(HANGOUTS_CONVERSATION_EMOJI, loader);

        findAndHookMethod(cConversationKeyboard, HANGOUTS_CONVERSATION_DETECT_KEYBOARD, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_CONTEXT_FIELD);
                config.reload(context);

                if (!config.modEnabled) {
                    return;
                }

                if (config.emoji == Setting.UiEmoji.DEFAULT) {
                    return;
                } else if (config.emoji == Setting.UiEmoji.SHOW) {
                    param.setResult(false);
                } else {
                    param.setResult(true);
                }
            }
        });

    }
}
