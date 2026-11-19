package com.example.bankapp.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Refuses to start against a real database without a supplied password, or as the MySQL root user.
 */
@Configuration(proxyBeanMethods = false)
public class DatabaseCredentialsCheck {

	@Bean
	static BeanPostProcessor databaseCredentialsValidator() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof DataSourceProperties props) {
					validate(props);
				}
				return bean;
			}
		};
	}

	static void validate(DataSourceProperties props) {
		String driver = StringUtils.hasText(props.getDriverClassName()) ? props.getDriverClassName()
				: DatabaseDriver.fromJdbcUrl(props.getUrl()).getDriverClassName();
		if (EmbeddedDatabaseConnection.isEmbedded(driver, props.getUrl())) {
			return;
		}
		String password = props.getPassword();
		if (!StringUtils.hasText(password) || password.startsWith("${")) {
			throw new IllegalStateException(
					"No database password configured: set the SPRING_DATASOURCE_PASSWORD environment variable");
		}
		if ("root".equalsIgnoreCase(props.getUsername())) {
			throw new IllegalStateException(
					"The application must not connect as the database root user: set SPRING_DATASOURCE_USERNAME to a least-privilege user");
		}
	}

}
