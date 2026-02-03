# Locking & Concurrency - Concepts

## Table of Contents
- [What is Concurrency?](#what-is-concurrency)
- [The Concurrency Problem](#the-concurrency-problem)
- [Optimistic Locking (@Version)](#optimistic-locking-version)
- [Pessimistic Locking](#pessimistic-locking)
- [Lock Annotation](#lock-annotation)
- [Concurrency Handling Strategies](#concurrency-handling-strategies)
- [Best Practices](#best-practices)
- [Quick Reference](#quick-reference)

---

## What is Concurrency?

### Concept
**Concurrency** happens when multiple users/threads try to access and modify the same data **at the same time**.

### The Challenge
Without proper handling, concurrent modifications can lead to:
- **Lost updates** - One user's changes overwrite another's
- **Dirty reads** - Reading uncommitted changes
- **Inconsistent data** - Database state becomes invalid

---

## The Concurrency Problem

### Lost Update Example

**Scenario:** Two users updating the same bank account

```
Time    User A                          User B
----    ------                          ------
1       Read balance = $100
2                                       Read balance = $100
3       Withdraw $50
4       Write balance = $50
5                                       Withdraw $30
6                                       Write balance = $70
                                        
Result: Balance = $70 (should be $20!)
        User A's withdrawal is LOST
```

### Real-World Examples
- **E-commerce:** Two people buying the last item in stock
- **Banking:** Concurrent withdrawals from same account
- **Booking:** Two people reserving the same seat
- **Inventory:** Multiple orders reducing stock simultaneously

---

## Optimistic Locking (@Version)

### Concept
**Optimistic locking** assumes conflicts are **rare**. It checks for conflicts only when saving.

**Philosophy:** "Trust, but verify"

### How It Works
1. Add a version number to entity
2. When reading, record the version
3. When updating, check if version changed
4. If version changed → someone else modified it → throw exception
5. If version same → update and increment version

### Implementation

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    private String name;
    private Integer stock;
    
    @Version  // This enables optimistic locking
    private Long version;
}
```

### What Happens

```
User A reads Product (version = 1, stock = 10)
User B reads Product (version = 1, stock = 10)

User A updates stock to 9, saves
→ Version becomes 2

User B tries to update stock to 8, saves
→ Checks version: expects 1, but finds 2
→ Throws OptimisticLockException
→ Update rejected
```

### Version Field Rules
- Automatically managed by JPA
- Don't manually set/modify version field
- Can be `Long`, `Integer`, `Short`, `Timestamp`
- Incremented on every update

### Exception Handling

```java
try {
    productRepo.save(product);
} catch (OptimisticLockException e) {
    // Handle conflict
    // Option 1: Reload and retry
    // Option 2: Show error to user
    // Option 3: Merge changes
}
```

### When to Use
✅ **Use optimistic locking when:**
- Conflicts are **rare**
- Read-heavy application
- Long-running transactions
- User forms (data might be stale when submitted)
- Most business applications (default choice)

❌ **Don't use when:**
- Conflicts are **frequent**
- Must prevent conflicts at all costs
- Financial transactions requiring absolute consistency

---

## Pessimistic Locking

### Concept
**Pessimistic locking** assumes conflicts are **common**. It prevents conflicts by locking data immediately.

**Philosophy:** "Better safe than sorry"

### How It Works
1. When reading, acquire a database lock
2. Other transactions cannot read/modify locked data
3. Lock released when transaction completes
4. Blocks concurrent access

### Lock Modes

| Lock Mode | Behavior |
|-----------|----------|
| **PESSIMISTIC_READ** | Others can read, cannot write |
| **PESSIMISTIC_WRITE** | Others cannot read or write (exclusive) |
| **PESSIMISTIC_FORCE_INCREMENT** | Write lock + increment version |

### Implementation

```java
// Using repository method
Product product = productRepo.findById(1L, LockModeType.PESSIMISTIC_WRITE);

// Using @Lock annotation
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findById(Long id);
}

// Using EntityManager
Product product = entityManager.find(Product.class, 1L, LockModeType.PESSIMISTIC_WRITE);
```

### What Happens

```
User A acquires PESSIMISTIC_WRITE lock on Product ID 1
→ Database locks the row

User B tries to read Product ID 1
→ Blocked, waits for User A to finish

User A updates and commits
→ Lock released

User B can now access Product ID 1
```

### Lock Timeout

```java
// Set timeout (milliseconds)
Map<String, Object> properties = new HashMap<>();
properties.put("javax.persistence.lock.timeout", 5000);

Product product = entityManager.find(Product.class, 1L, 
    LockModeType.PESSIMISTIC_WRITE, properties);

// Throws LockTimeoutException if cannot acquire lock in 5 seconds
```

### When to Use
✅ **Use pessimistic locking when:**
- Conflicts are **frequent**
- Data consistency is critical (financial transactions)
- Short transactions
- Preventing race conditions is essential
- Multiple updates to same data expected

❌ **Don't use when:**
- Long transactions (blocks others)
- Read-heavy workload
- Scalability is priority

---

## Lock Annotation

### Concept
`@Lock` annotation specifies locking strategy for repository query methods.

### Lock Modes

#### 1. OPTIMISTIC (Default)
```java
@Lock(LockModeType.OPTIMISTIC)
Optional<Product> findById(Long id);

// Uses @Version field
// No database lock
// Checks version on commit
```

#### 2. OPTIMISTIC_FORCE_INCREMENT
```java
@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
Optional<Product> findById(Long id);

// Increments version even if no changes
// Useful to track that entity was accessed
```

#### 3. PESSIMISTIC_READ
```java
@Lock(LockModeType.PESSIMISTIC_READ)
Optional<Product> findById(Long id);

// Shared lock (read lock)
// Others can read, but cannot modify
// SQL: SELECT ... LOCK IN SHARE MODE
```

#### 4. PESSIMISTIC_WRITE
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Product> findById(Long id);

// Exclusive lock (write lock)
// Others cannot read or write
// SQL: SELECT ... FOR UPDATE
```

#### 5. PESSIMISTIC_FORCE_INCREMENT
```java
@Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
Optional<Product> findById(Long id);

// Pessimistic write lock + increment version
// Combines both locking strategies
```

### Custom Repository Methods

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findByCategory(String category);
}
```

### Service Layer Usage

```java
@Service
public class ProductService {
    
    @Transactional
    public void updateStock(Long productId, int quantity) {
        // Acquire pessimistic lock
        Product product = productRepo.findByIdWithLock(productId)
            .orElseThrow();
        
        // Modify safely (no one else can modify)
        product.setStock(product.getStock() - quantity);
        
        productRepo.save(product);
        
        // Lock released when transaction commits
    }
}
```

---

## Concurrency Handling Strategies

### 1. Optimistic Locking Strategy

**Best for:** Most applications (low conflict)

```java
@Entity
public class Account {
    @Id
    private Long id;
    private BigDecimal balance;
    
    @Version
    private Long version;
}

@Service
public class AccountService {
    
    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        account.setBalance(account.getBalance().subtract(amount));
        
        try {
            accountRepo.save(account);
        } catch (OptimisticLockException e) {
            // Retry or inform user
            throw new ConcurrentModificationException("Account was modified by another user");
        }
    }
}
```

### 2. Pessimistic Locking Strategy

**Best for:** High conflict scenarios (banking, inventory)

```java
@Service
public class InventoryService {
    
    @Transactional
    public void reserveStock(Long productId, int quantity) {
        // Lock immediately
        Product product = productRepo.findByIdWithLock(productId).orElseThrow();
        
        if (product.getStock() < quantity) {
            throw new InsufficientStockException();
        }
        
        product.setStock(product.getStock() - quantity);
        productRepo.save(product);
        
        // Lock released on commit
    }
}
```

### 3. Retry Strategy

**Best for:** Handling optimistic lock failures

```java
@Service
public class OrderService {
    
    private static final int MAX_RETRIES = 3;
    
    public void processOrder(Long orderId) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                processOrderInternal(orderId);
                return; // Success
            } catch (OptimisticLockException e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw new ConcurrentUpdateException("Failed after " + MAX_RETRIES + " attempts");
                }
                // Wait and retry
                Thread.sleep(100 * attempts);
            }
        }
    }
    
    @Transactional
    private void processOrderInternal(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus("PROCESSED");
        orderRepo.save(order);
    }
}
```

### 4. Compare-and-Swap (CAS) Strategy

**Best for:** Simple numeric updates

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity " +
           "WHERE p.id = :id AND p.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}

@Service
public class ProductService {
    
    public boolean purchaseProduct(Long productId, int quantity) {
        int updated = productRepo.decrementStock(productId, quantity);
        return updated > 0; // True if stock was sufficient and updated
    }
}
```

