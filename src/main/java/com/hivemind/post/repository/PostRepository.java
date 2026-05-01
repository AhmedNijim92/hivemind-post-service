package com.hivemind.post.repository;

import com.hivemind.post.entity.Post;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends CassandraRepository<Post, Object>
{
    // group_id is the partition key — no ALLOW FILTERING needed
    @Query("SELECT * FROM posts WHERE group_id = ?0")
    List<Post> findByGroupId(UUID groupId);

    @Query("SELECT * FROM posts WHERE group_id = ?0 AND post_id = ?1")
    Optional<Post> findByGroupIdAndPostId(UUID groupId, UUID postId);
}
