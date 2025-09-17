package com.trego.security;

import com.trego.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class FirebaseUserPrincipal implements UserDetails {
    
    private final User user;
    private final String firebaseUid;
    
    public FirebaseUserPrincipal(User user, String firebaseUid) {
        this.user = user;
        this.firebaseUid = firebaseUid;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getFirebaseUid() {
        return firebaseUid;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        // Firebase handles password authentication, so we don't store it
        return null;
    }
    
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return user.isActive();
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Firebase tokens handle expiration
    }
    
    @Override
    public boolean isEnabled() {
        return user.isActive() && user.isEmailVerified();
    }
}