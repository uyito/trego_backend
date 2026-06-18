package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.dto.*;
import com.trego.model.User;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetricsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class MetricsControllerTest {

    @Autowired MockMvc mvc;

    @MockBean MetricsService service;

    // SecurityConfig depends on these beans; mock them so @WebMvcTest can load
    @MockBean FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static RequestPostProcessor authenticatedAs(String firebaseUid) {
        User user = new User();
        user.setId(firebaseUid);
        user.setEmail(firebaseUid + "@test.example");
        user.setActive(true);
        user.setEmailVerified(true);
        user.setRoles(Collections.singletonList("USER"));
        FirebaseUserPrincipal principal = new FirebaseUserPrincipal(user, firebaseUid);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @BeforeEach
    void setUp() throws Exception {
        // The real FirebaseAuthenticationFilter calls filterChain.doFilter when no token
        // is present (delegating to the next filter). The Mockito mock's default void
        // behavior is a no-op, which would stop the filter chain — break unauth tests
        // (no 401) and authenticated tests (no handler invocation). Stub the mock to
        // pass through so Spring Security can do its job.
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(firebaseAuthenticationFilter).doFilter(
            any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        // The real JwtAuthenticationEntryPoint writes a 401 JSON body. The Mockito
        // mock's default void behavior is a no-op, so the response status stays at 200.
        // Stub commence() to set status 401 so the unauth tests can assert on it.
        doAnswer(invocation -> {
            HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(AuthenticationException.class));
    }

    @Test
    void getReturnsUnauthorizedWithoutAuth() throws Exception {
        mvc.perform(get("/metrics/me"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsSnapshot() throws Exception {
        MetricsSnapshotDto dto = new MetricsSnapshotDto();
        dto.setComputedAt(Instant.parse("2026-04-27T18:34:12Z"));
        WeeklyMetricsDto wk = new WeeklyMetricsDto();
        wk.setIsoYearWeek("2026-W17");
        dto.setThisWeek(wk);
        dto.setPrs(new PrsDto());
        LifetimeTotalsDto totals = new LifetimeTotalsDto();
        totals.setTotalRuns(0);
        dto.setTotals(totals);
        dto.setHistory(Collections.emptyList());

        when(service.getSnapshot(eq("test-user"))).thenReturn(dto);

        mvc.perform(get("/metrics/me").with(authenticatedAs("test-user")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.thisWeek.isoYearWeek").value("2026-W17"));
    }

    @Test
    void postRecomputeReturnsUnauthorizedWithoutAuth() throws Exception {
        mvc.perform(post("/metrics/me/recompute").with(csrf()))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void postRecomputeReturnsResult() throws Exception {
        when(service.recompute(eq("test-user"))).thenReturn(
            new RecomputeResultDto(Instant.parse("2026-04-27T18:34:12Z"), 42, 87L));

        mvc.perform(post("/metrics/me/recompute").with(csrf()).with(authenticatedAs("test-user")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.runCount").value(42))
           .andExpect(jsonPath("$.durationMs").value(87));
    }

    @Test
    void getGoalRequiresAuth() throws Exception {
        mvc.perform(get("/metrics/me/goal"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void getGoalReturnsGoal() throws Exception {
        when(service.getGoal(eq("test-user")))
            .thenReturn(new com.trego.dto.WeeklyGoalDto(25.0, 4, Instant.parse("2026-04-21T12:00:00Z")));

        mvc.perform(get("/metrics/me/goal").with(authenticatedAs("test-user")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.targetKm").value(25.0))
           .andExpect(jsonPath("$.targetRuns").value(4));
    }

    @Test
    void putGoalReturnsStoredGoal() throws Exception {
        when(service.setGoal(eq("test-user"), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new com.trego.dto.WeeklyGoalDto(30.0, 5, Instant.parse("2026-04-21T12:00:00Z")));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/metrics/me/goal").with(csrf()).with(authenticatedAs("test-user"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"targetKm\":30.0,\"targetRuns\":5}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.targetKm").value(30.0))
           .andExpect(jsonPath("$.targetRuns").value(5));
    }
}
