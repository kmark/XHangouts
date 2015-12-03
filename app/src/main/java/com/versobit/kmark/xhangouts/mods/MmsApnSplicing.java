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
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.Setting;

import java.lang.reflect.Constructor;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers.InvocationTargetError;
import de.robv.android.xposed.callbacks.IXUnhook;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public final class MmsApnSplicing extends Module {

    private static final String HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS1 = "bfr";
    private static final String HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS2 = "buu";
    private static final String HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS2_SENDMMSREQUEST = "a";
    private static final String HANGOUTS_BABEL_NETWORKQUEUE_INTERFACE = "bfs";

    private static final String HANGOUTS_MMSTRANSACTIONS = "cnc";
    private static final String HANGOUTS_MMSTRANSACTIONS_SENDSENDREQ1 = "a";
    private static final String HANGOUTS_MMSTRANSACTIONS_SENDSENDREQ2 = "a";

    private static final String HANGOUTS_TRANSACTIONSETTINGS = "dvc";
    private static final String HANGOUTS_TRANSACTIONSETTINGS_APNLISTFIELD = "b";

    private static final String HANGOUTS_MMS_MESSAGECLASS1 = "vo";
    private static final String HANGOUTS_MMS_MESSAGECLASS2 = "wi";

    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER = "dtv";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST1 = "a";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST2 = "a";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_AQUIREMMSNETWORK = "a";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_TIMERFIELD = "c";

    private static final String HANGOUTS_MMSSENDER = "adh";
    private static final String HANGOUTS_MMSSENDER_DOSEND = "a";

    private static final String HANGOUTS_MMS_APN = "dvd";
    private static final String HANGOUTS_MMS_APN_RAWMMSCFIELD = "c";
    private static final String HANGOUTS_MMS_APN_MMSCFIELD = "b";
    private static final String HANGOUTS_MMS_APN_PROXYFIELD = "d";
    private static final String HANGOUTS_MMS_APN_PORTFIELD = "f";
    private static final String HANGOUTS_MMS_APN_ISPROXYSET = "b";

    private static final String HANGOUTS_MMS_EXCEPTION = "dub";

    private static final String HANGOUTS_MMSC_RESPONSE = "acq";
    private static final String HANGOUTS_MMSC_RESPONSE_GET_MESSAGECLASS1 = "a";

    private Class cMmsSendReceiveManager;
    private Class cTransactionSettings;
    private Class cMmsSender;
    private Class cMmsApn;
    private Class cMmscResponse;
    private Class cMmsException;

    public MmsApnSplicing(Config config) {
        super(MmsApnSplicing.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        cMmsSendReceiveManager = findClass(HANGOUTS_MMSSENDRECEIVEMANAGER, loader);
        cTransactionSettings = findClass(HANGOUTS_TRANSACTIONSETTINGS, loader);
        cMmsSender = findClass(HANGOUTS_MMSSENDER, loader);
        cMmsApn = findClass(HANGOUTS_MMS_APN, loader);
        cMmscResponse = findClass(HANGOUTS_MMSC_RESPONSE, loader);
        cMmsException = findClass(HANGOUTS_MMS_EXCEPTION, loader);

        return new IXUnhook[] {
                findAndHookMethod(cMmsSendReceiveManager, HANGOUTS_MMSSENDRECEIVEMANAGER_AQUIREMMSNETWORK,
                        Context.class, aquireMmsNetwork),
                findAndHookMethod(cMmsSendReceiveManager, HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST2,
                        Context.class, cTransactionSettings, String.class, int.class, byte[].class, executeMmsRequest)
        };
    }

    private final XC_MethodHook aquireMmsNetwork = new XC_MethodHook() {
        // This prevents the initial request for MMS APN connectivity. It populates a new
        // TransactionSettings instance with APN data. This is normally done by the broadcast
        // receiver as it listens for connectivity state changes. Instead of waiting this method
        // returns with a valid result almost instantly forcing the MMS process to continue.
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            config.reload((Context)param.args[0]);
            if(!config.modEnabled || !config.apnSplicing) {
                return;
            }
            if(config.apnPreset == Setting.ApnPreset.CUSTOM && config.mmsc.isEmpty()) {
                log("APN splicing enabled but no MMSC has been specified.");
                return;
            }

            debug(String.format("MMS APN splicing configuration: %s, %s, %s, %d",
                    config.apnPreset.toString(), config.mmsc, config.proxyHost, config.proxyPort));

            String localMmsc = config.mmsc;
            String localProxyHost = config.proxyHost;
            int localProxyPort = config.proxyPort;
            if(config.apnPreset != Setting.ApnPreset.CUSTOM) {
                localMmsc = config.apnPreset.getMmsc();
                localProxyHost = config.apnPreset.getProxyHost();
                localProxyPort = config.apnPreset.getProxyPort();
            }
            if(localProxyHost.isEmpty()) {
                localProxyHost = null;
                localProxyPort = -1;
            } else {
                localProxyPort = localProxyPort == -1 ? 80 : localProxyPort;
            }

            Object timerField = getStaticObjectField(cMmsSendReceiveManager,
                    HANGOUTS_MMSSENDRECEIVEMANAGER_TIMERFIELD);
            // This /should/ synchronize with the actual static field not our local representation of it
            synchronized (timerField) {

                // Do not splice if not connected to mobile
                if(!isMobileConnected()) {
                    debug("Not on a mobile connection. Not splicing.");
                    return;
                }

                debug("GOING FOR IT!");

                // Create APN
                Constructor newMmsApn = findConstructorExact(cMmsApn, String.class, String.class, int.class);
                // MMSC, Proxy, Port
                Object instanceMmsApn = newMmsApn.newInstance(localMmsc, localProxyHost, localProxyPort);
                setObjectField(instanceMmsApn, HANGOUTS_MMS_APN_RAWMMSCFIELD, localMmsc);

                // Creates a TransactionSettings object (this is normally done by the broadcast receiver)
                Constructor newTransactionSettings = findConstructorExact(cTransactionSettings);
                newTransactionSettings.setAccessible(true);
                Object instanceTransactionSettings = newTransactionSettings.newInstance();

                // Add APN to the list
                List apnList = (List)getObjectField(instanceTransactionSettings,
                        HANGOUTS_TRANSACTIONSETTINGS_APNLISTFIELD);
                apnList.clear();
                // You bet your ass this is an unchecked call, Android Studio
                apnList.add(instanceMmsApn);

                // Return the needed TransactionSettings object
                param.setResult(instanceTransactionSettings);
            }
        }
    };

    private final XC_MethodHook executeMmsRequest = new XC_MethodHook() {
        // This hook replaces a call to the executeMmsRequest method of the MmsSendReceiveManager.
        // It calls the method that actually sends the MMS HTTP request. For some reason our little
        // connectivity shortcut causes this to fail. Instead of spending even more time trying to
        // figure out why that is, I've just replaced the entire method with something that's far
        // less picky about what you feed it. Returning null is sometimes a valid result. Admittedly
        // this replacement may not be nearly as robust as the original implementation.
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            config.reload((Context)param.args[0]);
            if(!config.modEnabled || !config.apnSplicing) {
                return;
            }

            debug(String.format("%s -> %s2 called", HANGOUTS_MMSSENDRECEIVEMANAGER, HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST2));

            if(!isMobileConnected()) {
                return;
            }

            for(Object o : param.args) {
                debug(String.format("%s -> %s2 args: %s", HANGOUTS_MMSSENDRECEIVEMANAGER, HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST2, o));
            }

            Object mmsc = param.args[2];
            Object isProxy = false;
            Object proxy = null;
            Object port = -1;
            for(Object o : param.args) {
                debug(String.format("args1: %s", o));
            }

            // When sending String will be null, int will be 1
            // When receiving String will be the message URL, int will be 2, byte[] will be null
            // When sending the delivery report String will be null, int will be 1

            if(param.args[2] == null) {
                // We do not have a custom URL so we need to pull one from the APN list

                // Get the first MMS APN in the list
                List apnList = (List)getObjectField(param.args[1], HANGOUTS_TRANSACTIONSETTINGS_APNLISTFIELD);
                for(Object o : apnList) {
                    debug("APN: " + o.toString());
                }
                Object apn = apnList.get(0);
                mmsc = getObjectField(apn, HANGOUTS_MMS_APN_MMSCFIELD);
                isProxy = callMethod(apn, HANGOUTS_MMS_APN_ISPROXYSET);
                proxy = getObjectField(apn, HANGOUTS_MMS_APN_PROXYFIELD);
                port = getObjectField(apn, HANGOUTS_MMS_APN_PORTFIELD);
            }
            debug(String.format("Executing MMS HTTP request. %s, %s, %s, %s, %s, %s, %s, %s",
                    param.args[0], mmsc, param.args[4], param.args[3],
                    isProxy, proxy, port, false));

            byte[] result;
            try {
                result = (byte[])callStaticMethod(cMmsSender, HANGOUTS_MMSSENDER_DOSEND,
                        param.args[0],
                        mmsc,
                        param.args[4],
                        param.args[3],
                        isProxy,
                        proxy,
                        port,
                        false);
            } catch (InvocationTargetError ex) {
                log("MMS HTTP request failed!");
                debug(ex.getCause());
                Constructor mmsException = findConstructorExact(cMmsException, String.class);
                param.setThrowable((Throwable)mmsException.newInstance("MMS HTTP request failed: " + ex.getCause()));
                return;
            }
            // XposedHelpers.callMethod(methodHookParam.args[1], "a", apn);


            if(result == null) {
                debug("RESULT NULL, RETURNING NULL");
                param.setResult(null);
                return;
            }
            debug("RESULT");
            // debug(new String(result, "UTF-8"));
            if(result.length > 0) {
                try {
                    Object instanceMmscResponse = findConstructorExact(cMmscResponse, byte[].class).newInstance(result);
                    debug("LOCAL RT IS GOOD");
                    param.setResult(callMethod(instanceMmscResponse, HANGOUTS_MMSC_RESPONSE_GET_MESSAGECLASS1));
                    return;
                } catch (RuntimeException ex) {
                    debug("LOCALRT? RUNTIME EXCEPTION");
                    param.setResult(null);
                    return;
                }
            }
            debug("ZERO LENGTH, RETURNING NULL");
            param.setResult(null);
        }
    };

    private static boolean isMobileConnected() {
        ConnectivityManager cm = (ConnectivityManager)AndroidAppHelper.currentApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE && ni.isConnected();
    }

}
