/*
 * Copyright (C) 2014-2016 Kevin Mark
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
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
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.versobit.kmark.xhangouts.Config;
import com.versobit.kmark.xhangouts.R;
import com.versobit.kmark.xhangouts.Setting;
import com.versobit.kmark.xhangouts.XHangouts;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

import static com.versobit.kmark.xhangouts.XHangouts.HANGOUTS_RES_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setStaticIntField;

@SuppressWarnings("deprecation")
public final class UiColorize {

    private static final String[] HANGOUTS_QUANTUM_COLOR_SUFFIXES = {"50", "100", "200", "300",
            "400", "500", "600", "700", "800", "900", "A100", "A200", "A400", "A700"};

    private static final String ANDROID_GRAPHICS_BITMAPFACTORY_DECODERESOURCE = "decodeResource";

    private static final String HANGOUTS_COLOR_FAB = "fab_hangouts_primary_color";
    private static final String HANGOUTS_COLOR_INDICATOR = "indicator_color";
    private static final String HANGOUTS_COLOR_ONGOING_BG = "ongoing_hangout_background";
    private static final String HANGOUTS_COLOR_PRIMARY = "primary";
    private static final String HANGOUTS_COLOR_PRIMARY_DARK = "primary_dark";
    private static final String HANGOUTS_COLOR_PRIMARY_HANGOUTS = "hangouts_primary_color";
    private static final String HANGOUTS_COLOR_QUANTUM_GOOGGREEN = "quantum_googgreen";

    private static final String HANGOUTS_COLOR_BUBBLE_IN = "incoming_conversation_bubble_background";
    private static final String HANGOUTS_COLOR_BUBBLE_IN_OTR = "incoming_conversation_bubble_background_otr";
    private static final String HANGOUTS_COLOR_FONT_IN = "realtimechat_message_text_incoming";
    private static final String HANGOUTS_COLOR_FONT_IN_OTR = "realtimechat_message_text_incoming_otr";
    private static final String HANGOUTS_COLOR_LINK_IN = "realtimechat_message_link_incoming";
    private static final String HANGOUTS_COLOR_LINK_IN_OTR = "realtimechat_message_link_incoming_otr";

    private static final String HANGOUTS_COLOR_BUBBLE_OUT = "outgoing_conversation_bubble_background";
    private static final String HANGOUTS_COLOR_BUBBLE_OUT_OTR = "outgoing_conversation_bubble_background_otr";
    private static final String HANGOUTS_COLOR_FONT_OUT = "realtimechat_message_text_outgoing";
    private static final String HANGOUTS_COLOR_FONT_OUT_OTR = "realtimechat_message_text_outgoing_otr";
    private static final String HANGOUTS_COLOR_LINK_OUT = "realtimechat_message_link_outgoing";
    private static final String HANGOUTS_COLOR_LINK_OUT_OTR = "realtimechat_message_link_outgoing_otr";

    private static final String HANGOUTS_DRAWABLE_GOOGLE = "googlelogo_dark20_color_132x44";
    private static final String HANGOUTS_DRAWABLE_GOOGLE_LARGE = "googlelogo_dark20_color_184x60";
    private static final String HANGOUTS_DRAWABLE_JHPS = "join_hangout_pressed_state";
    private static final String HANGOUTS_DRAWABLE_JHAS = "join_hangout_active_state";
    private static final String HANGOUTS_DRAWABLE_ONGOING_BG = "hangout_ongoing_bg";
    private static final String HANGOUTS_DRAWABLE_ONGOING_BGP = "hangout_ongoing_bg_pressed";
    private static final String HANGOUTS_DRAWABLE_AB_TAB = "action_bar_tab";
    private static final float HANGOUTS_DRAWABLE_AB_TAB_HUE = ColorUtils.hueFromRgb(0xff27541b);
    private static final String HANGOUTS_DRAWABLE_DEFAULT_AVATAR = "default_avatar";
    private static final String HANGOUTS_DRAWABLE_DEFAULT_AVATAR_LARGE = "default_avatar_large";

    private static final String HANGOUTS_LAYOUT_DIALER = "call_contact_picker_fragment";

    private static final int RES_ID_UNSET = 0;
    private static int resDefaultAvatar = RES_ID_UNSET;
    private static int resDefaultAvatarLarge = RES_ID_UNSET;
    private static int sysDpi = 0;
    private static int UNREAD_COLOR = 0xffffffff;

    private static final int COLOR_GROUP_1 = 0xffffffff; // Text
    private static final int COLOR_GROUP_2 = 0xff808080; // Timestamps, secondary text color & icon colors
    private static int COLOR_GROUP_3 = 0xff212121; // Main background color
    private static int COLOR_GROUP_4 = 0xff303030; // Secondary background color
    private static final int COLOR_GROUP_5 = 0xff424242; // Dividers and incoming message bubbles
    private static final int COLOR_GROUP_6 = 0xff000000; // Floating action button text color

    private static final String HANGOUTS_ONGOING_COLOR = "fxl";
    private static final String HANGOUTS_ONGOING_LIST = "fuw";
    private static final String HANGOUTS_RECENT_CALLS = "fut";
    private static final String HANGOUTS_CONVO_LIST = "com.google.android.apps.hangouts.views.ConversationListItemView";
    private static final String HANGOUTS_SNACKBAR = "com.google.android.libraries.quantum.snackbar.Snackbar";

    private static final String HANGOUTS_ONGOING_COLOR_ID = "dY"; // quantum_bluegrey600
    private static final String HANGOUTS_ONGOING_SET_COLOR = "y"; // private void y()

    private static final String HANGOUTS_A = "a";
    private static final String HANGOUTS_B = "b";
    private static final String HANGOUTS_C = "c";
    // FIXME: Can we get away with a Weak/Soft Reference here?
    private static Context context;


    public static void initZygote() {
        sysDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
    }

    public static void handleLoadPackage(final Config config, final ClassLoader loader) {
        if (!config.modEnabled || !config.theming) {
            return;
        }

        // Overrides BitmapFactory.decodeResource to replace the default avatar bitmap
        findAndHookMethod(BitmapFactory.class, ANDROID_GRAPHICS_BITMAPFACTORY_DECODERESOURCE,
                Resources.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Resources res = (Resources) param.args[0];
                int id = (int) param.args[1];
                if (resDefaultAvatar == RES_ID_UNSET) {
                    resDefaultAvatar = res.getIdentifier(HANGOUTS_DRAWABLE_DEFAULT_AVATAR,
                            "drawable", HANGOUTS_RES_PKG_NAME);
                }
                if (resDefaultAvatarLarge == RES_ID_UNSET) {
                    resDefaultAvatarLarge = res.getIdentifier(HANGOUTS_DRAWABLE_DEFAULT_AVATAR_LARGE,
                            "drawable", HANGOUTS_RES_PKG_NAME);
                }
                if (id == resDefaultAvatar) {
                    //noinspection ConstantConditions
                    param.setResult(((BitmapDrawable) res.getDrawable(id)).getBitmap());
                } else if (id == resDefaultAvatarLarge) {
                    //noinspection ConstantConditions
                    param.setResult(((BitmapDrawable) res.getDrawable(id)).getBitmap());
                }
            }
        });

        final Class cConversationList = findClass(HANGOUTS_CONVO_LIST, loader);
        findAndHookMethod(cConversationList, HANGOUTS_A, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (config.darkTheme) {
                    // Color the contact name
                    if ((int) param.args[2] == Typeface.BOLD) {
                        param.args[0] = config.highlightUnread ? UNREAD_COLOR : COLOR_GROUP_1;
                    } else {
                        param.args[0] = COLOR_GROUP_1;
                    }
                    // Color the message snippet and timestamp
                    param.args[1] = COLOR_GROUP_2;

                } else {
                    // Highlight unread messages for the light theme too
                    if (config.highlightUnread) {
                        if ((int) param.args[2] == Typeface.BOLD) {
                            param.args[0] = UNREAD_COLOR;
                        }
                    }
                }
            }
        });

        if (config.darkTheme) {
            // Set the color of an ongoing call
            final Class ongoingConvoList = findClass(HANGOUTS_ONGOING_LIST, loader);
            findAndHookConstructor(ongoingConvoList, Context.class, AttributeSet.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // We need the context to get the resource ID's
                    context = (Context) param.args[0];
                }
            });
            final Class ongoingColor = findClass(HANGOUTS_ONGOING_COLOR, loader);
            findAndHookMethod(ongoingConvoList, HANGOUTS_ONGOING_SET_COLOR, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // Set a new color
                    int id = context.getResources().getIdentifier("quantum_grey800", "color", HANGOUTS_RES_PKG_NAME);
                    setStaticIntField(ongoingColor, HANGOUTS_ONGOING_COLOR_ID, id);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Restore the original color
                    int id = context.getResources().getIdentifier("quantum_bluegrey600", "color", HANGOUTS_RES_PKG_NAME);
                    setStaticIntField(ongoingColor, HANGOUTS_ONGOING_COLOR_ID, id);
                }
            });

            // Various conversation list icons
            findAndHookMethod(cConversationList, HANGOUTS_A, Drawable.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ((Drawable) param.args[0]).setColorFilter(COLOR_GROUP_2, PorterDuff.Mode.SRC_IN);
                }
            });
            findAndHookMethod(cConversationList, HANGOUTS_B, Drawable.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ((Drawable) param.args[0]).setColorFilter(COLOR_GROUP_2, PorterDuff.Mode.SRC_IN);
                }
            });
            findAndHookMethod(cConversationList, HANGOUTS_C, Drawable.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ((Drawable) param.args[0]).setColorFilter(COLOR_GROUP_2, PorterDuff.Mode.SRC_IN);
                }
            });

            // Recent calls name & number
            final Class cRecentCalls = findClass(HANGOUTS_RECENT_CALLS, loader);
            findAndHookMethod(cRecentCalls, HANGOUTS_A, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout root = (FrameLayout) param.thisObject;
                    int id = root.getContext().getResources().getIdentifier("name", "id", HANGOUTS_RES_PKG_NAME);
                    ((TextView) root.findViewById(id)).setTextColor(COLOR_GROUP_1);
                }
            });

            // Snackbar text color
            final Class cSnackbar = findClass(HANGOUTS_SNACKBAR, loader);
            findAndHookMethod(cSnackbar, HANGOUTS_A, ColorStateList.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ((TextView) getObjectField(param.thisObject, HANGOUTS_B)).setTextColor(COLOR_GROUP_1);
                }
            });

        }
    }

    private static int getColorFromResources(Resources res, String name) {
        return res.getColor(res.getIdentifier(name, "color", HANGOUTS_RES_PKG_NAME));
    }

    private static void replaceColor(XResources res, String name, int color) {
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "color", name, color);
    }

    private static void replaceDrawableColor(XResources res, String name, final int color) {
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", name, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(color);
            }
        });
    }

    private static void replaceAvatarColor(XResources res, String name, int customResId, int dpi, int color) {
        int existingResID = res.getIdentifier(name, "drawable", HANGOUTS_RES_PKG_NAME);
        final Bitmap customBitmap;
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inMutable = true;
            customBitmap = BitmapFactory.decodeResource(res, existingResID, options);

            Canvas canvas = new Canvas(customBitmap);
            canvas.setDensity(dpi);
            canvas.drawColor(color);

            // BitmapFactory.decodeResource doesn't work here
            // noinspection ConstantConditions
            Bitmap avatar = ((BitmapDrawable) res.getDrawable(customResId)).getBitmap();
            canvas.drawBitmap(avatar, 0, 0, null);
            avatar.recycle();
        }
        res.setReplacement(existingResID, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                // If copies aren't made then Hangouts sometimes force closes
                return new BitmapDrawable(xResources, customBitmap.copy(Bitmap.Config.ARGB_8888, false));
            }
        });
    }

    private static void replaceLayoutBackgroundColor(XResources res, String layoutName, final String ResId, final int color) {
        res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", layoutName, new XC_LayoutInflated() {
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                if (ResId == null) {
                    liparam.view.setBackgroundColor(color);
                } else {
                    liparam.view.findViewById(liparam.res.getIdentifier(ResId, "id", HANGOUTS_RES_PKG_NAME))
                            .setBackgroundColor(color);
                }
            }
        });
    }

    private static void themeListItemView(XResources res, String layoutName, final boolean extraLine) {
        res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", layoutName, new XC_LayoutInflated() {
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                ((LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("icon", "id",
                        HANGOUTS_RES_PKG_NAME)).getParent()).setBackgroundColor(COLOR_GROUP_5);
                ((ImageView) liparam.view.findViewById(liparam.res.getIdentifier("icon", "id",
                        HANGOUTS_RES_PKG_NAME))).setColorFilter(COLOR_GROUP_1, PorterDuff.Mode.SRC_IN);
                ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("text", "id",
                        HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_1);
                if (extraLine) {
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("byline", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_2);
                }
            }
        });
    }

    private static void setTextColor(XResources res, String layoutName, final String ResId, final int color) {
        res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", layoutName, new XC_LayoutInflated() {
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                ((TextView) liparam.view.findViewById(liparam.res.getIdentifier(ResId, "id",
                        HANGOUTS_RES_PKG_NAME))).setTextColor(color);
            }
        });
    }


    @SuppressLint("DefaultLocale")
    public static void handleInitPackageResources(final Config config, final XResources res) {
        if (!config.modEnabled || !config.theming) {
            return;
        }

        XHangouts.debug(config.appColor.name());

        // An option to use black backgrounds
        if (config.blackBackgrounds) {
            COLOR_GROUP_3 = 0xff000000;
            COLOR_GROUP_4 = 0xff212121;
        }

        // Handle any custom DPI that Hangouts might be set to
        final XModuleResources moduleRes = XModuleResources.createInstance(XHangouts.modulePath, res);

        final int hangoutsDpi = res.getDisplayMetrics().densityDpi;
        XHangouts.debug(String.format("System: %d / Hangouts: %d", sysDpi, hangoutsDpi));

        // Add our drawables
        final int smallAvatarId = XResources.getFakeResId(moduleRes, R.drawable.avatar);
        res.setReplacement(smallAvatarId, moduleRes.fwd(R.drawable.avatar));
        final int largeAvatarId = XResources.getFakeResId(moduleRes, R.drawable.avatar_large);
        res.setReplacement(largeAvatarId, moduleRes.fwd(R.drawable.avatar_large));

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

        // Set the unread conversation color
        UNREAD_COLOR = appColors[5];

        // The above replacements do not style everything. Some manual fixes.
        replaceDrawableColor(res, HANGOUTS_DRAWABLE_JHPS, appColors[3]);
        replaceDrawableColor(res, HANGOUTS_DRAWABLE_JHAS, appColors[4]);

        final Drawable actBarTab = res.getDrawable(
                res.getIdentifier(HANGOUTS_DRAWABLE_AB_TAB, "drawable", HANGOUTS_RES_PKG_NAME)
        );
        final float hueDiff = ColorUtils.hueFromRgb(appColors[5]) - HANGOUTS_DRAWABLE_AB_TAB_HUE;
        res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", HANGOUTS_DRAWABLE_AB_TAB, new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                //noinspection ConstantConditions
                Drawable coloredTab = actBarTab.mutate().getConstantState().newDrawable();
                coloredTab.setColorFilter(ColorUtils.adjustHue(hueDiff));
                return coloredTab;
            }
        });

        // We can't use ic_dialpad_header.png because it contains green and adjusting the hue doesn't work
        final int googleLogo;
        if (sysDpi <= 320) {
            googleLogo = res.getIdentifier(HANGOUTS_DRAWABLE_GOOGLE, "drawable", HANGOUTS_RES_PKG_NAME);
        } else {
            googleLogo = res.getIdentifier(HANGOUTS_DRAWABLE_GOOGLE_LARGE, "drawable", HANGOUTS_RES_PKG_NAME);
        }

        // If we're using the dark theme then we need to lighten the Google logo
        if (config.darkTheme) {
            final Bitmap customLogo;
            {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inMutable = true;
                customLogo = BitmapFactory.decodeResource(res, googleLogo, options);
                Canvas canvas = new Canvas(customLogo);
                canvas.setDensity(hangoutsDpi);
                Paint p = new Paint();
                p.setColorFilter(ColorUtils.adjustBrightness(100));

                // BitmapFactory.decodeResource doesn't work here
                // noinspection ConstantConditions
                Bitmap logo = ((BitmapDrawable) res.getDrawable(googleLogo)).getBitmap();
                for (int i = 0; i < 9; i++) {
                    canvas.drawBitmap(logo, 0, 0, p);
                }
                logo.recycle();
            }
            res.setReplacement(googleLogo, new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                    return new BitmapDrawable(xResources, customLogo);
                }
            });
        }

        res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", HANGOUTS_LAYOUT_DIALER, new XC_LayoutInflated() {
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                ImageView dialpadHeader = (ImageView) liparam.view.findViewById(
                        liparam.res.getIdentifier("dialpad_results_placeholder", "id", HANGOUTS_RES_PKG_NAME));
                dialpadHeader.setBackgroundColor(config.darkTheme ? COLOR_GROUP_3 : COLOR_GROUP_1);
                dialpadHeader.setImageResource(googleLogo);
            }
        });

        // Replace bubble, font and hyperlink colors
        replaceColor(res, HANGOUTS_COLOR_BUBBLE_IN, config.incomingColor);
        replaceColor(res, HANGOUTS_COLOR_BUBBLE_IN_OTR, config.incomingColorOTR);
        replaceColor(res, HANGOUTS_COLOR_FONT_IN, config.incomingFontColor);
        replaceColor(res, HANGOUTS_COLOR_FONT_IN_OTR, config.incomingFontColorOTR);
        replaceColor(res, HANGOUTS_COLOR_LINK_IN, config.incomingLinkColor);
        replaceColor(res, HANGOUTS_COLOR_LINK_IN_OTR, config.incomingLinkColorOTR);
        replaceColor(res, HANGOUTS_COLOR_BUBBLE_OUT, config.themeBubblesLight ? appColors[5] : config.outgoingColor);
        replaceColor(res, HANGOUTS_COLOR_BUBBLE_OUT_OTR, config.themeBubblesLight ? appColors[5] : config.outgoingColorOTR);
        replaceColor(res, HANGOUTS_COLOR_FONT_OUT, config.outgoingFontColor);
        replaceColor(res, HANGOUTS_COLOR_FONT_OUT_OTR, config.outgoingFontColorOTR);
        replaceColor(res, HANGOUTS_COLOR_LINK_OUT, config.outgoingLinkColor);
        replaceColor(res, HANGOUTS_COLOR_LINK_OUT_OTR, config.outgoingLinkColorOTR);

        // Make the dialers dividers a little more consistent
        replaceColor(res, "dialpad_separator_line_color", 0xffcccccc);

        res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "phone_call_dialer_fragment", new XC_LayoutInflated() {
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                LinearLayout parent = (LinearLayout) liparam.view;
                parent.getChildAt(0).getLayoutParams().height = (int) (1.0f * res.getDisplayMetrics().density + 0.5f);
                parent.getChildAt(7).setVisibility(View.GONE);
            }
        });

        // Change some colors and hide the divider
        res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "recent_calls_list_fragment", new XC_LayoutInflated() {
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                if (config.darkTheme) {
                    // clear_recent_calls
                    TextView recentCalls = (TextView) liparam.view.findViewById(liparam.res
                            .getIdentifier("clear_recent_calls", "id", HANGOUTS_RES_PKG_NAME));
                    recentCalls.setTextColor(COLOR_GROUP_1);
                    // recent_clear_text
                    ((TextView) ((RelativeLayout) recentCalls.getParent()).getChildAt(0))
                            .setTextColor(COLOR_GROUP_1);
                    // not_found_hint
                    LinearLayout parent = (LinearLayout) liparam.view.findViewById(liparam.res
                            .getIdentifier("not_found_hint", "id", HANGOUTS_RES_PKG_NAME));
                    ((TextView) parent.getChildAt(1)).setTextColor(COLOR_GROUP_2);
                    ((TextView) parent.getChildAt(2)).setTextColor(COLOR_GROUP_2);
                }
                // The divider
                liparam.view.findViewById(liparam.res.getIdentifier("recent_calls_divider", "id",
                        HANGOUTS_RES_PKG_NAME)).setVisibility(View.INVISIBLE);

            }
        });


        if (config.darkTheme) {
            // Background colors
            replaceLayoutBackgroundColor(res, "conversation_fragment_v2", "list_layout_parent", COLOR_GROUP_3);
            replaceLayoutBackgroundColor(res, "conversation_list_activity", null, COLOR_GROUP_3);
            replaceLayoutBackgroundColor(res, "attachment_picker_fragment", null, COLOR_GROUP_4);


            // Empty list text e.g. no conversations
            setTextColor(res, "conversation_list_fragment", "zero_state_text", COLOR_GROUP_2);


            // Invites text color
            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "invite_list_item_view", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("inviter", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(config.highlightUnread ? UNREAD_COLOR : COLOR_GROUP_1);
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("invite_text", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_1);
                }
            });


            // Divider
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "compose_message_view_bg",
                    config.blackBackgrounds ? moduleRes.fwd(R.drawable.compose_message_view_bg_black) :
                            moduleRes.fwd(R.drawable.compose_message_view_bg));


            // Stickers sliding tab
            replaceLayoutBackgroundColor(res, "sticker_pager_fragment_v2", "sticker_sliding_tabs", COLOR_GROUP_3);


            // Emoji selector border color - we could use 0xff282828 but it looks better without
            replaceColor(res, "emoji_v2_border", COLOR_GROUP_4);


            // Spinner
            final Drawable offlineIcon = res.getDrawable(res.getIdentifier("ic_msg_toggle_gray", "drawable", HANGOUTS_RES_PKG_NAME));
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "ic_msg_toggle_gray", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources res, int id) throws Throwable {
                    //noinspection ConstantConditions
                    Drawable icon = offlineIcon.mutate().getConstantState().newDrawable();
                    icon.setColorFilter(COLOR_GROUP_2, PorterDuff.Mode.SRC_IN);
                    return icon;
                }
            });


            // Arrow under the spinner (quantum_ic_arrow_drop_down_black_18)
            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "transport_spinner_view", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    FrameLayout parent = (FrameLayout) liparam.view;
                    ((ImageView) parent.getChildAt(2)).setColorFilter(COLOR_GROUP_2);
                }
            });


            // Color some icons
            replaceColor(res, "quantum_bluegrey500", COLOR_GROUP_2);
            replaceColor(res, "realtimechat_system_information_foreground", COLOR_GROUP_1);

            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "conversation_list_item_view", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    ((ImageView) liparam.view.findViewById(liparam.res
                            .getIdentifier("conversation_muted_indicator", "id", HANGOUTS_RES_PKG_NAME)))
                            .setColorFilter(COLOR_GROUP_2);
                    ((ImageView) liparam.view.findViewById(liparam.res
                            .getIdentifier("user_blocked_indicator", "id", HANGOUTS_RES_PKG_NAME)))
                            .setColorFilter(COLOR_GROUP_2);
                    ((ImageView) liparam.view.findViewById(liparam.res
                            .getIdentifier("gv_voicemail_indicator", "id", HANGOUTS_RES_PKG_NAME)))
                            .setColorFilter(COLOR_GROUP_2);
                    ((ImageView) liparam.view.findViewById(liparam.res
                            .getIdentifier("hangoutButton", "id", HANGOUTS_RES_PKG_NAME)))
                            .setColorFilter(COLOR_GROUP_1);
                    liparam.view.findViewById(liparam.res
                            .getIdentifier("voicemail_duration_snippet", "id", HANGOUTS_RES_PKG_NAME))
                            .setBackgroundColor(COLOR_GROUP_4);
                    ((TextView) liparam.view.findViewById(liparam.res
                            .getIdentifier("voicemail_duration_snippet", "id", HANGOUTS_RES_PKG_NAME)))
                            .setTextColor(COLOR_GROUP_2);
                    ((TextView) liparam.view.findViewById(liparam.res
                            .getIdentifier("voicemail_message_snippet", "id", HANGOUTS_RES_PKG_NAME)))
                            .setTextColor(COLOR_GROUP_2);
                }
            });


            // Color the status icons
            replaceColor(res, "rich_status_content_color", COLOR_GROUP_1);


            // Sticker color
            final Drawable sticker = res.getDrawable(res.getIdentifier("ic_stickers", "drawable", HANGOUTS_RES_PKG_NAME));
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "ic_stickers", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources res, int id) throws Throwable {
                    //noinspection ConstantConditions
                    Drawable icon = sticker.mutate().getConstantState().newDrawable();
                    icon.setColorFilter(COLOR_GROUP_2, PorterDuff.Mode.SRC_IN);
                    return icon;
                }
            });


            // Attach image action button icon color
            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "fragment_gallery_picker", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    ((ImageView) liparam.view.findViewById(liparam.res
                            .getIdentifier("floating_system_photo_picking_button", "id", HANGOUTS_RES_PKG_NAME)))
                            .setColorFilter(COLOR_GROUP_2);
                }
            });


            // Text input color
            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "compose_message_view_v2", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    EditText msgText = (EditText) liparam.view.findViewById(liparam.res
                            .getIdentifier("message_text", "id", HANGOUTS_RES_PKG_NAME));
                    msgText.setTextColor(COLOR_GROUP_1);
                    msgText.setHintTextColor(COLOR_GROUP_2);
                }
            });


            // Ellipsis color
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "ellipsis_large",
                    moduleRes.fwd(R.drawable.ellipsis_large));
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "ellipsis_medium",
                    moduleRes.fwd(R.drawable.ellipsis_medium));
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "ellipsis_small",
                    moduleRes.fwd(R.drawable.ellipsis_small));


            // Replace bubble, font and hyperlink colors
            replaceColor(res, HANGOUTS_COLOR_BUBBLE_IN, config.incomingDarkColor);
            replaceColor(res, HANGOUTS_COLOR_BUBBLE_IN_OTR, config.incomingDarkColorOTR);
            replaceColor(res, HANGOUTS_COLOR_FONT_IN, config.incomingDarkFontColor);
            replaceColor(res, HANGOUTS_COLOR_FONT_IN_OTR, config.incomingDarkFontColorOTR);
            replaceColor(res, HANGOUTS_COLOR_LINK_IN, config.themeHyperlinks ? appColors[5] : config.incomingDarkLinkColor);
            replaceColor(res, HANGOUTS_COLOR_LINK_IN_OTR, config.themeHyperlinks ? appColors[5] : config.incomingDarkLinkColorOTR);
            replaceColor(res, HANGOUTS_COLOR_BUBBLE_OUT, config.themeBubblesDark ? appColors[5] : config.outgoingDarkColor);
            replaceColor(res, HANGOUTS_COLOR_BUBBLE_OUT_OTR, config.themeBubblesDark ? appColors[5] : config.outgoingDarkColorOTR);
            replaceColor(res, HANGOUTS_COLOR_FONT_OUT, config.outgoingDarkFontColor);
            replaceColor(res, HANGOUTS_COLOR_FONT_OUT_OTR, config.outgoingDarkFontColorOTR);
            replaceColor(res, HANGOUTS_COLOR_LINK_OUT, config.outgoingDarkLinkColor);
            replaceColor(res, HANGOUTS_COLOR_LINK_OUT_OTR, config.outgoingDarkLinkColorOTR);


            // OTR notifications, missed calls etc
            themeListItemView(res, "hangout_event_message_list_item_view", true);
            themeListItemView(res, "otr_modification_message_list_item_view", false);
            themeListItemView(res, "system_message_list_item_view", false);


            // Timestamp color
            setTextColor(res, "message_status", "time", COLOR_GROUP_2);


            // FAB
            replaceColor(res, "fab_hangouts_inactive_color", COLOR_GROUP_3);
            replaceColor(res, "fab_hangouts_image_tint_color", COLOR_GROUP_2);


            // Ongoing call
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "hangout_ongoing_bg_v2",
                    moduleRes.fwd(R.drawable.hangout_ongoing_bg_v2));


            // Theme the snackbars
            replaceLayoutBackgroundColor(res, "conversation_fragment_v2_fauxbar", "snackbar", COLOR_GROUP_4);
            replaceLayoutBackgroundColor(res, "hangouts_snackbar", null, COLOR_GROUP_4);

            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "snackbar", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("message", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_1);
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("action", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(appColors[5]);
                }
            });

            replaceColor(res, "hangout_snackbar_background", COLOR_GROUP_4);
            replaceColor(res, "snackbar_background", COLOR_GROUP_4);


            // Main conversation background color
            replaceLayoutBackgroundColor(res, "babel_home_activity", "drawer_layout", COLOR_GROUP_3);
            replaceLayoutBackgroundColor(res, "babel_home_activity_tabless", "drawer_layout", COLOR_GROUP_3);

            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "bg_item_selectable_conversation_list",
                    config.blackBackgrounds ? moduleRes.fwd(R.drawable.bg_item_selectable_conversation_list_black) :
                            moduleRes.fwd(R.drawable.bg_item_selectable_conversation_list));
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "conversation_list_item_selector",
                    moduleRes.fwd(R.drawable.conversation_list_item_selector));


            // Dialer text color
            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "call_contact_item", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("contact_detail_item", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_2);
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("timestamp_text", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_2);
                    ((TextView) liparam.view.findViewById(liparam.res.getIdentifier("rate_text", "id",
                            HANGOUTS_RES_PKG_NAME))).setTextColor(COLOR_GROUP_2);
                }
            });


            // Recent calls search
            res.hookLayout(HANGOUTS_RES_PKG_NAME, "layout", "call_contact_picker_fragment", new XC_LayoutInflated() {
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    EditText searchText = (EditText) liparam.view.findViewById(liparam.res
                            .getIdentifier("contact_searchbox", "id", HANGOUTS_RES_PKG_NAME));
                    searchText.setBackgroundColor(COLOR_GROUP_3);
                    searchText.setTextColor(COLOR_GROUP_1);
                    searchText.setHintTextColor(COLOR_GROUP_2);

                    // Divider
                    LinearLayout parent = (LinearLayout) liparam.view;
                    parent.getChildAt(1).setBackgroundColor(COLOR_GROUP_5);
                }
            });

            replaceColor(res, "hangouts_search_query_highlight_color", appColors[5]);


            // Dialpad colors
            replaceColor(res, "background_dialpad", COLOR_GROUP_3);
            replaceColor(res, "dialpad_separator_line_color", COLOR_GROUP_5);
            replaceColor(res, "dialpad_digits_text_color", COLOR_GROUP_1);
            replaceColor(res, "dialpad_secondary_text_color", COLOR_GROUP_2);
            replaceColor(res, "sufficient_contrast_hint_text", COLOR_GROUP_2);
            replaceColor(res, "background_edittext", COLOR_GROUP_3);
            replaceColor(res, "background_edittext_focused", COLOR_GROUP_3);
            replaceColor(res, "background_edittext_pressed", COLOR_GROUP_3);
            setTextColor(res, "dialpad_digits", "callFromDisplay", COLOR_GROUP_1);


            // Floating action button background and text color
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "quick_button_container_background",
                    moduleRes.fwd(R.drawable.quick_button_container_background));
            res.setReplacement(HANGOUTS_RES_PKG_NAME, "drawable", "quick_button_background",
                    moduleRes.fwd(R.drawable.quick_button_background));

            setTextColor(res, "quick_button_container", "quick_button_text", COLOR_GROUP_6);
            setTextColor(res, "quick_button", "quick_button_text", COLOR_GROUP_6);
        }


        // Fixes the send button color
        replaceColor(res, HANGOUTS_COLOR_FAB, appColors[5]);

        // Fixes the attachments icon color
        replaceColor(res, HANGOUTS_COLOR_PRIMARY_HANGOUTS, appColors[5]);

        // Fixes the indicator color that's seen when selecting a sticker
        replaceColor(res, HANGOUTS_COLOR_INDICATOR, appColors[5]);

        // Fixes "Sending as <number>" / Ongoing call bar on 4.x
        replaceColor(res, HANGOUTS_COLOR_ONGOING_BG, appColors[5]);

        // Fixes "Sending as <number>" / Ongoing call bar on 5.x
        replaceDrawableColor(res, HANGOUTS_DRAWABLE_ONGOING_BG, appColors[5]);
        replaceDrawableColor(res, HANGOUTS_DRAWABLE_ONGOING_BGP, appColors[5]);

        // This will colorize the small green contact avatar
        replaceAvatarColor(res, HANGOUTS_DRAWABLE_DEFAULT_AVATAR, smallAvatarId, hangoutsDpi, appColors[7]);

        // This will colorize the large green contact avatar
        replaceAvatarColor(res, HANGOUTS_DRAWABLE_DEFAULT_AVATAR_LARGE, largeAvatarId, hangoutsDpi, appColors[7]);

        // Fixes status bar on 5.x
        replaceColor(res, HANGOUTS_COLOR_PRIMARY, appColors[5]);
        replaceColor(res, HANGOUTS_COLOR_PRIMARY_DARK, appColors[7]);
        replaceColor(res, HANGOUTS_COLOR_QUANTUM_GOOGGREEN, appColors[5]);
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

        private static ColorFilter adjustBrightness(float value) {
            ColorMatrix cm = new ColorMatrix();
            adjustBrightness(cm, value);
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

        private static void adjustBrightness(ColorMatrix cm, float value) {
            value = cleanValue(value, 100);
            if (value == 0) {
                return;
            }

            float[] mat = new float[]{
                    1, 0, 0, 0, value,
                    0, 1, 0, 0, value,
                    0, 0, 1, 0, value,
                    0, 0, 0, 1, 0,
                    0, 0, 0, 0, 1
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
