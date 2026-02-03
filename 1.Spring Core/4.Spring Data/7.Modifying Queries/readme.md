# Modifying Queries - Concepts

## Table of Contents
- [What are Modifying Queries?](#what-are-modifying-queries)
- [@Modifying Annotation](#modifying-annotation)
- [Update Queries](#update-queries)
- [Delete Queries](#delete-queries)
- [Bulk Operations](#bulk-operations)
- [Flush & Clear Behavior](#flush--clear-behavior)
- [Best Practices](#best-practices)
- [Quick Reference](#quick-reference)

---

## What are Modifying Queries?

### Concept
**Modifying queries** are custom queries that **change data** (UPDATE or DELETE) instead of just reading it.

### Why Use Them?
- **Performance:** Update/delete many records at once (bulk operations)
- **Efficiency:** Single query vs loading entities and modifying one-by-one
- **Simplicity:** Direct database operations

### Standard vs Modifying Queries

**Standard (Read) Query:**
```java
@Query("SELECT u FROM User u WHERE u.status = :status")
List<User> findByStatus(String status);
// Reads data
```

**Modifying Query:**
```java
@Modifying
@Query("UPDATE User u SET u.status = :status WHERE u.active = false")
int updateInactiveUserStatus(String status);
// Changes data
```

---

## @Modifying Annotation

### Concept
**@Modifying** marks a query method as data-changing (UPDATE/DELETE).

Required for any query that modifies data.

### Basic Usage

```java
@Modifying
@Query("UPDATE Product p SET p.price = :price WHERE p.id = :id")
int updatePrice(@Param("id") Long id, @Param("price") BigDecimal price);
```

### Return Types

```java
// Number of rows affected
@Modifying
@Query("UPDATE User u SET u.status = 'ACTIVE'")
int activateAllUsers();  // Returns: number of updated rows

// Void (don't care about count)
@Modifying
@Query("DELETE FROM User u WHERE u.status = 'DELETED'")
void removeDeletedUsers();
```

### Common Return Types
- `int` or `Integer` - Number of affected rows
- `void` - No return value
- **NOT** entity objects (entities are not returned from modifying queries)

---

### clearAutomatically Parameter

**Concept:** Whether to clear persistence context after query execution

```java
@Modifying(clearAutomatically = true)
@Query("UPDATE User u SET u.status = :status")
int updateAllStatuses(String status);
```

**Why clear?**
- Modifying queries bypass the persistence context
- Cached entities in memory might be stale
- Clearing ensures fresh data on next read

**When to use:**
- `clearAutomatically = true` - Query affects many entities (recommended)
- `clearAutomatically = false` - Query affects few entities or performance critical

---

### flushAutomatically Parameter

**Concept:** Whether to flush pending changes before executing query

```java
@Modifying(flushAutomatically = true)
@Query("UPDATE Product p SET p.featured = true WHERE p.rating > 4.5")
int markFeaturedProducts();
```

**Why flush?**
- Ensures pending entity changes are saved to database first
- Modifying query operates on current database state
- Prevents query from seeing stale data

**When to use:**
- `flushAutomatically = true` - If entities modified in same transaction
- `flushAutomatically = false` - No pending changes (default)

---

## Update Queries

### Concept
**Update queries** modify existing records directly in the database.

### Simple Update

```java
@Modifying
@Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
int updateUserStatus(@Param("id") Long id, @Param("status") String status);
```

**Generated SQL:**
```sql
UPDATE users SET status = ? WHERE id = ?
```

### Update Multiple Fields

```java
@Modifying
@Query("UPDATE Product p SET p.price = :price, p.discount = :discount " +
       "WHERE p.category = :category")
int updateProductPricing(@Param("category") String category,
                         @Param("price") BigDecimal price,
                         @Param("discount") BigDecimal discount);
```

### Conditional Update

```java
@Modifying
@Query("UPDATE Order o SET o.status = 'SHIPPED' " +
       "WHERE o.status = 'PROCESSING' AND o.paymentReceived = true")
int shipProcessedOrders();
```

### Update with Calculation

```java
@Modifying
@Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id")
int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);
```

### Update with Subquery

```java
@Modifying
@Query("UPDATE User u SET u.status = 'VIP' " +
       "WHERE u.id IN (SELECT o.customer.id FROM Order o " +
       "GROUP BY o.customer.id HAVING SUM(o.total) > 10000)")
int promoteHighSpenders();
```

---

## Delete Queries

### Concept
**Delete queries** remove records directly from the database.

### Simple Delete

```java
@Modifying
@Query("DELETE FROM User u WHERE u.status = :status")
int deleteByStatus(String status);
```

**Generated SQL:**
```sql
DELETE FROM users WHERE status = ?
```

### Conditional Delete

```java
@Modifying
@Query("DELETE FROM Token t WHERE t.expiryDate < :date")
int deleteExpiredTokens(@Param("date") LocalDateTime date);
```

### Delete with Join

```java
@Modifying
@Query("DELETE FROM OrderItem oi WHERE oi.order.id = :orderId")
int deleteOrderItems(@Param("orderId") Long orderId);
```

### Delete All

```java
@Modifying
@Query("DELETE FROM AuditLog")
void deleteAllAuditLogs();
```

---

### Delete vs deleteById

**Using repository method (loads entity first):**
```java
userRepo.deleteById(1L);
// 1. SELECT * FROM users WHERE id = 1
// 2. DELETE FROM users WHERE id = 1
// (2 queries)
```

**Using @Modifying query (direct delete):**
```java
@Modifying
@Query("DELETE FROM User u WHERE u.id = :id")
void deleteUser(Long id);
// DELETE FROM users WHERE id = ?
// (1 query)
```

**When to use each:**
- **Repository method:** Need cascade deletes, entity lifecycle callbacks
- **@Modifying query:** Pure deletion, better performance

---

## Bulk Operations

### Concept
**Bulk operations** modify multiple records in a **single query** without loading entities.

### Why Bulk Operations?

**❌ Bad - Individual Updates:**
```java
List<Product> products = productRepo.findByCategory("Electronics");
for (Product product : products) {
    product.setDiscount(0.10);
    productRepo.save(product);
}
// If 1000 products: 1000 UPDATE queries!
```

**✅ Good - Bulk Update:**
```java
@Modifying
@Query("UPDATE Product p SET p.discount = 0.10 WHERE p.category = 'Electronics'")
int applyBulkDiscount();
// 1 UPDATE query for all products!
```

### Performance Comparison

| Operation | Individual | Bulk | Speedup |
|-----------|-----------|------|---------|
| Update 1000 rows | 1000 queries | 1 query | 1000x |
| Delete 500 rows | 500 queries | 1 query | 500x |

### Bulk Update Examples

#### Percentage Increase
```java
@Modifying
@Query("UPDATE Product p SET p.price = p.price * 1.10 WHERE p.category = :category")
int increasePricesByCategory(String category);
// Increase all prices by 10%
```

#### Status Updates
```java
@Modifying
@Query("UPDATE Order o SET o.status = 'CANCELLED' " +
       "WHERE o.status = 'PENDING' AND o.createdDate < :cutoffDate")
int cancelOldPendingOrders(LocalDateTime cutoffDate);
```

#### Reset Values
```java
@Modifying
@Query("UPDATE User u SET u.loginAttempts = 0 WHERE u.loginAttempts > 0")
int resetAllLoginAttempts();
```

### Bulk Delete Examples

#### Cleanup Operations
```java
@Modifying
@Query("DELETE FROM Session s WHERE s.lastAccess < :threshold")
int deleteInactiveSessions(LocalDateTime threshold);
```

#### Archive and Delete
```java
@Modifying
@Query("DELETE FROM Order o WHERE o.status = 'COMPLETED' AND o.completedDate < :date")
int deleteOldOrders(LocalDateTime date);
```

---

### Bulk Operations Limitations

**Cannot use:**
- Cascade operations
- Entity lifecycle callbacks (@PreUpdate, @PostUpdate)
- Optimistic locking (@Version is not checked)
- Second-level cache (not updated)

**Persistence context not updated:**
```java
// Load entity
Product product = productRepo.findById(1L);  // price = 100

// Bulk update
productRepo.bulkUpdatePrice(1L, new BigDecimal("150"));

// Entity still has old value!
product.getPrice();  // Still 100, not 150!

// Solution: refresh or clear context
entityManager.refresh(product);  // Now 150
```

---

## Flush & Clear Behavior

### Concept
**Flush** writes pending changes to database.  
**Clear** removes entities from persistence context (cache).

### The Problem

```java
@Transactional
public void updateProducts() {
    // 1. Modify entity in memory
    Product product = productRepo.findById(1L);
    product.setPrice(new BigDecimal("200"));
    // Change NOT yet in database (pending)
    
    // 2. Execute modifying query
    productRepo.bulkUpdateDiscount("Electronics", 0.10);
    // Query operates on database directly
    // Doesn't see pending price change!
}
```

### Solutions

#### Solution 1: flushAutomatically = true

```java
@Modifying(flushAutomatically = true)
@Query("UPDATE Product p SET p.discount = :discount WHERE p.category = :category")
int bulkUpdateDiscount(String category, BigDecimal discount);
```

**What happens:**
1. Flushes pending changes (price = 200) to DB
2. Executes bulk update
3. Both changes are visible

#### Solution 2: Manual Flush

```java
@Transactional
public void updateProducts() {
    Product product = productRepo.findById(1L);
    product.setPrice(new BigDecimal("200"));
    
    entityManager.flush();  // Write pending changes
    
    productRepo.bulkUpdateDiscount("Electronics", 0.10);
}
```

#### Solution 3: clearAutomatically = true

```java
@Modifying(clearAutomatically = true)
@Query("UPDATE Product p SET p.price = :price WHERE p.category = :category")
int bulkUpdatePrice(String category, BigDecimal price);
```

**What happens:**
1. Executes bulk update in database
2. Clears persistence context
3. Next entity load gets fresh data from database

---

### When to Flush vs Clear

| Use Case | Action |
|----------|--------|
| Have pending entity changes before modifying query | `flushAutomatically = true` |
| Modified entities might be loaded later | `clearAutomatically = true` |
| Both scenarios | Both = true |
| No entity operations in same transaction | Both = false (default) |

---

### Complete Example

```java
@Transactional
public void complexUpdate() {
    // 1. Modify some entities
    Product product1 = productRepo.findById(1L);
    product1.setPrice(new BigDecimal("100"));
    
    Product product2 = productRepo.findById(2L);
    product2.setPrice(new BigDecimal("200"));
    
    // 2. Flush pending changes
    entityManager.flush();
    
    // 3. Bulk update
    productRepo.bulkUpdateCategory("Electronics", "Tech");
    
    // 4. Clear stale cached entities
    entityManager.clear();
    
    // 5. Fresh load
    Product refreshed = productRepo.findById(1L);
    // Now has updated category from bulk operation
}
```

---

## Best Practices

### 1. Always Use @Modifying for UPDATE/DELETE

```java
// ❌ Missing @Modifying - will fail
@Query("UPDATE User u SET u.status = 'ACTIVE'")
void activate();

// ✅ Correct
@Modifying
@Query("UPDATE User u SET u.status = 'ACTIVE'")
void activate();
```

### 2. Use Bulk Operations for Mass Updates

```java
// ❌ Bad - loads all entities
List<User> users = userRepo.findAll();
users.forEach(u -> u.setStatus("ACTIVE"));
userRepo.saveAll(users);

// ✅ Good - single query
@Modifying
@Query("UPDATE User u SET u.status = 'ACTIVE'")
int activateAll();
```

### 3. Set clearAutomatically for Bulk Updates

```java
@Modifying(clearAutomatically = true)
@Query("UPDATE Product p SET p.featured = true WHERE p.rating > 4.5")
int markFeatured();
```

### 4. Use @Transactional with @Modifying

```java
@Modifying
@Transactional  // Required for data modification
@Query("DELETE FROM Token t WHERE t.expired = true")
void deleteExpiredTokens();
```

### 5. Return Count for Verification

```java
// ✅ Return count to verify operation
@Modifying
@Query("UPDATE User u SET u.verified = true WHERE u.email = :email")
int verifyUser(String email);

// Check result
int updated = userRepo.verifyUser("user@example.com");
if (updated == 0) {
    throw new UserNotFoundException();
}
```

### 6. Use Native Queries for Database-Specific Features

```java
@Modifying
@Query(value = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?1", 
       nativeQuery = true)
int updateLastLogin(Long id);
```

### 7. Avoid Modifying Queries with Complex Business Logic

```java
// ❌ Bad - complex logic better in service
@Modifying
@Query("UPDATE Order o SET o.discount = ...")

// ✅ Good - use service method with entity manipulation
@Transactional
public void calculateOrderDiscount(Long orderId) {
    Order order = orderRepo.findById(orderId);
    // Complex discount calculation
    order.setDiscount(calculatedDiscount);
}
```

---

## Quick Reference

### @Modifying Parameters
```java
@Modifying(
    clearAutomatically = false,  // Clear persistence context after?
    flushAutomatically = false   // Flush pending changes before?
)
```

### Common Patterns

```java
// Update
@Modifying
@Query("UPDATE Entity e SET e.field = :value WHERE e.id = :id")
int update(Long id, Type value);

// Delete
@Modifying
@Query("DELETE FROM Entity e WHERE e.condition = :value")
int delete(Type value);

// Bulk update
@Modifying(clearAutomatically = true)
@Query("UPDATE Entity e SET e.field = :value WHERE e.category = :cat")
int bulkUpdate(String cat, Type value);
```

### Return Types
```java
int / Integer  // Number of affected rows
void           // No return
boolean        // true if rows affected (rarely used)
```

---

## Summary

### Core Concepts

**@Modifying** = Required for UPDATE/DELETE queries

**Update Queries** = Modify existing records

**Delete Queries** = Remove records

**Bulk Operations** = Modify many records in single query (fast!)

**Flush** = Write pending changes to database

**Clear** = Remove cached entities from memory

### Key Points

1. **@Modifying is mandatory** for UPDATE/DELETE
2. **Bulk operations are fast** - single query vs many
3. **Persistence context not updated** by modifying queries
4. **Use clearAutomatically** to avoid stale data
5. **Use flushAutomatically** if pending entity changes exist
6. **Always wrap in @Transactional**

### Performance Rules

```
Individual Operations:
- Load entities: N SELECT queries
- Modify in memory
- Save: N UPDATE queries
- Total: 2N queries 🐌

Bulk Operations:
- Single UPDATE query
- No entity loading
- Total: 1 query 🚀
```

### Decision Tree

```
Need to modify data?
├─ Single record + need entity logic?
│  └─ Use repository.save() or delete()
│
├─ Multiple records + simple update?
│  └─ Use @Modifying bulk query
│
├─ Performance critical?
│  └─ Use @Modifying bulk query
│
└─ Need cascade/callbacks?
   └─ Use repository methods
```

### Checklist

- [ ] Add @Modifying to UPDATE/DELETE queries
- [ ] Add @Transactional to modifying methods
- [ ] Set clearAutomatically = true for bulk operations
- [ ] Set flushAutomatically = true if pending entity changes
- [ ] Return int to track affected rows
- [ ] Consider native query for DB-specific features
- [ ] Test query actually modifies expected rows

---

**Modify efficiently, flush wisely! ✏️**
