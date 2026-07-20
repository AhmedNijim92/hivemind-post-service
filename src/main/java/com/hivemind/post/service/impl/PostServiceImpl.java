package com.hivemind.post.service.impl;

import com.hivemind.common.event.PostCreatedEvent;
import com.hivemind.post.dto.AddCommentRequest;
import com.hivemind.post.dto.CreatePostRequest;
import com.hivemind.post.dto.PostDto;
import com.hivemind.post.entity.Comment;
import com.hivemind.post.entity.Post;
import com.hivemind.post.entity.PostLike;
import com.hivemind.post.repository.CommentRepository;
import com.hivemind.post.repository.PostLikeRepository;
import com.hivemind.post.repository.PostRepository;
import com.hivemind.post.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements IPostService
{
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;

    @Override
    public PostDto createPost(UUID authorId, String authorName, CreatePostRequest request)
    {
        Post post = Post.builder()
                .groupId(request.getGroupId())
                .postId(UUID.randomUUID())
                .authorId(authorId)
                .authorName(authorName)
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        postRepository.save(post);

        PostCreatedEvent event = PostCreatedEvent.builder()
                .postId(post.getPostId())
                .groupId(post.getGroupId())
                .authorId(authorId)
                .content(post.getContent())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send("post-created-topic", event);

        log.info("Post created: {} in group: {}", post.getPostId(), post.getGroupId());
        return toDto(post);
    }

    @Override
    public PostDto getPostById(UUID groupId, UUID postId)
    {
        Post post = postRepository.findByGroupIdAndPostId(groupId, postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        return toDto(post);
    }

    @Override
    public List<PostDto> getPostsByGroup(UUID groupId)
    {
        return postRepository.findByGroupId(groupId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostDto> getPostsByGroups(List<UUID> groupIds)
    {
        // Fetch posts from each group partition and merge, sorted by createdAt descending
        return groupIds.stream()
                .flatMap(groupId -> postRepository.findByGroupId(groupId).stream())
                .map(this::toDto)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public void likePost(UUID groupId, UUID postId, UUID userId)
    {
        Post post = postRepository.findByGroupIdAndPostId(groupId, postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        // Check if already liked
        if (postLikeRepository.findByPostIdAndUserId(postId, userId).isPresent())
        {
            throw new RuntimeException("Already liked this post");
        }

        // Record the like
        PostLike like = PostLike.builder()
                .postId(postId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        postLikeRepository.save(like);

        // Increment counter
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
        log.info("Post {} liked by user {}", postId, userId);
    }

    @Override
    public Comment addComment(UUID groupId, UUID postId, UUID authorId, String authorName, AddCommentRequest request)
    {
        Post post = postRepository.findByGroupIdAndPostId(groupId, postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        Comment comment = Comment.builder()
                .postId(postId)
                .commentId(UUID.randomUUID())
                .authorId(authorId)
                .authorName(authorName)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return comment;
    }

    @Override
    public List<Comment> getComments(UUID postId)
    {
        return commentRepository.findByPostId(postId);
    }

    private PostDto toDto(Post post)
    {
        return PostDto.builder()
                .postId(post.getPostId())
                .groupId(post.getGroupId())
                .authorId(post.getAuthorId())
                .authorName(post.getAuthorName())
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
