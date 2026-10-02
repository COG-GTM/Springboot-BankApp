package com.cascade.disclosure.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * With the migrated run loaded (core-outputs/migrated/daily_accrual.csv, exported from the PostgreSQL parity run),
 * the parity endpoint must surface exactly the accounts whose statement cents differ from the recorded legacy core
 * — ESC-INT-001..003 of demo/reports/bk-parity.md — and apply the §1030.3(f)(2) tolerance to each, agreeing with the
 * Python regulatory step (demo/bk/regulatory.py). Nothing is rounded away: a within-tolerance difference is still a row.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ParityEndpointTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void parityListsTheEscalatedAccountsWithToleranceVerdicts() throws Exception {
        mvc.perform(get("/api/disclosures/parity"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.migratedLoaded").value(true))
           .andExpect(jsonPath("$.accounts").value(29))
           .andExpect(jsonPath("$.differing").value(3))
           .andExpect(jsonPath("$.exceedingTolerance").value(2))
           .andExpect(jsonPath("$.rows[*].accountId").value(containsInAnyOrder(500011, 500023, 500037)))
           .andExpect(jsonPath("$.rows[?(@.accountId==500011)].apyLegacy").value(3.87))
           .andExpect(jsonPath("$.rows[?(@.accountId==500011)].apyMigrated").value(3.74))
           .andExpect(jsonPath("$.rows[?(@.accountId==500011)].withinTolerance").value(false))
           .andExpect(jsonPath("$.rows[?(@.accountId==500023)].apyLegacy").value(3.84))
           .andExpect(jsonPath("$.rows[?(@.accountId==500023)].apyMigrated").value(3.74))
           .andExpect(jsonPath("$.rows[?(@.accountId==500023)].withinTolerance").value(false))
           .andExpect(jsonPath("$.rows[?(@.accountId==500037)].apyLegacy").value(3.7))
           .andExpect(jsonPath("$.rows[?(@.accountId==500037)].apyMigrated").value(3.69))
           .andExpect(jsonPath("$.rows[?(@.accountId==500037)].withinTolerance").value(true));
    }

    @Test
    void migratedApyEarnedIsServedPerAccount() throws Exception {
        mvc.perform(get("/api/disclosures/accounts/500011/apy-earned").param("source", "migrated"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.source").value("migrated"))
           .andExpect(jsonPath("$.apyEarned").value(3.74));
    }
}
