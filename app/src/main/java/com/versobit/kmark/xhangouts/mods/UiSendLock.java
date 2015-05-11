/*
 * Copyright (C) 2015 Kevin Mark
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
import android.widget.ImageButton;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;
import eu.chainfire.libsuperuser.Shell;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiSendLock extends Module {

    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW = "com.google.android.apps.hangouts.conversation.impl.ComposeMessageView";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_SENDBUTTON = "c";

    private Shell.Builder shell;

    public UiSendLock(Config config) {
        super(UiSendLock.class.getSimpleName(), config);
        newShell();
    }

    private void newShell() {
        shell = new Shell.Builder().useSU().addCommand("input keyevent 26");
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cComposeMessageView = findClass(HANGOUTS_VIEWS_COMPOSEMSGVIEW, loader);

        return new IXUnhook[] {
                findAndHookConstructor(cComposeMessageView,
                        Context.class, AttributeSet.class, onNewComposeMessageView),
                findAndHookMethod(cComposeMessageView, "k", preventOverwrite)
        };
    }

    private final XC_MethodHook onNewComposeMessageView = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            config.reload((Context)param.args[0]);

            if(!config.modEnabled) {
                return;
            }

            ((ImageButton)getObjectField(param.thisObject, HANGOUTS_VIEWS_COMPOSEMSGVIEW_SENDBUTTON))
                    .setOnLongClickListener(onSendLongClick);
        }
    };

    private final View.OnLongClickListener onSendLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            // Lock
            shell.open();
            // Send
            v.callOnClick();
            // Reset lock
            newShell();
            return true;
        }
    };

    private final XC_MethodHook preventOverwrite = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if(config.modEnabled) {
                // Prevent the setOnLongClick from being overwritten
                param.setResult(null);
            }
        }
    };
}
