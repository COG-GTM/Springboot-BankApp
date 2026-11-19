package com.example.bankapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class DatabaseCredentialsCheckTest {

	private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/bankappdb";

	@Test
	void defaultPropertiesShipNoPasswordAndNoRootUser() throws Exception {
		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

		assertThat(props.getProperty("spring.datasource.password")).isEqualTo("${SPRING_DATASOURCE_PASSWORD}");
		assertThat(props.getProperty("spring.datasource.username")).isEqualTo("${SPRING_DATASOURCE_USERNAME:bankapp}");
	}

	@Test
	void rejectsMissingPassword() {
		assertThatThrownBy(() -> validate(MYSQL_URL, "bankapp", null))
				.hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
		assertThatThrownBy(() -> validate(MYSQL_URL, "bankapp", ""))
				.hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
		assertThatThrownBy(() -> validate(MYSQL_URL, "bankapp", "${SPRING_DATASOURCE_PASSWORD}"))
				.hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
	}

	@Test
	void rejectsRootUser() {
		assertThatThrownBy(() -> validate(MYSQL_URL, "root", "s3cret"))
				.hasMessageContaining("root");
		assertThatThrownBy(() -> validate(MYSQL_URL, "ROOT", "s3cret"))
				.hasMessageContaining("root");
	}

	@Test
	void acceptsLeastPrivilegeUserWithSuppliedPassword() {
		assertThatNoException().isThrownBy(() -> validate(MYSQL_URL, "bankapp", "s3cret"));
	}

	@Test
	void skipsEmbeddedTestDatabase() {
		assertThatNoException().isThrownBy(() -> validate("jdbc:h2:mem:bankappdb", "sa", ""));
	}

	private static void validate(String url, String username, String password) {
		DataSourceProperties props = new DataSourceProperties();
		props.setUrl(url);
		props.setUsername(username);
		props.setPassword(password);
		DatabaseCredentialsCheck.validate(props);
	}

}
