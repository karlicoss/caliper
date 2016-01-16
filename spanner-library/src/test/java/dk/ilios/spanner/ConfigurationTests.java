package dk.ilios.spanner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class ConfigurationTests {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDefaultConfig() throws MalformedURLException {
        SpannerConfig defaultConfig = new SpannerConfig.Builder().build();
        assertEquals("", defaultConfig.getApiKey());
        assertEquals(null, defaultConfig.getBaseLineFile());
        assertEquals(null, defaultConfig.getResultsFile());
        assertEquals(SpannerConfig.NOT_ENABLED, defaultConfig.getMinFailureLimit(), 0.0f);
        assertEquals(SpannerConfig.NOT_ENABLED, defaultConfig.getMeanFailureLimit(), 0.0f);
        assertEquals(SpannerConfig.NOT_ENABLED, defaultConfig.getMedianFailureLimit(), 0.0f);
        assertEquals(SpannerConfig.NOT_ENABLED, defaultConfig.getMaxFailureLimit(), 0.0f);
        assertFalse(defaultConfig.isUploadResults());
        assertTrue(defaultConfig.warnIfWrongTestGranularity());
        assertEquals(new URL("https://microbenchmarks.appspot.com"), defaultConfig.getUploadUrl());
        assertEquals(1, defaultConfig.getNoBenchmarkThreads());
        assertEquals(1, defaultConfig.getTrialsPrExperiment());
        assertEquals(0, defaultConfig.getResultProcessors().size());
        assertEquals(2, defaultConfig.getInstrumentConfigurations().size());
    }

    @Test
    public void testWrongResultsFolders() throws IOException {
        File nullFolder = null;
        File readonlyFolder = tempFolder.newFolder("foo");
        readonlyFolder.setWritable(false);

        SpannerConfig.Builder builder = new SpannerConfig.Builder();
        try {
            builder.saveResults(nullFolder, "foo");
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            builder.saveResults(readonlyFolder, "foo");
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testWrongBaselineFile() throws IOException {
        File nullFile = null;
        File folder = tempFolder.newFolder("foo");

        SpannerConfig.Builder builder = new SpannerConfig.Builder();
        try {
            builder.useBaseline(nullFile);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            builder.useBaseline(folder);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }
}
