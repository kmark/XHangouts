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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.content.res.XResources;

import com.versobit.kmark.xhangouts.BuildConfig;
import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.SettingsActivity;

import java.lang.reflect.Array;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class UiQuickSettings extends Module {

    private static final String HANGOUTS_NAV_MENUITEM_BASE = "ccm";
    private static final String HANGOUTS_NAV_MENUITEM_HELP = "bwo";

    private static final String HANGOUTS_MENU_POPULATOR = "hlp";

    private static final int HANGOUTS_RES_MENU_TITLE = XResources.getFakeResId(BuildConfig.APPLICATION_ID + ":string/hangouts_menu_title");
    private static final int HANGOUTS_RES_MENU_ICON = XResources.getFakeResId(BuildConfig.APPLICATION_ID + ":drawable/ic_hangouts_menu");
    private static final String ACTUAL_TITLE = "XHangouts v" +
            BuildConfig.VERSION_NAME.substring(0, BuildConfig.VERSION_NAME.lastIndexOf('-'));
    private String modulePath = null;

    private Class cMenuItemBase = null;
    private Class cMenuItemBaseArray = null;

    public UiQuickSettings(Config config) {
        super(UiQuickSettings.class.getSimpleName(), config);
    }

    @Override
    public void init(IXposedHookZygoteInit.StartupParam startup) {
        modulePath = startup.modulePath;
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        cMenuItemBase = findClass(HANGOUTS_NAV_MENUITEM_BASE, loader);
        cMenuItemBaseArray = Array.newInstance(cMenuItemBase, 0).getClass();
        Class cMenuItemHelp = findClass(HANGOUTS_NAV_MENUITEM_HELP, loader);
        Class cMenuPop = findClass(HANGOUTS_MENU_POPULATOR, loader);

        return new IXUnhook[] {
                // Field corrections
                findAndHookMethod(cMenuItemBase, "a", XC_MethodReplacement.returnConstant(HANGOUTS_RES_MENU_TITLE)),
                findAndHookMethod(cMenuItemBase, "a", Activity.class, onMenuItemClick),
                findAndHookMethod(cMenuItemBase, "b", XC_MethodReplacement.returnConstant(HANGOUTS_RES_MENU_ICON)),
                findAndHookMethod(cMenuItemBase, "c", XC_MethodReplacement.returnConstant(8)),
                findAndHookMethod(cMenuItemBase, "d", XC_MethodReplacement.returnConstant(2)),
                findAndHookMethod(cMenuItemBase, "e", XC_MethodReplacement.returnConstant(8)),

                // Push the Help & feedback entry down
                findAndHookMethod(cMenuItemHelp, "c", XC_MethodReplacement.returnConstant(9)),
                findAndHookMethod(cMenuItemHelp, "e", XC_MethodReplacement.returnConstant(9)),

                // Populate dat menu
                findAndHookMethod(cMenuPop, "a", Class.class, Object[].class, populateMenu)
        };
    }

    private static final XC_MethodReplacement onMenuItemClick = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setComponent(new ComponentName(BuildConfig.APPLICATION_ID,
                    SettingsActivity.class.getName()));
            ((Activity)param.args[0]).startActivity(i);
            return null;
        }
    };

    private final XC_MethodHook populateMenu = new XC_MethodHook() {
        // This method is called by the onAttachBinder method of the NavigationDrawerFragment
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            // If it's not an array of that class we're not interested
            if(!cMenuItemBaseArray.isInstance(param.args[1])) {
                return;
            }

            // This is a var-arg method
            Object[] array = (Object[])param.args[1];

            // Filter out a call we do not want to process
            if (array.length < 4) {
                return;
            }

            // Create a new array to hold the original and our new entry
            Object[] newArray = new Object[array.length + 1];
            System.arraycopy(array, 0, newArray, 0, array.length);
            // Create the base class which is now usable for our purposes
            newArray[newArray.length - 1] = cMenuItemBase.newInstance();
            // Hand it over to the actual method
            param.args[1] = newArray;
        }
    };

    @Override
    public void resources(XResources res) {
        // Get the resources for this module
        XModuleResources xModRes = XModuleResources.createInstance(modulePath, res);

        // Add a new "fake" resource and instantly replace it with the string we actually want
        res.setReplacement(res.addResource(xModRes, R.string.hangouts_menu_title), ACTUAL_TITLE);

        // Add the desired menu icon to the Google Hangouts resources for use like above
        res.addResource(xModRes, R.drawable.ic_hangouts_menu);
    }
}
