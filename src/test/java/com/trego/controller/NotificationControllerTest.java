package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.model.User;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.NotificationFeedService;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class NotificationControllerTest {

    @Autowired MockMvc mvc;

    @MockBean NotificationFeedService service;
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
    void listRequiresAuth() throws Exception {
        mvc.perform(get("/social/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsItemsAndUnreadCount() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notifications", List.of(Map.of("id", "n1", "message", "Bob B liked your post", "read", false)));
        result.put("unreadCount", 1);
        when(service.list(eq("alice"), anyInt())).thenReturn(result);

        mvc.perform(get("/social/notifications").with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].message").value("Bob B liked your post"));
    }

    @Test
    void unreadCountEndpoint() throws Exception {
        when(service.unreadCount(eq("alice"))).thenReturn(3);

        mvc.perform(get("/social/notifications/unread-count").with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void markReadSucceeds() throws Exception {
        mvc.perform(put("/social/notifications/n1/read").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void markAllReadSucceeds() throws Exception {
        mvc.perform(put("/social/notifications/read-all").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteSucceeds() throws Exception {
        mvc.perform(delete("/social/notifications/n1").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
