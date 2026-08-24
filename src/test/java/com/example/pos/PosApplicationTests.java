package com.example.pos;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "pos.seed.demo-enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PosApplicationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.7-alpine")
            .withDatabaseName("pos_test")
            .withUsername("pos_test")
            .withPassword("pos_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

	@Test
    void contextLoads() {
    }

    @Test
    void cashierCanHydrateMedicineReferences() throws Exception {
        AuthenticatedSession cashier = login("cashier@demo.com", "cashier123");

        assertOk(cashier.cookie(), "/api/v1/categories?size=10");
        assertOk(cashier.cookie(), "/api/v1/units?size=10");
        assertOk(cashier.cookie(), "/api/v1/manufacturers?size=10");
        assertOk(cashier.cookie(), "/api/v1/tax-categories?activeOnly=true&size=10");
        assertOk(cashier.cookie(), "/api/v1/medicines?size=10");
        assertOk(cashier.cookie(), "/api/v1/pos/lookup?name=paracetamol");
    }

    @Test
    void pharmacistCanHydrateInventoryAndDashboard() throws Exception {
        AuthenticatedSession pharmacist = login("pharmacist@demo.com", "pharmacist123");

        assertOk(pharmacist.cookie(), "/api/v1/categories?size=10");
        assertOk(pharmacist.cookie(), "/api/v1/tax-categories?activeOnly=true&size=10");
        assertOk(pharmacist.cookie(), "/api/v1/stock?size=10");
        assertOk(pharmacist.cookie(),
                "/api/v1/batches?branchId=" + pharmacist.branchId() + "&size=10");
        assertOk(pharmacist.cookie(), "/api/v1/goods-received?size=10");
        assertOk(pharmacist.cookie(), "/api/v1/stock-movements?size=10");
        mockMvc.perform(get("/api/v1/sale-returns")
                        .param("saleId", "00000000-0000-0000-0000-000000000001")
                        .cookie(pharmacist.cookie()))
                .andExpect(status().isNotFound());
        assertOk(pharmacist.cookie(), "/api/v1/reports/dashboard?branchId=" + pharmacist.branchId());
    }

    @Test
    void storeKeeperCanReadInventoryReportButNotSalesReport() throws Exception {
        AuthenticatedSession storeKeeper = login("storekeeper@demo.com", "stock1234");

        assertOk(storeKeeper.cookie(), "/api/v1/tax-categories?activeOnly=true&size=10");
        assertOk(storeKeeper.cookie(), "/api/v1/goods-received?size=10");
        assertOk(storeKeeper.cookie(),
                "/api/v1/reports/inventory-summary?branchId=" + storeKeeper.branchId());
        mockMvc.perform(get("/api/v1/reports/sales-summary")
                        .param("branchId", storeKeeper.branchId())
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .cookie(storeKeeper.cookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanRequestPharmacyWideReports() throws Exception {
        AuthenticatedSession owner = login("admin@demo.com", "admin123");

        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("branchId", owner.branchId())
                        .param("pharmacyWide", "true")
                        .cookie(owner.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pharmacyWide").value(true));
        mockMvc.perform(get("/api/v1/reports/inventory-summary")
                        .param("branchId", owner.branchId())
                        .param("pharmacyWide", "true")
                        .cookie(owner.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pharmacyWide").value(true));
    }

    @Test
    void ownerCanRegisterTerminalAndPersistHardwareTelemetry() throws Exception {
        AuthenticatedSession owner = login("admin@demo.com", "admin123");
        String terminalName = "Test register " + UUID.randomUUID();

        var registration = mockMvc.perform(post("/api/v1/terminals/register")
                        .with(csrf())
                        .cookie(owner.cookie())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", terminalName,
                                "terminalType", "WINDOWS",
                                "manufacturer", "Test",
                                "model", "Integration",
                                "platform", "Browser",
                                "osVersion", "Test",
                                "branchId", owner.branchId()))))
                .andExpect(status().isCreated())
                .andReturn();
        String terminalId = objectMapper.readTree(registration.getResponse().getContentAsByteArray())
                .at("/data/terminalId").asText();

        mockMvc.perform(post("/api/v1/terminals/{terminalId}/peripherals", terminalId)
                        .with(csrf())
                        .cookie(owner.cookie())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "type", "PRINTER",
                                "manufacturer", "Generic ESC/POS",
                                "model", "Thermal 80 mm",
                                "connectionType", "NETWORK",
                                "configuration", "192.168.1.100:9100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configuration").value("192.168.1.100:9100"));

        mockMvc.perform(post("/api/v1/terminals/{terminalId}/heartbeat", terminalId)
                        .with(csrf())
                        .cookie(owner.cookie())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "terminalId", terminalId,
                                "networkType", "ONLINE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("received"));
    }

    private AuthenticatedSession login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String branchId = response.at("/data/user/activeBranch/id").asText();
        Cookie sessionCookie = Objects.requireNonNull(result.getResponse().getCookie("SESSION"));
        return new AuthenticatedSession(sessionCookie, branchId);
    }

    private void assertOk(Cookie cookie, String endpoint) throws Exception {
        mockMvc.perform(get(endpoint).cookie(cookie)).andExpect(status().isOk());
    }

    private record LoginRequest(String email, String password) {
    }

    private record AuthenticatedSession(Cookie cookie, String branchId) {
    }

}
