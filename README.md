# Bloom Filter Application

A high-performance Spring Boot application demonstrating the use of Bloom filters for efficient username existence checks. This application optimizes database queries by using a probabilistic data structure (Bloom filter) to quickly determine if a username might exist, reducing unnecessary database lookups.

## Features

- **Efficient Username Lookups**: Uses Bloom filters to determine if a username exists without always querying the database
- **Redis Integration**: Persists the Bloom filter in Redis for sharing across application instances
- **Batch Processing**: Efficiently loads large datasets using batch processing
- **Configurable Parameters**: Customizable Bloom filter size, false positive rate, and batch size
- **Optimized Performance**: Designed to handle millions of users with minimal memory footprint
- **REST API**: Simple REST endpoints for checking username existence

## Technologies Used

- **Java 24**: Latest Java version for optimal performance
- **Spring Boot 3.5.3**: Modern Spring Boot framework
- **Spring Data JPA**: For database access
- **Spring Data Redis**: For Redis integration
- **PostgreSQL**: Primary database for user data
- **Redis**: For Bloom filter persistence
- **Lombok**: For reducing boilerplate code
- **SLF4J**: For logging

## Architecture Overview

The application follows a standard Spring Boot architecture with the following components:

### Core Components

1. **Bloom Filter Implementation**:
   - `BloomFilter.java`: Base implementation of the Bloom filter algorithm
   - `PersistableBloomFilter.java`: Extension that supports serialization for Redis persistence

2. **Services**:
   - `UserNameBloomFilterService`: Manages the Bloom filter, including initialization, persistence, and lookups
   - `AuthService`: Provides authentication-related operations using the Bloom filter
   - `UserGenerationService`: Generates sample user data for testing

3. **Controllers**:
   - `AuthController`: Exposes REST endpoints for username existence checks

4. **Configuration**:
   - `BloomFilterConfig`: Calculates optimal Bloom filter parameters
   - `RedisConfig`: Configures Redis connection and serialization

5. **Entities**:
   - `User`: Represents user data
   - `Profile`: Represents user profile data

### How It Works

1. On application startup, the Bloom filter is initialized:
   - If Redis is enabled and a persisted filter exists, it's loaded from Redis
   - Otherwise, it's built from the database using batch processing

2. When checking if a username exists:
   - First, the Bloom filter is checked
   - If the filter indicates the username definitely doesn't exist, return false
   - If the filter indicates the username might exist, verify with the database

3. When a new user is created:
   - The username is added to the Bloom filter
   - Optionally, the updated filter is saved to Redis

## Setup and Installation

### Prerequisites

- Java 24 or later
- Maven 3.6 or later
- PostgreSQL 12 or later
- Redis 6 or later (optional, can be disabled)

### Installation Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/bloom-filter.git
   cd bloom-filter
   ```

2. **Configure the database**:
   - Create a PostgreSQL database named `bloom-filter-db`
   - Update database credentials in `application.properties` if needed

3. **Configure Redis** (optional):
   - Ensure Redis is running on localhost:6379 or update the configuration
   - Set `bloom.filter.redis.enabled=false` if you don't want to use Redis

4. **Build the application**:
   ```bash
   mvn clean install
   ```

5. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

## Configuration Options

The application can be configured through `application.properties`:

### Database Configuration
```properties
spring.datasource.username=postgres
spring.datasource.password=Abcd@1234
spring.datasource.url=jdbc:postgresql://localhost:5432/bloom-filter-db
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Bloom Filter Configuration
```properties
# Expected number of insertions (users) in the Bloom filter
bloom.filter.expected.insertions=10000000

# Desired false positive rate (lower values = larger filter but fewer false positives)
bloom.filter.false.positive.rate=0.01

# Batch size for loading users from the database
bloom.filter.batch.size=10000
```

### Redis Configuration
```properties
# Enable/disable Redis for Bloom filter persistence
bloom.filter.redis.enabled=true

# Redis connection URL
spring.data.redis.url=redis://localhost:6379

# Redis key for storing the Bloom filter
bloom.filter.redis.key=bloom:filter:usernames

# Whether to rebuild the filter on startup even if a persisted version exists
bloom.filter.force.rebuild=false

# Whether to save the filter to Redis after each username addition
bloom.filter.save.on.add=false
```

## API Endpoints

### Check Username Existence
```
GET /users/exists?userName={username}
```
Returns `true` if the username exists, `false` otherwise.

## Usage Examples

### Checking if a Username Exists

```
// Using the AuthService
boolean exists = authService.userNameExists("testuser");

// Using the REST API
// GET /users/exists?userName=testuser
```

### Adding a New Username to the Bloom Filter

```
// When creating a new user
User user = new User();
user.setUserName("newuser");
// ... set other properties
userRepository.save(user);

// Add to Bloom filter
bloomFilterService.addUserName(user.getUserName());
```

## Performance Considerations

### Memory Usage
The Bloom filter's memory usage depends on the expected number of insertions and the desired false positive rate. With the default configuration (10 million users, 1% false positive rate), the filter uses approximately 120MB of memory.

### False Positives
Bloom filters can produce false positives (indicating a username might exist when it doesn't), but never false negatives. The false positive rate is configurable through the `bloom.filter.false.positive.rate` property.

### Redis Persistence
Enabling Redis persistence allows the Bloom filter to be shared across multiple application instances and persisted across restarts. However, it adds some overhead for serialization and network communication.

### Batch Processing
The application uses batch processing to efficiently load large datasets from the database. The batch size is configurable through the `bloom.filter.batch.size` property.