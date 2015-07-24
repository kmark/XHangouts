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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;

import de.robv.android.xposed.XposedBridge;

import static com.versobit.kmark.xhangouts.SettingsProvider.QUERY_ALL_KEY;
import static com.versobit.kmark.xhangouts.SettingsProvider.QUERY_ALL_VALUE;
import static com.versobit.kmark.xhangouts.SettingsProvider.TRUE;

public final class Config {

    private static final Uri ALL_PREFS_URI = Uri.parse("content://" + SettingsProvider.AUTHORITY + "/all");

    // Prevent reloading from occurring less than 2 seconds
    private static final long RELOAD_INTERVAL = 2000;

    private long lastReload = 0;

    // Give us some sane defaults, just in case
    public boolean modEnabled = true;
    public boolean resizing = true;
    public boolean rotation = true;
    public int rotateMode = -1;
    public int imageWidth = 1024;
    public int imageHeight = 1024;
    public Setting.ImageFormat imageFormat = Setting.ImageFormat.JPEG;
    public int imageQuality = 80;
    public boolean apnSplicing = false;
    public Setting.ApnPreset apnPreset = Setting.ApnPreset.CUSTOM;
    public String mmsc = "";
    public String proxyHost = "";
    public int proxyPort = -1;
    public Setting.UiEnterKey enterKey = Setting.UiEnterKey.EMOJI_SELECTOR;
    public boolean attachAnytime = true;
    public boolean hideEmoji = false;
    public boolean hideCallButtons = false;
    public boolean sendLock = false;
    public boolean disableProximity = false;
    public Setting.AppColor appColor = Setting.AppColor.GOOGLE_GREEN;
    public boolean soundEnabled = false;
    public String soundAudioCallIn = "";
    public String soundAudioCallOut = "";
    public String soundJoin = "";
    public String soundLeave = "";
    public String soundOutgoing = "";
    public String soundInCall = "";
    public String soundMessage = "";
    public boolean debug = false;

    public void reload(Context ctx) {
        reload(ctx, RELOAD_INTERVAL);
    }

    public void reload(Context ctx, long interval) {
        // Prevent wasteful reloads
        // Reloads can take anywhere from just 0.03ms to as much as 150ms or more
        if(lastReload + interval > SystemClock.elapsedRealtime()) {
            return;
        }

        Cursor prefs = ctx.getContentResolver().query(ALL_PREFS_URI, null, null, null, null);
        if(prefs == null) {
            XposedBridge.log("XHangouts: Failed to retrieve settings!");
            return;
        }
        Setting setting;
        while(prefs.moveToNext()) {
            try {
                setting = Setting.fromString(prefs.getString(QUERY_ALL_KEY));
            } catch (IllegalArgumentException ex) {
                // If we can't find an enum entry for a setting, avoid crashing and continue
                continue;
            }
            switch (setting) {
                case MOD_ENABLED:
                    modEnabled = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case MMS_RESIZE_ENABLED:
                    resizing = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case MMS_ROTATE_ENABLED:
                    rotation = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case MMS_ROTATE_MODE:
                    rotateMode = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case MMS_SCALE_WIDTH:
                    imageWidth = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case MMS_SCALE_HEIGHT:
                    imageHeight = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case MMS_IMAGE_TYPE:
                    imageFormat = Setting.ImageFormat.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case MMS_IMAGE_QUALITY:
                    imageQuality = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case MMS_APN_SPLICING_ENABLED:
                    apnSplicing = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case MMS_APN_SPLICING_APN_CONFIG_PRESET:
                    apnPreset = Setting.ApnPreset.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case MMS_APN_SPLICING_APN_CONFIG_MMSC:
                    mmsc = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case MMS_APN_SPLICING_APN_CONFIG_PROXY_HOSTNAME:
                    proxyHost = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case MMS_APN_SPLICING_APN_CONFIG_PROXY_PORT:
                    proxyPort = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_ENTER_KEY:
                    enterKey = Setting.UiEnterKey.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_ATTACH_ANYTIME:
                    attachAnytime = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_HIDE_EMOJI:
                    hideEmoji = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_HIDE_CALL_BUTTONS:
                    hideCallButtons = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_SEND_LOCK:
                    sendLock = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_DISABLE_PROXIMITY:
                    disableProximity = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_APP_COLOR:
                    appColor = Setting.AppColor.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case SOUND_ENABLED:
                    soundEnabled = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case SOUND_AUDIOCALLIN:
                    soundAudioCallIn = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case SOUND_AUDIOCALLOUT:
                    soundAudioCallOut = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case SOUND_JOIN:
                    soundJoin = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case SOUND_LEAVE:
                    soundLeave = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case SOUND_OUTGOING:
                    soundOutgoing = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case SOUND_INCALL:
                    soundInCall = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case SOUND_MESSAGE:
                    soundMessage = prefs.getString(QUERY_ALL_VALUE);
                    continue;
                case DEBUG:
                    debug = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
            }
        }
        prefs.close();
        lastReload = SystemClock.elapsedRealtime();
    }
}
