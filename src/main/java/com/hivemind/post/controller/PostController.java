package com.hivemind.post.controller;

import com.hivemind.common.dto.ApiResponse;
import com.hivemind.post.dto.AddCommentRequest;
import com.hivemind.post.dto.CreatePostRequest;
import com.hivemind.post.dto.PostDto;
import com.hivemind.post.entity.Comment;
import com.hivemind.post.service.IPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController
{
    private final IPostService postService;

    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "Unknown") String userName,
            @Valid @RequestBody CreatePostRequest request)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(userId, userName, request));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<PostDto>> getPostsByGroup(@PathVariable UUID groupId)
    {
        return ResponseEntity.ok(postService.getPostsByGroup(groupId));
    }

    @GetMapping("/{groupId}/{postId}")
    public ResponseEntity<PostDto> getPostById(
            @PathVariable UUID groupId,
            @PathVariable UUID postId)
    {
        return ResponseEntity.ok(postService.getPostById(groupId, postId));
    }

    @PostMapping("/{groupId}/{postId}/like")
    public ResponseEntity<ApiResponse> likePost(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        postService.likePost(groupId, postId, userId);
        return ResponseEntity.ok(new ApiResponse("Post liked"));
    }

    @PostMapping("/{groupId}/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "Unknown") String userName,
            @Valid @RequestBody AddCommentRequest request)
    {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.addComment(groupId, postId, userId, userName, request));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable UUID postId)
    {
        return ResponseEntity.ok(postService.getComments(postId));
    }
}
