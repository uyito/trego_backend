package com.trego.controller;

import com.trego.config.SecurityConfig;
import com.trego.model.User;
import com.trego.security.FirebaseAuthenticationFilter;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.security.JwtAuthenticationEntryPoint;
import com.trego.service.SocialService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SocialController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:3000")
class SocialControllerTest {

    @Autowired MockMvc mvc;

    @MockBean SocialService service;

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

    private static Map<String, Object> samplePost(String id, String content, boolean isOwn) {
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("id", "alice");
        author.put("name", "Alice");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("content", content);
        m.put("author", author);
        m.put("likesCount", 0);
        m.put("commentsCount", 0);
        m.put("userLiked", false);
        m.put("isOwn", isOwn);
        return m;
    }

    @Test
    void feedRequiresAuth() throws Exception {
        mvc.perform(get("/social/posts/feed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void feedReturnsPosts() throws Exception {
        when(service.getFeed(eq("alice"), anyInt(), anyInt()))
                .thenReturn(List.of(samplePost("p1", "hi", true)));

        mvc.perform(get("/social/posts/feed").with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.posts[0].content").value("hi"))
                .andExpect(jsonPath("$.posts[0].isOwn").value(true));
    }

    @Test
    void createPostReturnsPost() throws Exception {
        when(service.createPost(eq("alice"), any(), any(), eq("hello"), any(), any(), any()))
                .thenReturn(samplePost("p1", "hello", true));

        mvc.perform(post("/social/posts").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\",\"type\":\"general\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.post.content").value("hello"));
    }

    @Test
    void createPostRejectsEmptyContent() throws Exception {
        mvc.perform(post("/social/posts").with(csrf()).with(authenticatedAs("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getCommentsReturnsList() throws Exception {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", "c1");
        c.put("content", "nice");
        when(service.getComments(eq("p1"))).thenReturn(List.of(c));

        mvc.perform(get("/social/posts/p1/comments").with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].content").value("nice"));
    }

    @Test
    void reportReturnsSuccess() throws Exception {
        mvc.perform(post("/social/posts/p1/report").with(csrf()).with(authenticatedAs("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateForbiddenForNonAuthor() throws Exception {
        when(service.updatePost(eq("bob"), eq("p1"), any()))
                .thenThrow(new SecurityException("not author"));

        mvc.perform(patch("/social/posts/p1").with(csrf()).with(authenticatedAs("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hacked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteMissingReturnsNotFound() throws Exception {
        doThrow(new NoSuchElementException("missing"))
                .when(service).deletePost(eq("alice"), eq("ghost"));

        mvc.perform(delete("/social/posts/ghost").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteOwnReturnsSuccess() throws Exception {
        mvc.perform(delete("/social/posts/p1").with(csrf()).with(authenticatedAs("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
