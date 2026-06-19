package com.trego.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class SocialServiceTest {

    static final String ALICE = "alice-uid";
    static final String BOB = "bob-uid";

    InMemorySocialRepository repo;
    SocialService service;

    @BeforeEach
    void setUp() {
        repo = new InMemorySocialRepository();
        service = new SocialService(repo);
    }

    private String createPost(String uid, String content, String visibility) {
        Map<String, Object> p = service.createPost(uid, uid + "-name", null, content,
                "general", List.of(), visibility);
        return (String) p.get("id");
    }

    @Test
    void createPostReturnsViewModelWithAuthorAndOwnFlag() {
        Map<String, Object> post = service.createPost(ALICE, "Alice A", null, "hello",
                "workout", List.of("img1"), "public");

        assertEquals("hello", post.get("content"));
        assertEquals("workout", post.get("type"));
        assertEquals(true, post.get("isOwn"));
        assertEquals(false, post.get("userLiked"));
        assertEquals(0, post.get("likesCount"));
        assertEquals(0, post.get("commentsCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> author = (Map<String, Object>) post.get("author");
        assertEquals(ALICE, author.get("id"));
        assertEquals("Alice A", author.get("name"));
        assertNotNull(post.get("createdAt"));
    }

    @Test
    void feedShowsPublicPostsFromOthersButNotPrivateOnes() {
        createPost(BOB, "bob public", "public");
        createPost(BOB, "bob private", "private");

        List<Map<String, Object>> feed = service.getFeed(ALICE, 20, 0);

        assertEquals(1, feed.size());
        assertEquals("bob public", feed.get(0).get("content"));
        assertEquals(false, feed.get(0).get("isOwn"));
    }

    @Test
    void feedShowsViewerOwnPrivatePosts() {
        createPost(ALICE, "alice private", "private");

        List<Map<String, Object>> feed = service.getFeed(ALICE, 20, 0);

        assertEquals(1, feed.size());
        assertEquals("alice private", feed.get(0).get("content"));
        assertEquals(true, feed.get(0).get("isOwn"));
    }

    @Test
    void feedNewestFirstAndRespectsPagination() {
        createPost(ALICE, "first", "public");
        createPost(ALICE, "second", "public");
        createPost(ALICE, "third", "public");

        List<Map<String, Object>> page1 = service.getFeed(ALICE, 2, 0);
        assertEquals(2, page1.size());
        assertEquals("third", page1.get(0).get("content"));
        assertEquals("second", page1.get(1).get("content"));

        List<Map<String, Object>> page2 = service.getFeed(ALICE, 2, 2);
        assertEquals(1, page2.size());
        assertEquals("first", page2.get(0).get("content"));
    }

    @Test
    void toggleLikeFlipsStateAndCount() {
        String id = createPost(ALICE, "likeable", "public");

        Map<String, Object> liked = service.toggleLike(BOB, id);
        assertEquals(true, liked.get("userLiked"));
        assertEquals(1, liked.get("likesCount"));

        Map<String, Object> unliked = service.toggleLike(BOB, id);
        assertEquals(false, unliked.get("userLiked"));
        assertEquals(0, unliked.get("likesCount"));
    }

    @Test
    void likeReflectedInPerViewerFeed() {
        String id = createPost(ALICE, "p", "public");
        service.toggleLike(BOB, id);

        Map<String, Object> forBob = service.getFeed(BOB, 20, 0).get(0);
        Map<String, Object> forAlice = service.getFeed(ALICE, 20, 0).get(0);
        assertEquals(true, forBob.get("userLiked"));
        assertEquals(false, forAlice.get("userLiked"));
        assertEquals(1, forBob.get("likesCount"));
    }

    @Test
    void addCommentIncrementsCountAndReturnsComment() {
        String id = createPost(ALICE, "p", "public");

        Map<String, Object> result = service.addComment(BOB, "Bob B", null, id, "nice run!");
        assertEquals(1, result.get("commentsCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> comment = (Map<String, Object>) result.get("comment");
        assertEquals("nice run!", comment.get("content"));
        @SuppressWarnings("unchecked")
        Map<String, Object> author = (Map<String, Object>) comment.get("author");
        assertEquals(BOB, author.get("id"));

        List<Map<String, Object>> comments = service.getComments(id);
        assertEquals(1, comments.size());
        assertEquals("nice run!", comments.get(0).get("content"));
    }

    @Test
    void reportPersistsReport() {
        String id = createPost(ALICE, "p", "public");
        service.report(BOB, id, "spam");
        assertEquals(1, repo.reports.size());
        assertEquals("spam", repo.reports.get(0).getReason());
        assertEquals(BOB, repo.reports.get(0).getReporterId());
    }

    @Test
    void updatePostByAuthorChangesContent() {
        String id = createPost(ALICE, "old", "public");
        Map<String, Object> updated = service.updatePost(ALICE, id, "new");
        assertEquals("new", updated.get("content"));
        assertEquals("new", service.getFeed(ALICE, 20, 0).get(0).get("content"));
    }

    @Test
    void updatePostByNonAuthorThrows() {
        String id = createPost(ALICE, "old", "public");
        assertThrows(SecurityException.class, () -> service.updatePost(BOB, id, "hacked"));
    }

    @Test
    void deletePostByAuthorRemovesItAndComments() {
        String id = createPost(ALICE, "doomed", "public");
        service.addComment(BOB, "Bob", null, id, "c1");

        service.deletePost(ALICE, id);

        assertTrue(service.getFeed(ALICE, 20, 0).isEmpty());
        assertTrue(service.getComments(id).isEmpty());
    }

    @Test
    void deletePostByNonAuthorThrows() {
        String id = createPost(ALICE, "p", "public");
        assertThrows(SecurityException.class, () -> service.deletePost(BOB, id));
    }

    @Test
    void operationsOnMissingPostThrowNotFound() {
        assertThrows(NoSuchElementException.class, () -> service.toggleLike(ALICE, "nope"));
        assertThrows(NoSuchElementException.class, () -> service.updatePost(ALICE, "nope", "x"));
        assertThrows(NoSuchElementException.class, () -> service.deletePost(ALICE, "nope"));
    }
}
