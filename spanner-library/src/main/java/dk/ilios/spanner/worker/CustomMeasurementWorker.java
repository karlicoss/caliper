/*
 * Copyright (C) 2011 Google Inc.
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
 */

package dk.ilios.spanner.worker;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Method;
import java.util.SortedMap;

import dk.ilios.spanner.CustomMeasurement;
import dk.ilios.spanner.benchmark.BenchmarkClass;
import dk.ilios.spanner.config.CustomConfig;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.model.Value;
import dk.ilios.spanner.util.Util;

/**
 * Worker for methods doing their own measurements (custom measurements).
 */
public final class CustomMeasurementWorker extends Worker {
    private final CustomConfig options;
    private final String unit;
    private final String description;

    public CustomMeasurementWorker(BenchmarkClass benchmarkClass,
                         Method method,
                         CustomConfig options,
                         SortedMap<String, String> userParameters) {
        super(benchmarkClass.getInstance(), method, userParameters);
        this.options = options;
        CustomMeasurement annotation = benchmarkMethod.getAnnotation(CustomMeasurement.class);
        this.unit = annotation.units();
        this.description = annotation.description();
    }

    @Override
    public void preMeasure(boolean inWarmup) throws Exception {
        if (options.gcBeforeEachMeasurement() && !inWarmup) {
            Util.forceGc();
        }
    }

    @Override
    public Iterable<Measurement> measure() throws Exception {
        double measured = (Double) benchmarkMethod.invoke(benchmark);
        return ImmutableSet.of(new Measurement.Builder()
                .value(Value.create(measured, unit))
                .weight(1)
                .description(description)
                .build());
    }
}
