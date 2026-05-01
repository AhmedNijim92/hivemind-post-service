# Post Service

Content service for the HiveMind platform. Manages posts, comments, and likes with feed generation and caching.

## Details

| Property | Value |
|----------|-------|
| **Port** | `8084` |
| **Database** | Cassandra |
| **Cache** | Redis |
| **Messaging** | Kafka |
| **Role** | Posts + Comments + Likes |

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/*.jar

# Docker
docker build -t hivemind/post-service .
```

## Links

- [Main Repository](https://github.com/AhmedNijim92/hivemind-backend)
