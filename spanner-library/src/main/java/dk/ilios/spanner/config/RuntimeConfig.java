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

import java.util.concurrent.TimeUnit;

import dk.ilios.spanner.internal.Instrument;
import dk.ilios.spanner.internal.RuntimeInstrument;

/**
 * Type safe configuration object for Runtime {@link dk.ilios.spanner.internal.Instrument}s.
 */
public class RuntimeConfig extends InstrumentConfig {

    private static final Class<? extends Instrument> defaultInstrument = RuntimeInstrument.class;
    private static final String KEY_CLASS = "class";
    private static final String KEY_WARMUP = "warmup_time";
    private static final String KEY_WARMUP_TIMEUNIT = "warmup_unit";
    private static final String KEY_MAX_WARMUP_WALLTIME = "maxWarmupWall_time";
    private static final String KEY_MAX_WARMUP_WALLTIME_TIMEUNIT = "maxWarmupWall_unit";
    private static final String KEY_TIMING_INVERVAL = "timingInterval_time";
    private static final String KEY_TIMING_INTERVAL_TIMEUNIT = "timingInterval_unit";
    private static final String KEY_MEASUREMENTS = "measurements";
    private static final String KEY_GC_BEFORE_EACH = "gcBeforeEach";
    private static final String KEY_SUGGEST_GRANULARITY = "suggestGranularity";

    private final Class<? extends Instrument> instrumentClass;
    private final long warmupTime;
    private final TimeUnit warmupTimeUnit;
    private final long maxWarmupTime;
    private final TimeUnit maxWarmupTimeUnit;
    private final long timingInterval;
    private final TimeUnit timingIntervalUnit;
    private final int measurements;
    private final boolean gcBeforeEachMeasurement;
    private final boolean suggestGranularity;

    /**
     * Returns the default configuration.
     */
    public static RuntimeConfig defaultConfig() {
        return new RuntimeConfig.Builder().build();
    }

    /**
     * Returns a configuration suitable for being used by unit tests.
     */
    public static RuntimeConfig unittestConfig() {
        return new RuntimeConfig.Builder()
                .warmupTime(0, TimeUnit.SECONDS)
                .maxWarmupTime(0, TimeUnit.SECONDS)
                .timingInterval(1, TimeUnit.NANOSECONDS)
                .gcBeforeEachMeasurement(false)
                .suggestGranularity(false)
                .build();
    }

    private RuntimeConfig(Builder builder) {
        super(builder.instrumentClass);
        this.instrumentClass = builder.instrumentClass;
        this.warmupTime = builder.warmupTime;
        this.warmupTimeUnit = builder.warmupTimeUnit;
        this.maxWarmupTime = builder.maxWarmupTime;
        this.maxWarmupTimeUnit = builder.maxWarmupTimeUnit;
        this.timingInterval = builder.timingInterval;
        this.timingIntervalUnit = builder.timingIntervalUnit;
        this.measurements = builder.measurements;
        this.gcBeforeEachMeasurement = builder.gcBeforeEachMeasurement;
        this.suggestGranularity = builder.suggestGranularity;

        addOption(KEY_CLASS, instrumentClass.getName());
        addOption(KEY_WARMUP, Long.toString(warmupTime));
        addOption(KEY_WARMUP_TIMEUNIT, warmupTimeUnit.toString());
        addOption(KEY_MAX_WARMUP_WALLTIME, Long.toString(maxWarmupTime));
        addOption(KEY_MAX_WARMUP_WALLTIME_TIMEUNIT, maxWarmupTimeUnit.toString());
        addOption(KEY_TIMING_INVERVAL, Long.toString(timingInterval));
        addOption(KEY_TIMING_INTERVAL_TIMEUNIT, timingIntervalUnit.toString());
        addOption(KEY_MEASUREMENTS, Long.toString(measurements));
        addOption(KEY_GC_BEFORE_EACH, Boolean.toString(gcBeforeEachMeasurement));
        addOption(KEY_SUGGEST_GRANULARITY, Boolean.toString(suggestGranularity));
    }

    public Class<? extends Instrument> instrumentationClass() {
        return instrumentClass;
    }

    public long warmpupTime() {
        return warmupTime;
    }

    public TimeUnit warmupTimeUnit() {
        return warmupTimeUnit;
    }

    public long maxWarmupTime() {
        return maxWarmupTime;
    }

    public TimeUnit maxWarmupTimeUnit() {
        return maxWarmupTimeUnit;
    }

    public long timingInterval() {
        return timingInterval;
    }

    public TimeUnit timingIntervalUnit() {
        return timingIntervalUnit;
    }

    public int measurements() {
        return measurements;
    }

    public boolean gcBeforeEachMeasurement() {
        return gcBeforeEachMeasurement;
    }

    public boolean suggestGranularity() {
        return suggestGranularity;
    }

    /**
     * Builder for configuring a Runtime Instrument.
     */
    public static class Builder {
        private Class<? extends Instrument> instrumentClass = RuntimeInstrument.class;
        private long warmupTime = 0;
        private TimeUnit warmupTimeUnit = TimeUnit.SECONDS;
        private long maxWarmupTime = 10;
        private TimeUnit maxWarmupTimeUnit = TimeUnit.MINUTES;
        private long timingInterval = 500;
        private TimeUnit timingIntervalUnit = TimeUnit.MILLISECONDS;
        private int measurements = 9;
        private boolean gcBeforeEachMeasurement = true;
        private boolean suggestGranularity = true;

        public Builder instrumentClass(Class<? extends Instrument> instrumentClass) {
            this.instrumentClass = instrumentClass;
            return this;
        }

        public Builder warmupTime(long warmupTime, TimeUnit unit) {
            this.warmupTime = warmupTime;
            this.warmupTimeUnit = unit;
            return this;
        }

        public Builder maxWarmupTime(long maxWarmupTime, TimeUnit unit) {
            this.maxWarmupTime = maxWarmupTime;
            this.maxWarmupTimeUnit = unit;
            return this;
        }

        /**
         * Amount of wall clock time a benchmark should run for. Spanner chooses repetition counts such that the total
         * timing interval comes out near this value. Higher values take longer, but are more precise (less vulnerable
         * to fixed costs)
         *
         * Default value is {@code 500ms}.
         */
        public Builder timingInterval(long timingInterval, TimeUnit unit) {
            this.timingInterval = timingInterval;
            this.timingIntervalUnit = unit;
            return this;
        }

        /**
         * Number of measurements to record after any warmup time. These will provide the basis for the
         * final benchmark result.
         *
         * @param measurements Number of measurements to do.
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

        /**
         * Whether or not to make suggestions about whether a benchmark should be a pico/micro/macro
         * benchmark.  Note that this will not effect errors that result from benchmarks that are unable to
         * take proper measurements due to granularity issues.
         *
         * Default value is {@code true}.
         */
        public Builder suggestGranularity(boolean suggestGranularity) {
            this.suggestGranularity = suggestGranularity;
            return this;
        }

        public RuntimeConfig build() {
            return new RuntimeConfig(this);
        }
    }
}
