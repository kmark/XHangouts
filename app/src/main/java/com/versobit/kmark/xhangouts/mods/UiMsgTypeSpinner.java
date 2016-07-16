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

import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.Spinner;

import com.versobit.kmark.xhangouts.Config;

import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiMsgTypeSpinner {
    private static final String HANGOUTS_CONVERSATION_TEXTFRAME = "bth";

    private static final String HANGOUTS_CONVERSATION_LAYOUT = "f";
    private static final String HANGOUTS_CONVERSATION_METHOD = "a";
    private static final String HANGOUTS_CONVERSATION_SPINNER = "a";

    public static void handleLoadPackage(Config config, ClassLoader loader) {
        // This doesn't need a menu setting as it fixes a bug in Hangouts
        if (!config.modEnabled) {
            return;
        }

        Class cTextFrame = findClass(HANGOUTS_CONVERSATION_TEXTFRAME, loader);

        findAndHookMethod(cTextFrame, HANGOUTS_CONVERSATION_METHOD, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View msgView = (View) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_LAYOUT);
                Spinner TransportSpinner = (Spinner) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_SPINNER);
                if (TransportSpinner.getVisibility() != View.GONE) {
                    if (isRTL()) {
                        msgView.setPadding(msgView.getPaddingLeft(), msgView.getPaddingTop(), 0, msgView.getPaddingBottom());
                    } else {
                        msgView.setPadding(0, msgView.getPaddingTop(), msgView.getPaddingRight(), msgView.getPaddingBottom());
                    }
                }
            }
        });

    }

    private static boolean isRTL() {
        Locale defaultLocale = Locale.getDefault();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return TextUtils.getLayoutDirectionFromLocale(defaultLocale) == View.LAYOUT_DIRECTION_RTL;
        } else {
            return Character.getDirectionality(defaultLocale.getDisplayName(defaultLocale)
                    .charAt(0)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT;
        }
    }
}
