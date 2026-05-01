package com.hivemind.post.repository;

import com.hivemind.post.entity.Comment;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends CassandraRepository<Comment, Object>
{
    @Query("SELECT * FROM comments WHERE post_id = ?0")
    List<Comment> findByPostId(UUID postId);
}
