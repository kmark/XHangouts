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

package com.versobit.kmark.xhangouts;

import android.app.AlertDialog;
import android.content.Context;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class MmsTypeQualityDialog extends AlertDialog {

    private final Preference settingPref;
    private SharedPreferences prefs;

    private Spinner spinnerFormat;
    private SeekBar seekQuality;
    private TextView txtQuality;
    private ImageView imgPreview1;
    private ImageView imgPreview2;
    private TextView txtPreview1;
    private TextView txtPreview2;

    private Setting.ImageFormat format;
    private int quality;

    private int formatValues[];
    private String lossless;
    private Bitmap bmpPreview1;
    private Bitmap bmpPreview2;

    MmsTypeQualityDialog(final Preference settingPref) {
        super(settingPref.getContext());
        this.settingPref = settingPref;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.dialog_mms_type_quality, null);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        spinnerFormat = (Spinner)v.findViewById(R.id.dialog_mms_type_quality_type);
        seekQuality = (SeekBar)v.findViewById(R.id.dialog_mms_type_quality_quality);
        txtQuality = (TextView)v.findViewById(R.id.dialog_mms_type_quality_qlabel);
        imgPreview1 = (ImageView)v.findViewById(R.id.dialog_mms_type_quality_preview1);
        imgPreview2 = (ImageView)v.findViewById(R.id.dialog_mms_type_quality_preview2);
        txtPreview1 = (TextView)v.findViewById(R.id.dialog_mms_type_quality_p1text);
        txtPreview2 = (TextView)v.findViewById(R.id.dialog_mms_type_quality_p2text);

        format = Setting.ImageFormat.fromInt(prefs.getInt(Setting.MMS_IMAGE_TYPE.toString(), Setting.ImageFormat.JPEG.toInt()));
        quality = prefs.getInt(Setting.MMS_IMAGE_QUALITY.toString(), 80);

        formatValues = getContext().getResources().getIntArray(R.array.pref_mms_image_type_values);
        lossless = getContext().getString(R.string.dialog_mms_type_quality_lossless);

        bmpPreview1 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.fajita);
        bmpPreview2 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.screenshot);

        // Only time we need to set the spinner and seek bar
        // FIXME: the integer value of format is not guaranteed to be the positional index of the spinner values, see formatValues
        spinnerFormat.setSelection(format.toInt());
        seekQuality.setProgress(Math.round(map(quality, 0, 100, 0, seekQuality.getMax())));

        // Let's get it started
        refresh(true);

        spinnerFormat.setOnItemSelectedListener(onSelected);
        seekQuality.setOnSeekBarChangeListener(onSeek);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), onSubmit);

        setView(v);
        super.onCreate(savedInstanceState);
    }

    private void refresh() {
        refresh(false);
    }

    private void refresh(final boolean everything) {
        String strQuality = format == Setting.ImageFormat.PNG ? lossless : String.valueOf(quality);
        txtQuality.setText(getContext().getString(R.string.dialog_mms_type_quality_qlabel, strQuality));
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
            txtPreview1.setText(getContext().getString(R.string.dialog_mms_type_quality_kb, baos.size() / 1000f));
            baos.close();

            baos = new ByteArrayOutputStream();
            bmpPreview2.compress(cmpFormat, format == Setting.ImageFormat.PNG ? 0 : quality, baos);
            imgPreview2.setImageBitmap(BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size()));
            txtPreview2.setText(getContext().getString(R.string.dialog_mms_type_quality_kb, baos.size() / 1000f));
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

    private OnClickListener onSubmit = new OnClickListener() {
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
