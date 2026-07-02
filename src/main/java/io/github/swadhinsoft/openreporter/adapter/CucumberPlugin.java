package io.github.swadhinsoft.openreporter.adapter;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.WriteEvent;
import io.github.swadhinsoft.openreporter.OpenReporter;
import io.github.swadhinsoft.openreporter.model.TestResultModel;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cucumber 7+ adapter for OpenReporter.
 *
 * <h3>Setup</h3>
 * Add to {@code @CucumberOptions}:
 * <pre>{@code
 * @CucumberOptions(plugin = {"io.github.swadhinsoft.openreporter.adapter.CucumberPlugin"})
 * }</pre>
 *
 * In your Cucumber {@code @Before} hook:
 * <pre>{@code
 * OpenReporter.getInstance().registerDriver(driver);
 * OpenReporter.getInstance().setBrowser("chrome");
 * }</pre>
 *
 * In your Cucumber {@code @After} hook:
 * <pre>{@code
 * OpenReporter.getInstance().unregisterDriver();
 * }</pre>
 */
public class CucumberPlugin implements ConcurrentEventListener {

    private static final OpenReporter REPORTER = OpenReporter.getInstance();
    private static final Map<String, ScenarioState> SCENARIOS = new ConcurrentHashMap<>();
    private static final Set<EventPublisher> PUBLISHERS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        if (!PUBLISHERS.add(publisher)) {
            return;
        }

        publisher.registerHandlerFor(TestCaseStarted.class,  this::onTestCaseStarted);
        publisher.registerHandlerFor(TestStepFinished.class,  this::onTestStepFinished);
        publisher.registerHandlerFor(WriteEvent.class,        this::onWrite);
        publisher.registerHandlerFor(EmbedEvent.class,        this::onEmbed);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class,  e -> REPORTER.generateReport());
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void onTestCaseStarted(TestCaseStarted event) {
        String name    = event.getTestCase().getName();
        String browser = REPORTER.getRegisteredBrowser();
        String tags    = event.getTestCase().getTags().toString();

        SCENARIOS.put(scenarioKey(event), new ScenarioState());
        REPORTER.startTest(name, name, "Cucumber", browser, "");
        REPORTER.log("Scenario: " + name);
        REPORTER.log("Browser: " + browser);
        if (!tags.isEmpty() && !tags.equals("[]")) {
            REPORTER.log("Tags: " + tags);
        }
    }

    private void onTestStepFinished(TestStepFinished event) {
        ScenarioState state = state(scenarioKey(event));
        state.sawExecution = true;

        if (!(event.getTestStep() instanceof PickleStepTestStep)) {
            return;
        }

        state.pickleSteps++;
        PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
        String description = step.getStep().getKeyword() + step.getStep().getText();
        String rawStatus = event.getResult().getStatus().name();
        REPORTER.step(description + statusSuffix(rawStatus));
        TestResultModel model = REPORTER.getCurrentTest();
        if (model != null && model.getSteps() != null && !model.getSteps().isEmpty()) {
            model.getSteps().get(model.getSteps().size() - 1).setStatus(rawStatus);
        }
    }

    private void onWrite(WriteEvent event) {
        ScenarioState state = state(scenarioKey(event));
        state.sawExecution = true;
        REPORTER.log(event.getText());
    }

    private void onEmbed(EmbedEvent event) {
        ScenarioState state = state(scenarioKey(event));
        state.sawExecution = true;

        String mimeType = event.getMediaType();
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = event.getMimeType();
        }
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/octet-stream";
        }

        String name = event.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Attachment " + (state.attachments + 1);
        }
        state.attachments++;

        byte[] data = event.getData();
        String content = isText(mimeType)
                ? new String(data, StandardCharsets.UTF_8)
                : "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(data);

        REPORTER.attachment(name, mimeType, content);
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        ScenarioState state = SCENARIOS.remove(scenarioKey(event));
        if (state != null && !state.sawExecution) {
            REPORTER.discardCurrentTest();
            return;
        }

        TestResultModel m = REPORTER.getCurrentTest();
        String rawStatus  = event.getResult().getStatus().name();

        String status;
        switch (rawStatus) {
            case "PASSED":
                status = "PASSED";
                REPORTER.log("Scenario passed");
                break;
            case "FAILED":
                status = "FAILED";
                if (m != null && event.getResult().getError() != null) {
                    Throwable error = event.getResult().getError();
                    m.addLog("Scenario FAILED — " + error.getMessage());
                    m.setErrorMessage(error.getMessage());
                    m.setStackTrace(toStackTrace(error));

                    WebDriver driver = REPORTER.getRegisteredDriver();
                    if (driver != null) {
                        try {
                            String screenshot = "data:image/png;base64,"
                                    + ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                            m.setScreenshotBase64(screenshot);
                            m.addAttachment(new io.github.swadhinsoft.openreporter.model.AttachmentModel(
                                    "Failure screenshot", "image/png", screenshot, null));
                            m.addLog("Screenshot captured");
                        } catch (Exception e) {
                            m.addLog("Screenshot capture failed: " + e.getMessage());
                        }
                    }
                }
                break;
            default: // SKIPPED, PENDING, UNDEFINED, AMBIGUOUS
                status = "SKIPPED";
                REPORTER.log("Scenario " + rawStatus.toLowerCase());
                break;
        }

        Long durationMs = event.getResult().getDuration() != null
                ? event.getResult().getDuration().toMillis()
                : null;
        REPORTER.finishTest(status, durationMs);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private ScenarioState state(String key) {
        return SCENARIOS.computeIfAbsent(key, ignored -> new ScenarioState());
    }

    private String scenarioKey(io.cucumber.plugin.event.TestCaseEvent event) {
        return event.getTestCase().getId() + ":" + Thread.currentThread().getId();
    }

    private String statusSuffix(String status) {
        return "PASSED".equals(status) || "FAILED".equals(status) ? "" : " [" + status.toLowerCase() + "]";
    }

    private boolean isText(String mimeType) {
        String lower = mimeType.toLowerCase();
        return lower.startsWith("text/")
                || lower.contains("json")
                || lower.contains("xml")
                || lower.contains("javascript")
                || lower.contains("yaml")
                || lower.contains("csv")
                || lower.contains("log");
    }

    private static class ScenarioState {
        private boolean sawExecution;
        private int pickleSteps;
        private int attachments;
    }
}
