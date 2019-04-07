/*
 * Copyright (C) 2017-2019 Kevin Mark
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

import com.versobit.kmark.xhangouts.Config;

import de.robv.android.xposed.XC_MethodReplacement;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class RestoreSMS {

    // This might stop working in Hangouts v21+
    private static final String HANGOUTS_DEP_SMS_LIST = "gyg";
    private static final String HANGOUTS_DEP_SMS_CONV = "gxk";
    private static final String HANGOUTS_DEP_SMS_NOTI = "gxq";

    private static final String HANGOUTS_A = "a";
    private static final String HANGOUTS_B = "b";
    private static final String HANGOUTS_C = "c";
    private static final String HANGOUTS_D = "d";
    private static final String HANGOUTS_E = "e";
    private static final String HANGOUTS_F = "f";

    public static void handleLoadPackage(final Config config, ClassLoader loader) {
        if (!config.modEnabled) {
            return;
        }

        Class cDeprecatedSMSList = findClass(HANGOUTS_DEP_SMS_LIST, loader);
        Class cDeprecatedSMS = findClass(HANGOUTS_DEP_SMS_CONV, loader);
        Class cDeprecatedSMSNotify = findClass(HANGOUTS_DEP_SMS_NOTI, loader);

        findAndHookMethod(cDeprecatedSMSList, HANGOUTS_A, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //shouldShowPromo
                return false;
            }
        });
        findAndHookMethod(cDeprecatedSMSList, HANGOUTS_D, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //babel_sms_dep_banner_persistent
                return false;
            }
        });

        findAndHookMethod(cDeprecatedSMS, HANGOUTS_A, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //handleSmsIntent
                return null;
            }
        });
        findAndHookMethod(cDeprecatedSMS, HANGOUTS_C, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //babel_sms_dep_snackbar_enabled
                return false;
            }
        });
        findAndHookMethod(cDeprecatedSMS, HANGOUTS_D, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //babel_sms_force_dep_enabled
                return false;
            }
        });
        findAndHookMethod(cDeprecatedSMS, HANGOUTS_E, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //similar to above
                return false;
            }
        });
        findAndHookMethod(cDeprecatedSMS, HANGOUTS_F, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //babel_sms_dep_enabled
                return false;
            }
        });

        findAndHookMethod(cDeprecatedSMSNotify, HANGOUTS_A, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //babel_sms_dep_notif_19_enabled
                return false;
            }
        });
        findAndHookMethod(cDeprecatedSMSNotify, HANGOUTS_B, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //babel_sms_dep_msg_notif_enabled
                return false;
            }
        });
        findAndHookMethod(cDeprecatedSMSNotify, HANGOUTS_C, Context.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                //Babel_SmsDepNotif
                return false;
            }
        });

    }
}
