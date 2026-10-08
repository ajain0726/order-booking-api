package com.fynxt.trading.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP contract for all six endpoints and the error format.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String trader;

    @BeforeEach
    void newTrader() {
        trader = "T-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private ResultActions placeOrder(String stock, int qty, String side) throws Exception {
        String body = """
                {"traderId":"%s","stock":"%s","sector":"TECH","quantity":%d,"side":"%s"}
                """.formatted(trader, stock, qty, side);
        return mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long placedOrderId(String stock, int qty, String side) throws Exception {
        String response = placeOrder(stock, qty, side).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("id").asLong();
    }

    private ResultActions addHolding(String stock, String sector, int qty) throws Exception {
        String body = """
                {"stock":"%s","sector":"%s","quantity":%d}
                """.formatted(stock, sector, qty);
        return mvc.perform(post("/api/v1/traders/{t}/portfolio/holdings", trader)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void placeOrderReturnsCreatedPendingOrder() throws Exception {
        placeOrder("AAPL", 50, "BUY")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/v1/orders/")))
                .andExpect(jsonPath("$.traderId").value(trader))
                .andExpect(jsonPath("$.stock").value("AAPL"))
                .andExpect(jsonPath("$.sector").value("TECH"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void fillThenPortfolioReflectsHolding() throws Exception {
        long id = placedOrderId("AAPL", 150, "BUY");

        mvc.perform(post("/api/v1/orders/{id}/fill", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FILLED"));

        addHolding("TSLA", "TECH", 80).andExpect(status().isOk());

        mvc.perform(get("/api/v1/traders/{t}/portfolio", trader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traderId").value(trader))
                .andExpect(jsonPath("$.positions.AAPL").value(150))
                .andExpect(jsonPath("$.positions.TSLA").value(80))
                .andExpect(jsonPath("$.sectorBreakdown.TECH").value(230));
    }

    @Test
    void cancelPendingOrder() throws Exception {
        long id = placedOrderId("AAPL", 5, "BUY");

        mvc.perform(post("/api/v1/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelFilledOrderReturnsDescriptiveConflict() throws Exception {
        long id = placedOrderId("AAPL", 5, "BUY");
        mvc.perform(post("/api/v1/orders/{id}/fill", id)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/orders/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATE"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Order " + id + " cannot be cancelled because it is FILLED; only PENDING orders can be cancelled"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/" + id + "/cancel"));
    }

    @Test
    void unknownOrderIs404() throws Exception {
        mvc.perform(post("/api/v1/orders/{id}/fill", 999_999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void fourthPendingOrderIs422() throws Exception {
        placeOrder("AAPL", 1, "BUY").andExpect(status().isCreated());
        placeOrder("AAPL", 1, "BUY").andExpect(status().isCreated());
        placeOrder("AAPL", 1, "BUY").andExpect(status().isCreated());

        placeOrder("AAPL", 1, "BUY")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PENDING_ORDER_LIMIT_EXCEEDED"));
    }

    @Test
    void sellWithoutSharesIs422() throws Exception {
        placeOrder("AAPL", 10, "SELL")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_HOLDINGS"));
    }

    @Test
    void invalidBodyListsEveryFieldError() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"traderId\":\"\",\"stock\":\"AAPL\",\"sector\":\"TECH\",\"quantity\":0,\"side\":\"BUY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details", hasItem(startsWith("quantity:"))))
                .andExpect(jsonPath("$.details", hasItem(startsWith("traderId:"))));
    }

    @Test
    void unknownSideIsMalformed() throws Exception {
        placeOrder("AAPL", 1, "SHORT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void addHoldingValidatesQuantity() throws Exception {
        addHolding("NVDA", "TECH", -5)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void overlapMatchesWorkedExample() throws Exception {
        addHolding("AAPL", "TECH", 10).andExpect(status().isOk());
        addHolding("TSLA", "TECH", 10).andExpect(status().isOk());
        addHolding("NVDA", "TECH", 100).andExpect(status().isOk());

        mvc.perform(get("/api/v1/traders/{t}/portfolio/overlap", trader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlaps[0].basket").value("TECH_HEAVY"))
                .andExpect(jsonPath("$.overlaps[0].overlap").value("75.00%"))
                .andExpect(jsonPath("$.overlaps[1].basket").value("FINANCE_HEAVY"))
                .andExpect(jsonPath("$.overlaps[1].overlap").value("0.00%"))
                .andExpect(jsonPath("$.overlaps[2].basket").value("BALANCED"))
                .andExpect(jsonPath("$.overlaps[2].overlap").value("50.00%"))
                .andExpect(jsonPath("$.dominantBasket").value("TECH_HEAVY"))
                .andExpect(jsonPath("$.riskFlag").value("HIGH"));
    }

    @Test
    void unknownPathIs404NotServerError() throws Exception {
        mvc.perform(get("/api/v1/nope")).andExpect(status().isNotFound());
    }
}
