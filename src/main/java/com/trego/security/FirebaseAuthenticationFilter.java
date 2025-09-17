package com.trego.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.trego.model.User;
import com.trego.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    
    @Autowired
    private FirebaseAuth firebaseAuth;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String authorizationHeader = request.getHeader("Authorization");
            
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                
                // Try JWT token first
                if (tryJwtAuthentication(token, request)) {
                    logger.debug("JWT authentication successful");
                } else if (tryFirebaseAuthentication(token, request)) {
                    logger.debug("Firebase authentication successful");
                } else {
                    logger.debug("Token authentication failed");
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean tryJwtAuthentication(String token, HttpServletRequest request) {
        try {
            if (!jwtUtil.isAccessToken(token)) {
                return false;
            }
            
            String userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                Optional<User> userOpt = authService.findUserById(userId);
                if (userOpt.isPresent() && jwtUtil.validateToken(token, userId)) {
                    User user = userOpt.get();
                    
                    FirebaseUserPrincipal userPrincipal = new FirebaseUserPrincipal(user, userId);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, userPrincipal.getAuthorities()
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("JWT authentication failed: {}", e.getMessage());
        }
        return false;
    }
    
    private boolean tryFirebaseAuthentication(String idToken, HttpServletRequest request) {
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            
            if (uid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                Optional<User> userOpt = authService.findUserById(uid);
                if (userOpt.isEmpty()) {
                    // Create user if doesn't exist (Firebase sync)
                    userOpt = authService.syncFirebaseUser(decodedToken);
                }
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    FirebaseUserPrincipal userPrincipal = new FirebaseUserPrincipal(user, uid);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, userPrincipal.getAuthorities()
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    return true;
                }
            }
        } catch (FirebaseAuthException e) {
            logger.debug("Firebase authentication failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during Firebase authentication: {}", e.getMessage());
        }
        return false;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Remove context path if present
        String contextPath = request.getContextPath();
        if (path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        
        // Check if this is a public endpoint that doesn't require authentication
        return path.startsWith("/actuator/") || 
               path.equals("/auth/register") || 
               path.equals("/auth/login") ||
               path.equals("/auth/forgot-password") ||
               path.equals("/auth/reset-password") ||
               path.equals("/auth/verify-email") ||
               path.startsWith("/public/");
    }
}