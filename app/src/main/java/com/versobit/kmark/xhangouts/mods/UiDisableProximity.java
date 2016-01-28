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

import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public final class UiDisableProximity {

    private static final String ANDROID_HARDWARE_SENSORMANAGER_DEFAULT = "getDefaultSensor";

    public static void handleLoadPackage(final Config config) {
        if(!config.modEnabled || !config.disableProximity) {
            return;
        }

        findAndHookMethod(SensorManager.class, ANDROID_HARDWARE_SENSORMANAGER_DEFAULT, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if((int)param.args[0] == Sensor.TYPE_PROXIMITY) {
                    param.setResult(null);
                }
            }
        });

    }
}
