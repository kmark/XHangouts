/*
 * Copyright (C) 2014-2020 Kevin Mark
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_RES_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public final class UiCallButtons {

    private static final String HANGOUTS_ACT_CONVERSATION_SUPER = "dvr";
    private static final String HANGOUTS_ACT_CONVERSATION_SUPER_OPOM = "onPrepareOptionsMenu";

    private static final String HANGOUTS_ENUM_CALL = "chy";
    private static final String HANGOUTS_MENU_CALL = "csm";
    private static final String HANGOUTS_MENU_CALL_CONTEXT = "b";
    private static final String HANGOUTS_MENU_CALL_OPIS = "onOptionsItemSelected";

    private static final String HANGOUTS_MENU_CONVO_CALL = "realtimechat_conversation_call_menu_item";
    private static final String HANGOUTS_MENU_CONVO_VIDEOCALL = "start_hangout_menu_item";

    private static final String HANGOUTS_STR_CALL_WITH_HANGOUTS = "call_over_data_with_hangouts_text";
    private static final String HANGOUTS_STR_CALL_WITH_PSTN = "call_over_pstn_network_text";

    private static final String CALLING_HANGOUTS_CONTACT = "Calling Hangouts contact(s)";

    // Impossible default ID since resource IDs start with 0x7f
    private static final int RES_ID_UNSET = 0;

    private static final int MAX_NUM_LEN = 15;

    // Resources.getIdentifier is expensive so we're caching results
    private static int menuItemCallResId = RES_ID_UNSET;
    private static int menuItemVideoCallResId = RES_ID_UNSET;

    @SuppressLint("StaticFieldLeak")
    private static Context context = null;
    private static Class cHandleCalls = null;
    private static String gaiaID = null;
    private static String phoneNumber = null;

    public static void handleLoadPackage(final Config config, final ClassLoader loader) {
        if (!config.modEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        try {
            Class cConversationActSuper = findClass(HANGOUTS_ACT_CONVERSATION_SUPER, loader);
            Class cMenuOptions = findClass(HANGOUTS_MENU_CALL, loader);
            cHandleCalls = findClass(HANGOUTS_ENUM_CALL, loader);

            findAndHookMethod(cConversationActSuper, HANGOUTS_ACT_CONVERSATION_SUPER_OPOM, Menu.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!config.hideCallButtons) {
                        return;
                    }
                    if (menuItemCallResId == RES_ID_UNSET || menuItemVideoCallResId == RES_ID_UNSET) {
                        Resources res = AndroidAppHelper.currentApplication().getResources();
                        menuItemCallResId = res.getIdentifier(HANGOUTS_MENU_CONVO_CALL, "id", HANGOUTS_RES_PKG_NAME);
                        menuItemVideoCallResId = res.getIdentifier(HANGOUTS_MENU_CONVO_VIDEOCALL, "id", HANGOUTS_RES_PKG_NAME);
                    }
                    Menu menu = (Menu) param.args[0];
                    for (int i = 0; i < menu.size(); i++) {
                        MenuItem item = menu.getItem(i);
                        if (item.getItemId() == menuItemCallResId || item.getItemId() == menuItemVideoCallResId) {
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                        }
                    }
                }
            });

            findAndHookMethod(cMenuOptions, HANGOUTS_MENU_CALL_OPIS, MenuItem.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!config.enhanceCallButton) {
                        return;
                    }
                    if (menuItemCallResId == RES_ID_UNSET) {
                        Resources res = AndroidAppHelper.currentApplication().getResources();
                        menuItemCallResId = res.getIdentifier(HANGOUTS_MENU_CONVO_CALL, "id", HANGOUTS_RES_PKG_NAME);
                    }
                    if (((MenuItem) param.args[0]).getItemId() == menuItemCallResId) {
                        // We store some info so that it isn't lost when switching between message types (SMS, GV, Hangouts)
                        if (gaiaID == null) {
                            gaiaID = getID(param);
                            getNumber(param);
                        } else {
                            String ID = getID(param);
                            if (!gaiaID.equals(ID)) {
                                gaiaID = ID;
                                getNumber(param);
                            }
                        }

                        // Try another way to get the phone number if it's still null
                        if (phoneNumber == null) {
                            // bz = fae class (HANGOUTS_MENU_CALL class for ref)
                            Object altPhoneNumber = getObjectField(param.thisObject, "bz");
                            if (altPhoneNumber != null) {
                                phoneNumber = (String) callMethod(altPhoneNumber, "c"); // method within fae
                            }
                        }

                        // Check if we're calling a Hangouts contact
                        if (phoneNumber != null) {
                            if (!isHangoutsContact()) {
                                if (gaiaID.contains("g:")) {
                                    selectCallType(param);
                                } else {
                                    cellularCall();
                                }
                                param.setResult(true);
                            } else {
                                XHangouts.debug(CALLING_HANGOUTS_CONTACT);
                            }
                        } else {
                            XHangouts.debug(CALLING_HANGOUTS_CONTACT);
                        }
                    }
                }
            });

            // Get the context
            findAndHookMethod(cMenuOptions, HANGOUTS_MENU_CALL_CONTEXT, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    context = (Context) param.args[0];
                }
            });

        } catch (Throwable t) {
            XHangouts.debug("Found Xposed bug or Hangouts mismatch");
        }
    }

    private static void getNumber(XC_MethodHook.MethodHookParam param) {
        // k = cvs class (HANGOUTS_MENU_CALL class for ref)
        Object getPhoneNumber = getObjectField(param.thisObject, "k");
        phoneNumber = (String) callMethod(getPhoneNumber, "c"); // method within cvs
    }

    private static String getID(XC_MethodHook.MethodHookParam param) {
        return (String) getObjectField(param.thisObject, "aO"); // string in HANGOUTS_MENU_CALL class
    }

    private static boolean isHangoutsContact() {
        return phoneNumber.length() > MAX_NUM_LEN || !(gaiaID == null || gaiaID.isEmpty())
                && gaiaID.contains(phoneNumber) && gaiaID.contains("g:");
    }

    private static void callNumber(String callIntent, String number) {
        Intent intent = new Intent(callIntent, Uri.parse("tel:" + number));
        try {
            context.startActivity(intent);
        } catch (SecurityException se) {
            XHangouts.log(se);
        }
    }

    private static void selectCallType(final XC_MethodHook.MethodHookParam param) {
        Resources res = AndroidAppHelper.currentApplication().getResources();
        String callWithHangouts = res.getString(
                res.getIdentifier(HANGOUTS_STR_CALL_WITH_HANGOUTS, "string", HANGOUTS_RES_PKG_NAME));
        String callWithCellular = res.getString(
                res.getIdentifier(HANGOUTS_STR_CALL_WITH_PSTN, "string", HANGOUTS_RES_PKG_NAME));

        CharSequence options[] = new CharSequence[]{callWithHangouts, callWithCellular};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        hangoutsCall(param);
                        break;
                    case 1:
                        cellularCall();
                        break;
                }
            }
        });
        builder.show();
    }

    private static void hangoutsCall(XC_MethodHook.MethodHookParam param) {
        XHangouts.debug(CALLING_HANGOUTS_CONTACT);
        callMethod(param.thisObject, "a", Enum.valueOf(cHandleCalls, "AUDIO_CALL"), 60, 2724);
    }

    private static void cellularCall() {
        String number = phoneNumber.replaceAll("[\\s\\-()]", "");
        if (PhoneNumberUtils.isGlobalPhoneNumber(number)) {
            XHangouts.debug("Calling: " + number);
            callNumber(Intent.ACTION_CALL, number);
        } else {
            number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
            XHangouts.debug("Opening dialer with digits: " + number);
            callNumber(Intent.ACTION_DIAL, number);
        }
    }

}
