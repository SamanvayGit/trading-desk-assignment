package com.example.tradingdesk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TradingDeskApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addPortfolioPlaceBuyFillAndReadPortfolio() throws Exception {
        mockMvc.perform(post("/api/traders/API-T001/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stock": "AAPL",
                                  "sector": "TECH",
                                  "quantity": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traderId").value("API-T001"))
                .andExpect(jsonPath("$.positions.AAPL").value(100))
                .andExpect(jsonPath("$.sectorBreakdown.TECH").value(100));

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "API-T001",
                                  "stock": "TSLA",
                                  "sector": "TECH",
                                  "quantity": 50,
                                  "side": "BUY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.traderId").value("API-T001"))
                .andExpect(jsonPath("$.stock").value("TSLA"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        int orderId = extractOrderId(orderResult);

        mockMvc.perform(post("/api/orders/{orderId}/fill", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("FILLED"));

        mockMvc.perform(get("/api/traders/API-T001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.AAPL").value(100))
                .andExpect(jsonPath("$.positions.TSLA").value(50))
                .andExpect(jsonPath("$.sectorBreakdown.TECH").value(150));
    }

    @Test
    void sellOrderFillDecreasesPortfolio() throws Exception {
        mockMvc.perform(post("/api/traders/API-SELL/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stock": "NVDA",
                                  "sector": "TECH",
                                  "quantity": 100
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "API-SELL",
                                  "stock": "NVDA",
                                  "sector": "TECH",
                                  "quantity": 40,
                                  "side": "SELL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.side").value("SELL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        int orderId = extractOrderId(orderResult);

        mockMvc.perform(post("/api/orders/{orderId}/fill", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FILLED"));

        mockMvc.perform(get("/api/traders/API-SELL/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.NVDA").value(60))
                .andExpect(jsonPath("$.sectorBreakdown.TECH").value(60));
    }

    @Test
    void cancelPendingOrderReturnsCancelled() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "API-CANCEL",
                                  "stock": "MSFT",
                                  "sector": "TECH",
                                  "quantity": 25,
                                  "side": "BUY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        int orderId = extractOrderId(orderResult);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void sectorOverlapReturnsDominantBasketAndRiskFlag() throws Exception {
        addPosition("API-RISK", "AAPL", "TECH", 10);
        addPosition("API-RISK", "TSLA", "TECH", 10);
        addPosition("API-RISK", "NVDA", "TECH", 10);

        mockMvc.perform(get("/api/traders/API-RISK/portfolio/sector-overlap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dominantBasket").value("TECH_HEAVY"))
                .andExpect(jsonPath("$.riskFlag").value("HIGH"))
                .andExpect(jsonPath("$.overlaps[0].basket").value("TECH_HEAVY"))
                .andExpect(jsonPath("$.overlaps[0].overlap").value("75.00%"));
    }

    @Test
    void sellWithoutHoldingsReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "API-NO-HOLDINGS",
                                  "stock": "AAPL",
                                  "sector": "TECH",
                                  "quantity": 10,
                                  "side": "SELL"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.details[0]", containsString("Insufficient available holdings")));
    }

    @Test
    void fourthPendingOrderReturnsConflict() throws Exception {
        placeBuy("API-LIMIT", "AAPL");
        placeBuy("API-LIMIT", "MSFT");
        placeBuy("API-LIMIT", "GOOGL");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "API-LIMIT",
                                  "stock": "TSLA",
                                  "sector": "TECH",
                                  "quantity": 1,
                                  "side": "BUY"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]", containsString("already has 3 pending orders")));
    }

    @Test
    void invalidOrderRequestReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "",
                                  "stock": "AAPL",
                                  "sector": "TECH",
                                  "quantity": 0,
                                  "side": "BUY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void fillMissingOrderReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/fill", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.details[0]").value("Order 999999 not found"));
    }

    @Test
    void cancellingFilledOrderReturnsDescriptiveConflict() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "API-FILLED-CANCEL",
                                  "stock": "AMZN",
                                  "sector": "TECH",
                                  "quantity": 10,
                                  "side": "BUY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        int orderId = extractOrderId(orderResult);

        mockMvc.perform(post("/api/orders/{orderId}/fill", orderId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]", containsString("Cannot cancel order")))
                .andExpect(jsonPath("$.details[0]", containsString("FILLED")));
    }

    private void addPosition(String traderId, String stock, String sector, int quantity) throws Exception {
        mockMvc.perform(post("/api/traders/{traderId}/portfolio", traderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stock": "%s",
                                  "sector": "%s",
                                  "quantity": %d
                                }
                                """.formatted(stock, sector, quantity)))
                .andExpect(status().isOk());
    }

    private void placeBuy(String traderId, String stock) throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traderId": "%s",
                                  "stock": "%s",
                                  "sector": "TECH",
                                  "quantity": 1,
                                  "side": "BUY"
                                }
                                """.formatted(traderId, stock)))
                .andExpect(status().isCreated());
    }

    private int extractOrderId(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("orderId").asInt();
    }
}
