/*
 * Copyright (C) 2014 Kevin Mark
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

enum Setting {
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
    UI_ENTER_KEY,
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

    enum ImageFormat {
        JPEG(0),
        PNG(1);

        private final int value;
        private ImageFormat(final int value) {
            this.value = value;
        }

        static ImageFormat fromInt(int value) {
            for(ImageFormat u : values()) {
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
}
