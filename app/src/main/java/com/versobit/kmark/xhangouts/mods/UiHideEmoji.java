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

import android.view.View;
import android.widget.ImageButton;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiHideEmoji extends Module {

    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW = "com.google.android.apps.hangouts.conversation.impl.ComposeMessageView";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_EMOJILOGIC = "r";

    public UiHideEmoji(Config config) {
        super(UiHideEmoji.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cComposeMessageView = findClass(HANGOUTS_VIEWS_COMPOSEMSGVIEW, loader);

        return new IXUnhook[] {
                findAndHookMethod(cComposeMessageView, HANGOUTS_VIEWS_COMPOSEMSGVIEW_EMOJILOGIC,
                        determineEmojiAvailability)
        };
    }

    private final XC_MethodHook determineEmojiAvailability = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            config.reload(((View)param.thisObject).getContext());

            if(!config.modEnabled) {
                return;
            }

            debug(String.valueOf(config.hideEmoji));

            if(!config.hideEmoji) {
                return;
            }

            // Prevent the method from setting the visibility
            param.setResult(null);
        }
    };
}
