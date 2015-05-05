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

package com.versobit.kmark.xhangouts;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageInfo;

import com.versobit.kmark.xhangouts.mods.MmsApnSplicing;
import com.versobit.kmark.xhangouts.mods.MmsResizing;
import com.versobit.kmark.xhangouts.mods.UiAttachAnytime;
import com.versobit.kmark.xhangouts.mods.UiCallButtons;
import com.versobit.kmark.xhangouts.mods.UiColorize;
import com.versobit.kmark.xhangouts.mods.UiEnterKey;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class XHangouts implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final String TAG = XHangouts.class.getSimpleName();

    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";
    private static final String ACTIVITY_THREAD_CURRENTACTHREAD = "currentActivityThread";
    private static final String ACTIVITY_THREAD_GETSYSCTX = "getSystemContext";

    private static final Class ACTIVITY_THREAD = findClass(ACTIVITY_THREAD_CLASS, null);

    public static final String HANGOUTS_PKG_NAME = "com.google.android.talk";
    public static final String HANGOUTS_RES_PKG_NAME = "com.google.android.apps.hangouts";

    private static final String TESTED_VERSION_STR = "3.0.87531466";
    private static final int TESTED_VERSION_INT = 22260166;

    private final Config config = new Config();

    private final Module[] modules = new Module[] {
            new MmsResizing(config),
            new MmsApnSplicing(config),
            new UiEnterKey(config),
            new UiAttachAnytime(config),
            new UiCallButtons(config),
            new UiColorize(config)
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if(lpp.packageName.equals(BuildConfig.APPLICATION_ID)) {
            findAndHookMethod(XApp.class, "isActive", XC_MethodReplacement.returnConstant(true));
        }

        if(!lpp.packageName.equals(HANGOUTS_PKG_NAME)) {
            return;
        }

        Object activityThread = callStaticMethod(ACTIVITY_THREAD, ACTIVITY_THREAD_CURRENTACTHREAD);
        Context systemCtx = (Context)callMethod(activityThread, ACTIVITY_THREAD_GETSYSCTX);

        config.reload(systemCtx);
        if(!config.modEnabled) {
            return;
        }

        debug("--- LOADING XHANGOUTS ---", false);
        debug(String.format("XHangouts v%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), false);

        final PackageInfo pi = systemCtx.getPackageManager().getPackageInfo(HANGOUTS_PKG_NAME, 0);
        debug(String.format("Google Hangouts v%s (%d)", pi.versionName, pi.versionCode), false);
        // TODO: replace this with something more robust?
        if(pi.versionCode != TESTED_VERSION_INT) {
            log(String.format("Warning: Your Hangouts version differs from the version XHangouts was built against: v%s (%d)", TESTED_VERSION_STR, TESTED_VERSION_INT));
        }

        // Call hook method on all modules
        for(Module mod : modules) {
            mod.hook(lpp.classLoader);
        }

        debug("--- XHANGOUTS LOAD COMPLETE ---", false);
    }


    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam pkgRes) throws Throwable {
        if(!HANGOUTS_PKG_NAME.equals(pkgRes.packageName)) {
            return;
        }

        // Hit or miss depending on a number of factors I have yet to enumerate
        Context ctx = AndroidAppHelper.currentApplication();
        if(ctx == null) {
            Object activityThread = callStaticMethod(ACTIVITY_THREAD, ACTIVITY_THREAD_CURRENTACTHREAD);
            if(activityThread != null) {
                ctx = (Context)callMethod(activityThread, ACTIVITY_THREAD_GETSYSCTX);
            }
        }
        if(ctx != null) {
            // The XHangouts SettingsProvider will accept configuration requests from any app or context.
            // Not being able to reload the config isn't the end of the world since if we're in the
            // Hangouts app, settings have already been loaded in handleLoadPackage
            config.reload(ctx);
        }

        if(!config.modEnabled) {
            return;
        }

        // Call resources method on all modules
        for(Module mod : modules) {
            mod.resources(pkgRes.res);
        }

    }

    private void debug(String msg) {
        debug(msg, true);
    }

    private void debug(String msg, boolean useTag) {
        if(config.debug) {
            log(msg, useTag);
        }
    }

    private void debug(Throwable throwable) {
        if(config.debug) {
            log(throwable);
        }
    }

    private static void log(String msg) {
        log(msg, true);
    }

    static void log(String msg, boolean useTag) {
        XposedBridge.log((useTag ? TAG + ": " : "") + msg);
    }

    private static void log(Throwable throwable) {
        XposedBridge.log(throwable);
    }
}
