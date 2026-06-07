package com.hivemind.post.service;

import com.hivemind.post.dto.AddCommentRequest;
import com.hivemind.post.dto.CreatePostRequest;
import com.hivemind.post.dto.PostDto;
import com.hivemind.post.entity.Comment;

import java.util.List;
import java.util.UUID;

public interface IPostService
{
    PostDto createPost(UUID authorId, String authorName, CreatePostRequest request);

    PostDto getPostById(UUID groupId, UUID postId);

    List<PostDto> getPostsByGroup(UUID groupId);

    /** Get posts from multiple groups for feed aggregation, sorted by creation time descending */
    List<PostDto> getPostsByGroups(List<UUID> groupIds);

    void likePost(UUID groupId, UUID postId, UUID userId);

    Comment addComment(UUID groupId, UUID postId, UUID authorId, String authorName, AddCommentRequest request);

    List<Comment> getComments(UUID postId);
}
