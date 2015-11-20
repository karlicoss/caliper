package dk.ilios.spanner.junit;

import com.google.common.collect.ImmutableSortedMap;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dk.ilios.spanner.Benchmark;
import dk.ilios.spanner.BenchmarkConfiguration;
import dk.ilios.spanner.CustomMeasurement;
import dk.ilios.spanner.Spanner;
import dk.ilios.spanner.SpannerConfig;
import dk.ilios.spanner.exception.TrialFailureException;
import dk.ilios.spanner.model.Trial;

/**
 * Runner for handling the individual Benchmarks.
 */
public class SpannerRunner extends Runner {

    private Object testInstance;
    private TestClass testClass;
    private List<Method> testMethods = new ArrayList<>();
    private SpannerConfig benchmarkConfiguration;
    private Result result;

    public SpannerRunner(Class clazz) {
        testClass = new TestClass(clazz);
        try {
            testInstance = testClass.getJavaClass().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // Setup config (if any)
        List<FrameworkField> fields = testClass.getAnnotatedFields(BenchmarkConfiguration.class);

        if (fields.size() > 1) {
            throw new IllegalStateException("Only one @BenchmarkConfiguration allowed");
        }
        if (fields.size() > 0) {
            FrameworkField field = fields.get(0);
            try {
                if (!field.getType().equals(SpannerConfig.class)) {
                    throw new IllegalArgumentException("@BenchmarkConfiguration can only be set on " +
                            "SpannerConfiguration fields.");
                }
                benchmarkConfiguration = (SpannerConfig) field.get(testInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(field + " is not public", e);
            }
        }

        Method[] classMethods = clazz.getDeclaredMethods();
        for (int i = 0; i < classMethods.length; i++) {
            Method classMethod = classMethods[i];
            Class retClass = classMethod.getReturnType();
            int modifiers = classMethod.getModifiers();
            if (retClass == null || Modifier.isStatic(modifiers)
                    || !Modifier.isPublic(modifiers) || Modifier.isInterface(modifiers)
                    || Modifier.isAbstract(modifiers)) {
                continue;
            }
            if (classMethod.getAnnotation(Benchmark.class) != null) {
                testMethods.add(classMethod);
            }

            if (classMethod.getAnnotation(CustomMeasurement.class) != null) {
                testMethods.add(classMethod);
            }

            if (classMethod.getAnnotation(Ignore.class) != null) {
                testMethods.remove(classMethod);
            }
        }
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription(
                this.testClass.getName(),
                this.testClass.getJavaClass().getAnnotations()
        );
    }

    /**
     * @return the number of tests to be run by the receiver
     */
    @Override
    public int testCount() {
        return 0; // TODO Expose number from Spanner
    }

    //
    @Override
    public void run(final RunNotifier runNotifier) {
        runBenchmarks(runNotifier);
    }

    private void runBenchmarks(final RunNotifier runNotifier) {
        Spanner.runBenchmarks(testClass.getJavaClass(), testMethods, new Spanner.Callback() {

            public Trial currentTrail;

            @Override
            public void onStart() {
                // Because Description must have the same value when starting and finishing the unit test, we are introducing
                // a "fake" method called "Running". This acts as a placeholder for all running benchmarks, and we can
                // then determine how the unit test is displayed when it finishes or crashes.
                //
                // Only downside is that the duration of the benchmark test as measured by Junit will be 0s instead of the
                // actual value, but on the upside it is possible to show the value of the benchmark in the title.
                result = new Result();
                runNotifier.addListener(result.createListener());
                final Description RUNNING = Description.createTestDescription(testClass.getJavaClass(), "doingStuff");
                runNotifier.fireTestRunStarted(RUNNING);
            }

            @Override
            public void trialStarted(Trial trial) {
                currentTrail = trial;
            }

            @Override
            public void trialSuccess(Trial trial, Trial.Result result) {
                double resultMedian = result.getTrial().getMedian();
                Description spec = getDescription(trial, resultMedian);
                runNotifier.fireTestStarted(spec);
                if (trial.hasBaseline()) {
                    checkMetricChanges(trial, benchmarkConfiguration, runNotifier);
                }
                runNotifier.fireTestFinished(spec);
            }

            @Override
            public void trialFailure(Trial trial, Throwable error) {
                Description spec = getDescription(trial);
                runNotifier.fireTestStarted(spec);
                runNotifier.fireTestFailure(new Failure(spec, error));
                runNotifier.fireTestFinished(spec);
            }

            @Override
            public void trialEnded(Trial trial) {
                currentTrail = null;
            }

            @Override
            public void onComplete() {
                runNotifier.fireTestRunFinished(result);
            }

            @Override
            public void onError(Exception error) {
                // Something broke horribly. Further testing will be aborted.
                Description spec = getDescription(null);
                runNotifier.fireTestStarted(spec);
                runNotifier.fireTestFailure(new Failure(spec, error));
                runNotifier.fireTestFinished(spec);
                runNotifier.fireTestRunFinished(result);
            }
        });
    }

    // Verify that all configured metric changes does not exceed their allowed value
    private void checkMetricChanges(Trial trial, SpannerConfig benchmarkConfiguration, RunNotifier runNotifier) {
        StringBuilder sb = new StringBuilder();

        // Check all configured percentiles
        Set<Float> percentiles = benchmarkConfiguration.getPercentileFailureLimits();
        for (Float percentile : percentiles) {
            float maxLimit = benchmarkConfiguration.getPercentileFailureLimit(percentile);
            if (maxLimit == SpannerConfig.NOT_ENABLED) {
                continue;
            }
            Double change = trial.getChangeFromBaseline(percentile);
            if (Math.abs(change) > maxLimit) {
                sb.append("\n");
                String errorMsg = String.format("Change from baseline at %s was to big: %.2f%%. Limit is %.2f%%",
                        prettyPercentile(percentile), change * 100, maxLimit * 100);
                sb.append(errorMsg);
            }
        }

        float meanLimit = benchmarkConfiguration.getMeanFailureLimit();
        double meanChange = Math.abs(trial.getChangeFromBaselineMean());
        if (meanLimit != SpannerConfig.NOT_ENABLED && meanChange > meanLimit) {
            sb.append("\n");
            sb.append(String.format("Change from baseline mean was to big: %.2f%%. Limit is %.2f%%",
                    meanChange * 100, meanLimit * 100));
        }

        if (sb.length() > 0) {
            Description spec = getDescription(trial);
            runNotifier.fireTestFailure(new Failure(spec, new TrialFailureException(sb.toString())));
        }
    }

    private String prettyPercentile(Float percentile) {
        if (percentile == 0.0F) {
            return "Min.";
        } else if (percentile == 100.0F) {
            return "Max.";
        } else if (percentile == 50.0F) {
            return "Median";
        } else {
            return percentile + "th percentile";
        }
    }

    private String formatBenchmarkChange(Trial trial) {
        if (trial.hasBaseline()) {
            Double change = trial.getChangeFromBaseline(50) * 100;
            return String.format("[%s%.2f%%]", change > 0 ? "+" : "", change);
        } else {
            return "";
        }
    }

    /**
     * Returns the description used by the JUnit GUI.
     *
     * @param trial trial output to format.
     * @return description of the trial.
     */
    private Description getDescription(Trial trial) {
        if (trial == null) {
            return Description.createTestDescription(testClass.getJavaClass(), "Unknown");
        }

        Method method = trial.experiment().instrumentation().benchmarkMethod();
        return Description.createTestDescription(testClass.getJavaClass(), method.getName());
    }

    /**
     * Returns the description of a successful Trial used by the JUnit GUI.
     *
     * @param trial trial output to format.
     * @param result the result of the benchmark trial.
     * @return description of the trial.
     */
    private Description getDescription(Trial trial, double result) {
        Method method = trial.experiment().instrumentation().benchmarkMethod();
        String resultString = String.format(" [%.2f %s.]", result, trial.getUnit().toLowerCase());
        resultString += formatBenchmarkChange(trial);

        // Benchmark parameters
        ImmutableSortedMap<String, String> benchmarkParameters = trial.experiment().benchmarkSpec().parameters();
        String params = "";
        if (benchmarkParameters.size() > 0) {
            params = " " + benchmarkParameters.toString();
        }

        // Trial number
        String trialNumber = "";
        if (benchmarkConfiguration.getTrialsPrExperiment() > 1) {
            trialNumber = "#" + trial.getTrialNumber();
        }

        String methodDescription;
        methodDescription = String.format("%s%s%s %s",
                method.getName(),
                trialNumber,
                params,
                resultString);
        return Description.createTestDescription(testClass.getJavaClass(), methodDescription);
    }
}
