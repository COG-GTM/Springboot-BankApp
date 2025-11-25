package com.example.bankapp.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;

class GitValidationTest {

    @Test
    @DisplayName("Valid branch names should pass validation")
    void testValidBranchNames() {
        String[] validBranches = {
            "main",
            "master",
            "develop",
            "feature-branch",
            "feature_branch",
            "feature.branch",
            "release-1.0.0",
            "hotfix_2.3.4",
            "v1.0",
            "DevOps",
            "feature123",
            "my-feature_v1.2.3"
        };

        for (String branch : validBranches) {
            GitValidation.ValidationResult result = GitValidation.validateBranchName(branch);
            assertTrue(result.isValid(), "Branch name '" + branch + "' should be valid");
            assertNull(result.getErrorMessage());
        }
    }

    @ParameterizedTest
    @DisplayName("Branch names with special characters should fail validation")
    @ValueSource(strings = {
        "feature/branch",
        "feature branch",
        "feature@branch",
        "feature#branch",
        "feature$branch",
        "feature%branch",
        "feature^branch",
        "feature&branch",
        "feature*branch",
        "feature(branch)",
        "feature[branch]",
        "feature{branch}",
        "feature!branch",
        "feature+branch",
        "feature=branch",
        "feature`branch",
        "feature~branch",
        "feature|branch",
        "feature\\branch",
        "feature:branch",
        "feature;branch",
        "feature'branch",
        "feature\"branch",
        "feature<branch>",
        "feature,branch"
    })
    void testInvalidBranchNamesWithSpecialCharacters(String branch) {
        GitValidation.ValidationResult result = GitValidation.validateBranchName(branch);
        assertFalse(result.isValid(), "Branch name '" + branch + "' should be invalid");
        assertEquals("Invalid branch name format", result.getErrorMessage());
    }

    @Test
    @DisplayName("Empty branch name should fail validation")
    void testEmptyBranchName() {
        GitValidation.ValidationResult result = GitValidation.validateBranchName("");
        assertFalse(result.isValid());
        assertEquals("Branch name cannot be null or empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Null branch name should fail validation")
    void testNullBranchName() {
        GitValidation.ValidationResult result = GitValidation.validateBranchName(null);
        assertFalse(result.isValid());
        assertEquals("Branch name cannot be null or empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Valid Git URLs should pass validation")
    void testValidGitUrls() {
        String[] validUrls = {
            "https://github.com/user/repo.git",
            "https://github.com/user/repo",
            "http://github.com/user/repo.git",
            "http://github.com/user/repo",
            "https://gitlab.com/user/repo.git",
            "https://bitbucket.org/user/repo",
            "https://git.example.com/project/repo.git",
            "https://github.com/LondheShubham153/Springboot-BankApp.git"
        };

        for (String url : validUrls) {
            GitValidation.ValidationResult result = GitValidation.validateGitUrl(url);
            assertTrue(result.isValid(), "URL '" + url + "' should be valid");
            assertNull(result.getErrorMessage());
        }
    }

    @ParameterizedTest
    @DisplayName("Invalid Git URLs should fail validation")
    @ValueSource(strings = {
        "ftp://github.com/user/repo.git",
        "ssh://git@github.com/user/repo.git",
        "git@github.com:user/repo.git",
        "github.com/user/repo",
        "https://",
        "http://",
        "file:///path/to/repo",
        "not-a-url",
        "https://github.com/user/repo with spaces",
        "https://github.com/user/repo@branch"
    })
    void testInvalidGitUrls(String url) {
        GitValidation.ValidationResult result = GitValidation.validateGitUrl(url);
        assertFalse(result.isValid(), "URL '" + url + "' should be invalid");
        assertEquals("Invalid git URL format", result.getErrorMessage());
    }

    @Test
    @DisplayName("Empty Git URL should fail validation")
    void testEmptyGitUrl() {
        GitValidation.ValidationResult result = GitValidation.validateGitUrl("");
        assertFalse(result.isValid());
        assertEquals("Git URL cannot be null or empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Null Git URL should fail validation")
    void testNullGitUrl() {
        GitValidation.ValidationResult result = GitValidation.validateGitUrl(null);
        assertFalse(result.isValid());
        assertEquals("Git URL cannot be null or empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("performCheckout should throw exception for invalid branch")
    void testPerformCheckoutWithInvalidBranch() {
        GitValidation.GitCheckoutException exception = assertThrows(
            GitValidation.GitCheckoutException.class,
            () -> GitValidation.performCheckout("feature/invalid", "https://github.com/user/repo.git", null)
        );

        assertTrue(exception.getMessage().contains("Git checkout failed:"));
        assertTrue(exception.getMessage().contains("Branch: feature/invalid"));
        assertTrue(exception.getMessage().contains("Invalid branch name format"));
        assertEquals("feature/invalid", exception.getBranch());
        assertEquals("https://github.com/user/repo.git", exception.getUrl());
    }

    @Test
    @DisplayName("performCheckout should throw exception for invalid URL")
    void testPerformCheckoutWithInvalidUrl() {
        GitValidation.GitCheckoutException exception = assertThrows(
            GitValidation.GitCheckoutException.class,
            () -> GitValidation.performCheckout("main", "invalid-url", null)
        );

        assertTrue(exception.getMessage().contains("Git checkout failed:"));
        assertTrue(exception.getMessage().contains("URL: invalid-url"));
        assertTrue(exception.getMessage().contains("Invalid git URL format"));
        assertEquals("main", exception.getBranch());
        assertEquals("invalid-url", exception.getUrl());
    }

    @Test
    @DisplayName("performCheckout should succeed with valid inputs")
    void testPerformCheckoutWithValidInputs() {
        assertDoesNotThrow(() -> 
            GitValidation.performCheckout("main", "https://github.com/user/repo.git", null)
        );
    }

    @Test
    @DisplayName("performCheckout should succeed with credentials")
    void testPerformCheckoutWithCredentials() {
        assertDoesNotThrow(() -> 
            GitValidation.performCheckout("develop", "https://github.com/user/repo.git", "my-credentials")
        );
    }

    @Test
    @DisplayName("GitCheckoutException should format error message correctly")
    void testGitCheckoutExceptionFormatting() {
        GitValidation.GitCheckoutException exception = new GitValidation.GitCheckoutException(
            "feature-branch", 
            "https://github.com/user/repo.git", 
            "Connection timeout"
        );

        String expectedMessage = "Git checkout failed:\n" +
                                "Branch: feature-branch\n" +
                                "URL: https://github.com/user/repo.git\n" +
                                "Error: Connection timeout";
        
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @DisplayName("GitCheckoutException should wrap cause correctly")
    void testGitCheckoutExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Network error");
        GitValidation.GitCheckoutException exception = new GitValidation.GitCheckoutException(
            "main", 
            "https://github.com/user/repo.git", 
            cause
        );

        assertTrue(exception.getMessage().contains("Network error"));
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("ValidationResult should store valid state correctly")
    void testValidationResultValid() {
        GitValidation.ValidationResult result = new GitValidation.ValidationResult(true, null);
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("ValidationResult should store invalid state correctly")
    void testValidationResultInvalid() {
        GitValidation.ValidationResult result = new GitValidation.ValidationResult(false, "Error message");
        assertFalse(result.isValid());
        assertEquals("Error message", result.getErrorMessage());
    }
}
