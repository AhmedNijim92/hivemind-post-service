package com.hivemind.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto
{
    private UUID postId;
    private UUID groupId;
    private UUID authorId;
    private String authorName;
    private String content;
    private String mediaUrl;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
}
