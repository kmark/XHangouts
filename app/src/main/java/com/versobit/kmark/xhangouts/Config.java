/*
 * Copyright (C) 2014-2016 Kevin Mark
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

    // Ignore reloads that fall within two seconds of the last successful one
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
    public Setting.UiEmoji emoji = Setting.UiEmoji.DEFAULT;
    public Setting.UiButtons gallery = Setting.UiButtons.DEFAULT;
    public Setting.UiButtons camera = Setting.UiButtons.DEFAULT;
    public Setting.UiButtons video = Setting.UiButtons.DEFAULT;
    public Setting.UiButtons stickers = Setting.UiButtons.DEFAULT;
    public Setting.UiButtons location = Setting.UiButtons.DEFAULT;
    public Setting.UiEnterKey enterKey = Setting.UiEnterKey.EMOJI_SELECTOR;
    public boolean enhanceCallButton = true;
    public boolean hideCallButtons = false;
    public boolean sendLock = false;
    public boolean disableProximity = false;
    public boolean showDebugOptions = false;
    public Setting.AppColor appColor = Setting.AppColor.GOOGLE_GREEN;
    public boolean darkTheme = false;
    public boolean blackBackgrounds = false;
    public boolean themeBubblesDark = true;
    public boolean themeBubblesLight = false;
    public boolean themeHyperlinks = true;
    public boolean soundEnabled = false;
    public String soundAudioCallIn = "";
    public String soundAudioCallOut = "";
    public String soundJoin = "";
    public String soundLeave = "";
    public String soundOutgoing = "";
    public String soundInCall = "";
    public boolean debug = false;
    public boolean theming = true;
    public boolean highlightUnread = true;
    public int outgoingColor = 0xffcfd8dc;
    public int outgoingColorOTR = 0xff455a64;
    public int outgoingFontColor = 0xff263238;
    public int outgoingFontColorOTR = 0xffffffff;
    public int outgoingLinkColor = 0xff3b78e7;
    public int outgoingLinkColorOTR = 0xffffffff;
    public int incomingColor = 0xffffffff;
    public int incomingColorOTR = 0xffcfd8dc;
    public int incomingFontColor = 0xff263238;
    public int incomingFontColorOTR = 0xff263238;
    public int incomingLinkColor = 0xff3b78e7;
    public int incomingLinkColorOTR = 0xff3b78e7;
    public int outgoingDarkColor = 0xff424242;
    public int outgoingDarkColorOTR = 0xff424242;
    public int outgoingDarkFontColor = 0xffffffff;
    public int outgoingDarkFontColorOTR = 0xffffffff;
    public int outgoingDarkLinkColor = 0xff212121;
    public int outgoingDarkLinkColorOTR = 0xff212121;
    public int incomingDarkColor = 0xff424242;
    public int incomingDarkColorOTR = 0xff424242;
    public int incomingDarkFontColor = 0xffffffff;
    public int incomingDarkFontColorOTR = 0xffffffff;
    public int incomingDarkLinkColor = 0xff212121;
    public int incomingDarkLinkColorOTR = 0xff212121;

    public void reload(Context ctx) {
        reload(ctx, RELOAD_INTERVAL);
    }

    public void reload(Context ctx, long interval) {
        // Prevent wasteful reloads
        // Reloads can take anywhere from just 0.03ms to as much as 150ms or more
        if (lastReload + interval > SystemClock.elapsedRealtime()) {
            return;
        }

        Cursor prefs = ctx.getContentResolver().query(ALL_PREFS_URI, null, null, null, null);
        if (prefs == null) {
            XposedBridge.log("XHangouts: Failed to retrieve settings!");
            return;
        }
        Setting setting;
        while (prefs.moveToNext()) {
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
                case UI_EMOJI:
                    emoji = Setting.UiEmoji.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_GALLERY:
                    gallery = Setting.UiButtons.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_CAMERA:
                    camera = Setting.UiButtons.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_VIDEO:
                    video = Setting.UiButtons.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_STICKERS:
                    stickers = Setting.UiButtons.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_LOCATION:
                    location = Setting.UiButtons.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_ENTER_KEY:
                    enterKey = Setting.UiEnterKey.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_ENHANCE_CALL_BUTTON:
                    enhanceCallButton = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
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
                case UI_SHOW_DEBUG_OPTIONS:
                    showDebugOptions = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_ENABLE_THEMING:
                    theming = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_APP_COLOR:
                    appColor = Setting.AppColor.fromInt(prefs.getInt(QUERY_ALL_VALUE));
                    continue;
                case UI_DARK_THEME:
                    darkTheme = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_BLACK_BACKGROUNDS:
                    blackBackgrounds = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_DARK_THEME_BUBBLES:
                    themeBubblesDark = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_LIGHT_THEME_BUBBLES:
                    themeBubblesLight = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_THEME_HYPERLINKS:
                    themeHyperlinks = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_HIGHLIGHT_UNREAD:
                    highlightUnread = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
                    continue;
                case UI_COLOR_INCOMING:
                    incomingColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_INCOMING_OTR:
                    incomingColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_INCOMING_FONT:
                    incomingFontColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_INCOMING_FONT_OTR:
                    incomingFontColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_INCOMING_LINK:
                    incomingLinkColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_INCOMING_LINK_OTR:
                    incomingLinkColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_OUTGOING:
                    outgoingColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_OUTGOING_OTR:
                    outgoingColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_OUTGOING_FONT:
                    outgoingFontColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_OUTGOING_FONT_OTR:
                    outgoingFontColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_OUTGOING_LINK:
                    outgoingLinkColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_COLOR_OUTGOING_LINK_OTR:
                    outgoingLinkColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_INCOMING:
                    incomingDarkColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_INCOMING_OTR:
                    incomingDarkColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_INCOMING_FONT:
                    incomingDarkFontColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_INCOMING_FONT_OTR:
                    incomingDarkFontColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_INCOMING_LINK:
                    incomingDarkLinkColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_INCOMING_LINK_OTR:
                    incomingDarkLinkColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_OUTGOING:
                    outgoingDarkColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_OUTGOING_OTR:
                    outgoingDarkColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_OUTGOING_FONT:
                    outgoingDarkFontColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_OUTGOING_FONT_OTR:
                    outgoingDarkFontColorOTR = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_OUTGOING_LINK:
                    outgoingDarkLinkColor = prefs.getInt(QUERY_ALL_VALUE);
                    continue;
                case UI_DARK_COLOR_OUTGOING_LINK_OTR:
                    outgoingDarkLinkColorOTR = prefs.getInt(QUERY_ALL_VALUE);
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
                case DEBUG:
                    debug = prefs.getInt(QUERY_ALL_VALUE) == TRUE;
            }
        }
        prefs.close();
        lastReload = SystemClock.elapsedRealtime();
    }
}
