package com.example.bankapp.validation;

import java.util.regex.Pattern;

public class GitValidation {

    private static final Pattern BRANCH_NAME_PATTERN = Pattern.compile("^[\\w.-]+$");
    private static final Pattern GIT_URL_PATTERN = Pattern.compile("^https?://[\\w.-]+(/[\\w.-]+)*(\\.git)?$");

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class GitCheckoutException extends RuntimeException {
        private final String branch;
        private final String url;

        public GitCheckoutException(String branch, String url, String message) {
            super(formatErrorMessage(branch, url, message));
            this.branch = branch;
            this.url = url;
        }

        public GitCheckoutException(String branch, String url, Throwable cause) {
            super(formatErrorMessage(branch, url, cause.getMessage()), cause);
            this.branch = branch;
            this.url = url;
        }

        private static String formatErrorMessage(String branch, String url, String message) {
            return "Git checkout failed:\n" +
                   "Branch: " + branch + "\n" +
                   "URL: " + url + "\n" +
                   "Error: " + message;
        }

        public String getBranch() {
            return branch;
        }

        public String getUrl() {
            return url;
        }
    }

    public static ValidationResult validateBranchName(String branch) {
        if (branch == null || branch.isEmpty()) {
            return new ValidationResult(false, "Branch name cannot be null or empty");
        }
        if (!BRANCH_NAME_PATTERN.matcher(branch).matches()) {
            return new ValidationResult(false, "Invalid branch name format");
        }
        return new ValidationResult(true, null);
    }

    public static ValidationResult validateGitUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new ValidationResult(false, "Git URL cannot be null or empty");
        }
        if (!GIT_URL_PATTERN.matcher(url).matches()) {
            return new ValidationResult(false, "Invalid git URL format");
        }
        return new ValidationResult(true, null);
    }

    public static void performCheckout(String branch, String url, String credentialsId) 
            throws GitCheckoutException {
        ValidationResult branchValidation = validateBranchName(branch);
        if (!branchValidation.isValid()) {
            throw new GitCheckoutException(branch, url, branchValidation.getErrorMessage());
        }

        ValidationResult urlValidation = validateGitUrl(url);
        if (!urlValidation.isValid()) {
            throw new GitCheckoutException(branch, url, urlValidation.getErrorMessage());
        }
    }
}
