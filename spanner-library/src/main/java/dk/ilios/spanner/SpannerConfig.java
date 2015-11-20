package dk.ilios.spanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import dk.ilios.spanner.config.InstrumentConfig;
import dk.ilios.spanner.config.RuntimeInstrumentConfig;
import dk.ilios.spanner.output.ResultProcessor;
import dk.ilios.spanner.util.ShortDuration;

/**
 * Class for adding custom configuration of a Spanner run.
 */
public class SpannerConfig {

    public static final float NOT_ENABLED = -1.0F;

    private final File resultsFile;
    private final File baseLineFile;
    private final boolean warnIfWrongTestGranularity;
    private final URL uploadUrl;
    private final String apiKey;
    private final boolean uploadResults;
    private final Map<Float, Float> percentileFailureLimits;
    private float meanFailureLimit;
    private int maxBenchmarkThreads;
    private int trialsPrExperiment;
    private Set<InstrumentConfig> configs = new HashSet<>();
    private Set<ResultProcessor> resultProcessors;

    private SpannerConfig(Builder builder) {
        this.resultsFile = builder.resultsFile;
        this.baseLineFile = builder.baseLineFile;
        this.warnIfWrongTestGranularity = builder.warnIfWrongTestGranularity;
        this.uploadResults = builder.uploadResults;
        this.uploadUrl = builder.uploadUrl;
        this.apiKey = builder.apiKey;
        this.percentileFailureLimits = builder.percentileFailureLimits;
        this.meanFailureLimit = builder.meanFailureLimit;
        this.maxBenchmarkThreads = builder.maxBenchmarkThreads;
        this.trialsPrExperiment = builder.trialsPrExperiment;
        this.resultProcessors = builder.resultProcessors;
        if (builder.instrumentationConfigs.isEmpty()) {
            configs.add(RuntimeInstrumentConfig.defaultConfig());
        } else {
            configs.addAll(builder.instrumentationConfigs);
        }
    }

    public File getResultsFile() {
        return resultsFile;
    }

    public File getBaseLineFile() {
        return baseLineFile;
    }

    public boolean warnIfWrongTestGranularity() {
        return warnIfWrongTestGranularity;
    }

