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

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.versobit.kmark.xhangouts.dialogs.AboutDialog;
import com.versobit.kmark.xhangouts.dialogs.ISettingsPrefDialog;
import com.versobit.kmark.xhangouts.dialogs.MmsApnConfigDialog;
import com.versobit.kmark.xhangouts.dialogs.MmsScaleDialog;
import com.versobit.kmark.xhangouts.dialogs.MmsTypeQualityDialog;
import com.versobit.kmark.xhangouts.dialogs.UiAppColorDialog;

final public class SettingsActivity extends PreferenceActivity {

    private static final String ALIAS = BuildConfig.APPLICATION_ID + ".SettingsActivityLauncher";

    static void setDefaultPreferences(Context ctx) {
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_mms, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_ui, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_about, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getFragmentManager().findFragmentById(android.R.id.content) == null) {
            //noinspection ConstantConditions
            if(!XApp.isActive()) {
                Toast.makeText(this, R.string.warning_not_loaded, Toast.LENGTH_LONG).show();
            }
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static final class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceCategory header;

            // Add general preferences.
            addPreferencesFromResource(R.xml.pref_general);

            findPreference(Setting.LAUNCHER_ICON.toString()).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity(), ALIAS),
                            (boolean)newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                    );
                    return true;
                }
            });

            // Add MMS preferences, and a corresponding header.
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_mms);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_mms);

            Preference scale = findPreference(Setting.MMS_SCALE_PREFKEY.toString());
            Preference image = findPreference(Setting.MMS_IMAGE_PREFKEY.toString());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int scaleWidth = prefs.getInt(Setting.MMS_SCALE_WIDTH.toString(), 1024);
            int scaleHeight = prefs.getInt(Setting.MMS_SCALE_HEIGHT.toString(), 1024);
            updateMmsScaleSummary(scale, scaleWidth, scaleHeight);

            scale.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MmsScaleDialog().setSettingPref(preference)
                            .show(getFragmentManager(), MmsScaleDialog.FRAGMENT_TAG);
                    return true;
                }
            });
            updatePrefDialog(MmsScaleDialog.FRAGMENT_TAG, scale);

            Setting.ImageFormat format = Setting.ImageFormat.fromInt(prefs.getInt(Setting.MMS_IMAGE_TYPE.toString(), Setting.ImageFormat.JPEG.toInt()));
            int quality = prefs.getInt(Setting.MMS_IMAGE_QUALITY.toString(), 80);
            updateMmsTypeQualitySummary(image, format, quality);

            image.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MmsTypeQualityDialog().setSettingPref(preference)
                            .show(getFragmentManager(), MmsTypeQualityDialog.FRAGMENT_TAG);
                    return true;
                }
            });
            updatePrefDialog(MmsTypeQualityDialog.FRAGMENT_TAG, image);

            bindPreferenceSummaryToValue(findPreference(Setting.MMS_ROTATE_MODE.toString()));

            Preference apnConfig = findPreference(Setting.MMS_APN_SPLICING_APN_CONFIG_PREFKEY.toString());
            Setting.ApnPreset apnPreset = Setting.ApnPreset.fromInt(prefs.getInt(Setting.MMS_APN_SPLICING_APN_CONFIG_PRESET.toString(), Setting.ApnPreset.CUSTOM.toInt()));
            updateMmsApnConfigSummary(apnConfig, apnPreset);

            apnConfig.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MmsApnConfigDialog().setSettingPref(preference)
                            .show(getFragmentManager(), MmsApnConfigDialog.FRAGMENT_TAG);
                    return true;
                }
            });
            updatePrefDialog(MmsApnConfigDialog.FRAGMENT_TAG, apnConfig);

            // Add UI Tweaks preferences, and a corresponding header.
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_ui);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_ui);
            bindPreferenceSummaryToValue(findPreference(Setting.UI_ENTER_KEY.toString()));
            Preference colorConfig = findPreference(Setting.UI_APP_COLOR.toString());
            updateUiAppColorSummary(colorConfig,
                    Setting.AppColor.fromInt(prefs.getInt(Setting.UI_APP_COLOR.toString(), 7)));

            colorConfig.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new UiAppColorDialog().setSettingPref(preference)
                            .show(getFragmentManager(), UiAppColorDialog.FRAGMENT_TAG);
                    return true;
                }
            });
            updatePrefDialog(UiAppColorDialog.FRAGMENT_TAG, colorConfig);


            // Add About preferences, and a corresponding header.
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_about);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_about);
            setupVersionPreference(findPreference(Setting.ABOUT_VERSION.toString()));
        }

        private void updatePrefDialog(String tag, Preference pref) {
            Fragment frag = getFragmentManager().findFragmentByTag(tag);
            if(frag != null && frag instanceof ISettingsPrefDialog) {
                ((ISettingsPrefDialog)frag).setSettingPref(pref);
            }
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);

                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of name below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see #sBindPreferenceSummaryToValueListener
         */
        private static void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        private void setupVersionPreference(final Preference preference) {
            final Context ctx = getActivity();
            preference.setTitle(ctx.getString(R.string.pref_title_about_version, BuildConfig.VERSION_NAME));
            String gHangoutsVerName = "???";
            int gHangoutsVerCode = 0;
            try {
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(XHangouts.HANGOUTS_PKG_NAME, 0);
                gHangoutsVerName = pi.versionName;
                gHangoutsVerCode = pi.versionCode;

            } catch (PackageManager.NameNotFoundException ex) {
                //
            }
            //noinspection ConstantConditions
            String loaded = XApp.isActive() ? ctx.getString(R.string.pref_title_about_version_loaded) :
                    ctx.getString(R.string.pref_title_about_version_notloaded);
            preference.setSummary(ctx.getString(R.string.pref_desc_about_version, gHangoutsVerName, gHangoutsVerCode, loaded));
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AboutDialog().show(getFragmentManager(), AboutDialog.FRAGMENT_TAG);
                    return false;
                }
            });
        }

        public static void updateMmsScaleSummary(final Preference preference, final int width, final int height) {
            preference.setSummary(preference.getContext().getString(R.string.pref_desc_mms_scale, width, height));
        }

        public static void updateMmsTypeQualitySummary(final Preference preference, final Setting.ImageFormat format, final int quality) {
            Context ctx = preference.getContext();
            String strQuality = format == Setting.ImageFormat.PNG ? ctx.getString(R.string.dialog_mms_type_quality_lossless) : String.valueOf(quality);
            preference.setSummary(ctx.getString(R.string.pref_desc_mms_image_type, format.toString(), strQuality.toLowerCase()));
        }

        public static void updateMmsApnConfigSummary(final Preference preference, final Setting.ApnPreset preset) {
            preference.setSummary(preset.toString());
        }

        public static void updateUiAppColorSummary(final Preference preference, final Setting.AppColor color) {
            String[] names = preference.getContext().getResources().getStringArray(R.array.pref_ui_app_color_titles);
            preference.setSummary(names[color.toInt()]);
        }
    }
}
