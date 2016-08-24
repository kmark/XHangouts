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

import com.versobit.kmark.xhangouts.Config;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public final class UiDebugOptions {
    private static final String HANGOUTS_DEBUG = "fwk";
    private static final String HANGOUTS_IS_DEBUG_ENABLED = "a";

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        if (!config.modEnabled || !config.showDebugOptions) {
            return;
        }

        findAndHookMethod(HANGOUTS_DEBUG, loader, HANGOUTS_IS_DEBUG_ENABLED, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

    }
}