### 5. Queue-Based Strategy

**Best for:** High contention, order matters

```java
// Use message queue (RabbitMQ, Kafka)
// Serialize all updates to same entity
// Process one at a time
```

### 6. Event Sourcing Strategy

**Best for:** Complex business logic, audit requirements

```java
// Store events instead of state
// Rebuild state from events
// Conflicts become business events
```

---

## Best Practices

### 1. Choose Right Locking Strategy

```java
// ✅ Optimistic for most cases
@Version
private Long version;

// ✅ Pessimistic for critical operations
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Account> findById(Long id);
```

### 2. Keep Transactions Short

```java
// ❌ BAD - Long transaction holds lock
@Transactional
public void processOrder(Order order) {
    Product product = productRepo.findByIdWithLock(order.getProductId());
    sendEmail();  // Slow operation
    callExternalAPI();  // Slow operation
    product.setStock(product.getStock() - 1);
}

// ✅ GOOD - Short transaction
@Transactional
public void updateStock(Long productId, int quantity) {
    Product product = productRepo.findByIdWithLock(productId);
    product.setStock(product.getStock() - quantity);
}

public void processOrder(Order order) {
    updateStock(order.getProductId(), 1);  // Quick transaction
    sendEmail();  // Outside transaction
    callExternalAPI();  // Outside transaction
}
```

