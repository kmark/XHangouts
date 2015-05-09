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

package com.versobit.kmark.xhangouts.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.SettingsActivity;

public final class UiAppColorDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_uiappcolor";

    private static final Setting.AppColor[] colors = Setting.AppColor.values();
    private String[] colorNames = null;
    private Preference settingPref = null;
    private SharedPreferences prefs = null;
    private LayoutInflater inflater = null;

    public UiAppColorDialog setSettingPref(Preference settingPref) {
        this.settingPref = settingPref;
        return this;
    }

    @Override @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        colorNames = getResources().getStringArray(R.array.pref_ui_app_color_titles);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        inflater = getActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.dialog_ui_app_color, null);

        GridView grid = (GridView)v.findViewById(R.id.dialog_ui_app_color_grid);
        grid.setAdapter(new AppColorAdapter());

        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setTitle(R.string.pref_title_ui_app_color)
                .create();
    }

    private final View.OnClickListener onColorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ViewHolder tag = (ViewHolder)v.getTag();
            prefs.edit().putInt(Setting.UI_APP_COLOR.toString(), tag.color.toInt()).apply();
            SettingsActivity.SettingsFragment.updateUiAppColorSummary(settingPref, tag.color);
            dismiss();
        }
    };

    private final class AppColorAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return colors.length;
        }

        @Override
        public Object getItem(int position) {
            return colors[position];
        }

        @Override
        public long getItemId(int position) {
            return colors[position].toInt();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if(convertView == null) {
                convertView = inflater.inflate(R.layout.dialog_ui_app_color_item, parent, false);
                holder = new ViewHolder();
                holder.root = convertView.findViewById(R.id.dialog_ui_app_color_item_root);
                holder.root.setOnClickListener(onColorClickListener);
                holder.colorView = convertView.findViewById(R.id.dialog_ui_app_color_item_color);
                holder.text = (TextView)convertView.findViewById(R.id.dialog_ui_app_color_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            if(colors[position] != null && colorNames[position] != null) {
                holder.color = colors[position];
                holder.colorView.setBackgroundColor(colors[position].getBaseColor());
                holder.text.setText(colorNames[position]);
            }

            return convertView;
        }
    }

    private final static class ViewHolder {
        Setting.AppColor color;
        View root;
        View colorView;
        TextView text;
    }
}
