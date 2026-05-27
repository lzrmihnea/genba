package eu.px.genba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end auth flow against a real pgvector-enabled PostgreSQL: Liquibase
 * migrates from clean, SeedAdminUserRunner provisions the test admin, then we
 * exercise login → me → refresh and a couple of error paths.
 *
 * <p>Requires a reachable Docker daemon. {@code @Disabled} by default because
 * Docker Desktop 4.60+ (this Mac's version) proxies its socket through a
 * mode that returns a stub {@code /info} response with Status 400 — even on
 * TestContainers 1.21.3 (the current latest). Two ways to enable locally:
 *
 * <ol>
 *   <li><b>Docker Desktop</b>: Settings → Advanced → enable "Allow the default
 *       Docker socket to be used (requires password)". This creates the
 *       traditional {@code /var/run/docker.sock} that TestContainers' Unix
 *       strategy can talk to directly without going through the proxy.</li>
 *   <li><b>Alternative runtime</b>: Colima / OrbStack / Lima with
 *       {@code export DOCKER_HOST=unix:///path/to/socket}.</li>
 * </ol>
 *
 * <p>Once either is in place, remove the {@code @Disabled} annotation.
 *
 * <p>The auth flow this test exercises has been manually verified end-to-end
 * against a live pgvector PostgreSQL container during sections 5–8 bring-up:
 * login, /me, /refresh, RO/EN error localization, missing-token 401.
 */
@Disabled("Docker Desktop 4.60+ proxy returns Status 400 on /info; see class javadoc")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    private static final String SEED_EMAIL = "test-admin@genba.test";
    private static final String SEED_PASSWORD = "test-pass-2026";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg15")
                    .withDatabaseName("genba_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("seed.admin-email", () -> SEED_EMAIL);
        registry.add("seed.admin-password", () -> SEED_PASSWORD);
        registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-32-characters-long-for-hs256");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void loginThenMeThenRefresh_returnsTokensAndOrg() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(SEED_EMAIL, SEED_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(SEED_EMAIL))
                .andExpect(jsonPath("$.organizations.length()").value(1))
                .andExpect(jsonPath("$.organizations[0].orgRole").value("OWNER"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(SEED_EMAIL))
                .andExpect(jsonPath("$.organizations[0].organizationName").value("Mihnea's Builds"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401InvalidCredentials_englishByDefault() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(SEED_EMAIL, "WRONG"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.messageKey").value("auth.error.invalidCredentials"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_withWrongPassword_localizesToRomanian() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(SEED_EMAIL, "WRONG"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email sau parolă incorecte"));
    }

    @Test
    void me_withoutToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void refresh_withInvalidToken_returns401TokenInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("not-a-real-jwt"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));
    }
}
