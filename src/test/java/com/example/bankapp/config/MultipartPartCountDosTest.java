package com.example.bankapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.multipart.MultipartResolver;

/**
 * CVE-2025-48988: Tomcat before 10.1.42 has no limit on the number of multipart parts.
 */
class MultipartPartCountDosTest {

    @Test
    void embeddedTomcatLimitsMultipartPartCount() {
        int[] version = parse(ServerInfo.getServerNumber());
        assertThat(version[0]).isEqualTo(10);
        assertThat(version[1]).isEqualTo(1);
        assertThat(version[2]).as("Tomcat %s", ServerInfo.getServerNumber()).isGreaterThanOrEqualTo(42);
    }

    @Test
    void applicationPropertiesDisableMultipartParsing() {
        new WebApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(MultipartAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(MultipartConfigElement.class);
                    assertThat(context).doesNotHaveBean(MultipartResolver.class);
                });
    }

    private static int[] parse(String serverNumber) {
        String[] parts = serverNumber.split("\\.");
        return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }
}
