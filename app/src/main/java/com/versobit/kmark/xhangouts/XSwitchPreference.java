/*
 * Copyright (C) 2014 Kevin Mark
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
import android.preference.SwitchPreference;
import android.util.AttributeSet;

// Thanks to Joe on Stack Overflow for this WTF-worthy fix
// http://stackoverflow.com/a/15744076/238374
// Android bug report: https://code.google.com/p/android/issues/detail?id=26194

public final class XSwitchPreference extends SwitchPreference {
    public XSwitchPreference(Context ctx) {
        super(ctx);
    }

    public XSwitchPreference(Context ctx, AttributeSet attrSet) {
        super(ctx, attrSet);
    }

    public XSwitchPreference(Context ctx, AttributeSet attrSet, int defStyle) {
        super(ctx, attrSet, defStyle);
    }
}
