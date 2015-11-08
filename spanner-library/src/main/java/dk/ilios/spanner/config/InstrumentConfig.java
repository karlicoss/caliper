/*
 * Copyright (C) 2012 Google Inc.
 * Copyright (C) 2015 Christian Melchior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original author gak@google.com (Gregory Kick)
 */

package dk.ilios.spanner.config;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import dk.ilios.spanner.internal.Instrument;

/**
 * This is the configuration passed to the instrument by the user. This differs from the
 * InstrumentSpec in that any number of configurations can yield the same spec
 * (due to default option values).
 */
public abstract class InstrumentConfig {
    private final Class<? extends Instrument> clazz;
    private final Map<String, String> options = new HashMap<>();

    InstrumentConfig(Class<? extends Instrument> clazz) {
        this.clazz = clazz;
    }

    public String className() {
        return clazz.getName();
    }

    /**
     * Returns the string representation of all options.
     */
    public Map<String, String> options() {
        return options;
    }

    /**
     * Add the string representation of an option.
     *
     * @param key
     * @param option
     */
    protected void addOption(String key, String option) {
        options.put(key, option);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof InstrumentConfig) {
            InstrumentConfig that = (InstrumentConfig) obj;
            return className().equals(that.className())
                    && this.options.equals(that.options);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className(), options);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("className", className())
                .add("options", options)
                .toString();
    }

    public Class<? extends Instrument> getInstrumentClass() {
        return clazz;
    }
}
