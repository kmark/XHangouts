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

package com.versobit.kmark.xhangouts;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_PKG_NAME;
import static com.versobit.kmark.xhangouts.XHangouts.versionSupported;

/**
 * Prevents a user from accidentally updating Hangouts to a version which the
 * current XHangouts module does not support via StorePrefs.
 *
 * See http://forum.xda-developers.com/xposed/modules/-t3306801
 */
@SuppressWarnings("unused")
public final class StorePrefsHandler {

    /**
     * Called from a separate thread upon class instantiation
     */
    public void init() {
        //
    }

    /**
     * Called when the user clicks update in the Play store.
     * @param pkgName The package name of the app
     * @param vCode The version code of the app
     * @param vName The version name of the app
     * @return True if the application should be updated, false otherwise
     */
    public boolean shouldUserUpdate(String pkgName, int vCode, String vName) {
        return !HANGOUTS_PKG_NAME.equals(pkgName) || versionSupported(vCode);
    }

    /**
     * Called when the Play store attempts to automatically update an app.
     * @param pkgName The package name of the app
     * @param vCode The version code of the app
     * @return True if the application can be automatically updated, false otherwise
     */
    public boolean canAutoUpdate(String pkgName, int vCode) {
        return shouldUserUpdate(pkgName, vCode, null);
    }

}
