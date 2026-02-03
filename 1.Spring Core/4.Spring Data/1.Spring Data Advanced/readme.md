# Spring Data JPA - Complete Revision Notes

## Table of Contents

### Core Topics
1. [Transactions](#1-transactions)
2. [Specifications & Criteria API](#2-specifications--criteria-api)
3. [Query by Example (QBE)](#3-query-by-example-qbe)
4. [Caching](#4-caching)
5. [Locking & Concurrency](#5-locking--concurrency)
6. [Fetch Strategies & Performance](#6-fetch-strategies--performance)
7. [Modifying Queries](#7-modifying-queries)

---

# 1. Transactions

## What is a Transaction?
A sequence of operations that execute as a **single unit** - all succeed or all fail together.

## @Transactional Usage

### Basic Usage
```java
@Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    // All operations succeed or rollback together
}
```

### Key Parameters
```java
@Transactional(
    propagation = Propagation.REQUIRED,     // Default
    isolation = Isolation.READ_COMMITTED,   // Default
    readOnly = false,                        // Default
    timeout = -1,                            // No timeout
    rollbackFor = Exception.class            // Rollback on which exceptions
)
```

## Propagation Types

| Type | Behavior |
|------|----------|
| **REQUIRED** (default) | Join existing or create new |
| **REQUIRES_NEW** | Always create new, suspend existing |
| **MANDATORY** | Must have existing, else throw exception |
| **SUPPORTS** | Use if exists, run without if not |
| **NOT_SUPPORTED** | Always run without transaction |
| **NEVER** | Must not have transaction |
| **NESTED** | Create savepoint within existing |

### Common Usage
```java
// Most common - join or create
@Transactional(propagation = Propagation.REQUIRED)

// Independent operation (logging, notifications)
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

## Isolation Levels

| Level | Dirty Read | Non-Repeatable | Phantom | Use Case |
|-------|-----------|----------------|---------|----------|
| **READ_UNCOMMITTED** | ❌ | ❌ | ❌ | Approximate reports |
| **READ_COMMITTED** | ✅ | ❌ | ❌ | Default, most cases |
| **REPEATABLE_READ** | ✅ | ✅ | ❌ | Financial calculations |
| **SERIALIZABLE** | ✅ | ✅ | ✅ | Critical operations |

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public BigDecimal calculateTotal(Long orderId) {
    // Price won't change during calculation
}
```

## Read-Only Transactions

```java
@Transactional(readOnly = true)
public List<Product> getAllProducts() {
    // Optimized for reading - no dirty checking
}
```

**Benefits:**
- Performance optimization
- Skips dirty checking
- Clear intent

## Rollback Rules

```java
// Default: Rollback on RuntimeException only
@Transactional

// Rollback on checked exceptions too
@Transactional(rollbackFor = Exception.class)

// Don't rollback on specific exception
@Transactional(noRollbackFor = EmailException.class)
```

## Best Practices

✅ **Use service layer** - Not controllers  
✅ **Keep transactions short** - Don't include slow operations  
✅ **Default to REQUIRED** - Most common case  
✅ **Use readOnly for reads** - Performance boost  
✅ **Handle exceptions** - Graceful error handling  

❌ **Don't use on private methods** - Won't work (proxy issue)  
❌ **Avoid self-invocation** - Use dependency injection  
❌ **Don't catch without re-throw** - Prevents rollback  

---

# 2. Specifications & Criteria API

## What is It?
Type-safe way to build **dynamic queries** using Java code instead of strings.

## Core Concepts

### Specification Interface
```java
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
```

**Parameters:**
- `root` - The entity being queried
- `query` - The overall query
- `cb` - Tool to build conditions (CriteriaBuilder)

### JpaSpecificationExecutor
```java
public interface ProductRepository extends JpaRepository<Product, Long>,
                                           JpaSpecificationExecutor<Product> {
    // Automatically get:
    // - findAll(Specification)
    // - findOne(Specification)
    // - count(Specification)
}
```

## Creating Specifications

### Basic Specification
```java
public static Specification<Product> priceLessThan(BigDecimal price) {
    return (root, query, cb) -> 
        cb.lessThan(root.get("price"), price);
}
```

### Combining Specifications
```java
// AND
Specification<Product> spec = Specification
    .where(hasCategory("Electronics"))
    .and(priceLessThan(new BigDecimal("100")));

// OR
spec = Specification
    .where(hasCategory("Electronics"))
    .or(hasCategory("Computers"));

// NOT
spec = Specification.not(isExpensive());
```

## Dynamic Query Building

```java
public List<Product> search(ProductFilter filter) {
    Specification<Product> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        if (filter.getCategory() != null) {
            predicates.add(cb.equal(root.get("category"), filter.getCategory()));
        }
        
        if (filter.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    
    return productRepo.findAll(spec);
}
```

## CriteriaBuilder Methods

### Comparison
```java
cb.equal(x, y)              // =
cb.notEqual(x, y)           // !=
cb.greaterThan(x, y)        // >
cb.lessThan(x, y)           // <
cb.between(x, min, max)     // BETWEEN
```

### Logical
```java
cb.and(p1, p2)              // AND
cb.or(p1, p2)               // OR
cb.not(p)                   // NOT
```

### String
```java
cb.like(x, pattern)         // LIKE
cb.lower(x)                 // LOWER()
```

### Null
```java
cb.isNull(x)                // IS NULL
cb.isNotNull(x)             // IS NOT NULL
```

### Collections
```java
x.in(values)                // IN
```

## When to Use

| Scenario | Use |
|----------|-----|
| Simple static query | Method name or @Query |
| 2-3 dynamic filters | Specification |
| Complex search with many filters | Specification |
| Reusable query logic | Specification |

---

# 3. Query by Example (QBE)

## What is It?
Search by providing an **example entity** with values to match.

## Basic Usage

```java
// 1. Create probe (example)
Product probe = new Product();
probe.setCategory("Electronics");
probe.setPrice(new BigDecimal("100"));

// 2. Create Example
Example<Product> example = Example.of(probe);

// 3. Search
List<Product> results = productRepo.findAll(example);
// Finds: category = 'Electronics' AND price = 100
```

## ExampleMatcher

### String Matching
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withIgnoreCase()                              // Case-insensitive
    .withStringMatcher(StringMatcher.CONTAINING);  // LIKE %value%

Example<Product> example = Example.of(probe, matcher);
```

### String Matchers
- **EXACT** - Exact match
- **STARTING** - LIKE 'value%'
- **ENDING** - LIKE '%value'
- **CONTAINING** - LIKE '%value%'

### Ignore Fields
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withIgnorePaths("id", "createdDate");  // Ignore these fields
```

### Per-Field Matching
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withMatcher("name", match -> match.contains().ignoreCase())
    .withMatcher("category", match -> match.exact());
```

## Limitations

❌ **Cannot do:**
- Range queries (price > 100)
- OR conditions across different fields
- Complex nested queries
- Aggregations (COUNT, SUM)

✅ **Can only do:**
- Exact matching
- String pattern matching (LIKE)
- Simple AND conditions

## QBE vs Specifications

| Feature | QBE | Specifications |
|---------|-----|----------------|
| Simplicity | ✅ Very easy | Complex |
| Range queries | ❌ No | ✅ Yes |
| OR conditions | ⚠️ Limited | ✅ Full |
| Learning curve | Easy | Moderate |
| Flexibility | Limited | Very flexible |

**Use QBE for:** Simple search forms, prototyping  
**Use Specifications for:** Complex queries, ranges, OR conditions

---

# 4. Caching

## Cache Types

### 1. First-Level Cache (Hibernate)
**Scope:** Session/Transaction  
**Managed by:** Hibernate (automatic)  
**Always enabled:** Cannot disable

```java
@Transactional
public void example() {
    User user1 = userRepo.findById(1L);  // Query DB
    User user2 = userRepo.findById(1L);  // From cache (same session)
    // Only 1 query executed
}
```

**Lifespan:** Until transaction ends

---

### 2. Second-Level Cache (Hibernate)
**Scope:** Application-wide  
**Managed by:** Hibernate (manual setup)  
**Shared:** Across all sessions

#### Enable
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=...
```

#### Mark Entity
```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    // ...
}
```

#### Cache Strategies
- **READ_ONLY** - Never updated
- **READ_WRITE** - Most common
- **NONSTRICT_READ_WRITE** - Rarely updated
- **TRANSACTIONAL** - Full consistency

---

### 3. Spring Cache (@Cacheable)
**Scope:** Application-wide  
**Managed by:** Spring  
**Works with:** Any method (not just JPA)

#### Enable
```java
@SpringBootApplication
@EnableCaching
public class Application { }
```

#### Annotations
```java
// Cache method result
@Cacheable("products")
public Product getProduct(Long id) { }

// Update cache
@CachePut(value = "products", key = "#product.id")
public Product updateProduct(Product product) { }

// Remove from cache
@CacheEvict(value = "products", key = "#id")
public void deleteProduct(Long id) { }

// Clear entire cache
@CacheEvict(value = "products", allEntries = true)
public void clearAll() { }
```

#### Custom Keys
```java
@Cacheable(value = "users", key = "#email.toLowerCase()")
public User findByEmail(String email) { }
```

#### Conditional Caching
```java
@Cacheable(value = "products", condition = "#price > 100")
public Product findProduct(Long id, BigDecimal price) { }

@Cacheable(value = "users", unless = "#result == null")
public User findUser(Long id) { }
```

## Cache Providers

| Provider | Type | Use Case |
|----------|------|----------|
| **ConcurrentHashMap** | Local | Development |
| **Ehcache** | Local | Production (single instance) |
| **Redis** | Distributed | Production (multiple instances) |
| **Caffeine** | Local | High performance |

### Redis Setup
```xml
<dependency>
    <groupId>spring-boot-starter-data-redis</groupId>
</dependency>
```

```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000  # 1 hour
```

### Ehcache Setup
```xml
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
```

```properties
spring.cache.type=jcache
spring.cache.jcache.config=classpath:ehcache.xml
```

## Best Practices

✅ **Cache expensive operations** - Complex calculations  
✅ **Set appropriate TTL** - Based on data freshness needs  
✅ **Clear on updates** - Use @CachePut / @CacheEvict  
✅ **Monitor performance** - Track hit ratio  

❌ **Don't cache everything** - Memory waste  
❌ **Don't cache frequently changing data**  
❌ **Don't cache user-specific sensitive data** (unless per-user cache)

---

# 5. Locking & Concurrency

## The Problem
Multiple users modifying same data simultaneously can cause **lost updates**.

## Locking Strategies

### 1. Optimistic Locking (@Version)
**Philosophy:** "Trust but verify"  
**When:** Conflicts are **rare**

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    @Version  // Enables optimistic locking
    private Long version;
}
```

**How it works:**
1. Read entity with version
2. User modifies data
3. Save checks version
4. If version changed → throw OptimisticLockException
5. If same → update and increment version

**Handle exception:**
```java
try {
    productRepo.save(product);
} catch (OptimisticLockException e) {
    // Reload and retry or show error
}
```

---

### 2. Pessimistic Locking
**Philosophy:** "Lock first, prevent conflicts"  
**When:** Conflicts are **frequent**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Product> findById(Long id);
```

**Lock Modes:**
- **PESSIMISTIC_READ** - Shared lock (others can read)
- **PESSIMISTIC_WRITE** - Exclusive lock (others cannot read/write)
- **PESSIMISTIC_FORCE_INCREMENT** - Write lock + increment version

**What happens:**
- Database row is locked
- Other transactions wait
- Lock released on commit

---

## Comparison

| Aspect | Optimistic | Pessimistic |
|--------|-----------|-------------|
| **When locks** | On save | On read |
| **Conflicts** | Rare | Common |
| **Performance** | Better | Worse |
| **Scalability** | Better | Worse |
| **Use case** | Most apps | Critical operations |

## Decision Tree

```
Conflicts likely?
├─ NO → Optimistic (@Version)
└─ YES → Continue

Critical to prevent conflicts?
├─ YES → Pessimistic (@Lock)
└─ NO → Optimistic with retry

Transaction short?
├─ YES → Pessimistic OK
└─ NO → Optimistic (pessimistic blocks too long)
```

## Concurrency Strategies

### 1. Retry Strategy
```java
int attempts = 0;
while (attempts < MAX_RETRIES) {
    try {
        processOrder(orderId);
        return;
    } catch (OptimisticLockException e) {
        attempts++;
        Thread.sleep(100 * attempts);
    }
}
```

### 2. Compare-and-Swap (Atomic)
```java
@Query("UPDATE Product p SET p.stock = p.stock - :qty " +
       "WHERE p.id = :id AND p.stock >= :qty")
int decrementStock(Long id, int qty);
```

---

# 6. Fetch Strategies & Performance

## Lazy vs Eager Loading

### Lazy (Default for Collections)
**Load when accessed**

```java
@OneToMany(fetch = FetchType.LAZY)  // Default
private List<OrderItem> items;
```

**Pros:** Fast load, saves memory  
**Cons:** N+1 problem, LazyInitializationException

### Eager
**Load immediately**

```java
@ManyToOne(fetch = FetchType.EAGER)  // Default
private Customer customer;
```

**Pros:** All data available  
**Cons:** Slower, more memory

### Default Types
- **@OneToOne** → EAGER
- **@ManyToOne** → EAGER
- **@OneToMany** → LAZY
- **@ManyToMany** → LAZY

---

## The N+1 Problem

**Problem:**
```java
List<Order> orders = orderRepo.findAll();  // 1 query

for (Order order : orders) {
    order.getCustomer().getName();  // N queries!
}
// Total: 1 + N queries 🐌
```

**Example:**
```
Query 1: SELECT * FROM orders           (100 orders)
Query 2: SELECT * FROM customers WHERE id = 1
Query 3: SELECT * FROM customers WHERE id = 2
...
Query 101: SELECT * FROM customers WHERE id = 100

Total: 101 queries!
```

---

## Solutions

### 1. Entity Graph
```java
@EntityGraph(attributePaths = {"customer", "items"})
List<Order> findAll();
// Loads orders WITH customer and items in single query
```

**Named Entity Graph:**
```java
@Entity
@NamedEntityGraph(
    name = "Order.withCustomer",
    attributeNodes = @NamedAttributeNode("customer")
)
public class Order { }

@EntityGraph("Order.withCustomer")
List<Order> findAll();
```

---

### 2. Fetch Join
```java
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();
```

**Multiple joins:**
```java
@Query("SELECT DISTINCT o FROM Order o " +
       "JOIN FETCH o.customer " +
       "JOIN FETCH o.items")
List<Order> findAllWithData();
```

**DISTINCT is important** - prevents duplicates with collections

---

### 3. Batch Fetching
```java
@Entity
public class Order {
    @ManyToOne
    @BatchSize(size = 10)  // Load 10 customers at once
    private Customer customer;
}
```

**Global:**
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=10
```

**Result:**
```
Instead of 100 queries → 10 queries (batches of 10)
```

---

## Query Optimization

### 1. Projection (DTO)
```java
@Query("SELECT new com.example.OrderDTO(o.id, o.total) FROM Order o")
List<OrderDTO> findSummary();
// Loads only needed columns
```

### 2. Pagination
```java
Page<Order> orders = orderRepo.findAll(PageRequest.of(0, 20));
```

### 3. Read-Only
```java
@Transactional(readOnly = true)
public List<Order> getAll() {
    return orderRepo.findAll();
}
```

### 4. Indexing
```java
@Entity
@Table(indexes = {
    @Index(name = "idx_customer", columnList = "customer_id"),
    @Index(name = "idx_date", columnList = "order_date")
})
public class Order { }
```

## Best Practices

✅ **Default to LAZY** - Override when needed  
✅ **Use fetch joins** - Solve N+1  
✅ **Paginate large results**  
✅ **Use projections** - When full entity not needed  
✅ **Add database indexes**  
✅ **Profile queries** - Enable SQL logging  

❌ **Avoid multiple collection joins** - Cartesian product  
❌ **Don't eager load everything**  
❌ **Don't load all data at once** - Use pagination  

---

# 7. Modifying Queries

## @Modifying Annotation

**Required for UPDATE/DELETE queries**

```java
@Modifying
@Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
int updateStatus(@Param("id") Long id, @Param("status") String status);
```

### Parameters
```java
@Modifying(
    clearAutomatically = false,  // Clear persistence context after?
    flushAutomatically = false   // Flush pending changes before?
)
```

**When to use:**
- `clearAutomatically = true` - If entities might be loaded later
- `flushAutomatically = true` - If pending entity changes exist

---

## Update Queries

```java
// Simple update
@Modifying
@Query("UPDATE Product p SET p.price = :price WHERE p.id = :id")
int updatePrice(Long id, BigDecimal price);

// Multiple fields
@Modifying
@Query("UPDATE Product p SET p.price = :price, p.discount = :disc " +
       "WHERE p.category = :cat")
int updatePricing(String cat, BigDecimal price, BigDecimal disc);

// With calculation
@Modifying
@Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id")
int decrementStock(Long id, int qty);
```

---

## Delete Queries

```java
// Simple delete
@Modifying
@Query("DELETE FROM User u WHERE u.status = :status")
int deleteByStatus(String status);

// Conditional delete
@Modifying
@Query("DELETE FROM Token t WHERE t.expiryDate < :date")
int deleteExpired(LocalDateTime date);
```

### Delete vs deleteById

**Repository method (2 queries):**
```java
userRepo.deleteById(1L);
// SELECT + DELETE
```

**@Modifying query (1 query):**
```java
@Modifying
@Query("DELETE FROM User u WHERE u.id = :id")
void deleteUser(Long id);
// DELETE only
```

---

## Bulk Operations

**Problem:**
```java
// ❌ Slow - 1000 queries
List<Product> products = productRepo.findByCategory("Electronics");
for (Product p : products) {
    p.setDiscount(0.10);
    productRepo.save(p);
}
```

**Solution:**
```java
// ✅ Fast - 1 query
@Modifying
@Query("UPDATE Product p SET p.discount = 0.10 WHERE p.category = 'Electronics'")
int applyBulkDiscount();
```

**Performance:** 1000x faster!

---

## Flush & Clear

### The Problem
```java
Product p = productRepo.findById(1L);
p.setPrice(200);  // Pending (not in DB yet)

productRepo.bulkUpdate();  // Doesn't see price change!
```

### Solutions

**1. Flush before bulk operation**
```java
@Modifying(flushAutomatically = true)
@Query("UPDATE Product ...")
```

**2. Clear after bulk operation**
```java
@Modifying(clearAutomatically = true)
@Query("UPDATE Product ...")
```

---

## Best Practices

✅ **Always use @Modifying** - For UPDATE/DELETE  
✅ **Use bulk operations** - For mass updates  
✅ **Set clearAutomatically** - For bulk updates  
✅ **Wrap in @Transactional** - Required  
✅ **Return count** - Verify operation  

❌ **Don't forget @Modifying** - Will fail  
❌ **Don't load entities for simple updates** - Use bulk  
❌ **Don't mix entity changes with bulk** - Flush/clear issues  

---

# Quick Comparison Guide

## When to Use What?

| Task | Tool |
|------|------|
| Simple exact match search | Method names or QBE |
| Dynamic search (2-3 filters) | QBE or Specifications |
| Complex search (many filters, ranges) | Specifications |
| Mass update/delete | @Modifying bulk queries |
| Performance critical query | Fetch joins + Projections |
| Concurrent updates (rare conflicts) | Optimistic locking (@Version) |
| Concurrent updates (frequent) | Pessimistic locking (@Lock) |
| Frequently accessed data | Caching (@Cacheable) |
| Long transaction | Optimistic locking |

---

## Performance Checklist

- [ ] Use LAZY loading by default
- [ ] Solve N+1 with fetch joins or @EntityGraph
- [ ] Enable SQL logging in development
- [ ] Add database indexes on queried columns
- [ ] Use pagination for large results
- [ ] Use projections when full entity not needed
- [ ] Set batch fetch size globally
- [ ] Mark read queries as readOnly
- [ ] Cache frequently accessed data
- [ ] Use bulk operations for mass updates
- [ ] Add @Version for concurrent updates
- [ ] Profile slow queries in production

---

## Common Pitfalls to Avoid

❌ **Transactions**
- Private methods with @Transactional
- Self-invocation
- Long transactions with external calls

❌ **Fetching**
- Multiple collection fetch joins (Cartesian product)
- Eager loading everything
- Forgetting DISTINCT with fetch joins

❌ **Caching**
- Caching everything
- Not clearing cache on updates
- Caching frequently changing data

❌ **Locking**
- Using pessimistic locks in long transactions
- Not handling OptimisticLockException

❌ **Modifying Queries**
- Forgetting @Modifying annotation
- Not using clearAutomatically for bulk operations
- Using individual updates instead of bulk

---

## Essential Annotations Reference

```java
// Transactions
@Transactional
@Transactional(readOnly = true)
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Transactional(isolation = Isolation.SERIALIZABLE)

// Locking
@Version
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Lock(LockModeType.OPTIMISTIC)

// Caching
@EnableCaching
@Cacheable("cacheName")
@CachePut(value = "cacheName", key = "#id")
@CacheEvict(value = "cacheName", allEntries = true)
@Cacheable  // On entity (second-level cache)

// Fetching
@EntityGraph(attributePaths = {"field1", "field2"})
@BatchSize(size = 10)

// Modifying
@Modifying
@Modifying(clearAutomatically = true)
@Modifying(flushAutomatically = true)

// Database
@Index(name = "idx_name", columnList = "column")
```

---

**End of Revision Notes** 📚

*Remember: Understand concepts → Apply patterns → Optimize when needed*
