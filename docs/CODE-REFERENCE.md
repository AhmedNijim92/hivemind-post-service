# Post Service — Code-Level Reference

## PostServiceApplication

**Package:** `com.hivemind.post`

**Annotations:**
- `@SpringBootApplication` — Enables auto-configuration, component scanning, and configuration properties
- `@EnableDiscoveryClient` — Registers with Eureka service registry
- `@EnableCaching` — Enables Spring's annotation-driven cache management (backed by Redis)
- `@EnableKafka` — Enables Kafka producer annotations

**Design Pattern:** Application Entry Point (Spring Boot convention)

### Methods

#### `main(String[] args)`
- **Signature:** `public static void main(String[] args)`
- **Logic:** `SpringApplication.run(PostServiceApplication.class, args)`
- **Returns:** void

---

## CassandraConfig

**Package:** `com.hivemind.post.config`

**Extends:** `AbstractCassandraConfiguration`

**Annotations:**
- `@Configuration`

**Design Pattern:** Template Method — overrides hook methods from abstract parent

### Overridden Methods

#### `getKeyspaceName()`
- **Returns:** `"post_keyspace"`

#### `getContactPoints()`
- **Returns:** Cassandra contact points (from configuration or default `"localhost"`)

#### `getPort()`
- **Returns:** Cassandra port (default `9042`)

#### `getLocalDataCenter()`
- **Returns:** `"datacenter1"`

#### `getSchemaAction()`
- **Returns:** `SchemaAction.CREATE_IF_NOT_EXISTS`

#### `getEntityBasePackages()`
- **Returns:** `new String[] { "com.hivemind.post.entity" }`

#### `getKeyspaceCreations()`
- **Logic:** Creates keyspace with SimpleStrategy, replication factor = 1, DURABLE_WRITES = true
- **Returns:** `List<CreateKeyspaceSpecification>`

---

## CacheConfig

**Package:** `com.hivemind.post.config`

**Annotations:**
- `@Configuration`
- `@EnableCaching`

**Design Pattern:** Factory Method — creates configured cache manager with short TTL for post data

### Beans

#### `cacheManager(RedisConnectionFactory connectionFactory)`
- **Signature:** `@Bean public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)`
- **Logic:**
  1. Creates `RedisCacheConfiguration.defaultCacheConfig()`
  2. Sets entry TTL to 5 minutes (`Duration.ofMinutes(5)`) — shorter than user-service due to higher update frequency
  3. Configures value serialization with `GenericJackson2JsonRedisSerializer`
  4. Builds `RedisCacheManager` with the connection factory and config
- **Returns:** `RedisCacheManager`

---

## KafkaProducerConfig

**Package:** `com.hivemind.post.config`

**Annotations:**
- `@Configuration`

**Design Pattern:** Factory Method — creates configured Kafka producer components

### Beans

#### `producerFactory()`
- **Signature:** `@Bean public ProducerFactory<String, PostCreatedEvent> producerFactory()`
- **Logic:** Configures producer with:
  - `bootstrap.servers` from application properties
  - Key serializer: `StringSerializer`
  - Value serializer: `JsonSerializer` (for PostCreatedEvent)
- **Returns:** `DefaultKafkaProducerFactory<String, PostCreatedEvent>`

#### `kafkaTemplate()`
- **Signature:** `@Bean public KafkaTemplate<String, PostCreatedEvent> kafkaTemplate()`
- **Logic:** Wraps the `producerFactory()` in a `KafkaTemplate`
- **Returns:** `KafkaTemplate<String, PostCreatedEvent>`

---

## PostController

**Package:** `com.hivemind.post.controller`

**Annotations:**
- `@RestController`
- `@RequestMapping("/api/v1/posts")`

**Design Pattern:** Façade — exposes simplified REST API over service layer

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| postService | IPostService |

### Endpoints

#### `POST /`
- **Signature:** `public ResponseEntity<PostDto> createPost(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Name") String userName, @Valid @RequestBody CreatePostRequest request)`
- **Logic:** Delegates to `postService.createPost(userId, userName, request)`
- **Returns:** `201 Created` with `PostDto`
- **Headers:** `X-User-Id` (user UUID), `X-User-Name` (display name) — injected by API Gateway

#### `GET /group/{groupId}`
- **Signature:** `public ResponseEntity<List<PostDto>> getPostsByGroup(@PathVariable UUID groupId)`
- **Logic:** Delegates to `postService.getPostsByGroup(groupId)`
- **Returns:** `List<PostDto>` — all posts in the group, ordered by creation time DESC

#### `GET /{groupId}/{postId}`
- **Signature:** `public ResponseEntity<PostDto> getPostById(@PathVariable UUID groupId, @PathVariable UUID postId)`
- **Logic:** Delegates to `postService.getPostById(groupId, postId)`
- **Returns:** `PostDto`

