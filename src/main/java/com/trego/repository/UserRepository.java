package com.trego.repository;

import com.trego.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository extends FirestoreRepository<User> {
    
    public UserRepository() {
        super("users", User::fromFirestoreMap);
    }
    
    public Optional<User> findByEmail(String email) throws ExecutionException, InterruptedException {
        List<User> users = findByField("email", email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public List<User> findBySubscriptionStatus(String subscriptionStatus) throws ExecutionException, InterruptedException {
        return findByField("subscriptionStatus", subscriptionStatus);
    }
    
    public List<User> findActiveUsers() throws ExecutionException, InterruptedException {
        return findByField("isActive", true);
    }
    
    public List<User> findUnverifiedUsers() throws ExecutionException, InterruptedException {
        return findByField("emailVerified", false);
    }
    
    public boolean emailExists(String email) throws ExecutionException, InterruptedException {
        List<User> users = findByField("email", email);
        return !users.isEmpty();
    }
    
    public long countActiveUsers() throws ExecutionException, InterruptedException {
        List<User> activeUsers = findActiveUsers();
        return activeUsers.size();
    }
    
    public long countPremiumUsers() throws ExecutionException, InterruptedException {
        List<User> premiumUsers = findBySubscriptionStatus("PREMIUM");
        List<User> premiumPlusUsers = findBySubscriptionStatus("PREMIUM_PLUS");
        return premiumUsers.size() + premiumPlusUsers.size();
    }
}