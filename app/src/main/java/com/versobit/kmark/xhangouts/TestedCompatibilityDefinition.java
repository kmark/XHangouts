/*
 * Copyright (C) 2018 Kevin Mark
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

public final class TestedCompatibilityDefinition {

    private String version;
    private int min;
    private int max;

    TestedCompatibilityDefinition(String version, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max");
        }

        this.version = version;
        this.min = min;
        this.max = max;
    }

    public String getVersion() {
        return version;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isCompatible(int version) {
        return version >= min && version <= max;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s[version=%s, min=%d, max=%d]",
                TestedCompatibilityDefinition.class.getSimpleName(), version, min, max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestedCompatibilityDefinition that = (TestedCompatibilityDefinition) o;

        if (min != that.min) return false;
        if (max != that.max) return false;
        return version != null ? version.equals(that.version) : that.version == null;
    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + min;
        result = 31 * result + max;
        return result;
    }
}