#### `POST /{groupId}/{postId}/like`
- **Signature:** `public ResponseEntity<ApiResponse> likePost(@PathVariable UUID groupId, @PathVariable UUID postId, @RequestHeader("X-User-Id") UUID userId)`
- **Logic:** Delegates to `postService.likePost(groupId, postId, userId)`
- **Returns:** `ApiResponse` with success message

#### `POST /{groupId}/{postId}/comments`
- **Signature:** `public ResponseEntity<Comment> addComment(@PathVariable UUID groupId, @PathVariable UUID postId, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Name") String userName, @Valid @RequestBody AddCommentRequest request)`
- **Logic:** Delegates to `postService.addComment(groupId, postId, userId, userName, request)`
- **Returns:** `201 Created` with `Comment` entity

#### `GET /{postId}/comments`
- **Signature:** `public ResponseEntity<List<Comment>> getComments(@PathVariable UUID postId)`
- **Logic:** Delegates to `postService.getComments(postId)`
- **Returns:** `List<Comment>` — all comments on the post, ordered by creation time DESC

---

## Post (Entity)

**Package:** `com.hivemind.post.entity`

**Annotations:**
- `@Table("posts")` — Maps to Cassandra `posts` table

**Design Pattern:** Composite Key — partitioned by group for efficient group-level queries

### Fields

| Field | Type | Key Type | Description |
|-------|------|----------|-------------|
| groupId | UUID | `PARTITIONED` | Group this post belongs to |
| postId | UUID | `CLUSTERED` (DESC) | Unique post identifier, ordered descending for newest-first |
| authorId | UUID | | User who created the post |
| authorName | String | | Author's display name (denormalized) |
| content | String | | Post text content |
| mediaUrl | String | | Optional media attachment URL |
| likeCount | int | | Number of likes |
| commentCount | int | | Number of comments |
| createdAt | LocalDateTime | | Post creation timestamp |

**Note:** `postId` is clustered in DESC order to enable efficient "newest first" queries within a group partition.

---

## Comment (Entity)

**Package:** `com.hivemind.post.entity`

**Annotations:**
- `@Table("comments")` — Maps to Cassandra `comments` table

**Design Pattern:** Composite Key — partitioned by post for efficient comment retrieval

### Fields

| Field | Type | Key Type | Description |
|-------|------|----------|-------------|
| postId | UUID | `PARTITIONED` | Post this comment belongs to |
| commentId | UUID | `CLUSTERED` (DESC) | Unique comment identifier, ordered descending |
| authorId | UUID | | User who wrote the comment |
| authorName | String | | Author's display name (denormalized) |
| content | String | | Comment text content |
| createdAt | LocalDateTime | | Comment creation timestamp |

---

## PostRepository

**Package:** `com.hivemind.post.repository`

**Extends:** `CassandraRepository<Post, Object>`

**Design Pattern:** Repository pattern

### Methods

#### `findByGroupId(UUID groupId)`
- **Signature:** `@Query List<Post> findByGroupId(UUID groupId)`
- **Logic:** Fetches all posts in a group partition (returned in clustered order — newest first)
- **Returns:** `List<Post>`

#### `findByGroupIdAndPostId(UUID groupId, UUID postId)`
- **Signature:** `@Query Optional<Post> findByGroupIdAndPostId(UUID groupId, UUID postId)`
- **Logic:** Looks up a specific post by composite key
- **Returns:** `Optional<Post>`

---

## CommentRepository

**Package:** `com.hivemind.post.repository`

**Extends:** `CassandraRepository<Comment, Object>`

**Design Pattern:** Repository pattern

### Methods

#### `findByPostId(UUID postId)`
- **Signature:** `@Query List<Comment> findByPostId(UUID postId)`
- **Logic:** Fetches all comments for a post (returned in clustered order — newest first)
- **Returns:** `List<Comment>`

---

## IPostService (Interface)

**Package:** `com.hivemind.post.service`

### Method Signatures

| Method | Parameters | Returns |
|--------|-----------|---------|
| `createPost` | `UUID userId, String userName, CreatePostRequest request` | `PostDto` |
| `getPostById` | `UUID groupId, UUID postId` | `PostDto` |
| `getPostsByGroup` | `UUID groupId` | `List<PostDto>` |
| `likePost` | `UUID groupId, UUID postId, UUID userId` | `void` |
| `addComment` | `UUID groupId, UUID postId, UUID userId, String userName, AddCommentRequest request` | `Comment` |
| `getComments` | `UUID postId` | `List<Comment>` |

---

## PostServiceImpl

**Package:** `com.hivemind.post.service.impl`

