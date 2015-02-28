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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class XHangouts implements IXposedHookLoadPackage {

    private static final String TAG = "XHangouts";

    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";
    private static final String ACTIVITY_THREAD_CURRENTACTHREAD = "currentActivityThread";
    private static final String ACTIVITY_THREAD_GETSYSCTX = "getSystemContext";

    static final String HANGOUTS_PKG_NAME = "com.google.android.talk";

    // TODO: Find a better way to manage these strings
    private static final String HANGOUTS_ESAPP_CLASS = "com.google.android.apps.hangouts.phone.EsApplication";
    private static final String HANGOUTS_ESAPP_ONCREATE = "onCreate";

    private static final String HANGOUTS_PROCESS_MMS_IMG_CLASS = "cex";
    // private static a(IIIILandroid/net/Uri;)[B
    private static final String HANGOUTS_PROCESS_MMS_IMG_METHOD = "a";

    private static final String HANGOUTS_ESPROVIDER_CLASS = "com.google.android.apps.hangouts.content.EsProvider";
    // private static e(Ljava/lang/String;)Ljava/lang/String
    private static final String HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE = "e";

    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW = "com.google.android.apps.hangouts.conversation.v2.ComposeMessageView";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_EDITTEXT = "h";
    // public onEditorAction(Landroid/widget/TextView;ILandroid/view/KeyEvent;)Z
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_ONEDITORACTION = "onEditorAction";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_EMOJIBUTTON = "d";
    private static final String HANGOUTS_VIEWS_COMPOSEMSGVIEW_ADDATTACHMENT = "l";

    private static final String HANGOUTS_ACT_CONVERSATION_SUPER = "apc";
    private static final String HANGOUTS_ACT_CONVERSATION_SUPER_OPOM = "onPrepareOptionsMenu";

    private static final String HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS1 = "byp";
    private static final String HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS2 = "boj";
    private static final String HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS2_SENDMMSREQUEST = "a";
    private static final String HANGOUTS_BABEL_REQUESTWRITER_SQLHELPER = "byk";

    private static final String HANGOUTS_MMSTRANSACTIONS = "cev";
    private static final String HANGOUTS_MMSTRANSACTIONS_SENDSENDREQ1 = "a";
    private static final String HANGOUTS_MMSTRANSACTIONS_SENDSENDREQ2 = "a";
    private static final String HANGOUTS_MMSTRANSACTIONS_DEBUGFIELD = "a";

    private static final String HANGOUTS_TRANSACTIONSETTINGS = "cfq";
    private static final String HANGOUTS_TRANSACTIONSETTINGS_APNLISTFIELD = "b";

    private static final String HANGOUTS_MMS_MESSAGECLASS1 = "vm";
    private static final String HANGOUTS_MMS_MESSAGECLASS2 = "wg";

    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER = "ceq";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST1 = "a";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST2 = "a";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_AQUIREMMSNETWORK = "b";
    private static final String HANGOUTS_MMSSENDRECEIVEMANAGER_TIMERFIELD = "b";

    private static final String HANGOUTS_MMSSENDER = "wo";
    private static final String HANGOUTS_MMSSENDER_DOSEND = "a";

    private static final String HANGOUTS_MMS_APN = "cfr";
    private static final String HANGOUTS_MMS_APN_RAWMMSCFIELD = "c";
    private static final String HANGOUTS_MMS_APN_MMSCFIELD = "b";
    private static final String HANGOUTS_MMS_APN_PROXYFIELD = "d";
    private static final String HANGOUTS_MMS_APN_PORTFIELD = "f";
    private static final String HANGOUTS_MMS_APN_ISPROXYSET = "b";

    private static final String HANGOUTS_MMS_EXCEPTION = "ceu";

    private static final String HANGOUTS_MMSC_RESPONSE = "vx";
    private static final String HANGOUTS_MMSC_RESPONSE_GET_MESSAGECLASS1 = "a";

    private static final String ANDROID_UTIL_LOG_CLASS = "android.util.Log";
    private static final String ANDROID_UTIL_LOG_ISLOGGABLE = "isLoggable";

    private static final String TESTED_VERSION_STR = "2.5.83281670";
    private static final int TESTED_VERSION_INT = 22181734;

    // Not certain if I need a WeakReference here. Without it could prevent the Context from being closed?
    private WeakReference<Context> hangoutsCtx;

    private static final class Config {

        private static final Uri ALL_PREFS_URI = Uri.parse("content://" + SettingsProvider.AUTHORITY + "/all");

        // Give us some sane defaults, just in case
        private static boolean modEnabled = true;
        private static boolean resizing = true;
        private static boolean rotation = true;
        private static int rotateMode = -1;
        private static int imageWidth = 640;
        private static int imageHeight = 640;
        private static Setting.ImageFormat imageFormat = Setting.ImageFormat.JPEG;
        private static int imageQuality = 60;
        private static boolean apnSplicing = false;
        private static Setting.ApnPreset apnPreset = Setting.ApnPreset.CUSTOM;
        private static String mmsc = "";
        private static String proxyHost = "";
        private static int proxyPort = -1;
        private static int enterKey = Setting.UiEnterKey.EMOJI_SELECTOR.toInt();
        private static boolean attachAnytime = true;
        private static boolean hideCallButtons = false;
        private static boolean debug = false;

        private static void reload(Context ctx) {
            Cursor prefs = ctx.getContentResolver().query(ALL_PREFS_URI, null, null, null, null);
            if(prefs == null) {
                log("Failed to retrieve settings!");
                return;
            }
            while(prefs.moveToNext()) {
                switch (Setting.fromString(prefs.getString(SettingsProvider.QUERY_ALL_KEY))) {
                    case MOD_ENABLED:
                        modEnabled = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                        continue;
                    case MMS_RESIZE_ENABLED:
                        resizing = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                        continue;
                    case MMS_ROTATE_ENABLED:
                        rotation = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                        continue;
                    case MMS_ROTATE_MODE:
                        rotateMode = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case MMS_SCALE_WIDTH:
                        imageWidth = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case MMS_SCALE_HEIGHT:
                        imageHeight = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case MMS_IMAGE_TYPE:
                        imageFormat = Setting.ImageFormat.fromInt(prefs.getInt(SettingsProvider.QUERY_ALL_VALUE));
                        continue;
                    case MMS_IMAGE_QUALITY:
                        imageQuality = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case MMS_APN_SPLICING_ENABLED:
                        apnSplicing = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                        continue;
                    case MMS_APN_SPLICING_APN_CONFIG_PRESET:
                        apnPreset = Setting.ApnPreset.fromInt(prefs.getInt(SettingsProvider.QUERY_ALL_VALUE));
                        continue;
                    case MMS_APN_SPLICING_APN_CONFIG_MMSC:
                        mmsc = prefs.getString(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case MMS_APN_SPLICING_APN_CONFIG_PROXY_HOSTNAME:
                        proxyHost = prefs.getString(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case MMS_APN_SPLICING_APN_CONFIG_PROXY_PORT:
                        proxyPort = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case UI_ENTER_KEY:
                        enterKey = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE);
                        continue;
                    case UI_ATTACH_ANYTIME:
                        attachAnytime = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                        continue;
                    case UI_HIDE_CALL_BUTTONS:
                        hideCallButtons = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                        continue;
                    case DEBUG:
                        debug = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                }
            }
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(loadPackageParam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(XApp.class.getCanonicalName(), loadPackageParam.classLoader, "isActive", XC_MethodReplacement.returnConstant(true));
        }
        if(!loadPackageParam.packageName.equals(HANGOUTS_PKG_NAME)) {
            return;
        }

        Object activityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass(ACTIVITY_THREAD_CLASS, null), ACTIVITY_THREAD_CURRENTACTHREAD);
        final Context systemCtx = (Context)XposedHelpers.callMethod(activityThread, ACTIVITY_THREAD_GETSYSCTX);

        Config.reload(systemCtx);
        if(!Config.modEnabled) {
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

        // Hangouts class definitions
        final Class ComposeMessageView = XposedHelpers.findClass(HANGOUTS_VIEWS_COMPOSEMSGVIEW, loadPackageParam.classLoader);
        final Class ConversationActSuper = XposedHelpers.findClass(HANGOUTS_ACT_CONVERSATION_SUPER, loadPackageParam.classLoader);
        final Class rWriterInnerClass1 = XposedHelpers.findClass(HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS1, loadPackageParam.classLoader);
        final Class rWriterSqlHelper = XposedHelpers.findClass(HANGOUTS_BABEL_REQUESTWRITER_SQLHELPER, loadPackageParam.classLoader);
        final Class transactionSettings = XposedHelpers.findClass(HANGOUTS_TRANSACTIONSETTINGS, loadPackageParam.classLoader);
        final Class mmsSendReceiveManager = XposedHelpers.findClass(HANGOUTS_MMSSENDRECEIVEMANAGER, loadPackageParam.classLoader);
        final Class mmsMsgClass1 = XposedHelpers.findClass(HANGOUTS_MMS_MESSAGECLASS1, loadPackageParam.classLoader);
        final Class mmsMsgClass2 = XposedHelpers.findClass(HANGOUTS_MMS_MESSAGECLASS2, loadPackageParam.classLoader);
        final Class mmsTransactions = XposedHelpers.findClass(HANGOUTS_MMSTRANSACTIONS, loadPackageParam.classLoader);
        final Class mmsSender = XposedHelpers.findClass(HANGOUTS_MMSSENDER, loadPackageParam.classLoader);

        // Get application context to use later
        XposedHelpers.findAndHookMethod(HANGOUTS_ESAPP_CLASS, loadPackageParam.classLoader, HANGOUTS_ESAPP_ONCREATE, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                debug("Context set.");
                hangoutsCtx = new WeakReference<Context>((Context)param.thisObject);
            }
        });


        // This is called when the user hits the send button on an image MMS
        // FIXME: there seem to be a few instances where this is not called, find alternate code paths
        XposedHelpers.findAndHookMethod(HANGOUTS_PROCESS_MMS_IMG_CLASS, loadPackageParam.classLoader, HANGOUTS_PROCESS_MMS_IMG_METHOD, int.class, int.class, int.class, int.class, Uri.class, new XC_MethodHook() {
            // int1 = ? (usually zero, it seems)
            // int2 = max scaled width, appears to be 640 if landscape or square, 480 if portrait
            // int3 = max scaled height, appears to be 640 if portrait, 480 if landscape or square
            // int4 ?, seems to be width * height - 1024 = 306176
            // Uri1 content:// path that references the input image

            // At least one instance has been reported of int2, int3, and int4 being populated with
            // much larger values resulting in an image much too large to be sent via MMS

            // We're not replacing the method so that even if we fail, which is conceivable, we
            // safely fall back to the original Hangouts result. This also means that if the Hangout
            // function call does something weird that needs to be done (that we don't do) it still
            // gets done. Downside is that we're running code that may never be used.

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Config.reload(systemCtx);
                if(!Config.modEnabled || !Config.resizing) {
                    return;
                }

                // Thanks to cottonBallPaws @ http://stackoverflow.com/a/4250279/238374

                final int paramWidth = (Integer)param.args[1];
                final int paramHeight = (Integer)param.args[2];
                final Uri imgUri = (Uri)param.args[4];

                // Prevents leak of Hangouts account email to the debug log
                final String safeUri = imgUri.toString().substring(0, imgUri.toString().indexOf("?"));

                debug(String.format("New MMS image! %d, %d, %s, %s, %s", paramWidth, paramHeight, safeUri, param.args[0], param.args[3]));
                String quality = Config.imageFormat == Setting.ImageFormat.PNG ? "lossless" : String.valueOf(Config.imageQuality);
                debug(String.format("Configuration: %d×%d, %s at %s quality", Config.imageWidth, Config.imageHeight,
                        Config.imageFormat.toString(), quality));

                ContentResolver esAppResolver = hangoutsCtx.get().getContentResolver();
                InputStream imgStream = esAppResolver.openInputStream(imgUri);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(imgStream, null, options);
                imgStream.close();

                int srcW = options.outWidth;
                int srcH = options.outHeight;

                debug(String.format("Original: %d×%d", srcW, srcH));

                int rotation = 0;
                if(Config.rotation) {
                    rotation = Config.rotateMode;
                    if(rotation == -1) {
                        // Find the rotated "real" dimensions to determine proper final scaling
                        // ExifInterface requires a real file path so we ask Hangouts to tell us where the cached file is located
                        String scratchId = imgUri.getPathSegments().get(1);
                        String filePath = (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass(HANGOUTS_ESPROVIDER_CLASS, loadPackageParam.classLoader), HANGOUTS_ESPROVIDER_GET_SCRATCH_FILE, scratchId);
                        if(new File(filePath).exists()) {
                            debug(String.format("Cache file located: %s", filePath));
                            ExifInterface exif = new ExifInterface(filePath);
                            // Let's pretend other orientation modes don't exist
                            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    rotation = 90;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    rotation = 180;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    rotation = 270;
                                    break;
                                default:
                                    rotation = 0;
                            }
                        } else {
                            rotation = 0;
                            log("Cache file does not exist! Skipping orientation correction.");
                            debug(String.format("Bad cache path: %s", filePath));
                            // We could work around this and write the image byte stream out and then
                            // read it back in but that would take a relatively long time. I've
                            // only seen the scratch pad method fail once (when I called the wrong
                            // function from EsProvider).
                        }
                    }
                    if (rotation != 0) {
                        // Technically we could just swap width and height if rotation = 90 or 270 but
                        // this is a much more fun reference implementation.
                        // TODO: apply rotation to max values as well? Rotated images are scaled more than non
                        Matrix imgMatrix = new Matrix();
                        imgMatrix.postRotate(rotation);
                        RectF imgRect = new RectF();
                        imgMatrix.mapRect(imgRect, new RectF(0, 0, srcW, srcH));
                        srcW = Math.round(imgRect.width());
                        srcH = Math.round(imgRect.height());
                        debug(String.format("Rotated: %d×%d, Rotation: %d°", srcW, srcH, rotation));
                    }
                }

                // Find the highest possible sample size divisor that is still larger than our maxes
                int inSS = 1;
                while((srcW / 2 > Config.imageWidth) || (srcH / 2 > Config.imageHeight)) {
                    srcW /= 2;
                    srcH /= 2;
                    inSS *= 2;
                }

                // Use the longest side to determine scale, this should always be <= 1
                float scale = ((float)(srcW > srcH ? Config.imageWidth : Config.imageHeight)) / (srcW > srcH ? srcW : srcH);

                debug(String.format("Estimated: %d×%d, Sample Size: 1/%d, Scale: %f", srcW, srcH, inSS, scale));

                // Load the sampled image into memory
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inSampleSize = inSS;
                options.inScaled = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                imgStream = esAppResolver.openInputStream(imgUri);
                Bitmap sampled = BitmapFactory.decodeStream(imgStream, null, options);
                imgStream.close();
                debug(String.format("Sampled: %d×%d", sampled.getWidth(), sampled.getHeight()));

                // Load our scale and rotation changes into a matrix and use it to create the final bitmap
                Matrix m = new Matrix();
                m.postScale(scale, scale);
                m.postRotate(rotation);
                Bitmap scaled = Bitmap.createBitmap(sampled, 0, 0, sampled.getWidth(), sampled.getHeight(), m, true);
                sampled.recycle();
                debug(String.format("Scaled: %d×%d", scaled.getWidth(), scaled.getHeight()));

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Bitmap.CompressFormat compressFormat = null;
                final int compressQ = Config.imageFormat == Setting.ImageFormat.PNG ? 0 : Config.imageQuality;
                switch (Config.imageFormat) {
                    case PNG:
                        compressFormat = Bitmap.CompressFormat.PNG;
                        break;
                    case JPEG:
                        compressFormat = Bitmap.CompressFormat.JPEG;
                        break;
                }
                scaled.compress(compressFormat, compressQ, output);
                final int bytes = output.size();
                scaled.recycle();

                param.setResult(output.toByteArray());
                output.close();
                debug(String.format("MMS image processing complete. %d bytes", bytes));
            }
        });

        XposedHelpers.findAndHookConstructor(ComposeMessageView, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                Config.reload((Context)param.args[0]);
                if(Config.modEnabled) {
                    Setting.UiEnterKey enterKey = Setting.UiEnterKey.fromInt(Config.enterKey);
                    debug(String.format("ComposeMessageView: %s, %s", enterKey.name(), Config.attachAnytime));
                    if(enterKey != Setting.UiEnterKey.EMOJI_SELECTOR) {
                        EditText et = (EditText)XposedHelpers.getObjectField(param.thisObject, HANGOUTS_VIEWS_COMPOSEMSGVIEW_EDITTEXT);
                        // Remove Emoji selector (works for new line)
                        int inputType = et.getInputType() ^ InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                        if(enterKey == Setting.UiEnterKey.SEND) {
                            // Disable multi-line input which shows the send button
                            inputType ^= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                        }
                        et.setInputType(inputType);
                    }
                    if(Config.attachAnytime) {
                        ImageButton d = (ImageButton)XposedHelpers.getObjectField(param.thisObject, HANGOUTS_VIEWS_COMPOSEMSGVIEW_EMOJIBUTTON);
                        d.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                ((Runnable)XposedHelpers.callStaticMethod(ComposeMessageView, HANGOUTS_VIEWS_COMPOSEMSGVIEW_ADDATTACHMENT, param.thisObject)).run();
                                return true;
                            }
                        });
                    }
                }
            }
        });

        // Called by at least SwiftKey and Fleksy on new line, but not the AOSP or Google keyboard
        XposedHelpers.findAndHookMethod(ComposeMessageView, HANGOUTS_VIEWS_COMPOSEMSGVIEW_ONEDITORACTION, TextView.class, int.class, KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int actionId = (Integer)param.args[1];
                if(Config.modEnabled && actionId == EditorInfo.IME_NULL && Config.enterKey == Setting.UiEnterKey.NEWLINE.toInt()) {
                    param.setResult(false); // We do not handle the enter action, and it adds a newline for us
                }
            }
        });

        XposedHelpers.findAndHookMethod(ConversationActSuper, HANGOUTS_ACT_CONVERSATION_SUPER_OPOM, Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!Config.modEnabled || !Config.hideCallButtons) {
                    return;
                }
                Menu menu = (Menu) param.args[0];
                for (int i = 0; i < menu.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    if("Call".equals(item.getTitle()) || "Video call".equals(item.getTitle())) {
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                }
            }
        });

        // These two lines appear to fully unlock Hangouts internal logging. There's a lot of it...
        //XposedHelpers.findAndHookMethod(ANDROID_UTIL_LOG_CLASS, loadPackageParam.classLoader, ANDROID_UTIL_LOG_ISLOGGABLE, String.class, int.class, XC_MethodReplacement.returnConstant(true));
        //XposedHelpers.setStaticBooleanField(XposedHelpers.findClass(HANGOUTS_MMSTRANSACTIONS, loadPackageParam.classLoader), HANGOUTS_MMSTRANSACTIONS_DEBUGFIELD, true);

        XposedHelpers.findAndHookMethod(HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS2, loadPackageParam.classLoader, HANGOUTS_BABEL_REQUESTWRITER_INNERCLASS2_SENDMMSREQUEST, Context.class, rWriterInnerClass1, rWriterSqlHelper, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                debug("bfc -> a called");
            }
        });

        XposedHelpers.findAndHookMethod(mmsTransactions, HANGOUTS_MMSTRANSACTIONS_SENDSENDREQ1, Context.class, String[].class, String.class, String.class, String.class, String.class, int.class, int.class, int.class, long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                debug("bvv -> a1 called");
            }
        });

        XposedHelpers.findAndHookMethod(mmsTransactions, HANGOUTS_MMSTRANSACTIONS_SENDSENDREQ2, Context.class, mmsMsgClass2, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                debug("bvv -> a2 called (before)");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                debug("bvv -> a2 called (after)");
            }
        });

        XposedHelpers.findAndHookMethod(mmsSendReceiveManager, HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST1, Context.class, transactionSettings, mmsMsgClass1, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                debug("bvq -> a1 called");
            }
        });

        // This prevents the initial request for MMS APN connectivity. It populates a new
        // TransactionSettings instance with APN data. This is normally done by the broadcast
        // receiver as it listens for connectivity state changes. Instead of waiting this method
        // returns with a valid result almost instantly forcing the MMS process to continue.
        XposedHelpers.findAndHookMethod(mmsSendReceiveManager, HANGOUTS_MMSSENDRECEIVEMANAGER_AQUIREMMSNETWORK, Context.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Config.reload((Context)param.args[0]);
                if(!Config.apnSplicing) {
                    return;
                }
                if(Config.apnPreset == Setting.ApnPreset.CUSTOM && Config.mmsc.isEmpty()) {
                    log("APN splicing enabled but no MMSC has been specified.");
                    return;
                }

                debug(String.format("MMS APN splicing configuration: %s, %s, %s, %d",
                        Config.apnPreset.toString(), Config.mmsc, Config.proxyHost, Config.proxyPort));

                String localMmsc = Config.mmsc;
                String localProxyHost = Config.proxyHost;
                int localProxyPort = Config.proxyPort;
                if(Config.apnPreset != Setting.ApnPreset.CUSTOM) {
                    localMmsc = Config.apnPreset.getMmsc();
                    localProxyHost = Config.apnPreset.getProxyHost();
                    localProxyPort = Config.apnPreset.getProxyPort();
                }
                if(localProxyHost.isEmpty()) {
                    localProxyHost = null;
                    localProxyPort = -1;
                } else {
                    localProxyPort = localProxyPort == -1 ? 80 : localProxyPort;
                }

                Object timerField = XposedHelpers.getStaticObjectField(mmsSendReceiveManager, HANGOUTS_MMSSENDRECEIVEMANAGER_TIMERFIELD);
                // This /should/ synchronize with the actual static field not our local representation of it
                synchronized (timerField) {

                    // Do not splice if not connected to mobile
                    if(!isMobileConnected(hangoutsCtx.get())) {
                        debug("Not on a mobile connection. Not splicing.");
                        return;
                    }

                    debug("GOING FOR IT!");

                    // Create APN
                    Class mmsApn = XposedHelpers.findClass(HANGOUTS_MMS_APN, loadPackageParam.classLoader);
                    Constructor<?> newMmsApn = XposedHelpers.findConstructorExact(mmsApn, String.class, String.class, int.class);
                    // MMSC, Proxy, Port
                    Object instanceMmsApn = newMmsApn.newInstance(localMmsc, localProxyHost, localProxyPort);
                    XposedHelpers.setObjectField(instanceMmsApn, HANGOUTS_MMS_APN_RAWMMSCFIELD, localMmsc);

                    // Creates a TransactionSettings object (this is normally done by the broadcast receiver)
                    Constructor<?> newTransactionSettings = XposedHelpers.findConstructorExact(transactionSettings);
                    newTransactionSettings.setAccessible(true);
                    Object instanceTransactionSettings = newTransactionSettings.newInstance();

                    // Add APN to the list
                    List apnList = (List)XposedHelpers.getObjectField(instanceTransactionSettings, HANGOUTS_TRANSACTIONSETTINGS_APNLISTFIELD);
                    apnList.clear();
                    // You bet your ass this is an unchecked call, Android Studio
                    apnList.add(instanceMmsApn);

                    // Return the needed TransactionSettings object
                    param.setResult(instanceTransactionSettings);
                }
            }
        });

        // This hook replaces a call to the executeMmsRequest method of the MmsSendReceiveManager.
        // It calls the method that actually sends the MMS HTTP request. For some reason our little
        // connectivity shortcut causes this to fail. Instead of spending even more time trying to
        // figure out why that is, I've just replaced the entire method with something that's far
        // less picky about what you feed it. Returning null is sometimes a valid result. Admittedly
        // this replacement may not be nearly as robust as the original implementation.
        XposedHelpers.findAndHookMethod(mmsSendReceiveManager, HANGOUTS_MMSSENDRECEIVEMANAGER_EXECUTEMMSREQUEST2, Context.class, transactionSettings, String.class, int.class, byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Config.reload((Context)param.args[0]);
                if(!Config.apnSplicing) {
                    return;
                }

                debug("bvq -> a2 called");

                if(!isMobileConnected(hangoutsCtx.get())) {
                    return;
                }

                for(Object o : param.args) {
                    debug(String.format("bvq -> a2 args: %s", o));
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
                    List apnList = (List)XposedHelpers.getObjectField(param.args[1], HANGOUTS_TRANSACTIONSETTINGS_APNLISTFIELD);
                    for(Object o : apnList) {
                        debug("APN: " + o.toString());
                    }
                    Object apn = apnList.get(0);
                    mmsc = XposedHelpers.getObjectField(apn, HANGOUTS_MMS_APN_MMSCFIELD);
                    isProxy = XposedHelpers.callMethod(apn, HANGOUTS_MMS_APN_ISPROXYSET);
                    proxy = XposedHelpers.getObjectField(apn, HANGOUTS_MMS_APN_PROXYFIELD);
                    port = XposedHelpers.getObjectField(apn, HANGOUTS_MMS_APN_PORTFIELD);
                }
                debug(String.format("Executing MMS HTTP request. %s, %s, %s, %s, %s, %s, %s, %s",
                        param.args[0], mmsc, param.args[4], param.args[3],
                        isProxy, proxy, port, false));

                byte[] result;
                try {
                    result = (byte[])XposedHelpers.callStaticMethod(mmsSender, HANGOUTS_MMSSENDER_DOSEND,
                            param.args[0],
                            mmsc,
                            param.args[4],
                            param.args[3],
                            isProxy,
                            proxy,
                            port,
                            false);
                } catch (XposedHelpers.InvocationTargetError ex) {
                    log("MMS HTTP request failed!");
                    debug(ex.getCause());
                    Constructor mmsException = XposedHelpers.findConstructorExact(HANGOUTS_MMS_EXCEPTION, loadPackageParam.classLoader, String.class);
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
                        Class mmscResponse = XposedHelpers.findClass(HANGOUTS_MMSC_RESPONSE, loadPackageParam.classLoader);
                        Object instanceMmscResponse = XposedHelpers.findConstructorExact(mmscResponse, byte[].class).newInstance(result);
                        debug("LOCAL RT IS GOOD");
                        param.setResult(XposedHelpers.callMethod(instanceMmscResponse, HANGOUTS_MMSC_RESPONSE_GET_MESSAGECLASS1));
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
        });

        // Ctx = RequestWriter
        // Str1 = MMSC (MMS url when not sending)
        // byte[] = post data? (null when not sending)
        // int1 = 1 (2 when not sending)
        // bool1 = false (true if proxy)
        // Str2 = null (proxy URL?)
        // int2 = -1 (proxy port? just port? -1 must mean default 80 port or no proxy)
        // bool2 = false (true if ipv6)
        XposedHelpers.findAndHookMethod(mmsSender, HANGOUTS_MMSSENDER_DOSEND, Context.class, String.class, byte[].class, int.class, boolean.class, String.class, int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                debug("sk -> a called");
                for(Object o : param.args) {
                    debug(String.format("sk -> a args: %s", o));
                }
                /*if(param.args[2] != null) {
                    debug("byte array: " + new String((byte[]) param.args[2], "UTF-8"));
                }*/
            }
        });

        debug("--- LOAD COMPLETE ---", false);
    }

    private static boolean isMobileConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE && ni.isConnected();
    }

    private static void debug(String msg) {
        debug(msg, true);
    }

    private static void debug(String msg, boolean tag) {
        if(Config.debug) {
            log(msg, tag);
        }
    }

    private static void debug(Throwable throwable) {
        if(Config.debug) {
            log(throwable);
        }
    }

    private static void log(String msg) {
        log(msg, true);
    }

    private static void log(String msg, boolean tag) {
        XposedBridge.log((tag ? TAG + ": " : "") + msg);
    }

    private static void log(Throwable throwable) {
        XposedBridge.log(throwable);
    }
}
