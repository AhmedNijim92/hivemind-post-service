package com.hivemind.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("posts")
public class Post
{
    @PrimaryKeyColumn(name = "group_id", type = PrimaryKeyType.PARTITIONED)
    private UUID groupId;

    @PrimaryKeyColumn(name = "post_id", type = PrimaryKeyType.CLUSTERED, ordering = org.springframework.data.cassandra.core.cql.Ordering.DESCENDING)
    private UUID postId;

    @Column("author_id")
    private UUID authorId;

    @Column("author_name")
    private String authorName;

    @Column("content")
    private String content;

    @Column("media_url")
    private String mediaUrl;

    @Column("like_count")
    private int likeCount;

    @Column("comment_count")
    private int commentCount;

    @Column("created_at")
    private LocalDateTime createdAt;
}
