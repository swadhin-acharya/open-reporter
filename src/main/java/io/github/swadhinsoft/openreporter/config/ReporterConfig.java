package io.github.swadhinsoft.openreporter.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * <p>Minimal {@code openreporter.json}:
 * <pre>{@code
 * {
 *   "title":       "My Test Report",
 *   "logo":        "src/test/resources/logo.png",
 *   "outputDir":   "target/open-reporter",
 *   "environment": "QA",
 *   "reportFileName": "report.html",
 *   "timestampedReport": false,
 *   "executionInfo": {
 *     "Platform":     "Android 16",
 *     "OS":           "Windows 11",
 *     "Device":       "Pixel 8 Pro",
 *     "App Version":  "3.1.4",
 *     "Environment":  "QA",
 *     "Custom Key":   "Custom Value"
 *   },
 *   "executionLogos": {
 *     "Platform":   "ANDROID",
 *     "MQTT Broker": "MQTT"
 *   }
 * }
 * }</pre>
 */
public class ReporterConfig {

    private static final ReporterConfig INSTANCE = new ReporterConfig();

    private String title       = "Test Execution Report";
    private String logo        = "";
    private String outputDir   = "target/open-reporter";
    private String environment = "";
    private String reportFileName = "report.html";
    private boolean timestampedReport = false;

    private final Map<String, String> executionInfo  = new LinkedHashMap<>();
    private final Map<String, String> executionLogos = new LinkedHashMap<>();

    private ReporterConfig() { load(); }

    public static ReporterConfig getInstance() { return INSTANCE; }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getTitle()       { return title; }
    public String getLogo()        { return logo; }
    public String getOutputDir()   { return outputDir; }
    public String getEnvironment() { return environment; }
    public String getReportFileName() { return reportFileName; }
    public boolean isTimestampedReport() { return timestampedReport; }
    public Map<String, String> getExecutionInfo()  { return executionInfo; }
    public Map<String, String> getExecutionLogos() { return executionLogos; }

    // ── Execution Info Fluent API ────────────────────────────────────────────

    public ReporterConfig platform(String v)       { return put("Platform", v); }
    public ReporterConfig os(String v)             { return put("OS", v); }
    public ReporterConfig device(String v)         { return put("Device", v); }
    public ReporterConfig deviceModel(String v)    { return put("Device Model", v); }
    public ReporterConfig phone(String v)          { return put("Phone", v); }
    public ReporterConfig appVersion(String v)     { return put("App Version", v); }
    public ReporterConfig firmwareVersion(String v){ return put("Firmware Version", v); }
    public ReporterConfig environment(String v)    { this.environment = v; return put("Environment", v); }
    public ReporterConfig country(String v)        { return put("Country", v); }
    public ReporterConfig router(String v)         { return put("Router", v); }
    public ReporterConfig wifiBand(String v)       { return put("WiFi Band", v); }
    public ReporterConfig mqttBroker(String v)     { return put("MQTT Broker", v); }
    public ReporterConfig macAddress(String v)     { return put("MAC Address", v); }
    public ReporterConfig buildNumber(String v)    { return put("Build Number", v); }
    public ReporterConfig branch(String v)         { return put("Branch", v); }
    public ReporterConfig commit(String v)         { return put("Commit", v); }
    public ReporterConfig executionType(String v)  { return put("Execution Type", v); }
    public ReporterConfig executedBy(String v)     { return put("Executed By", v); }
    public ReporterConfig executionId(String v)    { return put("Execution ID", v); }

    /** Add a custom execution info key/value pair. */
    public ReporterConfig addExecutionInfo(String key, String value) {
        return put(key, value);
    }

    // ── Logo Fluent API ─────────────────────────────────────────────────────

    /** Map a built-in or custom logo to an execution info key. */
    public ReporterConfig executionLogo(String fieldKey, String logoName) {
        if (logoName != null && !logoName.isEmpty()) {
            executionLogos.put(fieldKey, logoName);
        }
        return this;
    }

    public ReporterConfig platformLogo(String v)     { return executionLogo("Platform", v); }
    public ReporterConfig osLogo(String v)           { return executionLogo("OS", v); }
    public ReporterConfig mqttLogo(String v)         { return executionLogo("MQTT Broker", v); }

    // ── Private ───────────────────────────────────────────────────────────────

    private ReporterConfig put(String key, String value) {
        if (value != null && !value.isEmpty()) {
            executionInfo.put(key, value);
        }
        return this;
    }

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

            JsonNode info = root.path("executionInfo");
            if (info.isObject()) {
                info.fieldNames().forEachRemaining(k -> {
                    String v = info.path(k).asText();
                    if (!v.isEmpty()) executionInfo.put(k, v);
                });
            }

            JsonNode logos = root.path("executionLogos");
            if (logos.isObject()) {
                logos.fieldNames().forEachRemaining(k -> {
                    String v = logos.path(k).asText();
                    if (!v.isEmpty()) executionLogos.put(k, v);
                });
            }
        } catch (Exception e) {
            System.err.println("[OpenReporter] Config load warning — using defaults: " + e.getMessage());
        }
    }

    private JsonNode readConfigNode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 1. System property
        String sysProp = System.getProperty("openreporter.config");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (f.exists()) return mapper.readTree(f);
            System.err.println("[OpenReporter] Config file from -Dopenreporter.config not found: " + sysProp);
        }

        // 2. Project root
        File rootFile = new File(System.getProperty("user.dir"), "openreporter.json");
        if (rootFile.exists()) return mapper.readTree(rootFile);

        // 3. Classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("openreporter.json")) {
            if (is != null) return mapper.readTree(is);
        }

        return null; // use defaults
    }
}
