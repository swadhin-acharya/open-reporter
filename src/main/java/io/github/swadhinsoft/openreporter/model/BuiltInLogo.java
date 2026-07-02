package io.github.swadhinsoft.openreporter.model;

/**
 * Built-in icon registry for Execution Summary logos.
 * Each entry maps to a Font Awesome icon class and colour.
 */
public enum BuiltInLogo {

    ANDROID("fa-brands fa-android", "#3ddc84"),
    IOS("fa-brands fa-apple", "#555"),
    WINDOWS("fa-brands fa-windows", "#0078d4"),
    MACOS("fa-brands fa-apple", "#888"),
    LINUX("fa-brands fa-linux", "#fcc624"),
    MQTT("fa-solid fa-wifi", "#ff7139"),
    APPIUM("fa-solid fa-mobile-screen", "#3ddc84"),
    SELENIUM("fa-brands fa-chrome", "#4285f4"),
    JAVA("fa-brands fa-java", "#f89820"),
    TESTNG("fa-solid fa-vial", "#3fb950"),
    JUNIT("fa-solid fa-flask", "#58a6ff"),
    CUCUMBER("fa-solid fa-leaf", "#3fb950"),
    RESTASSURED("fa-solid fa-code", "#ffa657"),
    KAFKA("fa-solid fa-server", "#8b949e"),
    WEB("fa-solid fa-globe", "#58a6ff"),
    CHROME("fa-brands fa-chrome", "#4285f4"),
    EDGE("fa-brands fa-edge", "#0078d4"),
    FIREFOX("fa-brands fa-firefox-browser", "#ff7139"),
    SAFARI("fa-brands fa-safari", "#006cff"),
    DOCKER("fa-brands fa-docker", "#2496ed"),
    JENKINS("fa-brands fa-jenkins", "#d24939"),
    GITHUB("fa-brands fa-github", "var(--text)"),
    GITLAB("fa-brands fa-gitlab", "#fc6d26"),
    AZURE("fa-brands fa-microsoft", "#0078d4"),
    AWS("fa-brands fa-aws", "#ff9900"),
    BROWSERSTACK("fa-solid fa-cloud", "#ff7139"),
    DEVICE("fa-solid fa-mobile-screen", "#58a6ff"),
    FIRMWARE("fa-solid fa-chip", "#ffa657"),
    APP("fa-solid fa-tag", "#a5d6a7"),
    ENVIRONMENT("fa-solid fa-cloud", "#58a6ff"),
    COUNTRY("fa-solid fa-flag", "#ff7139"),
    NETWORK("fa-solid fa-network-wired", "#8b949e"),
    BRANCH("fa-solid fa-code-branch", "#d29922"),
    BUILD("fa-solid fa-hashtag", "#a5d6ff"),
    COMMIT("fa-solid fa-code-commit", "#8b949e"),
    USER("fa-solid fa-user", "#8b949e"),
    EXECUTION("fa-solid fa-fingerprint", "#58a6ff"),
    CLOCK("fa-regular fa-clock", "#8b949e"),
    WIFI("fa-solid fa-wifi", "#3fb950"),
    SIGNAL("fa-solid fa-signal", "#3fb950"),
    MICROCHIP("fa-solid fa-microchip", "#58a6ff");

    private final String faClass;
    private final String color;

    BuiltInLogo(String faClass, String color) {
        this.faClass = faClass;
        this.color = color;
    }

    public String toHtml() {
        return "<i class=\"" + faClass + "\" style=\"color:" + color + "\"></i>";
    }
}
