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

package com.versobit.kmark.xhangouts.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.SettingsActivity;

import java.util.Locale;
import java.util.Random;

public final class FilePickerPreference extends Preference implements View.OnLongClickListener {

    private final int pickerCode;
    private SharedPreferences prefs;

    public FilePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TypedArray styledAttrs = getContext().obtainStyledAttributes(attrs, R.styleable.FilePickerPreference);
        pickerCode = styledAttrs.getInt(R.styleable.FilePickerPreference_requestCode, new Random().nextInt());
        styledAttrs.recycle();

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Launch the file picker using the backing SettingsFragment
                ((SettingsActivity.SettingsFragment) ((Activity) getContext()).getFragmentManager()
                        .findFragmentById(android.R.id.content)).filePickerStartActForResult(
                        FilePickerPreference.this, FileUtils.createGetContentIntent(), pickerCode
                );
                return true;
            }
        });

        String path = prefs.getString(getKey(), "");
        if(path.isEmpty()) {
            setSummary(R.string.pref_desc_sound_default);
        } else {
            setSummary(path);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != pickerCode || resultCode != Activity.RESULT_OK) {
            return;
        }
        String file = FileUtils.getPath(getContext(), data.getData());
        if(file == null) {
            return;
        }
        setSummary(file);
        prefs.edit().putString(getKey(), file).apply();

        // Warn on potentially unsupported file
        if(!file.toLowerCase(Locale.US).matches("^.+\\.(?:mp4|m4a|aac|flac|mp3|mid|ogg|wav|mkv)$")) {
            Toast.makeText(getContext(), R.string.pref_toast_sound_type, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Toast.makeText(getContext(),
                getContext().getString(R.string.pref_toast_sound_cleared, getTitle()),
                Toast.LENGTH_SHORT).show();
        setSummary(R.string.pref_desc_sound_default);
        prefs.edit().putString(getKey(), "").apply();
        return true;
    }
}
