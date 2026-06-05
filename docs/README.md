# Post Service

> HiveMind Post & Comment Microservice

## Overview

The post-service handles creating posts within groups, liking posts, and managing comments. Posts are partitioned by group and ordered by time (newest first). Post creation events are published to Kafka for notifications.

## Service Info

| Property | Value |
|----------|-------|
| Port | 8084 |
| Service Name | `post-service` |
| Database | Apache Cassandra + Redis |
| Keyspace | `post_keyspace` |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2023.0.3 |
| Java | 17 |

## Architecture

```
Client (via Gateway)
  │
  ▼
PostController
  │
  ├── IPostService (createPost, getPostById, getPostsByGroup, likePost, addComment, getComments)
  │       ├── PostRepository (Cassandra)
  │       └── CommentRepository (Cassandra)
  │
  └── Kafka Producer → post-created-topic
```

## API Endpoints

Base path: `/api/v1/posts`
All endpoints require JWT (X-User-Id and X-User-Name headers injected by gateway).

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create a post in a group |
| GET | `/group/{groupId}` | Get all posts in a group |
| GET | `/{groupId}/{postId}` | Get a specific post |
| POST | `/{groupId}/{postId}/like` | Like a post |
| POST | `/{groupId}/{postId}/comments` | Add a comment |
| GET | `/{postId}/comments` | Get comments for a post |

### Request/Response Examples

#### POST /api/v1/posts
```json
// Request
{
  "groupId": "uuid",
  "content": "Hello everyone! This is my first post.",
  "mediaUrl": "https://media.hivemind.app/image.jpg"
}

// Response (201)
{
  "postId": "uuid",
  "groupId": "uuid",
  "authorId": "uuid",
  "authorName": "Ahmed",
  "content": "Hello everyone! This is my first post.",
  "mediaUrl": "https://media.hivemind.app/image.jpg",
  "likeCount": 0,
  "commentCount": 0,
  "createdAt": "2025-06-04T10:30:00"
}
```

#### POST /api/v1/posts/{groupId}/{postId}/comments
```json
// Request
{ "content": "Great post!" }

// Response (201)
{
  "postId": "uuid",
  "commentId": "uuid",
  "authorId": "uuid",
  "authorName": "Ahmed",
  "content": "Great post!",
  "createdAt": "2025-06-04T10:35:00"
}
```

## Data Model

### Post (Cassandra table: `posts`)

| Column | Type | Key Type | Description |
|--------|------|----------|-------------|
| group_id | UUID | PARTITION | Group the post belongs to |
| post_id | UUID | CLUSTERED (DESC) | Post identifier (newest first) |
| author_id | UUID | — | Post author |
| author_name | String | — | Author display name |
| content | String | — | Post text content |
| media_url | String | — | Attached media URL |
| like_count | int | — | Number of likes |
| comment_count | int | — | Number of comments |
| created_at | LocalDateTime | — | Creation timestamp |

### Comment (Cassandra table: `comments`)

| Column | Type | Key Type | Description |
|--------|------|----------|-------------|
| post_id | UUID | PARTITION | Parent post |
| comment_id | UUID | CLUSTERED (DESC) | Comment identifier (newest first) |
| author_id | UUID | — | Comment author |
| author_name | String | — | Author display name |
| content | String | — | Comment text |
| created_at | LocalDateTime | — | Creation timestamp |

## Kafka Events

### Produces: `post-created-topic`

Published when a new post is created:

```json
{
  "postId": "uuid",
  "groupId": "uuid",
  "authorId": "uuid",
  "content": "Hello everyone!",
  "timestamp": "2025-06-04T10:30:00"
}
```

**Consumers:**
- `notification-service` — generates notifications for group members

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| CASSANDRA_HOST | localhost | Cassandra contact point |
| CASSANDRA_PORT | 9042 | Cassandra port |
| CASSANDRA_DATACENTER | datacenter1 | Cassandra datacenter |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka brokers |
| REDIS_HOST | localhost | Redis host |
| REDIS_PORT | 6379 | Redis port |
| EUREKA_SERVER | http://localhost:8761/eureka | Eureka URL |

## Dependencies

- spring-boot-starter-web
- spring-boot-starter-data-cassandra
- spring-boot-starter-data-redis
- spring-boot-starter-cache
- spring-boot-starter-validation
- spring-boot-starter-actuator
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-config
- spring-kafka
- hivemind-common (1.0.0)
- lombok

## Running Locally

```bash
# Prerequisites: Cassandra, Kafka, Redis running
cd microservices/post-service
mvn spring-boot:run
```

Auto-creates `post_keyspace`, `posts`, and `comments` tables on startup.

## Known Issues

1. Redis caching temporarily disabled — same `LocalDate` serialization issue as user-service
2. No unlike endpoint — only `likePost` exists. Frontend prevents double-like client-side.
3. `like_count` and `comment_count` are incremented in-place (not atomic counters — could drift under high concurrency)
