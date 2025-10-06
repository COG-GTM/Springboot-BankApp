# EB-8: Update Test Code for Deprecated Test Annotations - BLOCKER ANALYSIS

## Executive Summary

**Status:** ❌ **BLOCKED** - Cannot proceed with implementation until Spring Boot 3.4+ upgrade (Epic EB-1) is completed.

**Ticket:** EB-8 - Update test code for deprecated test annotations  
**Investigation Date:** 2025-10-06  
**Current Spring Boot Version:** 3.3.3  
**Required Spring Boot Version:** 3.4.0+

## Investigation Results

### Comprehensive Search Performed

A thorough multi-branch investigation was conducted across the entire COG-GTM/Springboot-BankApp repository:

1. **Main Branch (DevOps):** No `@MockBean` or `@SpyBean` annotations found
2. **Test Coverage Branches:** Found annotations in test expansion branches
   - `devin/1759408856-mba-307-controller-tests`
   - `devin/1759413280-expand-test-coverage`
3. **Location of Annotations:**
   - `src/test/java/com/example/bankapp/controller/BankControllerTest.java` (1 instance of `@MockBean`)

### Key Findings

#### 1. Annotations Are NOT Deprecated in Spring Boot 3.3.3

The premise of this ticket is **incorrect** for the current codebase version:

- **Spring Boot 3.3.3 (Current):** `@MockBean` and `@SpyBean` are **NOT deprecated**
- **Spring Boot 3.4.0+ (Required):** These annotations become deprecated
- **Spring Boot 3.4.0+ (Required):** New replacement annotations (`@MockitoBean`, `@MockitoSpyBean`) are introduced

#### 2. Replacement Annotations Not Available

The new annotations cannot be used in Spring Boot 3.3.3:

```
❌ Package does not exist in 3.3.3:
   org.springframework.test.context.bean.override.mockito

✅ Package available starting in 3.4.0+:
   org.springframework.test.context.bean.override.mockito.MockitoBean
   org.springframework.test.context.bean.override.mockito.MockitoSpyBean
```

#### 3. Current Annotation Usage

**File:** `src/test/java/com/example/bankapp/controller/BankControllerTest.java`

```java
import org.springframework.boot.test.mock.mockito.MockBean;  // Currently valid in 3.3.3

@WebMvcTest(BankController.class)
class BankControllerTest {
    @MockBean  // NOT deprecated in Spring Boot 3.3.3
    private AccountService accountService;
}
```

## Determination: Scenario A

Per the investigation framework provided, this falls under **Scenario A:**

> **Scenario A: Annotations truly don't exist (as deprecated)**
> - Document that the ticket's premise is incorrect
> - Recommend marking the ticket as blocked or invalid
> - Suggest waiting for Epic EB-1 (Spring Boot 3.4.9 upgrade) completion and test coverage expansion

## Dependencies and Blockers

### Blocking Ticket
- **EB-1:** Spring Boot 3.4.9 Upgrade (MUST be completed first)

### Dependency Chain
```
EB-1 (Spring Boot 3.4.9 Upgrade)
  ↓
EB-8 (Update Deprecated Test Annotations) ← YOU ARE HERE
```

### Why This Order Matters

1. **No Deprecation Yet:** The annotations to be replaced are not deprecated in the current version
2. **No Replacement Available:** The new annotations don't exist in Spring Boot 3.3.3
3. **No Build Warnings:** Running the build produces zero deprecation warnings related to these annotations
4. **Clean Migration Path:** Waiting for 3.4+ upgrade ensures a clean, properly supported migration

## Recommended Actions

### Immediate Actions
1. ✅ Mark ticket EB-8 as **BLOCKED**
2. ✅ Add dependency on ticket EB-1 (Spring Boot 3.4.9 upgrade)
3. ✅ Document blocker in ticket comments with link to this analysis

### Post-EB-1 Completion Actions
1. Verify Spring Boot version is 3.4.0+
2. Check that deprecation warnings appear for `@MockBean` and `@SpyBean`
3. Perform the annotation replacement:
   ```java
   // Replace:
   import org.springframework.boot.test.mock.mockito.MockBean;
   @MockBean
   private AccountService accountService;
   
   // With:
   import org.springframework.test.context.bean.override.mockito.MockitoBean;
   @MockitoBean
   private AccountService accountService;
   ```
4. Run full test suite to verify compatibility
5. Verify no deprecation warnings remain

## Files Requiring Changes (Post-EB-1)

Based on current test expansion work:

| File | Annotation Count | Type |
|------|------------------|------|
| `src/test/java/com/example/bankapp/controller/BankControllerTest.java` | 1 | `@MockBean` |

**Note:** Additional files may need updates as test coverage expands.

## Technical Context

### Spring Boot 3.4 Bean Override Framework

Spring Boot 3.4 introduces a new test bean override framework:

- **Old (Deprecated in 3.4+):**
  - `org.springframework.boot.test.mock.mockito.MockBean`
  - `org.springframework.boot.test.mock.mockito.SpyBean`

- **New (Available in 3.4+):**
  - `org.springframework.test.context.bean.override.mockito.MockitoBean`
  - `org.springframework.test.context.bean.override.mockito.MockitoSpyBean`

### Migration Pattern (For Future Reference)

1. **Import Changes:**
   ```diff
   - import org.springframework.boot.test.mock.mockito.MockBean;
   + import org.springframework.test.context.bean.override.mockito.MockitoBean;
   ```

2. **Annotation Changes:**
   ```diff
   - @MockBean
   + @MockitoBean
   ```

3. **No Behavioral Changes:** The functionality remains the same; only the package and annotation names change.

## Effort Estimate

### Current Ticket (Blocked)
- **Effort:** 0 story points (blocked, no work can be done)

### Post-EB-1 Completion
- **Estimated Effort:** 2 story points
- **Time:** ~30 minutes
- **Scope:**
  - Update 1 known file
  - Run test suite
  - Verify no deprecation warnings
  - Create PR and merge

## Related Documentation

- [Spring Boot 3.4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes)
- [Spring Framework Test Bean Override Documentation](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/bean-overriding.html)

## Conclusion

This ticket cannot proceed until the Spring Boot 3.4.9 upgrade (EB-1) is completed. The annotations in question are not deprecated in the current version (3.3.3), and the replacement annotations do not exist. 

**Recommendation:** Mark EB-8 as **BLOCKED** with dependency on EB-1.

---

**Investigation Performed By:** Devin  
**Session:** https://app.devin.ai/sessions/1e8f2f80a7654994805031082093fdc6  
**Date:** 2025-10-06
