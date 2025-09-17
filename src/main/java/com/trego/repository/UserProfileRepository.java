package com.trego.repository;

import com.trego.model.UserProfile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class UserProfileRepository extends FirestoreRepository<UserProfile> {
    
    public UserProfileRepository() {
        super("userProfiles", UserProfile::fromFirestoreMap);
    }
    
    public Optional<UserProfile> findByUserId(String userId) throws ExecutionException, InterruptedException {
        List<UserProfile> profiles = findByField("userId", userId);
        return profiles.isEmpty() ? Optional.empty() : Optional.of(profiles.get(0));
    }
    
    public List<UserProfile> findByFitnessGoal(String goal) throws ExecutionException, InterruptedException {
        return findByField("fitnessGoals", goal);
    }
    
    public List<UserProfile> findByActivityLevel(String activityLevel) throws ExecutionException, InterruptedException {
        return findByField("activityLevel", activityLevel);
    }
    
    public List<UserProfile> findByExperienceLevel(String experience) throws ExecutionException, InterruptedException {
        return findByField("experience", experience);
    }
    
    public List<UserProfile> findByGender(String gender) throws ExecutionException, InterruptedException {
        return findByField("gender", gender);
    }
    
    public boolean profileExistsForUser(String userId) throws ExecutionException, InterruptedException {
        Optional<UserProfile> profile = findByUserId(userId);
        return profile.isPresent();
    }
    
    public void deleteByUserId(String userId) throws ExecutionException, InterruptedException {
        Optional<UserProfile> profile = findByUserId(userId);
        if (profile.isPresent()) {
            deleteById(profile.get().getId());
        }
    }
}