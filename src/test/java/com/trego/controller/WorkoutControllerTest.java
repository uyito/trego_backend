package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.model.User;
import com.trego.model.WorkoutSession;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.ActivitySessionService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkoutController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class WorkoutControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ActivitySessionService service;
    @MockBean FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static RequestPostProcessor authenticatedAs(String uid) {
        User user = new User();
        user.setId(uid);
        user.setEmail(uid + "@test.example");
        user.setActive(true);
        user.setEmailVerified(true);
        user.setRoles(Collections.singletonList("USER"));
        FirebaseUserPrincipal principal = new FirebaseUserPrincipal(user, uid);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(firebaseAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

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
    void postSessionRequiresAuth() throws Exception {
        mvc.perform(post("/workouts/sessions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void postSessionSavesAndReturnsIt() throws Exception {
        WorkoutSession saved = new WorkoutSession("u", "cardio");
        saved.setId("s1"); saved.setActivityType("hiking");
        when(service.logSession(eq("u"), any())).thenReturn(saved);

        mvc.perform(post("/workouts/sessions").with(csrf()).with(authenticatedAs("u"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activityType\":\"hiking\",\"logKind\":\"distanceCardio\",\"distance\":12.5}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.session.activityType").value("hiking"));
        verify(service).logSession(eq("u"), any());
    }

    @Test
    void getHistoryReturnsSessions() throws Exception {
        WorkoutSession s = new WorkoutSession("u", "strength"); s.setId("s1");
        when(service.getHistory("u")).thenReturn(List.of(s));
        mvc.perform(get("/workouts/sessions").with(authenticatedAs("u")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.sessions[0].id").value("s1"));
    }

    @Test
    void getPrsReturnsList() throws Exception {
        Map<String,Object> pr = new LinkedHashMap<>();
        pr.put("activityType", "running"); pr.put("bestDistance", 10.0);
        when(service.computePRs("u")).thenReturn(List.of(pr));
        mvc.perform(get("/workouts/prs").with(authenticatedAs("u")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.prs[0].activityType").value("running"));
    }

    @Test
    void getDetailReturnsSession() throws Exception {
        WorkoutSession s = new WorkoutSession("u", "cardio");
        s.setId("s1");
        s.setActivityType("hiking");
        when(service.getSession("u", "s1")).thenReturn(s);
        mvc.perform(get("/workouts/sessions/s1").with(authenticatedAs("u")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.session.id").value("s1"));
    }

    @Test
    void getDetailReturns404WhenMissing() throws Exception {
        when(service.getSession("u", "missing")).thenReturn(null);
        mvc.perform(get("/workouts/sessions/missing").with(authenticatedAs("u")))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.success").value(false));
    }
}
