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

package dk.ilios.spanner;

import org.junit.runner.RunWith;

import dk.ilios.spanner.junit.SpannerRunner;

@RunWith(SpannerRunner.class)
public class UnitTestCustomMeasurements {

//    private File filesDir = InstrumentationRegistry.getTargetContext().getFilesDir();
//    private File resultsDir = new File(filesDir, "results");
//    private File baseLineFile = Utils.copyFromAssets("baseline.json");
//
//    @BenchmarkConfiguration
//    public SpannerConfig configuration = new SpannerConfig.Builder()
//            .saveResults(resultsDir)
//            .createBaseline(resultsDir)
//            .useBaseline(baseLineFile)
//            .baselineFailure(1.0f) // Accept 100% difference, normally should be 10-15%
//            .uploadResults()
//            .build();

    // Public test parameters (value chosen and injected by Experiment)
    @Param(value = {"java.util.Date", "java.lang.Object"})
    public String value;

    // Private fields used by benchmark methods
    private Class testClass;
    private int reps = 1000000;

    @BeforeExperiment
    public void before() {
        try {
            testClass = Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterExperiment
    public void after() {

    }

    @CustomMeasurement(units = "ns")
    public double instanceOf() {
        boolean result = false;
        long start = System.nanoTime();
        for (int i = 0; i < reps; i++) {
             result = (testClass instanceof Object);
        }
        long end = System.nanoTime();
        return (end - start)/(double) reps;
    }

    @CustomMeasurement(units = "ns")
    public double directComparison() {
        boolean result = false;
        long start = System.nanoTime();
        for (int i = 0; i < reps; i++) {
            result = (testClass == Object.class);
        }
        long end = System.nanoTime();
        return (end - start)/(double) reps;
    }

    @CustomMeasurement(units = "ns")
    public double equalsTo() {
        boolean result = false;
        long start = System.nanoTime();
        for (int i = 0; i < reps; i++) {
            result = (testClass.equals(Object.class));
        }
        long end = System.nanoTime();
        return (end - start)/(double) reps;
    }
}
