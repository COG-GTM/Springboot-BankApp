package com.example.bankapp.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentValidationTest {

    @Test
    @DisplayName("Default constants should have expected values")
    void testDefaultConstants() {
        assertEquals(30, DeploymentValidation.DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
        assertEquals(30, DeploymentValidation.DEFAULT_HEALTH_CHECK_MAX_ATTEMPTS);
        assertEquals(10, DeploymentValidation.DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS);
    }

    @Test
    @DisplayName("Health check should pass when all services are healthy")
    void testHealthCheckAllServicesHealthy() {
        List<DeploymentValidation.ServiceStatus> services = Arrays.asList(
            new DeploymentValidation.ServiceStatus("web", true, "healthy"),
            new DeploymentValidation.ServiceStatus("db", true, "healthy"),
            new DeploymentValidation.ServiceStatus("cache", true, "healthy")
        );

        DeploymentValidation.HealthCheckResult result = 
            DeploymentValidation.checkServicesHealth(services, 1, 30);

        assertTrue(result.isAllHealthy());
        assertEquals("All services are healthy", result.getMessage());
        assertEquals(1, result.getAttemptNumber());
        assertEquals(3, result.getServices().size());
    }

    @Test
    @DisplayName("Health check should fail when some services are unhealthy")
    void testHealthCheckSomeServicesUnhealthy() {
        List<DeploymentValidation.ServiceStatus> services = Arrays.asList(
            new DeploymentValidation.ServiceStatus("web", true, "healthy"),
            new DeploymentValidation.ServiceStatus("db", false, "starting"),
            new DeploymentValidation.ServiceStatus("cache", false, "unhealthy")
        );

        DeploymentValidation.HealthCheckResult result = 
            DeploymentValidation.checkServicesHealth(services, 5, 30);

        assertFalse(result.isAllHealthy());
        assertTrue(result.getMessage().contains("Unhealthy services detected"));
        assertTrue(result.getMessage().contains("attempt 5/30"));
        assertEquals(5, result.getAttemptNumber());
    }

    @Test
    @DisplayName("Health check should timeout after max attempts")
    void testHealthCheckTimeout() {
        List<DeploymentValidation.ServiceStatus> services = Arrays.asList(
            new DeploymentValidation.ServiceStatus("web", false, "starting")
        );

        DeploymentValidation.HealthCheckResult result = 
            DeploymentValidation.checkServicesHealth(services, 30, 30);

        assertFalse(result.isAllHealthy());
        assertEquals("Health check timeout after 30 attempts", result.getMessage());
        assertEquals(30, result.getAttemptNumber());
    }

    @Test
    @DisplayName("Health check should handle empty service list")
    void testHealthCheckEmptyServiceList() {
        DeploymentValidation.HealthCheckResult result = 
            DeploymentValidation.checkServicesHealth(new ArrayList<>(), 1, 30);

        assertFalse(result.isAllHealthy());
        assertEquals("No services to check", result.getMessage());
    }

    @Test
    @DisplayName("Health check should handle null service list")
    void testHealthCheckNullServiceList() {
        DeploymentValidation.HealthCheckResult result = 
            DeploymentValidation.checkServicesHealth(null, 1, 30);

        assertFalse(result.isAllHealthy());
        assertEquals("No services to check", result.getMessage());
    }

    @Test
    @DisplayName("Graceful shutdown should succeed with valid timeout")
    void testGracefulShutdownSuccess() {
        List<String> containers = Arrays.asList("container1", "container2");

        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.performGracefulShutdown(containers, 30);

        assertTrue(result.isSuccessful());
        assertEquals(30, result.getTimeoutSeconds());
        assertTrue(result.getMessage().contains("Graceful shutdown completed"));
        assertTrue(result.getRemainingContainers().isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Graceful shutdown should fail with invalid timeout")
    @ValueSource(ints = {0, -1, -10, -100})
    void testGracefulShutdownInvalidTimeout(int timeout) {
        List<String> containers = Arrays.asList("container1");

        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.performGracefulShutdown(containers, timeout);

        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Invalid timeout value"));
    }

    @Test
    @DisplayName("Graceful shutdown should handle empty container list")
    void testGracefulShutdownEmptyContainerList() {
        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.performGracefulShutdown(new ArrayList<>(), 30);

        assertTrue(result.isSuccessful());
        assertEquals("No containers to stop", result.getMessage());
    }

    @Test
    @DisplayName("Graceful shutdown should handle null container list")
    void testGracefulShutdownNullContainerList() {
        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.performGracefulShutdown(null, 30);

        assertTrue(result.isSuccessful());
        assertEquals("No containers to stop", result.getMessage());
    }

    @Test
    @DisplayName("Verify cleanup should succeed when no containers remain")
    void testVerifyCleanupSuccess() {
        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.verifyCleanup(new ArrayList<>());

        assertTrue(result.isSuccessful());
        assertEquals("All containers stopped successfully", result.getMessage());
    }

    @Test
    @DisplayName("Verify cleanup should fail when containers remain")
    void testVerifyCleanupFailure() {
        List<String> remainingContainers = Arrays.asList("container1", "container2");

        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.verifyCleanup(remainingContainers);

        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Failed to stop all containers"));
        assertEquals(2, result.getRemainingContainers().size());
    }

    @Test
    @DisplayName("Verify cleanup should handle null container list")
    void testVerifyCleanupNullContainerList() {
        DeploymentValidation.ShutdownResult result = 
            DeploymentValidation.verifyCleanup(null);

        assertTrue(result.isSuccessful());
        assertEquals("All containers stopped successfully", result.getMessage());
    }

    @Test
    @DisplayName("ServiceStatus should store values correctly")
    void testServiceStatus() {
        DeploymentValidation.ServiceStatus status = 
            new DeploymentValidation.ServiceStatus("web-service", true, "running (healthy)");

        assertEquals("web-service", status.getServiceName());
        assertTrue(status.isHealthy());
        assertEquals("running (healthy)", status.getStatus());
    }

    @Test
    @DisplayName("ServiceStatus should handle unhealthy state")
    void testServiceStatusUnhealthy() {
        DeploymentValidation.ServiceStatus status = 
            new DeploymentValidation.ServiceStatus("db-service", false, "starting");

        assertEquals("db-service", status.getServiceName());
        assertFalse(status.isHealthy());
        assertEquals("starting", status.getStatus());
    }

    @Test
    @DisplayName("HealthCheckResult should store all values correctly")
    void testHealthCheckResult() {
        List<DeploymentValidation.ServiceStatus> services = Arrays.asList(
            new DeploymentValidation.ServiceStatus("web", true, "healthy")
        );

        DeploymentValidation.HealthCheckResult result = 
            new DeploymentValidation.HealthCheckResult(true, services, 5, "Test message");

        assertTrue(result.isAllHealthy());
        assertEquals(1, result.getServices().size());
        assertEquals(5, result.getAttemptNumber());
        assertEquals("Test message", result.getMessage());
    }

    @Test
    @DisplayName("ShutdownResult should store all values correctly")
    void testShutdownResult() {
        List<String> containers = Arrays.asList("container1");

        DeploymentValidation.ShutdownResult result = 
            new DeploymentValidation.ShutdownResult(false, 30, containers, "Test message");

        assertFalse(result.isSuccessful());
        assertEquals(30, result.getTimeoutSeconds());
        assertEquals(1, result.getRemainingContainers().size());
        assertEquals("Test message", result.getMessage());
    }

    @Test
    @DisplayName("DeploymentException should format message correctly")
    void testDeploymentException() {
        DeploymentValidation.DeploymentException exception = 
            new DeploymentValidation.DeploymentException("Service unavailable");

        assertEquals("Deployment failed: Service unavailable", exception.getMessage());
    }

    @Test
    @DisplayName("DeploymentException should wrap cause correctly")
    void testDeploymentExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Network error");
        DeploymentValidation.DeploymentException exception = 
            new DeploymentValidation.DeploymentException("Connection failed", cause);

        assertEquals("Deployment failed: Connection failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Health check should track attempt progression")
    void testHealthCheckAttemptProgression() {
        List<DeploymentValidation.ServiceStatus> unhealthyServices = Arrays.asList(
            new DeploymentValidation.ServiceStatus("web", false, "starting")
        );

        for (int attempt = 1; attempt < 30; attempt++) {
            DeploymentValidation.HealthCheckResult result = 
                DeploymentValidation.checkServicesHealth(unhealthyServices, attempt, 30);
            
            assertFalse(result.isAllHealthy());
            assertEquals(attempt, result.getAttemptNumber());
            assertTrue(result.getMessage().contains("attempt " + attempt + "/30"));
        }

        DeploymentValidation.HealthCheckResult finalResult = 
            DeploymentValidation.checkServicesHealth(unhealthyServices, 30, 30);
        assertTrue(finalResult.getMessage().contains("timeout"));
    }

    @Test
    @DisplayName("Health check should correctly identify single unhealthy service")
    void testHealthCheckSingleUnhealthyService() {
        List<DeploymentValidation.ServiceStatus> services = Arrays.asList(
            new DeploymentValidation.ServiceStatus("web", true, "healthy"),
            new DeploymentValidation.ServiceStatus("db", true, "healthy"),
            new DeploymentValidation.ServiceStatus("cache", true, "healthy"),
            new DeploymentValidation.ServiceStatus("worker", false, "starting")
        );

        DeploymentValidation.HealthCheckResult result = 
            DeploymentValidation.checkServicesHealth(services, 1, 30);

        assertFalse(result.isAllHealthy());
        assertTrue(result.getMessage().contains("1"));
    }
}
