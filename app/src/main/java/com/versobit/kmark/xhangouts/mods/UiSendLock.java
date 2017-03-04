/*
 * Copyright (C) 2015-2017 Kevin Mark
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
import android.util.AttributeSet;
import android.view.View;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;
import eu.chainfire.libsuperuser.Shell;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiSendLock {

    private static final String HANGOUTS_CONVERSATION_FLOATBTNCOUNTER = "com.google.android.apps.hangouts.conversation.v2.FloatingButtonWithCounter";
    private static final String HANGOUTS_CONVERSATION_FLOATBTNCOUNTER_VIEWGROUP = "c";

    private static Shell.Builder shell = null;
    private static volatile Shell.Interactive activeShell = null;

    private static void newShell() {
        shell = new Shell.Builder().useSU().addCommand("input keyevent 26");
    }


    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        Class cFloatBtnCounter = findClass(HANGOUTS_CONVERSATION_FLOATBTNCOUNTER, loader);

        findAndHookConstructor(cFloatBtnCounter, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                config.reload((Context) param.args[0]);

                if (!config.modEnabled) {
                    return;
                }

                XHangouts.debug(String.format("sendLock: %b", config.sendLock));

                if (!config.sendLock) {
                    return;
                }

                newShell();

                ((View) getObjectField(param.thisObject, HANGOUTS_CONVERSATION_FLOATBTNCOUNTER_VIEWGROUP))
                        .setOnLongClickListener(onSendLongClick);
            }
        });

        /*findAndHookMethod(cFloatBtnCounter, "", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.modEnabled && config.sendLock) {
                    // Prevent the setOnLongClick from being overwritten
                    param.setResult(null);
                }
            }
        });*/
    }


    private static final View.OnLongClickListener onSendLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            //Lock
            activeShell = shell.open();
            // Send
            v.callOnClick();
            // Reset lock
            newShell();
            // Safely close the shell after it's done
            new Thread(waitAndCloseShell).start();
            return true;
        }
    };

    private static final Runnable waitAndCloseShell = new Runnable() {
        @Override
        public void run() {
            if (activeShell != null) {
                activeShell.waitForIdle();
                activeShell.close();
            }
        }
    };
}
