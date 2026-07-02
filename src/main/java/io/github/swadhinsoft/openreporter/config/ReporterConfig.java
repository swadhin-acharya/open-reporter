package io.github.swadhinsoft.openreporter.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.swadhinsoft.openreporter.model.BuiltInLogo;

/**
 * Loads OpenReporter configuration from {@code openreporter.json}.
 *
 * <p>Lookup order:
 * <ol>
 *   <li>JVM system property: {@code -Dopenreporter.config=/path/to/file}</li>
 *   <li>{@code openreporter.json} in the project root ({@code user.dir})</li>
 *   <li>{@code openreporter.json} on the classpath (e.g. {@code src/test/resources})</li>
 *   <li>Built-in defaults — no file required</li>
 * </ol>
 *
 * <p>Example {@code openreporter.json}:
 * <pre>{@code
 * {
 *   "title":       "My Test Report",
 *   "logo":        "src/test/resources/logo.png",
 *   "outputDir":   "target/open-reporter",
 *   "environment": "QA",
 *   "reportFileName": "report.html",
 *   "timestampedReport": false,
 *   "executionSummary": {
 *     "Platform":     { "value": "Android 16",       "logo": "ANDROID" },
 *     "Device":       { "value": "DENALI_TRADE" },
 *     "MQTT Broker":  { "value": "Mosquitto",         "logo": "MQTT" },
 *     "Custom Key":   { "value": "Custom Value" }
 *   }
 * }
 * }</pre>
 */
public class ReporterConfig {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecutionEntry {
        private String value;
        private String logo;

        public ExecutionEntry() {}

        public ExecutionEntry(String value) { this.value = value; }

        public ExecutionEntry(String value, String logo) {
            this.value = value;
            this.logo = logo;
        }

        public String getValue() { return value; }
        public String getLogo()  { return logo; }

        public void setValue(String v) { this.value = v; }
        public void setLogo(String v)  { this.logo = v; }
    }

    private static final ReporterConfig INSTANCE = new ReporterConfig();

    private String title       = "Test Execution Report";
    private String logo        = "";
    private String outputDir   = "target/open-reporter";
    private String environment = "";
    private String reportFileName = "report.html";
    private boolean timestampedReport = false;

    private final Map<String, ExecutionEntry> executionSummary = new LinkedHashMap<>();

    private ReporterConfig() { load(); }

    public static ReporterConfig getInstance() { return INSTANCE; }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getTitle()       { return title; }
    public String getLogo()        { return logo; }
    public String getOutputDir()   { return outputDir; }
    public String getEnvironment() { return environment; }
    public String getReportFileName() { return reportFileName; }
    public boolean isTimestampedReport() { return timestampedReport; }
    public Map<String, ExecutionEntry> getExecutionSummary() { return executionSummary; }

    // ── Execution Summary Fluent API ─────────────────────────────────────────

    /** Set an execution info field with value only. */
    public ReporterConfig executionInfo(String key, String value) {
        if (value != null && !value.isEmpty()) {
            executionSummary.computeIfAbsent(key, k -> new ExecutionEntry()).setValue(value);
        }
        return this;
    }

    /** Set an execution info field with a built-in logo. */
    public ReporterConfig executionInfo(String key, String value, BuiltInLogo logo) {
        if (value != null && !value.isEmpty()) {
            ExecutionEntry entry = executionSummary.computeIfAbsent(key, k -> new ExecutionEntry());
            entry.setValue(value);
            if (logo != null) entry.setLogo(logo.name());
        }
        return this;
    }

    /** Set an execution info field with a custom logo (path or base64). */
    public ReporterConfig executionInfo(String key, String value, String logoPath) {
        if (value != null && !value.isEmpty()) {
            ExecutionEntry entry = executionSummary.computeIfAbsent(key, k -> new ExecutionEntry());
            entry.setValue(value);
            if (logoPath != null && !logoPath.isEmpty()) entry.setLogo(logoPath);
        }
        return this;
    }

    /** Add a custom execution info key/value pair (no logo). */
    public ReporterConfig addExecutionInfo(String key, String value) {
        return executionInfo(key, value);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void load() {
        try {
            JsonNode root = readConfigNode();
            if (root == null) return;
            title       = root.path("title").asText(title);
            logo        = root.path("logo").asText(logo);
            outputDir   = root.path("outputDir").asText(outputDir);
            environment = root.path("environment").asText(environment);
            reportFileName = root.path("reportFileName").asText(reportFileName);
            timestampedReport = root.path("timestampedReport").asBoolean(timestampedReport);

            JsonNode summary = root.path("executionSummary");
            if (summary.isObject()) {
                ObjectMapper mapper = new ObjectMapper();
                summary.fieldNames().forEachRemaining(k -> {
                    JsonNode node = summary.path(k);
                    if (node.isObject()) {
                        String val = node.path("value").asText();
                        if (!val.isEmpty()) {
                            ExecutionEntry entry = new ExecutionEntry(val);
                            String logoVal = node.path("logo").asText();
                            if (!logoVal.isEmpty()) entry.setLogo(logoVal);
                            executionSummary.put(k, entry);
                        }
                    } else {
                        String val = node.asText();
                        if (!val.isEmpty()) {
                            executionSummary.put(k, new ExecutionEntry(val));
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[OpenReporter] Config load warning — using defaults: " + e.getMessage());
        }
    }

    private JsonNode readConfigNode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String sysProp = System.getProperty("openreporter.config");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (f.exists()) return mapper.readTree(f);
            System.err.println("[OpenReporter] Config file from -Dopenreporter.config not found: " + sysProp);
        }

        File rootFile = new File(System.getProperty("user.dir"), "openreporter.json");
        if (rootFile.exists()) return mapper.readTree(rootFile);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("openreporter.json")) {
            if (is != null) return mapper.readTree(is);
        }

        return null;
    }
}
