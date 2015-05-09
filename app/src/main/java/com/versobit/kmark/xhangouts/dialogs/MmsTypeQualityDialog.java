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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.SettingsActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class MmsTypeQualityDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_mmstypequality";

    private Preference settingPref = null;
    private SharedPreferences prefs;

    private Spinner spinnerFormat;
    private SeekBar seekQuality;
    private TextView txtQuality;
    private ImageView imgPreview1;
    private TextView txtPreview1;

    private Setting.ImageFormat format;
    private int quality;

    private int formatValues[];
    private String lossless;
    private Bitmap bmpPreview1;

    public MmsTypeQualityDialog setSettingPref(Preference pref) {
        settingPref = pref;
        return this;
    }

    @Override @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_mms_type_quality, null);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        spinnerFormat = (Spinner)v.findViewById(R.id.dialog_mms_type_quality_type);
        seekQuality = (SeekBar)v.findViewById(R.id.dialog_mms_type_quality_quality);
        txtQuality = (TextView)v.findViewById(R.id.dialog_mms_type_quality_qlabel);
        imgPreview1 = (ImageView)v.findViewById(R.id.dialog_mms_type_quality_preview1);
        txtPreview1 = (TextView)v.findViewById(R.id.dialog_mms_type_quality_p1text);

        format = Setting.ImageFormat.fromInt(prefs.getInt(Setting.MMS_IMAGE_TYPE.toString(), Setting.ImageFormat.JPEG.toInt()));
        quality = prefs.getInt(Setting.MMS_IMAGE_QUALITY.toString(), 80);

        formatValues = getResources().getIntArray(R.array.pref_mms_image_type_values);
        lossless = getString(R.string.dialog_mms_type_quality_lossless);

        bmpPreview1 = BitmapFactory.decodeResource(getResources(), R.drawable.fajita);

        // Only time we need to set the spinner and seek bar
        // FIXME: the integer value of format is not guaranteed to be the positional index of the spinner values, see formatValues
        spinnerFormat.setSelection(format.toInt());
        seekQuality.setProgress(Math.round(map(quality, 0, 100, 0, seekQuality.getMax())));

        // Let's get it started
        refresh(true);

        spinnerFormat.setOnItemSelectedListener(onSelected);
        seekQuality.setOnSeekBarChangeListener(onSeek);

        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setTitle(R.string.pref_title_mms_image_type)
                .setPositiveButton(android.R.string.ok, onSubmit)
                .create();
    }

    private void refresh() {
        refresh(false);
    }

    private void refresh(final boolean everything) {
        String strQuality = format == Setting.ImageFormat.PNG ? lossless : String.valueOf(quality);
        txtQuality.setText(getString(R.string.dialog_mms_type_quality_qlabel, strQuality));
        seekQuality.setEnabled(format != Setting.ImageFormat.PNG);

        // Most devices will struggle to do this more than once per second, so we don't
        if(!everything) {
            return;
        }
        Bitmap.CompressFormat cmpFormat = null;
        switch (format) {
            case JPEG:
                cmpFormat = Bitmap.CompressFormat.JPEG;
                break;
            case PNG:
                cmpFormat = Bitmap.CompressFormat.PNG;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmpPreview1.compress(cmpFormat, format == Setting.ImageFormat.PNG ? 0 : quality, baos);
            imgPreview1.setImageBitmap(BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size()));
            txtPreview1.setText(getString(R.string.dialog_mms_type_quality_kb, baos.size() / 1000f));
            baos.close();
        } catch (IOException ex) {
            //
        }
    }

    private AdapterView.OnItemSelectedListener onSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            format = Setting.ImageFormat.fromInt(formatValues[position]);
            refresh(true);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

    private SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            quality = Math.round(map(progress, 0, seekBar.getMax(), 0, 100));
            refresh();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            refresh(true);
        }
    };

    private DialogInterface.OnClickListener onSubmit = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            prefs.edit().putInt(Setting.MMS_IMAGE_TYPE.toString(), format.toInt())
                    .putInt(Setting.MMS_IMAGE_QUALITY.toString(), quality).apply();
            SettingsActivity.SettingsFragment.updateMmsTypeQualitySummary(settingPref, format, quality);
        }
    };

    private static float map(float in, float inMin, float inMax, float outMin, float outMax) {
        return (in - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
}
