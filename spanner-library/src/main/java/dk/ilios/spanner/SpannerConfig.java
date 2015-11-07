package dk.ilios.spanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.ilios.spanner.internal.CustomMeasurementInstrument;
import dk.ilios.spanner.internal.Instrument;
import dk.ilios.spanner.internal.RuntimeInstrument;

/**
 * Class for adding custom configuration of a Spanner run.
 */
public class SpannerConfig {

    private static final Class<? extends Instrument> RUNTIME_INSTRUMENT = RuntimeInstrument.class;
    private static final Class<? extends Instrument> CUSTOM_INSTRUMENT = CustomMeasurementInstrument.class;

    private final File resultsFolder;
    private final File baseLineFile;
    private final boolean warnIfWrongTestGranularity;
    private final File baselineOutputFile;
    private final URL uploadUrl;
    private final String apiKey;
    private final boolean uploadResults;
    private float baselineFailure;
    private Set<Class<? extends Instrument>> instruments;

    private SpannerConfig(Builder builder) {
        this.resultsFolder = builder.resultsFolder;
        this.baseLineFile = builder.baseLineFile;
        this.warnIfWrongTestGranularity = builder.warnIfWrongTestGranularity;
        this.baselineOutputFile = builder.baselineOutputFile;
        this.uploadResults = builder.uploadResults;
        this.uploadUrl = builder.uploadUrl;
        this.apiKey = builder.apiKey;
        this.baselineFailure = builder.baselineFailure;
        this.instruments = new HashSet<>(2);
        instruments.add(RUNTIME_INSTRUMENT);
        instruments.add(CUSTOM_INSTRUMENT);
    }

    public File getResultsFolder() {
        return resultsFolder;
    }

    public File getBaseLineFile() {
        return baseLineFile;
    }

    public boolean warnIfWrongTestGranularity() {
        return warnIfWrongTestGranularity;
    }

    public File getBaselineOutputFile() {
        return baselineOutputFile;
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

    public float getBaselineFailure() {
        return baselineFailure;
    }

    public Set<Class<? extends Instrument>> getInstruments() {
        return instruments;
    }
    /**
     * Builder for fluent construction of a SpannerConfig object.
     */
    public static class Builder {
        private File resultsFolder = null;
        private File baseLineFile = null;
        private boolean warnIfWrongTestGranularity = true;
        private File baselineOutputFile = null;
        private boolean uploadResults = false;
        private String apiKey = "";
        private URL uploadUrl = getUrl("https://microbenchmarks.appspot.com");
        private float baselineFailure = 0.2f; // 20% difference from baseline will fail the experiment.

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
        public Builder saveResults(File dir) {
            dir.mkdirs();
            checkValidWritableFolder(dir);
            this.resultsFolder = dir;
            return this;
        }

        // TODO Add support for overriding the filename

        /**
         * Set a baseline for the tests being run.
         *
         * @param file Reference to the baseline file (see .
         * @return Builder object.
         */
        public Builder useBaseline(File file) {
            checkNotNull(file, "Baseline file was null");
            if (file.isDirectory()) {
                throw new IllegalArgumentException("File is a directory, not a baseline file: " + file);
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

        /**
         * Save the result of this of benchmark as a new baseline file called {@code baseline.json}.
         * @param dir Folder to save the new baseline file in.
         */
        public Builder createBaseline(File dir) {
            checkValidWritableFolder(dir);
            this.baselineOutputFile = new File(dir, "baseline.json");
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
         * The difference in percent from the baseline allowed before the experiment will be a failure.
         * @param percentage [0-1.0] for [0-100%]
         * @return the Builder.
         */
        public Builder baselineFailure(float percentage) {
            baselineFailure = Math.abs(percentage);
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
