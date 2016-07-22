/*
 * Copyright (C) 2016 Kevin Mark
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
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.XHangouts;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public final class UiScreenOff {

    private static final String HANGOUTS_CONVERSATIONACTIVITY = "com.google.android.apps.hangouts.phone.ConversationActivity";
    private static boolean mIsReceiving;
    private static Activity mActivity;

    static BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> task = manager.getRunningTasks(1);
            ComponentName componentInfo = task.get(0).topActivity;
            if (componentInfo.getPackageName().equals(mActivity.getPackageName())) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(startMain);
                    XHangouts.debug("Screen: turned off");
                }
            }
        }
    };

    public static void handleLoadPackage(Config config, ClassLoader loader) {
        if (!config.modEnabled || !config.screenOff) {
            return;
        }

        findAndHookMethod(HANGOUTS_CONVERSATIONACTIVITY, loader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = (Activity) param.thisObject;
                if (!mIsReceiving) {
                    IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                    mActivity.registerReceiver(mScreenStateReceiver, intentFilter);
                    mIsReceiving = true;
                    XHangouts.debug("Screen: receiver registered");
                }
            }
        });

        findAndHookMethod(HANGOUTS_CONVERSATIONACTIVITY, loader, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mIsReceiving && mActivity != null) {
                    mActivity.unregisterReceiver(mScreenStateReceiver);
                    mIsReceiving = false;
                    XHangouts.debug("Screen: unregistered receiver");
                }
            }
        });

    }
}
