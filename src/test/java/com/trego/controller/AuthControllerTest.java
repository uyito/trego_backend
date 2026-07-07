package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.dto.AuthResponse;
import com.trego.model.User;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.AuthService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AuthService authService;
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
    void setUsernameRequiresAuth() throws Exception {
        mvc.perform(put("/auth/username").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"runner\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setUsernameSucceeds() throws Exception {
        User u = new User();
        u.setId("alice");
        u.setUsername("runner");
        when(authService.setUsername(eq("alice"), eq("runner")))
                .thenReturn(AuthResponse.fromUser(u));

        mvc.perform(put("/auth/username").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"runner\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("runner"));
    }

    @Test
    void setUsernameTakenReturns409() throws Exception {
        when(authService.setUsername(eq("alice"), any()))
                .thenThrow(new IllegalStateException("That username is already taken"));

        mvc.perform(put("/auth/username").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"taken\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void setUsernameInvalidReturns400() throws Exception {
        when(authService.setUsername(eq("alice"), any()))
                .thenThrow(new IllegalArgumentException("Username must be 3–20 characters"));

        mvc.perform(put("/auth/username").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
