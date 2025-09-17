package com.trego.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;
    
    public String getSecret() {
        return secret;
    }
    
    public Long getExpiration() {
        return expiration;
    }
    
    public Long getRefreshExpiration() {
        return refreshExpiration;
    }
}