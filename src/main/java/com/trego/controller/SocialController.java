package com.trego.controller;

import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Social feed endpoints. Responses use flat top-level keys ({@code success},
 * {@code posts}/{@code post}/{@code comments}, ...) rather than the generic
 * {@code ApiResponse} envelope, because the Flutter {@code SocialService} reads
 * those keys directly off {@code response.data}.
 */
@RestController
@RequestMapping("/social")
public class SocialController {

    private static final Logger logger = LoggerFactory.getLogger(SocialController.class);

    @Autowired
    private SocialService service;

    @GetMapping("/posts/feed")
    public ResponseEntity<Map<String, Object>> feed(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            List<Map<String, Object>> posts = service.getFeed(principal.getFirebaseUid(), limit, offset);
            Map<String, Object> body = ok();
            body.put("posts", posts);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to load feed", e);
        }
    }

    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String content = str(req.get("content"));
            if (content == null || content.trim().isEmpty()) {
                return badRequest("Content is required");
            }
            Map<String, Object> post = service.createPost(
                    principal.getFirebaseUid(),
                    authorName(principal),
                    null,
                    content,
                    str(req.get("type")),
                    stringList(req.get("attachments")),
                    str(req.get("visibility")));
            Map<String, Object> body = ok();
            body.put("post", post);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to create post", e);
        }
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<Map<String, Object>> like(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            Map<String, Object> result = service.toggleLike(principal.getFirebaseUid(), id);
            Map<String, Object> body = ok();
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to like post", e);
        }
    }

    @PostMapping("/posts/{id}/comment")
    public ResponseEntity<Map<String, Object>> comment(
            @PathVariable String id,
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String content = str(req.get("content"));
            if (content == null || content.trim().isEmpty()) {
                return badRequest("Comment content is required");
            }
            Map<String, Object> result = service.addComment(
                    principal.getFirebaseUid(), authorName(principal), null, id, content);
            Map<String, Object> body = ok();
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to add comment", e);
        }
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<Map<String, Object>> comments(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            List<Map<String, Object>> comments = service.getComments(id);
            Map<String, Object> body = ok();
            body.put("comments", comments);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to load comments", e);
        }
    }

    @PostMapping("/posts/{id}/report")
    public ResponseEntity<Map<String, Object>> report(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String reason = req != null ? str(req.get("reason")) : null;
            service.report(principal.getFirebaseUid(), id, reason);
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            return fail("Failed to report post", e);
        }
    }

    @PatchMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String content = str(req.get("content"));
            if (content == null || content.trim().isEmpty()) {
                return badRequest("Content is required");
            }
            Map<String, Object> post = service.updatePost(principal.getFirebaseUid(), id, content);
            Map<String, Object> body = ok();
            body.put("post", post);
            return ResponseEntity.ok(body);
        } catch (NoSuchElementException e) {
            return notFound("Post not found");
        } catch (SecurityException e) {
            return forbidden("You can only edit your own posts");
        } catch (Exception e) {
            return fail("Failed to update post", e);
        }
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.deletePost(principal.getFirebaseUid(), id);
            return ResponseEntity.ok(ok());
        } catch (NoSuchElementException e) {
            return notFound("Post not found");
        } catch (SecurityException e) {
            return forbidden("You can only delete your own posts");
        } catch (Exception e) {
            return fail("Failed to delete post", e);
        }
    }

    // --- helpers ---

    private static boolean unauthenticated(FirebaseUserPrincipal principal) {
        return principal == null || principal.getUser() == null;
    }

    private static String authorName(FirebaseUserPrincipal principal) {
        String full = principal.getUser().getFullName();
        if (full != null && !full.trim().isEmpty() && !full.trim().equals(",")) {
            return full.trim();
        }
        return principal.getUser().getEmail();
    }

    private static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        return m;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object o) {
        if (o instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object item : (List<Object>) o) {
                if (item != null) out.add(item.toString());
            }
            return out;
        }
        return new ArrayList<>();
    }

    private static ResponseEntity<Map<String, Object>> unauthorized() {
        return status(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return status(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseEntity<Map<String, Object>> notFound(String message) {
        return status(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseEntity<Map<String, Object>> forbidden(String message) {
        return status(HttpStatus.FORBIDDEN, message);
    }

    private ResponseEntity<Map<String, Object>> fail(String message, Exception e) {
        logger.error("{}: {}", message, e.getMessage());
        return status(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseEntity<Map<String, Object>> status(HttpStatus code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(code).body(body);
    }
}
