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
import com.versobit.kmark.xhangouts.mods.ImageCompression;
import com.versobit.kmark.xhangouts.mods.ImageResizing;
import com.versobit.kmark.xhangouts.mods.Sound;
import com.versobit.kmark.xhangouts.mods.UiCallButtons;
import com.versobit.kmark.xhangouts.mods.UiColorize;
import com.versobit.kmark.xhangouts.mods.UiDisableProximity;
import com.versobit.kmark.xhangouts.mods.UiEnterKey;
import com.versobit.kmark.xhangouts.mods.UiQuickSettings;
import com.versobit.kmark.xhangouts.mods.UiSendLock;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.XposedHelpers.InvocationTargetError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class XHangouts implements IXposedHookZygoteInit,
        IXposedHookLoadPackage,  IXposedHookInitPackageResources {

    private static final String TAG = XHangouts.class.getSimpleName();

    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";
    private static final String ACTIVITY_THREAD_CURRENTACTHREAD = "currentActivityThread";
    private static final String ACTIVITY_THREAD_GETSYSCTX = "getSystemContext";

    private static final Class ACTIVITY_THREAD = findClass(ACTIVITY_THREAD_CLASS, null);

    public static final String HANGOUTS_PKG_NAME = "com.google.android.talk";
    public static final String HANGOUTS_RES_PKG_NAME = "com.google.android.apps.hangouts";

    private static final String TESTED_VERSION_STR = "6.0.107278502";
    private static final int TESTED_VERSION_INT = 22691338;
    private static final int VERSION_TOLERANCE = 30;

    private final Config config = new Config();

    private final Module[] modules = new Module[] {
            new ImageResizing(config),
            new ImageCompression(config),
            new MmsResizing(config),
            new MmsApnSplicing(config),
            new UiEnterKey(config),
            new UiCallButtons(config),
            new UiColorize(config),
            new UiQuickSettings(config),
            new UiSendLock(config),
            new UiDisableProximity(config),
            new Sound(config),
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        for(Module mod : modules) {
            mod.init(startupParam);
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if(BuildConfig.APPLICATION_ID.equals(lpp.packageName)) {
            // Passing in just XApp.class does not work :(
            findAndHookMethod(XApp.class.getName(), lpp.classLoader, "isActive",
                    XC_MethodReplacement.returnConstant(true));
            return;
        }

        if(!HANGOUTS_PKG_NAME.equals(lpp.packageName)) {
            return;
        }

        Object activityThread = callStaticMethod(ACTIVITY_THREAD, ACTIVITY_THREAD_CURRENTACTHREAD);
        Context systemCtx = (Context)callMethod(activityThread, ACTIVITY_THREAD_GETSYSCTX);

        config.reload(systemCtx);

        debug("--- LOADING XHANGOUTS ---", false);
        debug(String.format("XHangouts v%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), false);

        final PackageInfo pi = systemCtx.getPackageManager().getPackageInfo(HANGOUTS_PKG_NAME, 0);
        debug(String.format("Google Hangouts v%s (%d)", pi.versionName, pi.versionCode), false);

        // Do not warn unless Hangouts version is > +/- the VERSION_TOLERANCE of the supported version
        if(pi.versionCode > TESTED_VERSION_INT + VERSION_TOLERANCE ||
                pi.versionCode < TESTED_VERSION_INT - VERSION_TOLERANCE) {
            log(String.format("Warning: Your Hangouts version significantly differs from the version XHangouts was built against: v%s (%d)",
                    TESTED_VERSION_STR, TESTED_VERSION_INT), false);
        }

        // Avoid running modulesList unless required
        if(config.debug) {
            log(String.format("Modules: %s", modulesList()), false);
        }

        // Call hook method on all modules
        for(Module mod : modules) {
            // Attempt to hook other modules even if one of them fails with an Xposed error
            try {
                mod.hook(lpp.classLoader);
            } catch (ClassNotFoundError | InvocationTargetError | NoSuchMethodError ex) {
                log(String.format("Error: %s in %s...", ex.getClass().getSimpleName(),
                        mod.getClass().getSimpleName()));
                debug(ex);
            }
        }

        debug("--- XHANGOUTS LOAD COMPLETE ---", false);
    }


    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam pkgRes)
            throws Throwable {
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

    // Based on Arrays.toString(Object[])
    private String modulesList() {
        if(modules.length == 0) {
            return "[]";
        }
        // 12 = avg num of chars in class name of modules
        StringBuilder sb = new StringBuilder(modules.length * 12);
        sb.append('[');
        sb.append(modules[0].getClass().getSimpleName());
        for(int i = 1; i < modules.length; i++) {
            sb.append(", ");
            sb.append(modules[i].getClass().getSimpleName());
        }
        sb.append(']');
        return sb.toString();
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