    public URL getUploadUrl() {
        return uploadUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isUploadResults() {
        return uploadResults;
    }

    public Set<InstrumentConfig> getInstrumentConfigurations() {
        return configs;
    }

    public int getNoBenchmarkThreads() {
        return maxBenchmarkThreads;
    }

    public int getTrialsPrExperiment() {
        return trialsPrExperiment;
    }

    public ShortDuration getTimeLimit() {
        return ShortDuration.of(5, TimeUnit.MINUTES); // TODO Make this configurable?
    }

    public float getMinFailureLimit() {
        Float result = percentileFailureLimits.get(0F);
        if (result == null) {
            return NOT_ENABLED;
        } else {
            return result;
        }
    }

    public float getMaxFailureLimit() {
        Float result = percentileFailureLimits.get(1.0F);
        if (result == null) {
            return NOT_ENABLED;
        } else {
            return result;
        }
    }

    public float getMeanFailureLimit() {
        return meanFailureLimit;
    }

    public float getMedianFailureLimit() {
        Float result = percentileFailureLimits.get(0.5F);
        if (result == null) {
            return NOT_ENABLED;
        } else {
            return result;
        }
    }

    public Set<ResultProcessor> getResultProcessors() {
        return resultProcessors;
    }

    /**
     * Returns the change in percent that is the maximum amount a trial can change compared
     * to its baseline for the given percentile.
     *
     * @param percentile The difference in percent a trial must change for this percentile
     * @return Maximum change in percent for the percentile: [0, 1.0] or {@link #NOT_ENABLED}.
     */
    public float getPercentileFailureLimit(float percentile) {
        Float result = percentileFailureLimits.get(percentile);
        if (result == null) {
            return NOT_ENABLED;
        } else {
            return result;
        }
    }

    /**
     * Returns a list of all percentiles that should fail if it deviates from the baseline.
     * Min. is returned as {@code 0F} while max is returned as {@code 1.0F}.
     *
     * Being in this list does not mean that the percentile is enabled. It is still necessary to test for
     * {@link SpannerConfig#NOT_ENABLED}.
     */
    public Set<Float> getPercentileFailureLimits() {
        return percentileFailureLimits.keySet();
    }

    /**
     * Builder for fluent construction of a SpannerConfig object.
     */
    public static class Builder {
        private File resultsFile = null;
        private File baseLineFile = null;
        private boolean warnIfWrongTestGranularity = true;
        private boolean uploadResults = false;
        private String apiKey = "";
        private URL uploadUrl = getUrl("https://microbenchmarks.appspot.com");
        private int maxBenchmarkThreads = 1; // Maximum number of concurrent benchmark threads.
        private Set<InstrumentConfig> instrumentationConfigs = new HashSet<>();
        private int trialsPrExperiment = 1;
        private float meanFailureLimit = NOT_ENABLED;
        // All values > 0. All keys: [0,100] -> 0 = Min, 100 = max, 50 = median
        private Map<Float, Float> percentileFailureLimits = new HashMap<>();
        private Set<ResultProcessor> resultProcessors = new HashSet<>();

        public Builder() {
        }

        /**
         * Constructs an instance of {@link SpannerConfig}.
         */
        public SpannerConfig build() {
            return new SpannerConfig(this);
        }

        /**
         * Set the folder where any benchmark results should be stored.
         *
         * @param dir Reference to folder.
         * @return Builder object.
         */
        public Builder saveResults(File dir, String filename) {
            if (dir != null) {
                dir.mkdirs();
            }
            checkValidWritableFolder(dir);
            this.resultsFile = new File(dir, filename);
            return this;
        }

        /**
         * Set a baseline for the tests being run.
         *
         * @param file Baseline file to use.
         * @return Builder object.
         */
        public Builder useBaseline(File file) {
            checkNotNull(file, "Baseline file was null");
            if (file.isDirectory()) {
                throw new IllegalArgumentException("File is a directory, not a baseline file: " + file);
            }
            if (!file.exists()) {
                throw new IllegalArgumentException("Baseline file does not exists: " + file);
            }
            this.baseLineFile = file;
            return this;
        }

        /**
         * Setting this will cause Spanner to verify that the granularity of the tests are set correctly.
         * Otherwise it will throw an error.
         */
        public Builder warnIfWrongTestGranularity() {
            this.warnIfWrongTestGranularity = true;
            return this;
        }

        public Builder uploadResults() {
            uploadResults = true;
            return this;
        }

        public Builder uploadUrl(String url) {
            this.uploadUrl = getUrl(url);
            return this;
        }

        public Builder apiKey(String apiKey) {
            checkNotNull(apiKey, "Only non-null keys allowed");
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Add a new Instrumentation. Setting this will override all default instruments.
         */
        public Builder addInstrument(InstrumentConfig config) {
            checkNotNull(config, "Only non-null configurations allowed");
            instrumentationConfigs.add(config);
            return this;
        }

        /**
         * The difference in percent from the baseline minimum value allowed before the experiment will be a failure.
         * Use {@code -1} to disable it.
         *
         * Default value is {@code -1}.

         * @param percentage {@code 0.0F} is 0%, {@code 1.0F} is 100%.
         * @return the Builder.
         */
        public Builder minFailureLimit(float percentage) {
            return percentileFailureLimit(0F, percentage);
        }

        /**
         * The difference in percent from the baseline maximum value allowed before the experiment will be a failure.
         * Use {@code -1} to disable it.
         *
         * Default value is {@code -1}.

         * @param percentage {@code 0.0F} is 0%, {@code 1.0F} is 100%.
         * @return the Builder.
         */
        public Builder maxFailureLimit(float percentage) {
            return percentileFailureLimit(100.0F, percentage);
        }

        /**
         * The difference in percent from the baseline mean value allowed before the experiment will be a failure.
         * Use {@code -1} to disable it.
         *
         * Default value is {@code -1}.

         * @param percentage {@code 0.0F} is 0%, {@code 1.0F} is 100%.
         * @return the Builder.
         */
        public Builder medianFailureLimit(float percentage) {
            return percentileFailureLimit(50.0F, percentage);
        }

        /**
         * The difference in percent from the baseline percentile allowed before the experiment will be a failure.
         * Use {@code -1} to disable it.
         *
         * Default value is {@code -1}.
         *
         * @param percentile [0.0F, 100.0F]
         * @param percentage {@code 0.0F} is 0%, {@code 1.0F} is 100%.
         * @return the Builder.
         */
        public Builder percentileFailureLimit(float percentile, float percentage) {
            if (percentage < 0.0F || percentile > 100.0F) {
                throw new IllegalArgumentException("Percentile must be [0, 100.0]. Yours was: " + percentile);
            }
            percentileFailureLimits.put(percentile, Math.abs(percentage));
            return this;
        }

        /**
         * The difference in percent from the baseline average (mean) value allowed before the experiment will be a failure.
         * Use {@code -1} to disable it.
         *
         * Default value is {@code -1}.

         * @param percentage {@code 0.0F} is 0%, {@code 1.0F} is 100%.
         * @return the Builder.
         */
        public Builder meanFailureLimit(float percentage) {
            meanFailureLimit = Math.abs(percentage);
            return this;
        }

        /**
         * Maximum number of worker threads used to run the benchmarks.
         * The default value is {@code 1}.
         *
         * @param threadCount number of threads that can run benchmarks.
         * @return the Builder.
         */
        public Builder maxBenchmarkThreads(int threadCount) {
            this.maxBenchmarkThreads = threadCount;
            return this;
        }

        /**
         * Set the number of trials run for each experiment. Each trial is self-contained and will output
         * its results independently of the other trials for the same experiment.
         *
         * The default value is {@code 1}.
         *
         * @param trials number of trials to run pr. experiment.
         * @return the Builder.
         */
        public Builder trialsPrExperiment(int trials) {
            this.trialsPrExperiment = trials;
            return this;
        }

        /**
         * Add a custom results processor that can process all trial results.
         * This can e.g be used to convert the trial results to some custom output.
         *
         * @param processor {@link ResultProcessor} to add.
         * @return the Builder.
         */
        public Builder addResultProcessor(ResultProcessor processor) {
            checkNotNull(processor, "Non-null processor required.");
            this.resultProcessors.add(processor);
            return this;
        }

        private void checkNotNull(Object obj, String errorMessage) {
            if (obj == null) {
                throw new IllegalArgumentException(errorMessage);
            }
        }

        private URL getUrl(String url) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private void checkValidWritableFolder(File dir) {
            checkNotNull(dir, "Non-null results folder required.");
            if (!dir.isDirectory() || !dir.canWrite()) {
                throw new IllegalArgumentException("Results folder is either not a directory or not writable.");
            }
        }
    }
}
