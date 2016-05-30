/*
 * Copyright (C) 2015-2016 Kevin Mark
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_PKG_NAME;
import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_RES_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public final class Sound {

    private static final String ANDROID_MEDIAPLAYER_CREATE = "create";
    private static final String ANDROID_MEDIAPLAYER_SETDATASOURCE = "setDataSource";

    private static final String HANGOUTS_SOUND_AUDIO_CALL_IN = "hangout_audio_call_incoming_ringtone";
    private static final String HANGOUTS_SOUND_AUDIO_CALL_OUT = "hangout_audio_call_outgoing_ringtone";
    private static final String HANGOUTS_SOUND_JOIN = "hangout_join";
    private static final String HANGOUTS_SOUND_LEAVE = "hangout_leave";
    private static final String HANGOUTS_SOUND_OUTGOING = "hangout_outgoing_ringtone";
    private static final String HANGOUTS_SOUND_IN_CALL = "hangouts_incoming_call";

    // Impossible default ID since resource IDs start with 0x7f
    private static final int RES_ID_UNSET = 0;

    // Resources.getIdentifier is expensive so we're caching results
    private static int soundAudioCallIn = RES_ID_UNSET;
    private static int soundAudioCallOut = RES_ID_UNSET;
    private static int soundJoin = RES_ID_UNSET;
    private static int soundLeave = RES_ID_UNSET;
    private static int soundOutgoing = RES_ID_UNSET;
    private static int soundInCall = RES_ID_UNSET;


    public static void handleLoadPackage(final Config config) {
        // Handles the join and leave sounds
        findAndHookMethod(MediaPlayer.class, ANDROID_MEDIAPLAYER_CREATE, Context.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context ctx = (Context) param.args[0];
                config.reload(ctx);
                if (!config.modEnabled) {
                    return;
                }

                XHangouts.debug(String.format("createMediaPlayerFromResId: %b", config.soundEnabled));

                if (!config.soundEnabled) {
                    return;
                }

                // Are IDs cached?
                if (soundJoin == RES_ID_UNSET) {
                    // Find and cache IDs
                    Resources res = ctx.getResources();
                    soundJoin = res.getIdentifier(HANGOUTS_SOUND_JOIN, "raw", HANGOUTS_RES_PKG_NAME);
                    soundLeave = res.getIdentifier(HANGOUTS_SOUND_LEAVE, "raw", HANGOUTS_RES_PKG_NAME);
                    XHangouts.debug(String.format("join: 0x%x, leave: 0x%x", soundJoin, soundLeave));
                }

                int soundId = (int) param.args[1];
                String newSound;
                if (soundJoin == soundId) {
                    newSound = config.soundJoin;
                } else if (soundLeave == soundId) {
                    newSound = config.soundLeave;
                } else {
                    return;
                }

                if (newSound.isEmpty()) {
                    // No custom sound is configured for this particular ID
                    return;
                }

                if (isPermissionGranted(ctx)) {
                    // Do it
                    param.setResult(MediaPlayer.create(ctx, Uri.parse(newSound)));
                    XHangouts.debug(String.format("0x%x redirected to %s", soundId, newSound));
                } else {
                    XHangouts.debug(String.format("Denied access to %s", newSound));
                }
            }
        });

        // Handles all the other sounds
        findAndHookMethod(MediaPlayer.class, ANDROID_MEDIAPLAYER_SETDATASOURCE, Context.class, Uri.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context ctx = (Context) param.args[0];
                config.reload(ctx);
                if (!config.modEnabled) {
                    return;
                }

                XHangouts.debug(String.format("setDataSource: %b", config.soundEnabled));

                if (!config.soundEnabled) {
                    return;
                }

                // Should hopefully be in the format: android.resource://HANGOUTS_PKG_NAME/raw/soundId
                Uri soundUri = (Uri) param.args[1];
                if (!ContentResolver.SCHEME_ANDROID_RESOURCE.equals(soundUri.getScheme())
                        || !HANGOUTS_PKG_NAME.equals(soundUri.getHost())) {
                    return;
                }

                // Attempt to retrieve the last segment, assuming one exists
                String lastSegment = soundUri.getLastPathSegment();
                if (lastSegment == null) {
                    return;
                }

                // Parse out the sound resource ID from the trailing segment
                int soundId;
                try {
                    soundId = Integer.valueOf(lastSegment);
                } catch (NumberFormatException ex) {
                    XHangouts.log(ex);
                    return;
                }

                // Are IDs cached?
                if (soundAudioCallIn == RES_ID_UNSET) {
                    // Find and cache IDs
                    Resources res = ctx.getResources();
                    soundAudioCallIn = res.getIdentifier(HANGOUTS_SOUND_AUDIO_CALL_IN, "raw", HANGOUTS_RES_PKG_NAME);
                    soundAudioCallOut = res.getIdentifier(HANGOUTS_SOUND_AUDIO_CALL_OUT, "raw", HANGOUTS_RES_PKG_NAME);
                    soundOutgoing = res.getIdentifier(HANGOUTS_SOUND_OUTGOING, "raw", HANGOUTS_RES_PKG_NAME);
                    soundInCall = res.getIdentifier(HANGOUTS_SOUND_IN_CALL, "raw", HANGOUTS_RES_PKG_NAME);
                    XHangouts.debug(String.format("audioCallIn: 0x%x, audioCallOut: 0x%x, outgoing: 0x%x, inCall: 0x%x",
                            soundAudioCallIn, soundAudioCallOut, soundOutgoing, soundInCall));
                }

                // If only we could use a switch here...
                // I'm all ears for better ways to do this
                String newSound = "";
                if (soundAudioCallIn == soundId) {
                    newSound = config.soundAudioCallIn;
                } else if (soundAudioCallOut == soundId) {
                    newSound = config.soundAudioCallOut;
                } else if (soundOutgoing == soundId) {
                    newSound = config.soundOutgoing;
                } else if (soundInCall == soundId) {
                    newSound = config.soundInCall;
                }

                if (newSound.isEmpty()) {
                    // No custom sound is configured for this particular ID
                    return;
                }

                if (isPermissionGranted(ctx)) {
                    // Do it
                    ((MediaPlayer) param.thisObject).setDataSource(newSound);
                    param.setResult(null);
                    XHangouts.debug(String.format("0x%x redirected to %s", soundId, newSound));
                } else {
                    XHangouts.debug(String.format("Denied access to %s", newSound));
                }
            }
        });

    }

    private static boolean isPermissionGranted(Context context) {
        return context.getPackageManager().checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }
}
