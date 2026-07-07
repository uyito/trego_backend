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
    void friendsPostVisibleToFriendButNotStranger() {
        // Service where ALICE and BOB are friends, CAROL is not.
        FriendshipLookup lookup = (a, b) -> {
            var pair = java.util.Set.of(a, b);
            return pair.equals(java.util.Set.of(ALICE, BOB));
        };
        SocialService svc = new SocialService(repo, lookup);

        svc.createPost(BOB, "Bob B", null, "bob friends-only", "general", List.of(), "friends");

        // Friend sees it.
        List<Map<String, Object>> aliceFeed = svc.getFeed(ALICE, 20, 0);
        assertEquals(1, aliceFeed.size());
        assertEquals("bob friends-only", aliceFeed.get(0).get("content"));
        assertEquals(false, aliceFeed.get(0).get("isOwn"));

        // Stranger does not.
        assertTrue(svc.getFeed("carol-uid", 20, 0).isEmpty());

        // Author always sees their own.
        assertEquals(1, svc.getFeed(BOB, 20, 0).size());
    }

    @Test
    void friendsPostHiddenWhenFriendshipLookupDisabled() {
        // Default one-arg constructor disables friends-visibility.
        createPost(BOB, "bob friends-only", "friends");
        assertTrue(service.getFeed(ALICE, 20, 0).isEmpty());
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

    // --- notification emission ---

    @Test
    void likeEmitsPostLikeToAuthor() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = new SocialService(repo, (a, b) -> false, emitter);
        final String id = (String) svc.createPost(ALICE, "Alice", null, "hi",
                "general", java.util.List.of(), "public").get("id");

        svc.toggleLike(BOB, id);

        assertEquals(1, emitter.emitted.size());
        assertEquals(ALICE, emitter.emitted.get(0).recipientUid);
        assertEquals("post_like", emitter.emitted.get(0).type);
        assertEquals(BOB, emitter.emitted.get(0).actorUid);
    }

    @Test
    void unlikeDoesNotEmit() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = new SocialService(repo, (a, b) -> false, emitter);
        final String id = (String) svc.createPost(ALICE, "Alice", null, "hi",
                "general", java.util.List.of(), "public").get("id");

        svc.toggleLike(BOB, id);   // like → emits
        svc.toggleLike(BOB, id);   // unlike → no emit

        assertEquals(1, emitter.emitted.size());
    }

    @Test
    void commentEmitsPostCommentToAuthor() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = new SocialService(repo, (a, b) -> false, emitter);
        final String id = (String) svc.createPost(ALICE, "Alice", null, "hi",
                "general", java.util.List.of(), "public").get("id");

        svc.addComment(BOB, "Bob", null, id, "nice");

        assertEquals(1, emitter.emitted.size());
        assertEquals(ALICE, emitter.emitted.get(0).recipientUid);
        assertEquals("post_comment", emitter.emitted.get(0).type);
    }

    // --- @mentions ---

    /** Resolver that maps a fixed set of lowercased handles to uids. */
    static final class FakeResolver implements MentionResolver {
        final Map<String, String> byHandle;
        FakeResolver(Map<String, String> byHandle) { this.byHandle = byHandle; }
        @Override public java.util.Optional<String> resolveUid(String username) {
            return java.util.Optional.ofNullable(byHandle.get(username));
        }
    }

    private SocialService serviceWith(RecordingNotificationEmitter emitter, FakeResolver resolver) {
        return new SocialService(repo, (a, b) -> false, emitter, resolver);
    }

    @Test
    void createPostResolvesMentionsAndNotifies() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = serviceWith(emitter, new FakeResolver(Map.of("bob", BOB)));

        @SuppressWarnings("unchecked")
        Map<String, Object> post = svc.createPost(ALICE, "Alice", null,
                "great run @Bob!", "general", List.of(), "public");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mentions = (List<Map<String, Object>>) post.get("mentions");
        assertEquals(1, mentions.size());
        assertEquals(BOB, mentions.get(0).get("uid"));
        assertEquals("bob", mentions.get(0).get("username"));

        assertEquals(1, emitter.emitted.size());
        assertEquals(BOB, emitter.emitted.get(0).recipientUid);
        assertEquals("mention", emitter.emitted.get(0).type);
        assertEquals(ALICE, emitter.emitted.get(0).actorUid);
    }

    @Test
    void mentionOfSelfIsSkipped() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = serviceWith(emitter, new FakeResolver(Map.of("alice", ALICE)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mentions = (List<Map<String, Object>>) svc.createPost(
                ALICE, "Alice", null, "note to @alice", "general", List.of(), "public").get("mentions");

        assertTrue(mentions.isEmpty());
        assertTrue(emitter.emitted.isEmpty());
    }

    @Test
    void unresolvableMentionIsDropped() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = serviceWith(emitter, new FakeResolver(Map.of()));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mentions = (List<Map<String, Object>>) svc.createPost(
                ALICE, "Alice", null, "who is @ghost", "general", List.of(), "public").get("mentions");

        assertTrue(mentions.isEmpty());
        assertTrue(emitter.emitted.isEmpty());
    }

    @Test
    void duplicateHandleNotifiesOnce() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = serviceWith(emitter, new FakeResolver(Map.of("bob", BOB)));

        svc.createPost(ALICE, "Alice", null, "@bob @Bob @bob", "general", List.of(), "public");

        assertEquals(1, emitter.emitted.size());
    }

    @Test
    void commentMentionTargetsThePost() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final SocialService svc = serviceWith(emitter, new FakeResolver(Map.of("carol", "carol-uid")));
        final String postId = (String) svc.createPost(ALICE, "Alice", null, "hi",
                "general", List.of(), "public").get("id");

        svc.addComment(BOB, "Bob", null, postId, "cc @carol");

        // post_comment (to ALICE) + mention (to carol)
        assertEquals(2, emitter.emitted.size());
        var mention = emitter.emitted.stream().filter(e -> e.type.equals("mention")).findFirst().orElseThrow();
        assertEquals("carol-uid", mention.recipientUid);
        assertEquals("post", mention.targetType);
        assertEquals(postId, mention.targetId);
    }

    @Test
    void mentionsAreCappedAtTen() {
        final RecordingNotificationEmitter emitter = new RecordingNotificationEmitter();
        final java.util.Map<String, String> handles = new java.util.HashMap<>();
        final StringBuilder content = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            handles.put("user_" + i, "uid_" + i);
            content.append("@user_").append(i).append(' ');
        }
        final SocialService svc = serviceWith(emitter, new FakeResolver(handles));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mentions = (List<Map<String, Object>>) svc.createPost(
                ALICE, "Alice", null, content.toString(), "general", List.of(), "public").get("mentions");

        assertEquals(SocialService.MAX_MENTIONS, mentions.size());
        assertEquals(SocialService.MAX_MENTIONS, emitter.emitted.size());
    }
}
