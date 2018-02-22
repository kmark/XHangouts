/*
 * Copyright (C) 2018 Kevin Mark
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

public final class BuildConfigWrapper {

    public final boolean debug;
    public final String application_id;
    public final String build_type;
    public final String flavor;
    public final int version_code;
    public final String version_name;

    BuildConfigWrapper(boolean debug, String application_id, String build_type, String flavor,
                       int version_code, String version_name) {
        this.debug = debug;
        this.application_id = application_id;
        this.build_type = build_type;
        this.flavor = flavor;
        this.version_code = version_code;
        this.version_name = version_name;
    }

    static Object[] collect() {
        return new Object[] {
                BuildConfig.DEBUG,
                BuildConfig.APPLICATION_ID,
                BuildConfig.BUILD_TYPE,
                BuildConfig.FLAVOR,
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME,
        };
    }

    @SuppressWarnings("ConstantConditions")
    public boolean equalToCurrent() {
        if (debug != BuildConfig.DEBUG) return false;
        if (version_code != BuildConfig.VERSION_CODE) return false;
        if (application_id != null ? !application_id.equals(BuildConfig.APPLICATION_ID) : BuildConfig.APPLICATION_ID != null)
            return false;
        if (build_type != null ? !build_type.equals(BuildConfig.BUILD_TYPE) : BuildConfig.BUILD_TYPE != null)
            return false;
        if (flavor != null ? !flavor.equals(BuildConfig.FLAVOR) : BuildConfig.FLAVOR != null) return false;
        return version_name != null ? version_name.equals(BuildConfig.VERSION_NAME) : BuildConfig.VERSION_NAME == null;
    }

}
