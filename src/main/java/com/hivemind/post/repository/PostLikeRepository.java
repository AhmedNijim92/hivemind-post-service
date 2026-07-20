package com.hivemind.post.repository;

import com.hivemind.post.entity.PostLike;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends CassandraRepository<PostLike, UUID>
{
    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);
}