### 3. Handle Lock Exceptions

```java
// Always handle locking exceptions
try {
    accountService.withdraw(accountId, amount);
} catch (OptimisticLockException e) {
    // Inform user data was modified
    return "Account was modified by another user. Please try again.";
} catch (PessimisticLockException e) {
    // Timeout or deadlock
    return "System is busy. Please try again.";
}
```

### 4. Set Appropriate Timeouts

```java
// Prevent indefinite waiting
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({
    @QueryHint(name = "javax.persistence.lock.timeout", value = "5000")
})
Optional<Product> findByIdWithTimeout(Long id);
```

### 5. Use Database-Level Constraints

```java
// Combine with database constraints for extra safety
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"username"}))
public class User {
    @Version
    private Long version;
}
```

### 6. Monitor Deadlocks

```java
// Log and alert on deadlocks
try {
    // ... pessimistic locking code
} catch (PessimisticLockException e) {
    if (e.getCause() instanceof LockTimeoutException) {
        logger.error("Deadlock detected", e);
        // Alert monitoring system
    }
}
```

---

## Quick Reference

### Optimistic vs Pessimistic

| Aspect | Optimistic | Pessimistic |
|--------|-----------|-------------|
| **When locks** | On commit | On read |
| **Assumption** | Conflicts rare | Conflicts common |
| **Performance** | Better (no locks) | Worse (blocking) |
| **Consistency** | Eventually | Immediate |
| **Use case** | Most apps | Critical operations |
| **Scalability** | Better | Worse |

### Lock Modes

```java
// Optimistic
LockModeType.OPTIMISTIC
LockModeType.OPTIMISTIC_FORCE_INCREMENT

// Pessimistic
LockModeType.PESSIMISTIC_READ         // Shared lock
LockModeType.PESSIMISTIC_WRITE        // Exclusive lock
LockModeType.PESSIMISTIC_FORCE_INCREMENT
```

### Annotations

```java
@Version                              // Enable optimistic locking
@Lock(LockModeType.PESSIMISTIC_WRITE) // Specify lock mode
```

### Common Exceptions

```java
OptimisticLockException               // Version mismatch
PessimisticLockException              // Cannot acquire lock
LockTimeoutException                  // Lock timeout exceeded
```

---

## Summary

### Core Concepts

**Concurrency** = Multiple users accessing same data simultaneously

**Optimistic Locking** = Check for conflicts on save (trust first)

**Pessimistic Locking** = Lock immediately to prevent conflicts (lock first)

**@Version** = Field that enables optimistic locking

**@Lock** = Specify locking strategy for queries

### Decision Tree

```
Is data conflict likely?
├─ NO → Use Optimistic Locking (@Version)
└─ YES → Continue

Is it critical to prevent conflicts?
├─ YES → Use Pessimistic Locking (@Lock)
└─ NO → Use Optimistic with retry logic

Can transaction be kept short?
├─ YES → Pessimistic OK
└─ NO → Use Optimistic (pessimistic would block too long)
```

### Key Strategies

1. **Default:** Optimistic locking with @Version
2. **High conflict:** Pessimistic locking
3. **Failures:** Retry with exponential backoff
4. **Simple updates:** Database-level atomic operations
5. **Complex workflows:** Event sourcing or message queues

### Remember

1. **Optimistic is default** - Use for most cases
2. **Pessimistic for critical** - Banking, inventory
3. **Keep transactions short** - Especially with pessimistic locks
4. **Always handle exceptions** - Graceful degradation
5. **Set timeouts** - Prevent indefinite blocking
6. **Test under load** - Concurrency bugs appear under stress

---

**Lock smart, stay consistent! 🔒**
