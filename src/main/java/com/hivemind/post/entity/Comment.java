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
@Table("comments")
public class Comment
{
    @PrimaryKeyColumn(name = "post_id", type = PrimaryKeyType.PARTITIONED)
    private UUID postId;

    @PrimaryKeyColumn(name = "comment_id", type = PrimaryKeyType.CLUSTERED, ordering = org.springframework.data.cassandra.core.cql.Ordering.DESCENDING)
    private UUID commentId;

    @Column("author_id")
    private UUID authorId;

    @Column("author_name")
    private String authorName;

    @Column("content")
    private String content;

    @Column("created_at")
    private LocalDateTime createdAt;
}
