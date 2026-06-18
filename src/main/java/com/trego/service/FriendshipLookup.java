package com.trego.service;

/**
 * Narrow seam letting {@link SocialService} ask "are these two users friends?"
 * without depending on the whole {@link FriendService}. Implemented by
 * FriendService; tests can supply a trivial lambda.
 */
public interface FriendshipLookup {
    boolean areFriends(String a, String b);
}
