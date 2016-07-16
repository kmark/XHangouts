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

package com.versobit.kmark.xhangouts;

import android.graphics.Bitmap;

import java.util.Locale;

public enum Setting {
    MOD_ENABLED,
    LAUNCHER_ICON,
    MMS_RESIZE_ENABLED,
    MMS_ROTATE_ENABLED,
    MMS_ROTATE_MODE,
    MMS_SCALE_PREFKEY("mms_scale"),
    MMS_SCALE_WIDTH,
    MMS_SCALE_HEIGHT,
    MMS_IMAGE_PREFKEY("mms_image"),
    MMS_IMAGE_TYPE,
    MMS_IMAGE_QUALITY,
    UI_EMOJI,
    UI_GALLERY,
    UI_CAMERA,
    UI_VIDEO,
    UI_STICKERS,
    UI_LOCATION,
    UI_ENTER_KEY,
    UI_ENHANCE_CALL_BUTTON,
    UI_HIDE_CALL_BUTTONS,
    UI_SEND_LOCK,
    UI_DISABLE_PROXIMITY,
    UI_SHOW_DEBUG_OPTIONS,
    UI_ENABLE_THEMING,
    UI_APP_COLOR,
    UI_DARK_THEME,
    UI_BLACK_BACKGROUNDS,
    UI_DARK_THEME_BUBBLES,
    UI_LIGHT_THEME_BUBBLES,
    UI_THEME_HYPERLINKS,
    UI_HIGHLIGHT_UNREAD,
    UI_COLOR_INCOMING,
    UI_COLOR_INCOMING_OTR,
    UI_COLOR_INCOMING_FONT,
    UI_COLOR_INCOMING_FONT_OTR,
    UI_COLOR_INCOMING_LINK,
    UI_COLOR_INCOMING_LINK_OTR,
    UI_COLOR_OUTGOING,
    UI_COLOR_OUTGOING_OTR,
    UI_COLOR_OUTGOING_FONT,
    UI_COLOR_OUTGOING_FONT_OTR,
    UI_COLOR_OUTGOING_LINK,
    UI_COLOR_OUTGOING_LINK_OTR,
    UI_DARK_COLOR_INCOMING,
    UI_DARK_COLOR_INCOMING_OTR,
    UI_DARK_COLOR_INCOMING_FONT,
    UI_DARK_COLOR_INCOMING_FONT_OTR,
    UI_DARK_COLOR_INCOMING_LINK,
    UI_DARK_COLOR_INCOMING_LINK_OTR,
    UI_DARK_COLOR_OUTGOING,
    UI_DARK_COLOR_OUTGOING_OTR,
    UI_DARK_COLOR_OUTGOING_FONT,
    UI_DARK_COLOR_OUTGOING_FONT_OTR,
    UI_DARK_COLOR_OUTGOING_LINK,
    UI_DARK_COLOR_OUTGOING_LINK_OTR,
    SOUND_ENABLED,
    SOUND_AUDIOCALLIN,
    SOUND_AUDIOCALLOUT,
    SOUND_JOIN,
    SOUND_LEAVE,
    SOUND_OUTGOING,
    SOUND_INCALL,
    ABOUT_VERSION,
    DEBUG;

    private String name = null;

    // This allows us to have enum names that don't directly map to values
    Setting(String name) {
        this.name = name;
    }

    Setting() {
        //
    }

    static Setting fromString(String name) {
        for (Setting p : values()) {
            if (p.name == null) {
                try {
                    if (p == Setting.valueOf(name.toUpperCase(Locale.US))) {
                        return p;
                    }
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }
            if (name.equalsIgnoreCase(p.name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("No constant with name " + name + " found");
    }

    @Override
    public String toString() {
        return name != null ? name : name().toLowerCase();
    }

    public enum UiEnterKey {
        EMOJI_SELECTOR(0),
        NEWLINE(1),
        SEND(2);

        private final int value;

        UiEnterKey(final int value) {
            this.value = value;
        }

        static UiEnterKey fromInt(int value) {
            for (UiEnterKey u : values()) {
                if (value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }
    }

    public enum UiEmoji {
        DEFAULT(0),
        SHOW(1),
        HIDE(2);

        private final int value;

        UiEmoji(final int value) {
            this.value = value;
        }

        static UiEmoji fromInt(int value) {
            for (UiEmoji u : values()) {
                if (value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }
    }

    public enum UiButtons {
        DEFAULT(0),
        HIDE(1);

        private final int value;

        UiButtons(final int value) {
            this.value = value;
        }

        static UiButtons fromInt(int value) {
            for (UiButtons u : values()) {
                if (value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }
    }

    public enum ImageFormat {
        JPEG(0),
        PNG(1);

        private final int value;

        ImageFormat(final int value) {
            this.value = value;
        }

        public static ImageFormat fromInt(int value) {
            for (ImageFormat u : values()) {
                if (value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }

        public int toInt() {
            return value;
        }

        public Bitmap.CompressFormat toCompressFormat() {
            switch (this) {
                case JPEG:
                    return Bitmap.CompressFormat.JPEG;
                case PNG:
                    return Bitmap.CompressFormat.PNG;
            }
            return null;
        }
    }

    public enum AppColor {
        AMBER(0, "quantum_amber", 0xffffc107),
        BLUE_GREY(1, "quantum_bluegrey", 0xff607d8b),
        BROWN(2, "quantum_brown", 0xff795548),
        CYAN(3, "quantum_cyan", 0xff00bcd4),
        DEEP_ORANGE(4, "quantum_deeporange", 0xffff5722),
        DEEP_PURPLE(5, "quantum_deeppurple", 0xff673ab7),
        GOOGLE_BLUE(6, "quantum_googblue", 0xff4285f4),
        GOOGLE_GREEN(7, "quantum_googgreen", 0xff0f9d58),
        GOOGLE_RED(8, "quantum_googred", 0xffdb4437),
        GOOGLE_YELLOW(9, "quantum_googyellow", 0xfff4b400),
        GREY(10, "quantum_grey", 0xff9e9e9e),
        INDIGO(11, "quantum_indigo", 0xff3f51b5),
        LIGHT_BLUE(12, "quantum_lightblue", 0xff03a9f4),
        LIGHT_GREEN(13, "quantum_lightgreen", 0xff8bc34a),
        LIME(14, "quantum_lime", 0xffcddc39),
        ORANGE(15, "quantum_orange", 0xffff9800),
        PINK(16, "quantum_pink", 0xffe91e63),
        PURPLE(17, "quantum_purple", 0xff9c27b0),
        TEAL(18, "quantum_teal", 0xff009688),
        VANILLA_BLUE(19, "quantum_vanillablue", 0xff5677fc),
        VANILLA_GREEN(20, "quantum_vanillagreen", 0xff259b24),
        VANILLA_RED(21, "quantum_vanillared", 0xffe51c23),
        YELLOW(22, "quantum_yellow", 0xffffeb3b);

        private final int value;
        private final String prefix;
        private final int color;

        AppColor(int value, String prefix, int color) {
            this.value = value;
            this.prefix = prefix;
            this.color = color;
        }

        static AppColor fromInt(int value) {
            for (AppColor u : values()) {
                if (value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }

        public int toInt() {
            return value;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getBaseColor() {
            return color;
        }
    }
}
