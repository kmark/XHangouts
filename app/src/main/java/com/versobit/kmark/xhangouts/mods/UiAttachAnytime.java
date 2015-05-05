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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiAttachAnytime extends Module {

    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW = "com.google.android.apps.hangouts.conversation.impl.ComposeMessageView";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_EMOJIBUTTON = "d";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_ADDATTACHMENT = "l";

    private Class cComposeMessageView;

    public UiAttachAnytime(Config config) {
        super(UiAttachAnytime.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        cComposeMessageView = findClass(HANGOUTS_VIEWS_COMPOSEMSGVIEW, loader);

        return new XC_MethodHook.Unhook[] {
                findAndHookConstructor(cComposeMessageView,
                        Context.class, AttributeSet.class, onNewComposeMessageView)
        };
    }

    private XC_MethodHook onNewComposeMessageView = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            config.reload((Context) param.args[0]);

            if(!config.modEnabled) {
                return;
            }

            debug(String.valueOf(config.attachAnytime));

            if(!config.attachAnytime) {
                return;
            }

            ImageButton d = (ImageButton)getObjectField(param.thisObject,
                    HANGOUTS_VIEWS_COMPOSEMSGVIEW_EMOJIBUTTON);
            d.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ((Runnable)callStaticMethod(cComposeMessageView,
                            HANGOUTS_VIEWS_COMPOSEMSGVIEW_ADDATTACHMENT, param.thisObject)).run();
                    return true;
                }
            });
        }
    };

}
