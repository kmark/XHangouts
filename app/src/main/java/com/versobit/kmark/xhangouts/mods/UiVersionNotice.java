/*
 * Copyright (C) 2016, 2018 Kevin Mark
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Bundle;

import com.versobit.kmark.xhangouts.BuildConfig;
import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

import static com.versobit.kmark.xhangouts.XHangouts.TESTED_VERSION;
import static com.versobit.kmark.xhangouts.XHangouts.hangoutsVerCode;
import static com.versobit.kmark.xhangouts.XHangouts.hangoutsVerName;
import static com.versobit.kmark.xhangouts.XHangouts.unsupportedVersion;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public final class UiVersionNotice {

    // The primary Hangouts activity. Unlikely this will change too much.
    // We could try to retrieve the launch activity at runtime (I did try) but Android gives us an
    // activity-alias name and I don't think there's a way for us to resolve those.
    private static final String HANGOUTS_BABELHOMEACTIVITY = "com.google.android.apps.hangouts.phone.BabelHomeActivity";
    private static final String HANGOUTS_BABEL_APPUPGRADE_FORCE = "bgd";

    private static boolean hasRun = false;
    private static int dialogTitleId = 0;
    private static int dialogMsgId = 0;
    private static int dialogUpgradeId = 0;
    private static int dialogDowngradeId = 0;
    private static int dialogButtonId = 0;

    public static void handleLoadPackage(Config config, ClassLoader loader) {
        if (!config.modEnabled) {
            return;
        }

        // Prevent forced upgrades
        findAndHookMethod(HANGOUTS_BABEL_APPUPGRADE_FORCE, loader, "a", Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return false;
            }
        });

        try {
            findAndHookMethod(HANGOUTS_BABELHOMEACTIVITY, loader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (unsupportedVersion && !hasRun) {
                        final Activity act = (Activity) param.thisObject;
                        String direction = act.getString(hangoutsVerCode > TESTED_VERSION.getMax() ? dialogDowngradeId : dialogUpgradeId);
                        new AlertDialog.Builder(act)
                                .setTitle(dialogTitleId)
                                .setMessage(act.getString(dialogMsgId, hangoutsVerName, BuildConfig.VERSION_NAME, direction, TESTED_VERSION.getVersion()))
                                .setPositiveButton(dialogButtonId, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                    hasRun = true;
                }
            });
        } catch (Throwable t) {
            // The mismatch has already been logged
        }
    }

    public static void handleInitPackageResources(Config config, XResources res) {
        if (!config.modEnabled) {
            return;
        }

        XModuleResources moduleRes = XModuleResources.createInstance(XHangouts.modulePath, res);
        dialogTitleId = res.addResource(moduleRes, R.string.hangouts_vdialog_title);
        dialogMsgId = res.addResource(moduleRes, R.string.hangouts_vdialog_msg);
        dialogUpgradeId = res.addResource(moduleRes, R.string.hangouts_vdialog_upgrade);
        dialogDowngradeId = res.addResource(moduleRes, R.string.hangouts_vdialog_downgrade);
        dialogButtonId = res.addResource(moduleRes, R.string.hangouts_vdialog_button);
    }

}
