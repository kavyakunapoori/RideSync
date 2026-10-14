package com.carpool.ridesharing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndRoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public landing page should be accessible without authentication")
    void testPublicLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login page should be publicly accessible")
    void testPublicLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public ride search should be accessible without credentials")
    void testPublicSearchPage() throws Exception {
        mockMvc.perform(get("/passenger/search"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/dashboard should redirect to login")
    void testUnauthenticatedAdminAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "alice@passenger.com", roles = {"PASSENGER"})
    @DisplayName("Ordinary passenger cannot access administrator functions by changing URL (HTTP 403)")
    void testPassengerForbiddenFromAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "driver@carpool.com", roles = {"DRIVER"})
    @DisplayName("Driver cannot access administrator functions (HTTP 403)")
    void testDriverForbiddenFromAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@carpool.com", roles = {"ADMIN"})
    @DisplayName("Administrator with ROLE_ADMIN is authorized to view admin dashboard (HTTP 200)")
    void testAdminAuthorizedForAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }
}
