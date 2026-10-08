package com.fynxt.trading.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The demo traders from data.sql are loaded and consistent with the schema.
 * Read-only checks, so other tests sharing the database are unaffected.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SeedDataTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void seededTraderT001MatchesWorkedExample() throws Exception {
        mvc.perform(get("/api/v1/traders/T001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.AAPL").value(150))
                .andExpect(jsonPath("$.positions.TSLA").value(80))
                .andExpect(jsonPath("$.positions.NVDA").value(100))
                .andExpect(jsonPath("$.sectorBreakdown.TECH").value(330));

        mvc.perform(get("/api/v1/traders/T001/portfolio/overlap"))
                .andExpect(jsonPath("$.dominantBasket").value("TECH_HEAVY"))
                .andExpect(jsonPath("$.riskFlag").value("HIGH"));
    }

    @Test
    void seededTradersT002AndT003HaveExpectedRisk() throws Exception {
        mvc.perform(get("/api/v1/traders/T002/portfolio/overlap"))
                .andExpect(jsonPath("$.dominantBasket").value("FINANCE_HEAVY"))
                .andExpect(jsonPath("$.riskFlag").value("HIGH"));
        mvc.perform(get("/api/v1/traders/T003/portfolio/overlap"))
                .andExpect(jsonPath("$.dominantBasket").value("BALANCED"))
                .andExpect(jsonPath("$.riskFlag").value("MEDIUM"));
    }

    @Test
    void seededFilledOrderCannotBeCancelled() throws Exception {
        mvc.perform(get("/api/v1/orders/1"))
                .andExpect(jsonPath("$.traderId").value("T001"))
                .andExpect(jsonPath("$.status").value("FILLED"));
        mvc.perform(post("/api/v1/orders/1/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATE"));
    }
}
