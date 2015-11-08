/*
 * Copyright (C) 2013 Google Inc.
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

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import java.lang.reflect.Method;
import java.util.SortedMap;

import dk.ilios.spanner.AfterRep;
import dk.ilios.spanner.BeforeRep;
import dk.ilios.spanner.benchmark.BenchmarkClass;
import dk.ilios.spanner.config.RuntimeConfig;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.model.Value;
import dk.ilios.spanner.util.Reflection;
import dk.ilios.spanner.util.Util;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * The {@link Worker} implementation for macro benchmarks, i.e benchmarks which runtime are measured in milliseconds,
 * not nanoseconds.
 */
public class MacrobenchmarkWorker extends Worker {
    private final Stopwatch stopwatch;
    private final ImmutableSet<Method> beforeRepMethods;
    private final ImmutableSet<Method> afterRepMethods;
    private final boolean gcBeforeEach;

    public MacrobenchmarkWorker(BenchmarkClass benchmarkClass,
                                Method method,
                                Ticker ticker,
                                RuntimeConfig options,
                                SortedMap<String, String> userParameters) {

        super(benchmarkClass.getInstance(), method, userParameters);
        this.stopwatch = Stopwatch.createUnstarted(ticker);
        this.beforeRepMethods = Reflection.getAnnotatedMethods(benchmark.getClass(), BeforeRep.class);
        this.afterRepMethods = Reflection.getAnnotatedMethods(benchmark.getClass(), AfterRep.class);
        this.gcBeforeEach = options.gcBeforeEachMeasurement();
    }

    @Override
    public void preMeasure(boolean inWarmup) throws Exception {
        for (Method beforeRepMethod : beforeRepMethods) {
            beforeRepMethod.invoke(benchmark);
        }
        if (gcBeforeEach && !inWarmup) {
            Util.forceGc();
        }
    }

    @Override
    public Iterable<Measurement> measure() throws Exception {
        stopwatch.start();
        benchmarkMethod.invoke(benchmark);
        long nanos = stopwatch.stop().elapsed(NANOSECONDS);
        stopwatch.reset();
        return ImmutableSet.of(new Measurement.Builder()
                .description("runtime")
                .weight(1)
                .value(Value.create(nanos, "ns"))
                .build());
    }

    @Override
    public void postMeasure() throws Exception {
        for (Method afterRepMethod : afterRepMethods) {
            afterRepMethod.invoke(benchmark);
        }
    }
}
