/*
 * Copyright (C) 2015 Christian Melchior.
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
 */

package dk.ilios.spanner.config;

import dk.ilios.spanner.internal.CustomMeasurementInstrument;
import dk.ilios.spanner.internal.Instrument;

/**
 * Typesafe configuration object for Custom {@link dk.ilios.spanner.internal.Instrument}s.
 */
public class CustomInstrumentConfig extends InstrumentConfig {

    private static final String KEY_CLASS = "class";
    private static final String KEY_GC_BEFORE_EACH = "gcBeforeEach";
    private static final String KEY_MEASUREMENTS = "measurements";

    private final Class<? extends Instrument> instrumentClass;
    private final boolean gcBeforeEachMeasurement;
    private final int measurements;

    /**
     * Returns the default configuration.
     */
    public static CustomInstrumentConfig defaultConfig() {
        return new CustomInstrumentConfig.Builder().build();
    }

    /**
     * Returns a configuration suitable for being used by unit tests.
     */
    public static CustomInstrumentConfig unittestConfig() {
        return new CustomInstrumentConfig.Builder()
                .gcBeforeEachMeasurement(false)
                .build();
    }

    private CustomInstrumentConfig(Builder builder) {
        super(builder.instrumentClass);
        this.instrumentClass = builder.instrumentClass;
        this.gcBeforeEachMeasurement = builder.gcBeforeEachMeasurement;
        this.measurements = builder.measurements;

        addOption(KEY_CLASS, instrumentClass.getName());
        addOption(KEY_GC_BEFORE_EACH, Boolean.toString(gcBeforeEachMeasurement));
        addOption(KEY_MEASUREMENTS, Integer.toString(measurements));
    }

    public boolean gcBeforeEachMeasurement() {
        return gcBeforeEachMeasurement;
    }

    /**
     * Builder for configuring a Runtime Instrument.
     */
    public static class Builder {
        private Class<? extends Instrument> instrumentClass = CustomMeasurementInstrument.class;
        private boolean gcBeforeEachMeasurement = true;
        private int measurements = 1;

        public Builder instrumentClass(Class<? extends Instrument> instrumentClass) {
            this.instrumentClass = instrumentClass;
            return this;
        }

        /**
         * Number of measurements to record. These will provide the basis for the
         * final benchmark result.
         *
         * @param measurements Number of measurements to do. Default value is {@code 1}.
         */
        public Builder measurements(int measurements) {
            this.measurements = measurements;
            return this;
        }

        /**
         * Run GC before every measurement?
         *
         * Default value is {@code true}
         */
        public Builder gcBeforeEachMeasurement(boolean gcBeforeEachMeasurement) {
            this.gcBeforeEachMeasurement = gcBeforeEachMeasurement;
            return this;
        }

        public CustomInstrumentConfig build() {
            return new CustomInstrumentConfig(this);
        }
    }
}