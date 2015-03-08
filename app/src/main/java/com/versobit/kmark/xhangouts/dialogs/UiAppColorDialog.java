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

public class UiAppColorDialog extends AlertDialog {

    private static final Setting.AppColor[] colors = Setting.AppColor.values();
    private final String[] colorNames;
    final private Preference settingPref;
    final private SharedPreferences prefs;
    final private LayoutInflater inflater;

    public UiAppColorDialog(Preference settingPref) {
        super(settingPref.getContext());
        this.colorNames = getContext().getResources().getStringArray(R.array.pref_ui_app_color_titles);
        this.settingPref = settingPref;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.inflater = LayoutInflater.from(getContext());
    }

    @Override @SuppressLint("InflateParams")
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.dialog_ui_app_color, null);
        GridView grid = (GridView)v.findViewById(R.id.dialog_ui_app_color_grid);
        grid.setAdapter(new AppColorAdapter());
        setTitle(R.string.pref_title_ui_app_color);
        setView(v);
        super.onCreate(savedInstanceState);
    }

    private View.OnClickListener onColorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ViewHolder tag = (ViewHolder)v.getTag();
            prefs.edit().putInt(Setting.UI_APP_COLOR.toString(), tag.color.toInt()).apply();
            SettingsActivity.SettingsFragment.updateUiAppColorSummary(settingPref, tag.color);
            dismiss();
        }
    };

    private class AppColorAdapter extends BaseAdapter {

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

    private static class ViewHolder {
        Setting.AppColor color;
        View root;
        View colorView;
        TextView text;
    }
}
