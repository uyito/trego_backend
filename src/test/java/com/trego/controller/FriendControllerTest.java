package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.model.User;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.FriendService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class FriendControllerTest {

    @Autowired MockMvc mvc;

    @MockBean FriendService service;

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
    void friendsRequiresAuth() throws Exception {
        mvc.perform(get("/social/friends"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendRequestPending() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "pending");
        result.put("request", Map.of("id", "r1", "type", "outgoing"));
        when(service.sendRequest(eq("alice"), eq("bob@test.example"), any())).thenReturn(result);

        mvc.perform(post("/social/friends/request").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"bob@test.example\",\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void sendRequestUnknownEmailIs404() throws Exception {
        when(service.sendRequest(eq("alice"), any(), any()))
                .thenThrow(new NoSuchElementException("No user found for that email"));

        mvc.perform(post("/social/friends/request").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ghost@test.example\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void sendRequestAlreadyFriendsIs409() throws Exception {
        when(service.sendRequest(eq("alice"), any(), any()))
                .thenThrow(new IllegalStateException("You are already friends"));

        mvc.perform(post("/social/friends/request").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"bob@test.example\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void respondAccepts() throws Exception {
        mvc.perform(put("/social/friends/respond").with(csrf()).with(authenticatedAs("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"r1\",\"accept\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void respondForbiddenForNonRecipient() throws Exception {
        doThrow(new SecurityException("not recipient"))
                .when(service).respondToRequest(eq("carol"), eq("r1"), anyBoolean());

        mvc.perform(put("/social/friends/respond").with(csrf()).with(authenticatedAs("carol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"r1\",\"accept\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelRequest() throws Exception {
        mvc.perform(delete("/social/friends/request/r1").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getFriendsReturnsList() throws Exception {
        when(service.getFriends(eq("alice")))
                .thenReturn(List.of(Map.of("id", "f1", "uid", "bob", "name", "Bob B")));

        mvc.perform(get("/social/friends").with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends[0].uid").value("bob"));
    }

    @Test
    void getRequestsReturnsList() throws Exception {
        when(service.getRequests(eq("alice")))
                .thenReturn(List.of(Map.of("id", "r1", "type", "incoming")));

        mvc.perform(get("/social/friends/requests").with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests[0].type").value("incoming"));
    }

    @Test
    void unfriendSucceeds() throws Exception {
        mvc.perform(delete("/social/friends/bob").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
