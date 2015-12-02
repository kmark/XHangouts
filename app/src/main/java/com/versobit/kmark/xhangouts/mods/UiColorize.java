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

package com.versobit.kmark.xhangouts.mods;

import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.Module;
import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.Setting;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.IXUnhook;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_RES_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public final class UiColorize extends Module {

    private static final String[] HANGOUTS_QUANTUM_COLOR_SUFFIXES = {"50", "100", "200", "300",
            "400", "500", "600", "700", "800", "900", "A100", "A200", "A400", "A700"};

    private static final String ANDROID_GRAPHICS_BITMAPFACTORY_DECODERESOURCE = "decodeResource";

    private static final String HANGOUTS_COLOR_FAB = "fab_hangouts_primary_color";
    private static final String HANGOUTS_COLOR_ONGOING_BG = "ongoing_hangout_background";
    private static final String HANGOUTS_COLOR_PROMO_ELIG = "hangout_fmf_in_call_promo_eligible";
    private static final String HANGOUTS_COLOR_PRIMARY = "primary";
    private static final String HANGOUTS_COLOR_PRIMARY_DARK = "primary_dark";
    private static final String HANGOUTS_COLOR_QUANTUM_GOOGGREEN = "quantum_googgreen";

    private static final String HANGOUTS_DRAWABLE_JHPS = "join_hangout_pressed_state";
    private static final String HANGOUTS_DRAWABLE_JHAS = "join_hangout_active_state";
    private static final String HANGOUTS_DRAWABLE_ONGOING_BG = "hangout_ongoing_bg";
    private static final String HANGOUTS_DRAWABLE_ONGOING_BGP = "hangout_ongoing_bg_pressed";
    private static final String HANGOUTS_DRAWABLE_AB_TAB = "action_bar_tab";
    private static final float HANGOUTS_DRAWABLE_AB_TAB_HUE = ColorUtils.hueFromRgb(0xff27541b);
    private static final String HANGOUTS_DRAWABLE_DEFAULT_AVATAR = "default_avatar";

    private static final int RES_ID_UNSET = 0;
    private static int resDefaultAvatar = RES_ID_UNSET;
    private String modulePath = null;

    public UiColorize(Config config) {
        super(UiColorize.class.getSimpleName(), config);
    }

    @Override
    public void init(IXposedHookZygoteInit.StartupParam startup) {
        modulePath = startup.modulePath;
    }

    @Override
    public IXUnhook[] hook(ClassLoader loader) {
        return new IXUnhook[]{
                findAndHookMethod(BitmapFactory.class,
                        ANDROID_GRAPHICS_BITMAPFACTORY_DECODERESOURCE,
                        Resources.class, int.class, decodeResource)
        };
    }

    // Overrides BitmapFactory.decodeResource to replace the default avatar bitmap
    private final XC_MethodHook decodeResource = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Resources res = (Resources) param.args[0];
            int id = (int) param.args[1];
            if (resDefaultAvatar == RES_ID_UNSET) {
                resDefaultAvatar = res.getIdentifier(HANGOUTS_DRAWABLE_DEFAULT_AVATAR, "drawable", HANGOUTS_RES_PKG_NAME);
            }
            if (id == resDefaultAvatar) {
                //noinspection ConstantConditions
                param.setResult(((BitmapDrawable) res.getDrawable(id)).getBitmap());
            }
        }
    };

    private static int getColorFromResources(Resources res, String name) {
        return res.getColor(res.getIdentifier(name, "color", HANGOUTS_RES_PKG_NAME));
    }

    @Override
    public void resources(XResources res) {
        debug(config.appColor.name());

        if (config.appColor == Setting.AppColor.GOOGLE_GREEN) {
            return;
        }

        // Handle any custom DPI that Hangouts might be set to
        XModuleResources xModRes = XModuleResources.createInstance(modulePath, null);
        final int HANGOUTS_DRAWABLE_CUSTOM_AVATAR;
        final int hangoutsDPI = res.getDisplayMetrics().densityDpi;
        if (hangoutsDPI <= 160) {
            HANGOUTS_DRAWABLE_CUSTOM_AVATAR = res.addResource(xModRes, R.drawable.avatar_mdpi);
        } else if (hangoutsDPI <= 240) {
            HANGOUTS_DRAWABLE_CUSTOM_AVATAR = res.addResource(xModRes, R.drawable.avatar_hdpi);
        } else if (hangoutsDPI <= 320) {
            HANGOUTS_DRAWABLE_CUSTOM_AVATAR = res.addResource(xModRes, R.drawable.avatar_xhdpi);
        } else {
            HANGOUTS_DRAWABLE_CUSTOM_AVATAR = res.addResource(xModRes, R.drawable.avatar_xxhdpi);
        }

        // The resource name prefix representing the desired color (source)
        String fromPrefix = config.appColor.getPrefix();
        // The default GOOGLE_GREEN color we're replacing (destination)
        String toPrefix = Setting.AppColor.GOOGLE_GREEN.getPrefix();

        int totalColors = HANGOUTS_QUANTUM_COLOR_SUFFIXES.length;
        // Some colors are without accents, so we subtract them out
        if (config.appColor == Setting.AppColor.BROWN || config.appColor == Setting.AppColor.GREY ||
                config.appColor == Setting.AppColor.BLUE_GREY) {
            totalColors -= 4;
        }
        // Hold onto the found colors so we can use them afterwards
        final int[] appColors = new int[totalColors];
        // Loop over every available quantum color, replacing them
        for (int i = 0; i < totalColors; i++) {
            appColors[i] = getColorFromResources(res, fromPrefix + HANGOUTS_QUANTUM_COLOR_SUFFIXES[i]);
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "color",
                    toPrefix + HANGOUTS_QUANTUM_COLOR_SUFFIXES[i], appColors[i]);
        }

        // The above replacements do not style everything. Some manual fixes.
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", HANGOUTS_DRAWABLE_JHPS, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(appColors[3]);
            }
        });
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", HANGOUTS_DRAWABLE_JHAS, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(appColors[4]);
            }
        });

        final Drawable actBarTab = res.getDrawable(
                res.getIdentifier(HANGOUTS_DRAWABLE_AB_TAB, "drawable", HANGOUTS_RES_PKG_NAME)
        );
        final float hueDiff = ColorUtils.hueFromRgb(appColors[5]) - HANGOUTS_DRAWABLE_AB_TAB_HUE;
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", HANGOUTS_DRAWABLE_AB_TAB, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                Drawable coloredTab = actBarTab.mutate().getConstantState().newDrawable();
                coloredTab.setColorFilter(ColorUtils.adjustHue(hueDiff));
                return coloredTab;
            }
        });

        // Fixes the send button color
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", HANGOUTS_COLOR_FAB,
                appColors[5]);

        // Fixes "Sending as <number>" / Ongoing call bar on 4.x
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", HANGOUTS_COLOR_ONGOING_BG,
                appColors[5]);

        // Fixes "Sending as <number>" / Ongoing call bar on 5.x
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", HANGOUTS_COLOR_PROMO_ELIG,
                appColors[5]);
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", HANGOUTS_DRAWABLE_ONGOING_BG, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(appColors[5]);
            }
        });
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", HANGOUTS_DRAWABLE_ONGOING_BGP, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(appColors[5]);
            }
        });

        // This is all to colorize the default green contact avatar.
        resDefaultAvatar = res.getIdentifier(HANGOUTS_DRAWABLE_DEFAULT_AVATAR, "drawable", HANGOUTS_RES_PKG_NAME);
        final Bitmap coloredDefaultAvatar;
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inMutable = true;
            coloredDefaultAvatar = BitmapFactory.decodeResource(res, resDefaultAvatar, options);

            Canvas canvas = new Canvas(coloredDefaultAvatar);
            canvas.setDensity(hangoutsDPI);
            canvas.drawColor(appColors[7]);

            // BitmapFactory.decodeResource doesn't work here
            Bitmap avatar = ((BitmapDrawable) res.getDrawable(HANGOUTS_DRAWABLE_CUSTOM_AVATAR)).getBitmap();
            canvas.drawBitmap(avatar, 0, 0, null);
            avatar.recycle();
        }
        res.setReplacement(resDefaultAvatar, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                return new BitmapDrawable(xResources, coloredDefaultAvatar);
            }
        });

        // Fixes status bar on 5.x
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", HANGOUTS_COLOR_PRIMARY, appColors[5]);
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", HANGOUTS_COLOR_PRIMARY_DARK, appColors[7]);
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", HANGOUTS_COLOR_QUANTUM_GOOGGREEN, appColors[5]);
    }

    private static final class ColorUtils {
        // Thanks to Richard Lalancette at Stack Overflow and others for putting together adjustHue
        // http://stackoverflow.com/a/7917978/238374
        // https://groups.google.com/d/msg/android-developers/niFcg8OBmVM/Bj1j9s1cvFEJ
        // http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
        private static ColorFilter adjustHue(float value) {
            ColorMatrix cm = new ColorMatrix();
            adjustHue(cm, value);
            return new ColorMatrixColorFilter(cm);
        }

        private static void adjustHue(ColorMatrix cm, float value) {
            value = cleanValue(value, 180f) / 180f * (float) Math.PI;

            if (value == 0) {
                return;
            }
            float cosVal = (float) Math.cos(value);
            float sinVal = (float) Math.sin(value);
            float lumR = 0.213f;
            float lumG = 0.715f;
            float lumB = 0.072f;
            float[] mat = new float[]{
                    lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                    lumG + cosVal * (-lumG) + sinVal * (-lumG),
                    lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                    lumR + cosVal * (-lumR) + sinVal * (0.143f),
                    lumG + cosVal * (1 - lumG) + sinVal * (0.140f),
                    lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                    lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                    lumG + cosVal * (-lumG) + sinVal * (lumG),
                    lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0,
                    0f, 0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 0f, 1f
            };
            cm.postConcat(new ColorMatrix(mat));
        }

        // https://groups.google.com/d/msg/android-developers/niFcg8OBmVM/zRC-NNKSSfAJ
        private static float cleanValue(float p_val, float p_limit) {
            return Math.min(p_limit, Math.max(-p_limit, p_val));
        }

        // Retrieves the hue value in degrees from a packed (A)RGB color.
        // Adapted from a C algorithm by Eugene Vishnevsky.
        // http://www.cs.rit.edu/~ncs/color/t_convert.html
        private static float hueFromRgb(int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            float min = Math.min(Math.min(r, g), b);
            float max = Math.max(Math.max(r, g), b);
            float delta = max - min;

            if (max == 0x00 || min == 0xFF) {
                // Black or white
                return 0;
            }

            float h;

            if (r == max) {
                // Between yellow and magenta
                h = (g - b) / delta;
            } else if (g == max) {
                // Between cyan and yellow
                h = 2 + (b - r) / delta;
            } else {
                // Between magenta and cyan
                h = 4 + (r - g) / delta;
            }

            // Degrees
            h *= 60;
            if (h < 0) {
                h += 360;
            }

            return h;
        }
    }

}