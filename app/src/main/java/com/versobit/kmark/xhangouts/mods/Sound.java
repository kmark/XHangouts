/*
 * Copyright (C) 2015 Kevin Mark
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
import android.content.res.Resources;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_RES_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public final class Sound extends Module {

    private static final String HANGOUTS_UTIL_CLASS = "f";
    private static final String HANGOUTS_UTIL_CLASS_GET_RES_URI = "k";

    private static final String HANGOUTS_SOUND_ALERT = "hangout_alert";
    private static final String HANGOUTS_SOUND_AUDIO_CALL_IN = "hangout_audio_call_incoming_ringtone";
    private static final String HANGOUTS_SOUND_AUDIO_CALL_OUT = "hangout_audio_call_outgoing_ringtone";
    private static final String HANGOUTS_SOUND_JOIN = "hangout_join";
    private static final String HANGOUTS_SOUND_LEAVE = "hangout_leave";
    private static final String HANGOUTS_SOUND_OUTGOING = "hangout_outgoing_ringtone";
    private static final String HANGOUTS_SOUND_RINGTONE = "hangout_ringtone";
    private static final String HANGOUTS_SOUND_IN_CALL = "hangouts_incoming_call";
    private static final String HANGOUTS_SOUND_MESSAGE = "hangouts_message";

    // Impossible default ID since resource IDs start with 0x7f
    private static final int RES_ID_UNSET = 0;

    // Resources.getIdentifier is expensive so we're caching results
    private int soundAlert = RES_ID_UNSET;
    private int soundAudioCallIn = RES_ID_UNSET;
    private int soundAudioCallOut = RES_ID_UNSET;
    private int soundJoin = RES_ID_UNSET;
    private int soundLeave = RES_ID_UNSET;
    private int soundOutgoing = RES_ID_UNSET;
    private int soundRingtone = RES_ID_UNSET;
    private int soundInCall = RES_ID_UNSET;
    private int soundMessage = RES_ID_UNSET;

    public Sound(Config config) {
        super(Sound.class.getSimpleName(), config);
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        super.hook(loader);
        Class UtilClass = findClass(HANGOUTS_UTIL_CLASS, loader);

        return new IXUnhook[] {
                findAndHookMethod(UtilClass, HANGOUTS_UTIL_CLASS_GET_RES_URI, int.class, getResourceUri)
        };
    }

    private final XC_MethodHook getResourceUri = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Context ctx = getApplication();
            config.reload(ctx);
            if(!config.soundEnabled) {
                return;
            }

            // Are IDs cached?
            if(soundAlert == RES_ID_UNSET) {
                // Find and cache IDs
                Resources res = ctx.getResources();
                soundAlert = res.getIdentifier(HANGOUTS_SOUND_ALERT, "raw", HANGOUTS_RES_PKG_NAME);
                soundAudioCallIn = res.getIdentifier(HANGOUTS_SOUND_AUDIO_CALL_IN, "raw", HANGOUTS_RES_PKG_NAME);
                soundAudioCallOut = res.getIdentifier(HANGOUTS_SOUND_AUDIO_CALL_OUT, "raw", HANGOUTS_RES_PKG_NAME);
                soundJoin = res.getIdentifier(HANGOUTS_SOUND_JOIN, "raw", HANGOUTS_RES_PKG_NAME);
                soundLeave = res.getIdentifier(HANGOUTS_SOUND_LEAVE, "raw", HANGOUTS_RES_PKG_NAME);
                soundOutgoing = res.getIdentifier(HANGOUTS_SOUND_OUTGOING, "raw", HANGOUTS_RES_PKG_NAME);
                soundRingtone = res.getIdentifier(HANGOUTS_SOUND_RINGTONE, "raw", HANGOUTS_RES_PKG_NAME);
                soundInCall = res.getIdentifier(HANGOUTS_SOUND_IN_CALL, "raw", HANGOUTS_RES_PKG_NAME);
                soundMessage = res.getIdentifier(HANGOUTS_SOUND_MESSAGE, "raw", HANGOUTS_RES_PKG_NAME);
                debug("Resources loaded.");
            }

            // If only we could use a switch here...
            // I'm all ears for better ways to do this
            int soundId = (int)param.args[0];
            String path = "";
            if(soundAlert == soundId) {
                path = config.soundAlert;
            } else if(soundAudioCallIn == soundId) {
                path = config.soundAudioCallIn;
            } else if(soundAudioCallOut == soundId) {
                path = config.soundAudioCallOut;
            } else if(soundJoin == soundId) {
                path = config.soundJoin;
            } else if(soundLeave == soundId) {
                path = config.soundLeave;
            } else if(soundOutgoing == soundId) {
                path = config.soundOutgoing;
            } else if(soundRingtone == soundId) {
                path = config.soundRingtone;
            } else if(soundInCall == soundId) {
                path = config.soundInCall;
            } else if(soundMessage == soundId) {
                path = config.soundMessage;
            }

            if(!path.isEmpty()) {
                param.setResult(path);
            }
        }
    };
}
