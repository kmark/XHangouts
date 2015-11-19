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

import android.app.AndroidAppHelper;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_RES_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class UiCallButtons extends Module {

    private static final String HANGOUTS_ACT_CONVERSATION_SUPER = "bka";
    private static final String HANGOUTS_ACT_CONVERSATION_SUPER_OPOM = "onPrepareOptionsMenu";

    private static final String HANGOUTS_MENU_CONVO_CALL = "realtimechat_conversation_call_menu_item";
    private static final String HANGOUTS_MENU_CONVO_VIDEOCALL = "start_hangout_menu_item";

    // Impossible default ID since resource IDs start with 0x7f
    private static final int RES_ID_UNSET = 0;

    // Resources.getIdentifier is expensive so we're caching results
    private int menuItemCallResId = RES_ID_UNSET;
    private int menuItemVideoCallResId = RES_ID_UNSET;

    public UiCallButtons(Config config) {
        super(UiCallButtons.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        Class cConversationActSuper = findClass(HANGOUTS_ACT_CONVERSATION_SUPER, loader);

        return new IXUnhook[] {
                findAndHookMethod(cConversationActSuper, HANGOUTS_ACT_CONVERSATION_SUPER_OPOM,
                        Menu.class, onPrepareOptionsMenu)
        };
    }

    private final XC_MethodHook onPrepareOptionsMenu = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if(!config.modEnabled) {
                return;
            }

            debug(String.valueOf(config.hideCallButtons));

            if(!config.hideCallButtons) {
                return;
            }

            if(menuItemCallResId == RES_ID_UNSET || menuItemVideoCallResId == RES_ID_UNSET) {
                Resources res = AndroidAppHelper.currentApplication().getResources();
                menuItemCallResId = res.getIdentifier(HANGOUTS_MENU_CONVO_CALL, "id", HANGOUTS_RES_PKG_NAME);
                menuItemVideoCallResId = res.getIdentifier(HANGOUTS_MENU_CONVO_VIDEOCALL, "id", HANGOUTS_RES_PKG_NAME);
                debug(String.format("Found convo menu item resource IDs: 0x%x, 0x%x",
                        menuItemCallResId, menuItemVideoCallResId));
            }
            Menu menu = (Menu) param.args[0];
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if(item.getItemId() == menuItemCallResId || item.getItemId() == menuItemVideoCallResId) {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }
    };

}
