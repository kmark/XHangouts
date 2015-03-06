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

import java.util.Locale;

public enum Setting {
    MOD_ENABLED,
    MMS_RESIZE_ENABLED,
    MMS_ROTATE_ENABLED,
    MMS_ROTATE_MODE,
    MMS_SCALE_PREFKEY("mms_scale"),
    MMS_SCALE_WIDTH,
    MMS_SCALE_HEIGHT,
    MMS_IMAGE_PREFKEY("mms_image"),
    MMS_IMAGE_TYPE,
    MMS_IMAGE_QUALITY,
    MMS_APN_SPLICING_ENABLED,
    MMS_APN_SPLICING_APN_CONFIG_PREFKEY("mms_apn_splicing_apn_config"),
    MMS_APN_SPLICING_APN_CONFIG_PRESET,
    MMS_APN_SPLICING_APN_CONFIG_MMSC,
    MMS_APN_SPLICING_APN_CONFIG_PROXY_HOSTNAME,
    MMS_APN_SPLICING_APN_CONFIG_PROXY_PORT,
    UI_ENTER_KEY,
    UI_ATTACH_ANYTIME,
    UI_HIDE_CALL_BUTTONS,
    UI_APP_COLOR,
    ABOUT_VERSION,
    DEBUG;

    private String name = null;

    // This allows us to have enum names that don't directly map to values
    private Setting(String name) {
        this.name = name;
    }

    private Setting() {
        //
    }

    static Setting fromString(String name) {
        for(Setting p : values()) {
            if(p.name == null) {
                try {
                    if(p == Setting.valueOf(name.toUpperCase(Locale.US))) {
                        return p;
                    }
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }
            if(name.equalsIgnoreCase(p.name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("No constant with name " + name + " found");
    }

    @Override
    public String toString() {
        return name != null ? name : name().toLowerCase();
    }

    enum UiEnterKey {
        EMOJI_SELECTOR(0),
        NEWLINE(1),
        SEND(2);

        private final int value;
        private UiEnterKey(final int value) {
            this.value = value;
        }

        static UiEnterKey fromInt(int value) {
            for(UiEnterKey u : values()) {
                if(value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }

        int toInt() {
            return value;
        }
    }

    public enum ImageFormat {
        JPEG(0),
        PNG(1);

        private final int value;
        private ImageFormat(final int value) {
            this.value = value;
        }

        public static ImageFormat fromInt(int value) {
            for(ImageFormat u : values()) {
                if(value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }

        public int toInt() {
            return value;
        }
    }

    public enum ApnPreset {
        // The order here can be changed as desired but the first integer provided in the constructor
        // must remain the same.
        CUSTOM(0, "Custom", "", "", -1),
        ATT(2, "AT&T", "http://mmsc.mobile.att.net", "proxy.mobile.att.net", 80),
        CRICKET(3, "Cricket", "http://mmsc.aiowireless.net", "proxy.aiowireless.net", 80),
        VERIZON(1, "Verizon", "http://mms.vtext.com/servlets/mms", "", -1);

        private final int value;
        private final String name;
        private final String mmsc;
        private final String proxyHost;
        private final int proxyPort;

        private ApnPreset(int value, String name, String mmsc, String proxyHost, int proxyPort) {
            this.value = value;
            this.name = name;
            this.mmsc = mmsc;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
        }

        public static ApnPreset fromInt(int value) {
            for(ApnPreset u : values()) {
                if(value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }

        public int toInt() {
            return value;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getMmsc() {
            return mmsc;
        }

        public String getProxyHost() {
            return proxyHost;
        }

        public int getProxyPort() {
            return proxyPort;
        }
    }

    enum AppColor {
        AMBER(0, "quantum_amber"),
        BLUE_GREY(1, "quantum_bluegrey"),
        BROWN(2, "quantum_brown"),
        CYAN(3, "quantum_cyan"),
        DEEP_ORANGE(4, "quantum_deeporange"),
        DEEP_PURPLE(5, "quantum_deeppurple"),
        GOOGLE_BLUE(6, "quantum_googblue"),
        GOOGLE_GREEN(7, "quantum_googgreen"),
        GOOGLE_RED(8, "quantum_googred"),
        GOOGLE_YELLOW(9, "quantum_googyellow"),
        GREY(10, "quantum_grey"),
        INDIGO(11, "quantum_indigo"),
        LIGHT_BLUE(12, "quantum_lightblue"),
        LIGHT_GREEN(13, "quantum_lightgreen"),
        LIME(14, "quantum_lime"),
        ORANGE(15, "quantum_orange"),
        PINK(16, "quantum_pink"),
        PURPLE(17, "quantum_purple"),
        TEAL(18, "quantum_teal"),
        VANILLA_BLUE(19, "quantum_vanillablue"),
        VANILLA_GREEN(20, "quantum_vanillagreen"),
        VANILLA_RED(21, "quantum_vanillared"),
        YELLOW(22, "quantum_yellow");

        private final int value;
        private final String prefix;

        private AppColor(int value, String prefix) {
            this.value = value;
            this.prefix = prefix;
        }

        static AppColor fromInt(int value) {
            for(AppColor u : values()) {
                if(value == u.value) {
                    return u;
                }
            }
            throw new IllegalArgumentException("No constant with value " + value + " found");
        }

        int toInt() {
            return value;
        }

        String getPrefix() {
            return prefix;
        }
    }
}
