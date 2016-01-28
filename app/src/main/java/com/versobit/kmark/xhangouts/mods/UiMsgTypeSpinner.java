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

import android.view.View;
import android.widget.Spinner;

import com.versobit.kmark.xhangouts.Config;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiMsgTypeSpinner {
    private static final String HANGOUTS_CONVERSATION_RTL_HELP = "eep";
    private static final String HANGOUTS_CONVERSATION_TEXTFRAME = "avk";

    private static final String HANGOUTS_CONVERSATION_IS_RTL = "d";
    private static final String HANGOUTS_CONVERSATION_LAYOUT = "j";
    private static final String HANGOUTS_CONVERSATION_METHOD = "a";
    private static final String HANGOUTS_CONVERSATION_SPINNER = "g";

    private static Class cRTL;

    public static void handleLoadPackage(Config config, ClassLoader loader) {
        // This doesn't need a menu setting as it fixes a bug in Hangouts
        if (!config.modEnabled) {
            return;
        }

        Class cTextFrame = findClass(HANGOUTS_CONVERSATION_TEXTFRAME, loader);
        cRTL = findClass(HANGOUTS_CONVERSATION_RTL_HELP, loader);

        findAndHookMethod(cTextFrame, HANGOUTS_CONVERSATION_METHOD, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View msgView = (View) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_LAYOUT);
                Spinner TransportSpinner = (Spinner) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_SPINNER);
                if (TransportSpinner.getVisibility() != View.GONE) {
                    boolean RTL = (boolean) callStaticMethod(cRTL, HANGOUTS_CONVERSATION_IS_RTL);
                    if (!RTL) {
                        msgView.setPadding(0, msgView.getPaddingTop(), msgView.getPaddingRight(), msgView.getPaddingBottom());
                    } else {
                        msgView.setPadding(msgView.getPaddingLeft(), msgView.getPaddingTop(), 0, msgView.getPaddingBottom());
                    }
                }
            }
        });

    }
}
