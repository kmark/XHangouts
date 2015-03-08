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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.SettingsActivity;

public final class MmsApnConfigDialog extends AlertDialog {

    final private Preference settingPref;
    private SharedPreferences prefs;

    private Setting.ApnPreset[] apnPresetList;

    private Spinner spinPreset;
    private EditText txtMmsc;
    private EditText txtProxyHostname;
    private EditText txtProxyPort;

    private Setting.ApnPreset preset;

    private boolean selectingPreset = false;

    public MmsApnConfigDialog(final Preference settingPref) {
        super(settingPref.getContext());
        this.settingPref = settingPref;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.dialog_mms_apn_config, null);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        spinPreset = (Spinner)v.findViewById(R.id.dialog_mms_apn_config_preset);
        txtMmsc = (EditText)v.findViewById(R.id.dialog_mms_apn_config_mmsc);
        txtProxyHostname = (EditText)v.findViewById(R.id.dialog_mms_apn_config_proxy_host);
        txtProxyPort = (EditText)v.findViewById(R.id.dialog_mms_apn_config_proxy_port);

        apnPresetList = Setting.ApnPreset.values();
        String[] ordApnPresets = new String[apnPresetList.length];
        for(Setting.ApnPreset p : apnPresetList) {
            ordApnPresets[p.ordinal()] = p.toString();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_medium_item, ordApnPresets);
        adapter.setDropDownViewResource(R.layout.spinner_medium_dropdown_item);
        spinPreset.setAdapter(adapter);
        spinPreset.setOnItemSelectedListener(onSelected);

        preset = Setting.ApnPreset.fromInt(prefs.getInt(Setting.MMS_APN_SPLICING_APN_CONFIG_PRESET.toString(), Setting.ApnPreset.CUSTOM.toInt()));
        spinPreset.setSelection(preset.ordinal());

        if(preset == Setting.ApnPreset.CUSTOM) {
            txtMmsc.setText(prefs.getString(Setting.MMS_APN_SPLICING_APN_CONFIG_MMSC.toString(), ""));
            txtProxyHostname.setText(prefs.getString(Setting.MMS_APN_SPLICING_APN_CONFIG_PROXY_HOSTNAME.toString(), ""));
            int proxyPort = prefs.getInt(Setting.MMS_APN_SPLICING_APN_CONFIG_PROXY_PORT.toString(), -1);
            if(proxyPort != -1) {
                txtProxyPort.setText(String.valueOf(proxyPort));
            }
        }

        txtMmsc.addTextChangedListener(textWatcher);
        txtProxyHostname.addTextChangedListener(textWatcher);
        txtProxyPort.addTextChangedListener(textWatcher);


        setTitle(R.string.pref_title_mms_apn_splicing_apn_config);
        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), onSubmit);

        setView(v);
        super.onCreate(savedInstanceState);
    }

    private final AdapterView.OnItemSelectedListener onSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            preset = apnPresetList[position];
            if(preset == Setting.ApnPreset.CUSTOM) {
                return;
            }
            selectingPreset = true;
            txtMmsc.setText(preset.getMmsc());
            txtProxyHostname.setText(preset.getProxyHost());
            if(preset.getProxyPort() == -1) {
                txtProxyPort.setText("");
            } else {
                txtProxyPort.setText(String.valueOf(preset.getProxyPort()));
            }
            // The text watcher is on the same thread so we don't need to worry about a race
            selectingPreset = false;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

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
            if(!selectingPreset && preset != Setting.ApnPreset.CUSTOM) {
                preset = Setting.ApnPreset.CUSTOM;
                spinPreset.setSelection(0);
            }
            getButton(BUTTON_POSITIVE).setEnabled(txtMmsc.getText().length() != 0);
        }
    };

    private final OnClickListener onSubmit = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String mmsc = txtMmsc.getText().toString();
            if(mmsc.isEmpty()) {
                return;
            }
            String proxyHost = txtProxyHostname.getText().toString();
            int proxyPort = -1;
            String proxyPortString = txtProxyPort.getText().toString();
            if(!proxyPortString.isEmpty()) {
                try {
                    proxyPort = Integer.parseInt(proxyPortString);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
            prefs.edit().putInt(Setting.MMS_APN_SPLICING_APN_CONFIG_PRESET.toString(), preset.toInt())
                    .putString(Setting.MMS_APN_SPLICING_APN_CONFIG_MMSC.toString(), mmsc)
                    .putString(Setting.MMS_APN_SPLICING_APN_CONFIG_PROXY_HOSTNAME.toString(), proxyHost)
                    .putInt(Setting.MMS_APN_SPLICING_APN_CONFIG_PROXY_PORT.toString(), proxyPort).apply();
            SettingsActivity.SettingsFragment.updateMmsApnConfigSummary(settingPref, preset);
        }
    };
}
