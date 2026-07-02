package io.github.swadhinsoft.openreporter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single named step within a test.
 * Created via {@link io.github.swadhinsoft.openreporter.OpenReporter#step},
 * {@code stepPass}, or {@code stepFail}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepModel {

    private String description;
    private String status;    // INFO | PASS | FAIL (for LOG) or PASSED | FAILED | SKIPPED (for GHERKIN)
    private String type;      // "LOG" for framework logs, "GHERKIN" for Cucumber steps; null treated as LOG
    private Long   durationMs;
    private long   timestamp;

    /** Required for Jackson deserialisation. */
    public StepModel() {}

    public StepModel(String description, String status) {
        this(description, status, null, null);
    }

    public StepModel(String description, String status, String type) {
        this(description, status, type, null);
    }

    public StepModel(String description, String status, String type, Long durationMs) {
        this.description = description;
        this.status      = status;
        this.type        = type;
        this.durationMs  = durationMs;
        this.timestamp   = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public String getStatus()      { return status; }
    public String getType()        { return type; }
    public Long   getDurationMs()  { return durationMs; }
    public long   getTimestamp()   { return timestamp; }

    public void setDescription(String v) { this.description = v; }
    public void setStatus(String v)      { this.status = v; }
    public void setType(String v)        { this.type = v; }
    public void setDurationMs(Long v)    { this.durationMs = v; }
    public void setTimestamp(long v)     { this.timestamp = v; }
}
