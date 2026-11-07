package com.cascade.disclosure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Without a migrated run on the classpath the parity endpoint says so instead of reporting zero differences. */
@SpringBootTest
@AutoConfigureMockMvc
class ParityEndpointTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void parityReportsMigratedRunNotLoaded() throws Exception {
        mvc.perform(get("/api/disclosures/parity"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.migratedLoaded").value(false))
           .andExpect(jsonPath("$.accounts").value(29))
           .andExpect(jsonPath("$.differing").value(0));
    }
}