**Annotations:**
- `@Service`

**Implements:** `IPostService`

**Design Pattern:** Service Layer — encapsulates post and comment business logic

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| postRepository | PostRepository |
| commentRepository | CommentRepository |
| kafkaTemplate | KafkaTemplate<String, PostCreatedEvent> |

### Methods

#### `createPost(UUID userId, String userName, CreatePostRequest request)`
- **Signature:** `@Override public PostDto createPost(UUID userId, String userName, CreatePostRequest request)`
- **Logic:**
  1. Builds `Post` entity:
     - `groupId` = request.getGroupId()
     - `postId` = UUID.randomUUID()
     - `authorId` = userId
     - `authorName` = userName
     - `content` = request.getContent()
     - `mediaUrl` = request.getMediaUrl() (optional)
     - `likeCount` = 0
     - `commentCount` = 0
     - `createdAt` = LocalDateTime.now()
  2. Saves post via `postRepository.save(post)`
  3. Publishes `PostCreatedEvent` to Kafka topic `"post-created-topic"` (contains postId, groupId, authorId, authorName, content)
  4. Maps to PostDto and returns
- **Returns:** `PostDto`

#### `getPostById(UUID groupId, UUID postId)`
- **Signature:** `@Override public PostDto getPostById(UUID groupId, UUID postId)`
- **Logic:**
  1. Calls `postRepository.findByGroupIdAndPostId(groupId, postId)`
  2. If not found → throws RuntimeException ("Post not found")
  3. Maps to PostDto
- **Returns:** `PostDto`
- **Exceptions:** RuntimeException if post not found

#### `getPostsByGroup(UUID groupId)`
- **Signature:** `@Override public List<PostDto> getPostsByGroup(UUID groupId)`
- **Logic:**
  1. Calls `postRepository.findByGroupId(groupId)`
  2. Maps each Post to PostDto
- **Returns:** `List<PostDto>` — ordered newest first (by Cassandra clustering)

#### `likePost(UUID groupId, UUID postId, UUID userId)`
- **Signature:** `@Override public void likePost(UUID groupId, UUID postId, UUID userId)`
- **Logic:**
  1. Loads post via `postRepository.findByGroupIdAndPostId(groupId, postId)`
  2. If not found → throws exception
  3. Increments `likeCount` by 1
  4. Saves updated post
- **Returns:** void
- **Note:** Does not track which user liked — allows duplicate likes (simplified implementation)

#### `addComment(UUID groupId, UUID postId, UUID userId, String userName, AddCommentRequest request)`
- **Signature:** `@Override public Comment addComment(UUID groupId, UUID postId, UUID userId, String userName, AddCommentRequest request)`
- **Logic:**
  1. Loads post via `postRepository.findByGroupIdAndPostId(groupId, postId)`
  2. If not found → throws exception
  3. Builds `Comment` entity:
     - `postId` = postId
     - `commentId` = UUID.randomUUID()
     - `authorId` = userId
     - `authorName` = userName
     - `content` = request.getContent()
     - `createdAt` = LocalDateTime.now()
  4. Saves comment via `commentRepository.save(comment)`
  5. Increments post's `commentCount` by 1
  6. Saves updated post
- **Returns:** `Comment` entity

#### `getComments(UUID postId)`
- **Signature:** `@Override public List<Comment> getComments(UUID postId)`
- **Logic:** Calls `commentRepository.findByPostId(postId)`
- **Returns:** `List<Comment>` — ordered newest first (by Cassandra clustering)

---

## DTOs

**Package:** `com.hivemind.post.dto`

### CreatePostRequest

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| groupId | UUID | `@NotNull` | Target group for the post |
| content | String | `@NotBlank` | Post text content |
| mediaUrl | String | Optional | Attached media URL |

### AddCommentRequest

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| content | String | `@NotBlank` | Comment text content |

### PostDto

| Field | Type | Description |
|-------|------|-------------|
| postId | UUID | Unique post identifier |
| groupId | UUID | Group the post belongs to |
| authorId | UUID | Author's user ID |
| authorName | String | Author's display name |
| content | String | Post text content |
| mediaUrl | String | Media attachment URL |
| likeCount | int | Number of likes |
| commentCount | int | Number of comments |
| createdAt | LocalDateTime | Post creation timestamp |

### PostCreatedEvent (Kafka Event — produced)

| Field | Type | Description |
|-------|------|-------------|
| postId | UUID | New post's ID |
| groupId | UUID | Group ID |
| authorId | UUID | Author's user ID |
| authorName | String | Author's display name |
| content | String | Post content |

### ApiResponse

| Field | Type | Description |
|-------|------|-------------|
| message | String | Success/error message |
| success | boolean | Operation result |
