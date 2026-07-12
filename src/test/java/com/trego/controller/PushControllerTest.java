package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.model.User;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.DeviceTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(PushController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class PushControllerTest {

    @Autowired MockMvc mvc;

    @MockBean DeviceTokenService service;
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
    void registerRequiresAuth() throws Exception {
        mvc.perform(post("/push/tokens").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t1\",\"platform\":\"android\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerStoresTokenForUser() throws Exception {
        mvc.perform(post("/push/tokens").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t1\",\"platform\":\"android\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> uid = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tok = ArgumentCaptor.forClass(String.class);
        verify(service).register(uid.capture(), tok.capture(), eq("android"));
        assertEquals("alice", uid.getValue());
        assertEquals("t1", tok.getValue());
    }

    @Test
    void registerRejectsEmptyToken() throws Exception {
        mvc.perform(post("/push/tokens").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"android\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unregisterDeletesToken() throws Exception {
        mvc.perform(delete("/push/tokens/t1").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(service).unregister(eq("t1"));
    }
}
