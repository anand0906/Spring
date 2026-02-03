# Caching - Concepts

## Table of Contents
- [What is Caching?](#what-is-caching)
- [First-Level Cache (Hibernate)](#first-level-cache-hibernate)
- [Second-Level Cache](#second-level-cache)
- [Spring Cache Abstraction (@Cacheable)](#spring-cache-abstraction-cacheable)
- [Cache Eviction](#cache-eviction)
- [Cache Providers (Redis / Ehcache)](#cache-providers-redis--ehcache)
- [Best Practices](#best-practices)
- [Quick Reference](#quick-reference)

---

## What is Caching?

### Concept
**Caching** stores frequently accessed data in memory to avoid repeated database queries.

### Why Cache?
- **Performance** - Memory access is 100x faster than database
- **Reduced Load** - Less database queries
- **Scalability** - Handle more users with same resources

### Types of Cache in JPA/Spring

| Cache Type | Scope | Managed By | Lifespan |
|------------|-------|------------|----------|
| **First-Level** | Session/Transaction | Hibernate (automatic) | Single transaction |
| **Second-Level** | Application | Hibernate (configurable) | Application lifetime |
| **Spring Cache** | Application | Spring (annotation-based) | Application lifetime |

---

## First-Level Cache (Hibernate)

### Concept
**First-level cache** is a **session-scoped** cache. It's automatically enabled in Hibernate.

### How It Works
- Each Hibernate session has its own cache
- Entities loaded in a session are cached for that session
- Cache is cleared when session closes
- **Always enabled** - you cannot disable it

### Example Behavior
```java
@Transactional
public void example() {
    User user1 = userRepo.findById(1L);  // Query DB
    User user2 = userRepo.findById(1L);  // From cache (same session)
    // Only 1 database query executed
}

// Different transaction = different session
@Transactional
public void anotherMethod() {
    User user3 = userRepo.findById(1L);  // Query DB again (new session)
}
```

### Key Points
- **Scope:** Single transaction/session
- **Automatic:** Always on, no configuration needed
- **Lifetime:** Until transaction ends
- **Thread-safe:** No (each session is single-threaded)
- **Cannot be shared** across sessions

### When First-Level Cache Helps
- Loading same entity multiple times in one transaction
- Navigating bidirectional relationships
- Dirty checking for updates

---

## Second-Level Cache

### Concept
**Second-level cache** is an **application-scoped** cache shared across all sessions.

### How It Works
- Shared cache across entire application
- Survives session closure
- Must be explicitly configured
- Can cache entities, collections, and queries

### Levels of Caching

```
[Application]
    ↓
[Second-Level Cache] ← Shared across sessions
    ↓
[First-Level Cache]  ← Session-specific
    ↓
[Database]
```

### Configuration

#### 1. Enable in application.properties
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

#### 2. Mark Entities as Cacheable
```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    @Id
    private Long id;
    private String name;
}
```

### Cache Strategies (CacheConcurrencyStrategy)

| Strategy | Use Case | Reads | Writes |
|----------|----------|-------|--------|
| **READ_ONLY** | Never updated data | Fast | Not allowed |
| **READ_WRITE** | Read & write data | Fast | Safe |
| **NONSTRICT_READ_WRITE** | Rarely updated | Fast | Eventually consistent |
| **TRANSACTIONAL** | JTA transactions | Slow | Fully consistent |

### What Can Be Cached?

#### Entity Cache
```java
@Entity
@Cacheable
public class User {
    // User entity cached
}
```

#### Collection Cache
```java
@Entity
@Cacheable
public class Order {
    
    @OneToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<OrderItem> items;  // Collection cached
}
```

#### Query Cache
```properties
# Enable query cache
spring.jpa.properties.hibernate.cache.use_query_cache=true
```

```java
// Cache specific query results
TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
query.setHint("org.hibernate.cacheable", true);
```

### Key Points
- **Scope:** Application-wide
- **Manual:** Must be configured
- **Lifetime:** Application lifetime (until evicted)
- **Thread-safe:** Yes
- **Shared:** Across all sessions

---

## Spring Cache Abstraction (@Cacheable)

### Concept
Spring's **cache abstraction** provides annotation-based caching for **any method**, not just JPA.

### Key Annotations

#### @Cacheable - Cache Method Results
```java
@Service
public class ProductService {
    
    @Cacheable("products")
    public Product getProduct(Long id) {
        // Method called only if not in cache
        // Result is cached with key = id
        return productRepo.findById(id);
    }
}
```

**How it works:**
1. Check cache for key
2. If found → return cached value
3. If not found → execute method, cache result, return

#### @CachePut - Update Cache
```java
@CachePut(value = "products", key = "#product.id")
public Product updateProduct(Product product) {
    // Always executes method
    // Updates cache with new value
    return productRepo.save(product);
}
```

#### @CacheEvict - Remove from Cache
```java
@CacheEvict(value = "products", key = "#id")
public void deleteProduct(Long id) {
    productRepo.deleteById(id);
    // Removes item from cache
}
```

#### @Caching - Multiple Cache Operations
```java
@Caching(
    cacheable = @Cacheable("products"),
    evict = @CacheEvict(value = "productList", allEntries = true)
)
public Product getProduct(Long id) {
    return productRepo.findById(id);
}
```

### Cache Key Generation

#### Default Key (method parameters)
```java
@Cacheable("users")
public User getUser(Long id) {
    // Key = id
}

@Cacheable("users")
public List<User> getUsers(String name, Integer age) {
    // Key = combination of name and age
}
```

#### Custom Key (SpEL)
```java
@Cacheable(value = "users", key = "#user.id")
public User save(User user) {
    // Key = user.id
}

@Cacheable(value = "users", key = "#email.toLowerCase()")
public User findByEmail(String email) {
    // Key = email in lowercase
}
```

#### Conditional Caching
```java
@Cacheable(value = "products", condition = "#price > 100")
public Product findProduct(Long id, BigDecimal price) {
    // Only cache if price > 100
}

@Cacheable(value = "users", unless = "#result == null")
public User findUser(Long id) {
    // Don't cache if result is null
}
```

### Configuration

#### Enable Caching
```java
@SpringBootApplication
@EnableCaching
public class Application {
    // Caching enabled
}
```

#### Configure Cache Manager
```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products", "users");
    }
}
```

---

## Cache Eviction

### Concept
**Cache eviction** removes stale or outdated data from cache.

### Eviction Strategies

#### 1. Manual Eviction
```java
@CacheEvict(value = "products", key = "#id")
public void deleteProduct(Long id) {
    productRepo.deleteById(id);
}
```

#### 2. Evict All Entries
```java
@CacheEvict(value = "products", allEntries = true)
public void clearAllProducts() {
    // Clears entire "products" cache
}
```

#### 3. Evict Before Method Execution
```java
@CacheEvict(value = "products", beforeInvocation = true)
public void risky() {
    // Cache cleared BEFORE method runs
    // Useful if method might throw exception
}
```

#### 4. Scheduled Eviction
```java
@Scheduled(fixedRate = 3600000) // Every hour
@CacheEvict(value = "products", allEntries = true)
public void evictAllCaches() {
    // Clears cache periodically
}
```

### Time-Based Eviction (TTL)

Depends on cache provider (see Redis/Ehcache sections below).

### When to Evict?

| Scenario | Action |
|----------|--------|
| Entity updated | Evict specific entry |
| Entity deleted | Evict specific entry |
| Bulk update | Evict all entries |
| Data might be stale | Scheduled eviction |
| Application deployment | Clear all caches |

---

## Cache Providers (Redis / Ehcache)

### Concept
Spring Cache Abstraction works with different **cache providers** (implementations).

### Popular Providers

| Provider | Type | Use Case |
|----------|------|----------|
| **ConcurrentHashMap** | In-memory (local) | Development, single instance |
| **Ehcache** | In-memory (local) | Production, single instance |
| **Redis** | Distributed (remote) | Production, multiple instances |
| **Caffeine** | In-memory (local) | High performance, single instance |
| **Hazelcast** | Distributed | Production, clustering |

---

### Redis Cache

#### What is Redis?
- In-memory data store (key-value)
- Can be shared across multiple application instances
- Persistent (can save to disk)
- Supports TTL (time-to-live)

#### Setup

**1. Add Dependency**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**2. Configure Redis**
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000  # 1 hour in ms
```

**3. Use Annotations**
```java
@Cacheable("products")
public Product getProduct(Long id) {
    // Cached in Redis
}
```

#### Redis Benefits
- **Distributed** - Shared across servers
- **Persistent** - Survives application restart
- **TTL** - Automatic expiration
- **Eviction Policies** - LRU, LFU, etc.

#### Redis Configuration Example
```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))  // 1 hour TTL
            .disableCachingNullValues()     // Don't cache nulls
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
    }
}
```

---

### Ehcache

#### What is Ehcache?
- In-memory cache (local to application)
- Fast and lightweight
- Can write to disk for persistence
- Supports TTL and size limits

#### Setup

**1. Add Dependency**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
</dependency>
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
```

**2. Configure in application.properties**
```properties
spring.cache.type=jcache
spring.cache.jcache.config=classpath:ehcache.xml
```

**3. Create ehcache.xml**
```xml
<config>
    <cache alias="products">
        <expiry>
            <ttl unit="hours">1</ttl>
        </expiry>
        <heap unit="entries">1000</heap>
    </cache>
</config>
```

**4. Use Annotations**
```java
@Cacheable("products")
public Product getProduct(Long id) {
    // Cached in Ehcache
}
```

#### Ehcache Benefits
- **Fast** - In-memory, very low latency
- **Flexible** - Heap, off-heap, disk tiers
- **Size Limits** - Prevent memory issues
- **Statistics** - Built-in monitoring

---

### Choosing a Cache Provider

| Scenario | Recommended Provider |
|----------|---------------------|
| Single server application | Ehcache or Caffeine |
| Multiple server instances | Redis or Hazelcast |
| Need persistence | Redis |
| Highest performance (local) | Caffeine |
| Simple development | ConcurrentHashMap |
| Need distributed locking | Redis |

---

## Best Practices

### 1. Cache What's Expensive
```java
// ✅ Cache expensive operations
@Cacheable("reports")
public Report generateMonthlyReport() {
    // Complex calculation - good to cache
}

// ❌ Don't cache cheap operations
@Cacheable("currentTime")
public LocalDateTime now() {
    return LocalDateTime.now();  // Fast, no point caching
}
```

### 2. Use Appropriate TTL
```java
// Short-lived data
@Cacheable(value = "stock-prices")  // TTL: minutes
public BigDecimal getStockPrice(String symbol) { }

// Long-lived data
@Cacheable(value = "country-list")  // TTL: hours/days
public List<Country> getCountries() { }
```

### 3. Clear Cache on Updates
```java
@CachePut(value = "products", key = "#product.id")
public Product updateProduct(Product product) {
    return productRepo.save(product);
}

@CacheEvict(value = "products", key = "#id")
public void deleteProduct(Long id) {
    productRepo.deleteById(id);
}
```

### 4. Handle Cache Failures Gracefully
```java
// Cache failure shouldn't break application
// Spring handles this automatically - method executes if cache fails
```

### 5. Monitor Cache Performance
- Hit ratio (cache hits / total requests)
- Memory usage
- Eviction rate
- Miss penalty (time to fetch from DB)

### 6. Don't Cache Everything
**Don't cache:**
- Frequently changing data
- User-specific sensitive data (unless per-user cache)
- Large objects (memory waste)
- Data cheaper to recompute than cache

---

## Quick Reference

### Annotations
```java
@Cacheable("cacheName")           // Read from cache, populate if missing
@CachePut("cacheName")            // Always execute, update cache
@CacheEvict("cacheName")          // Remove from cache
@CacheEvict(allEntries = true)   // Clear entire cache
@Caching(...)                     // Multiple cache operations
```

### Key Expressions (SpEL)
```java
key = "#id"                       // Parameter
key = "#user.id"                  // Object property
key = "#email.toLowerCase()"      // Method call
key = "#p0"                       // First parameter (by position)
```

### Conditional
```java
condition = "#id > 0"             // Cache only if condition true
unless = "#result == null"        // Don't cache if condition true
```

### Configuration
```java
@EnableCaching                    // Enable caching
```

### Second-Level Cache Strategies
```java
CacheConcurrencyStrategy.READ_ONLY
CacheConcurrencyStrategy.READ_WRITE
CacheConcurrencyStrategy.NONSTRICT_READ_WRITE
CacheConcurrencyStrategy.TRANSACTIONAL
```

---

## Summary

### Cache Hierarchy

```
1. First-Level Cache (Hibernate)
   - Automatic
   - Session-scoped
   - Cannot disable

2. Second-Level Cache (Hibernate)
   - Manual configuration
   - Application-scoped
   - Entity/collection caching

3. Spring Cache (@Cacheable)
   - Annotation-based
   - Application-scoped
   - Method result caching
   - Works with any method (not just JPA)
```

### Key Concepts

**First-Level Cache** = Automatic session cache (always on)

**Second-Level Cache** = Shared entity cache (manual setup)

**Spring Cache** = Method-level caching (annotation-based)

**Cache Eviction** = Removing stale data from cache

**Cache Providers** = Implementation (Redis, Ehcache, etc.)

### Decision Matrix

| Question | Answer | Action |
|----------|--------|--------|
| Same entity loaded multiple times in transaction? | Yes | First-level cache (automatic) |
| Same entity loaded across transactions? | Yes | Second-level cache or @Cacheable |
| Cache method results (not entities)? | Yes | @Cacheable |
| Multiple application instances? | Yes | Use Redis |
| Single application instance? | Yes | Use Ehcache/Caffeine |
| Need cache persistence? | Yes | Use Redis |

### Remember

1. **First-level is always on** - No configuration needed
2. **Second-level needs setup** - Mark entities with @Cacheable
3. **Spring cache is flexible** - Can cache any method result
4. **Always evict on updates** - Keep cache consistent
5. **Choose right provider** - Redis for distributed, Ehcache for local
6. **Monitor performance** - Track hit ratio and memory

---

**Cache wisely, query less! 🚀**
