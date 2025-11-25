package com.example.bankapp.validation;

import java.util.List;
import java.util.ArrayList;

public class DeploymentValidation {

    public static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_HEALTH_CHECK_MAX_ATTEMPTS = 30;
    public static final int DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS = 10;

    public static class ServiceStatus {
        private final String serviceName;
        private final boolean healthy;
        private final String status;

        public ServiceStatus(String serviceName, boolean healthy, String status) {
            this.serviceName = serviceName;
            this.healthy = healthy;
            this.status = status;
        }

        public String getServiceName() {
            return serviceName;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class HealthCheckResult {
        private final boolean allHealthy;
        private final List<ServiceStatus> services;
        private final int attemptNumber;
        private final String message;

        public HealthCheckResult(boolean allHealthy, List<ServiceStatus> services, 
                                  int attemptNumber, String message) {
            this.allHealthy = allHealthy;
            this.services = services;
            this.attemptNumber = attemptNumber;
            this.message = message;
        }

        public boolean isAllHealthy() {
            return allHealthy;
        }

        public List<ServiceStatus> getServices() {
            return services;
        }

        public int getAttemptNumber() {
            return attemptNumber;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ShutdownResult {
        private final boolean successful;
        private final int timeoutSeconds;
        private final List<String> remainingContainers;
        private final String message;

        public ShutdownResult(boolean successful, int timeoutSeconds, 
                              List<String> remainingContainers, String message) {
            this.successful = successful;
            this.timeoutSeconds = timeoutSeconds;
            this.remainingContainers = remainingContainers;
            this.message = message;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public List<String> getRemainingContainers() {
            return remainingContainers;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class DeploymentException extends RuntimeException {
        public DeploymentException(String message) {
            super("Deployment failed: " + message);
        }

        public DeploymentException(String message, Throwable cause) {
            super("Deployment failed: " + message, cause);
        }
    }

    public static HealthCheckResult checkServicesHealth(List<ServiceStatus> services, 
                                                         int attemptNumber, 
                                                         int maxAttempts) {
        if (services == null || services.isEmpty()) {
            return new HealthCheckResult(false, new ArrayList<>(), attemptNumber, 
                "No services to check");
        }

        List<ServiceStatus> unhealthyServices = new ArrayList<>();
        for (ServiceStatus service : services) {
            if (!service.isHealthy()) {
                unhealthyServices.add(service);
            }
        }

        if (unhealthyServices.isEmpty()) {
            return new HealthCheckResult(true, services, attemptNumber, 
                "All services are healthy");
        }

        if (attemptNumber >= maxAttempts) {
            return new HealthCheckResult(false, services, attemptNumber, 
                "Health check timeout after " + maxAttempts + " attempts");
        }

        return new HealthCheckResult(false, services, attemptNumber, 
            "Unhealthy services detected: " + unhealthyServices.size() + 
            " (attempt " + attemptNumber + "/" + maxAttempts + ")");
    }

    public static ShutdownResult performGracefulShutdown(List<String> runningContainers, 
                                                          int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            return new ShutdownResult(false, timeoutSeconds, runningContainers, 
                "Invalid timeout value: " + timeoutSeconds);
        }

        if (runningContainers == null || runningContainers.isEmpty()) {
            return new ShutdownResult(true, timeoutSeconds, new ArrayList<>(), 
                "No containers to stop");
        }

        return new ShutdownResult(true, timeoutSeconds, new ArrayList<>(), 
            "Graceful shutdown completed with timeout " + timeoutSeconds + "s");
    }

    public static ShutdownResult verifyCleanup(List<String> remainingContainers) {
        if (remainingContainers == null || remainingContainers.isEmpty()) {
            return new ShutdownResult(true, 0, new ArrayList<>(), 
                "All containers stopped successfully");
        }

        return new ShutdownResult(false, 0, remainingContainers, 
            "Failed to stop all containers. Remaining: " + remainingContainers.size());
    }
}
