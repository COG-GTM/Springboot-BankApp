package com.cascade.disclosure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DisclosureControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void productApysAreWithinRegDdTolerance() throws Exception {
        mvc.perform(get("/api/disclosures/products"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(6)))
           .andExpect(jsonPath("$[?(@.productCode=='SAV-STD')].disclosedApy").value(3.72))
           .andExpect(jsonPath("$[?(@.productCode=='SAV-STD')].appendixAApy").value(3.72))
           .andExpect(jsonPath("$[?(@.withinTolerance==false)]").isEmpty());
    }

    @Test
    void loanAprsAreWithinRegZTolerance() throws Exception {
        mvc.perform(get("/api/disclosures/loans"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[?(@.productCode=='LOAN-PERS-36')].actuarialApr").value(13.05))
           .andExpect(jsonPath("$[?(@.withinTolerance==false)]").isEmpty());
    }

    @Test
    void apyEarnedForAGoldenAccountFromTheLegacyCore() throws Exception {
        mvc.perform(get("/api/disclosures/accounts/500002/apy-earned"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.source").value("legacy"))
           .andExpect(jsonPath("$.days").value(90))
           .andExpect(jsonPath("$.productCode").value("SAV-HY"));
        mvc.perform(get("/api/disclosures/accounts/500001/apy-earned")).andExpect(status().isNotFound());
    }

    @Test
    void indexRendersWithOracleCaveat() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("not Oracle")))
           .andExpect(content().string(containsString("Cascade Statement Savings")));
    }
}
