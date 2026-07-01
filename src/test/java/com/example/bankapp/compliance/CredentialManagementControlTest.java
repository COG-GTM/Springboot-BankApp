package com.example.bankapp.compliance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Re-performs audit control ITGC-SEC-06 (Credential & secrets management) against the
 * committed configuration.
 *
 * <p>Regulations: PCI DSS 4.0 Req 8.6.2 (no hard-coded passwords in configuration/property
 * files), Req 2.2 (secure configuration); SOX ICFR (§404); GDPR Art. 32 (confidentiality).
 *
 * <p>The control PASSES when every credential value in {@code application.properties} is
 * sourced from the environment via a bare {@code ${ENV_VAR}} placeholder — with no inline
 * literal default and no hard-coded secret anywhere in the file. This is a plain unit test
 * (no Spring context / database) so it can run as a fast, standalone CI gate.
 */
class CredentialManagementControlTest {

    private static final String CONFIG = "application.properties";

    /** A bare environment reference with NO inline default, e.g. ${SPRING_DATASOURCE_PASSWORD}. */
    private static final Pattern BARE_ENV_REF = Pattern.compile("^\\$\\{[A-Za-z0-9_.]+}$");

    /** Property keys that hold a secret and therefore must never carry a literal value. */
    private static final Pattern SECRET_KEY = Pattern.compile(".*(password|passwd|secret|token|apikey|api[-_.]?key).*",
            Pattern.CASE_INSENSITIVE);

    private String rawConfig() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG)) {
            assertNotNull(in, CONFIG + " must be present on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Properties loadConfig() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG)) {
            assertNotNull(in, CONFIG + " must be present on the classpath");
            props.load(in);
        }
        return props;
    }

    @Test
    @DisplayName("ITGC-SEC-06: DB password is externalised to the environment (no committed default)")
    void datasourcePasswordIsExternalised() throws Exception {
        Properties props = loadConfig();
        String password = props.getProperty("spring.datasource.password");
        assertNotNull(password, "spring.datasource.password must be defined");
        assertTrue(BARE_ENV_REF.matcher(password.trim()).matches(),
                "spring.datasource.password must be a bare ${ENV} reference with no inline default, but was: '"
                        + password + "'");
    }

    @Test
    @DisplayName("ITGC-SEC-06: no property that holds a secret carries a hard-coded literal value")
    void noSecretPropertyHasLiteralValue() throws Exception {
        Properties props = loadConfig();
        String offenders = props.stringPropertyNames().stream()
                .filter(k -> SECRET_KEY.matcher(k).matches())
                .filter(k -> !BARE_ENV_REF.matcher(props.getProperty(k).trim()).matches())
                .map(k -> k + "=" + props.getProperty(k))
                .collect(Collectors.joining(", "));
        assertTrue(offenders.isEmpty(),
                "Secret-bearing properties must be bare ${ENV} references, but these are not: " + offenders);
    }

    @Test
    @DisplayName("ITGC-SEC-06: the previously-committed DB password does not reappear in config")
    void knownCommittedSecretIsNotPresent() throws Exception {
        String config = rawConfig();
        assertFalse(config.contains("Test@123"),
                "The previously-committed DB password 'Test@123' must not be present in " + CONFIG);
    }
}
