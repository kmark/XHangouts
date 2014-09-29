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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.Map;

public final class SettingsProvider extends ContentProvider {

    static final String AUTHORITY = BuildConfig.PACKAGE_NAME + ".settings";

    private static final int CODE_ALL = 0;

    static final int QUERY_ALL_KEY = 0;
    static final int QUERY_ALL_VALUE = 1;

    static final int FALSE = 0;
    static final int TRUE = 1;

    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(AUTHORITY, "all", CODE_ALL);
    }

    private SharedPreferences prefs;

    @Override
    public String getType(Uri uri) {
        switch (matcher.match(uri)) {
            case CODE_ALL:
                return "vnd.android.cursor.dir/" + AUTHORITY;
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        switch (matcher.match(uri)) {
            case CODE_ALL:
                MatrixCursor cursor = new MatrixCursor(new String[]{ "key", "value" });
                for(Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                    Object[] row = new Object[] { entry.getKey(), entry.getValue() };
                    // Cursor doesn't support bool, convert to int
                    if(row[QUERY_ALL_VALUE] instanceof Boolean) {
                        // bool to int, 0 = false, 1 = true
                        row[QUERY_ALL_VALUE] = ((Boolean) row[QUERY_ALL_VALUE]) ? TRUE : FALSE;
                    }
                    cursor.addRow(row);
                }
                return cursor;
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is read-only.");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is read-only.");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("This provider is read-only.");
    }

}
