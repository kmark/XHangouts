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

package com.versobit.kmark.xhangouts.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.SettingsActivity;

public final class MmsScaleDialog extends AlertDialog {

    final private Preference settingPref;
    private SharedPreferences prefs;

    private EditText txtWidth;
    private EditText txtHeight;

    private int scaleWidth;
    private int scaleHeight;

    public MmsScaleDialog(final Preference settingPref) {
        super(settingPref.getContext());
        this.settingPref = settingPref;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.dialog_mms_scale, null);
        txtWidth = (EditText)v.findViewById(R.id.dialog_mms_scale_width);
        txtHeight = (EditText)v.findViewById(R.id.dialog_mms_scale_height);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        scaleWidth = prefs.getInt(Setting.MMS_SCALE_WIDTH.toString(), 1024);
        scaleHeight = prefs.getInt(Setting.MMS_SCALE_HEIGHT.toString(), 1024);

        txtWidth.setText(String.valueOf(scaleWidth));
        txtHeight.setText(String.valueOf(scaleHeight));
        txtWidth.addTextChangedListener(textWatcher);
        txtHeight.addTextChangedListener(textWatcher);

        setTitle(R.string.pref_title_mms_scale);
        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), onSubmit);

        setView(v);
        super.onCreate(savedInstanceState);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //
        }

        @Override
        public void afterTextChanged(Editable s) {
            getButton(BUTTON_POSITIVE).setEnabled(txtWidth.length() > 0 && txtHeight.length() > 0);
        }
    };

    private final OnClickListener onSubmit = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            try {
                scaleWidth = Integer.parseInt(txtWidth.getText().toString());
                scaleHeight = Integer.parseInt(txtHeight.getText().toString());
                prefs.edit().putInt(Setting.MMS_SCALE_WIDTH.toString(), scaleWidth)
                        .putInt(Setting.MMS_SCALE_HEIGHT.toString(), scaleHeight).apply();
                SettingsActivity.SettingsFragment.updateMmsScaleSummary(settingPref, scaleWidth, scaleHeight);
            } catch (NumberFormatException ex) {
                //
            }
        }
    };
}
