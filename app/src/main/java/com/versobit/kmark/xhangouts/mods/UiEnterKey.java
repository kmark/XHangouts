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
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.Setting;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class UiEnterKey extends Module {

    private static final String HANGOUTS_CONVERSATION_MSGEDITTEXT = "com.google.android.apps.hangouts.conversation.v2.MessageEditText";

    private static final String HANGOUTS_CONVERSATOIN_TEXTFRAME = "avk";
    // public onEditorAction(Landroid/widget/TextView;ILandroid/view/KeyEvent;)Z
    private static final String HANGOUTS_CONVERSATION_TEXTFRAME_ONEDITORACTION = "onEditorAction";

    public UiEnterKey(Config config) {
        super(UiEnterKey.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cMessageEditText = findClass(HANGOUTS_CONVERSATION_MSGEDITTEXT, loader);
        Class cTextFrame = findClass(HANGOUTS_CONVERSATOIN_TEXTFRAME, loader);

        return new IXUnhook[] {
                findAndHookConstructor(cMessageEditText, Context.class, AttributeSet.class, onNewMessageEditText),
                findAndHookMethod(cTextFrame, HANGOUTS_CONVERSATION_TEXTFRAME_ONEDITORACTION,
                        TextView.class, int.class, KeyEvent.class, onEditorAction)
        };
    }

    private final XC_MethodHook onNewMessageEditText = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            config.reload((Context) param.args[0]);

            if(!config.modEnabled) {
                return;
            }

            debug(config.enterKey.name());

            if(config.enterKey == Setting.UiEnterKey.EMOJI_SELECTOR) {
                return;
            }

            EditText et = (EditText)param.thisObject;
            // Remove Emoji selector (works for new line)
            int inputType = et.getInputType() ^ InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
            if(config.enterKey == Setting.UiEnterKey.SEND) {
                // Disable multi-line input which shows the send button
                inputType ^= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            }
            et.setInputType(inputType);
        }
    };

    private final XC_MethodHook onEditorAction = new XC_MethodHook() {
        // Called by at least SwiftKey and Fleksy on new line, but not the AOSP or Google keyboard
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int actionId = (Integer)param.args[1];
            if(config.modEnabled && actionId == EditorInfo.IME_NULL
                    && config.enterKey == Setting.UiEnterKey.NEWLINE) {
                param.setResult(false); // We do not handle the enter action, and it adds a newline for us
            }
        }
    };

}
