# Fetch Strategies & Performance - Concepts

## Table of Contents
- [What is Fetching?](#what-is-fetching)
- [Lazy vs Eager Loading](#lazy-vs-eager-loading)
- [The N+1 Query Problem](#the-n1-query-problem)
- [Entity Graph (@EntityGraph)](#entity-graph-entitygraph)
- [Fetch Joins](#fetch-joins)
- [Batch Fetching](#batch-fetching)
- [Query Optimization Techniques](#query-optimization-techniques)
- [Best Practices](#best-practices)
- [Quick Reference](#quick-reference)

---

## What is Fetching?

### Concept
**Fetching** is how JPA loads associated entities (relationships) from the database.

### The Challenge
When loading an entity with relationships, should related data be loaded:
- **Immediately** (eagerly)?
- **Later when needed** (lazily)?

### Example Scenario
```java
@Entity
public class Order {
    @Id
    private Long id;
    
    @ManyToOne
    private Customer customer;  // Should we load customer now or later?
    
    @OneToMany
    private List<OrderItem> items;  // Should we load items now or later?
}
```

---

## Lazy vs Eager Loading

### Concept
Defines **when** associated entities are loaded from the database.

### Lazy Loading (Default for Collections)

**Concept:** Load associated data **only when accessed**

```java
@OneToMany(fetch = FetchType.LAZY)  // Default
private List<OrderItem> items;
```

**How it works:**
```java
Order order = orderRepo.findById(1L);  // SELECT * FROM orders WHERE id = 1
// items NOT loaded yet

order.getItems().size();  // NOW: SELECT * FROM order_items WHERE order_id = 1
// items loaded when accessed
```

**Pros:**
- ✅ Fast initial load
- ✅ Saves memory
- ✅ Loads only what you need

**Cons:**
- ❌ Can cause N+1 problem
- ❌ LazyInitializationException if session closed

---

### Eager Loading

**Concept:** Load associated data **immediately** with parent entity

```java
@ManyToOne(fetch = FetchType.EAGER)  // Default for @ManyToOne
private Customer customer;
```

**How it works:**
```java
Order order = orderRepo.findById(1L);
// SELECT * FROM orders WHERE id = 1
// SELECT * FROM customers WHERE id = order.customer_id
// customer loaded immediately
```

**Pros:**
- ✅ No LazyInitializationException
- ✅ All data available immediately

**Cons:**
- ❌ Loads unnecessary data
- ❌ Slower initial load
- ❌ More memory usage

---

### Default Fetch Types

| Relationship | Default Fetch Type | Reason |
|--------------|-------------------|--------|
| **@OneToOne** | EAGER | Usually one related entity |
| **@ManyToOne** | EAGER | Usually one related entity |
| **@OneToMany** | LAZY | Could be many entities |
| **@ManyToMany** | LAZY | Could be many entities |

---

### LazyInitializationException

**The Problem:**
```java
@Transactional
public Order getOrder(Long id) {
    return orderRepo.findById(id);  // Transaction ends here
}

// Later, outside transaction
Order order = getOrder(1L);
order.getItems().size();  // ❌ LazyInitializationException!
// Session is closed, cannot load lazy items
```

**Solutions:**
1. Access within transaction
2. Use eager loading
3. Use fetch joins
4. Use @EntityGraph

---

## The N+1 Query Problem

### Concept
**N+1 problem** occurs when you execute **1 query** to get N entities, then **N additional queries** to load their relationships.

### Example Problem

```java
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
}

// Get all orders
List<Order> orders = orderRepo.findAll();  // 1 query

// Access customer for each order
for (Order order : orders) {
    System.out.println(order.getCustomer().getName());  // N queries!
}
```

**Total queries = 1 + N**
- 1 query to get orders
- N queries to get each customer (one per order)

### Visual Example

```
Query 1: SELECT * FROM orders                    (gets 100 orders)
Query 2: SELECT * FROM customers WHERE id = 1    (for order 1)
Query 3: SELECT * FROM customers WHERE id = 5    (for order 2)
Query 4: SELECT * FROM customers WHERE id = 3    (for order 3)
...
Query 101: SELECT * FROM customers WHERE id = 89 (for order 100)

Total: 101 queries for 100 orders! 🐌
```

### Why It's Bad
- **Performance:** Hundreds of queries instead of one
- **Network overhead:** Multiple round-trips to database
- **Slow application:** Noticeable delay for users

---

## Entity Graph (@EntityGraph)

### Concept
**@EntityGraph** defines which relationships to fetch eagerly for a specific query, **without changing entity mapping**.

Think of it as: "For this query, also load these relationships"

### Types

#### 1. Named Entity Graph (on Entity)
```java
@Entity
@NamedEntityGraph(
    name = "Order.withCustomer",
    attributeNodes = @NamedAttributeNode("customer")
)
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    
    @OneToMany(fetch = FetchType.LAZY)
    private List<OrderItem> items;
}
```

#### 2. Use in Repository
```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @EntityGraph(value = "Order.withCustomer")
    List<Order> findAll();
    // Loads orders WITH customers in single query
}
```

#### 3. Ad-hoc Entity Graph
```java
@EntityGraph(attributePaths = {"customer", "items"})
List<Order> findAll();
// Loads orders with customers AND items
```

### Entity Graph Types

| Type | Behavior | Use Case |
|------|----------|----------|
| **FETCH** | Specified = EAGER, rest = default | Load specific relationships |
| **LOAD** | Specified = EAGER, rest = LAZY | Override to eager |

```java
@EntityGraph(value = "Order.withCustomer", type = EntityGraph.EntityGraphType.FETCH)
List<Order> findAll();
```

### Nested Graphs

```java
@NamedEntityGraph(
    name = "Order.detailed",
    attributeNodes = {
        @NamedAttributeNode("customer"),
        @NamedAttributeNode(value = "items", subgraph = "items-subgraph")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "items-subgraph",
            attributeNodes = @NamedAttributeNode("product")
        )
    }
)
public class Order {
    @ManyToOne
    private Customer customer;
    
    @OneToMany
    private List<OrderItem> items;  // Each item has product
}
```

---

## Fetch Joins

### Concept
**Fetch joins** use JPQL/SQL JOIN to load parent and associated entities in **one query**.

### JPQL Fetch Join

```java
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();
// Single query with JOIN
```

**Generated SQL:**
```sql
SELECT o.*, c.*
FROM orders o
INNER JOIN customers c ON o.customer_id = c.id
```

### Multiple Fetch Joins

```java
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.customer " +
       "JOIN FETCH o.items")
List<Order> findAllWithCustomerAndItems();
```

### LEFT JOIN FETCH

```java
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.customer")
List<Order> findAllWithOptionalCustomer();
// Includes orders even if customer is null
```

### Distinct with Fetch Joins

```java
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();
// DISTINCT prevents duplicate orders if multiple items
```

### Fetch Join vs Entity Graph

| Feature | Fetch Join | Entity Graph |
|---------|-----------|--------------|
| **Where defined** | In query | In repository method |
| **Flexibility** | Very flexible | Less flexible |
| **Multiple collections** | Careful (Cartesian product) | Easier to use |
| **Custom queries** | Easy | Limited |

---

## Batch Fetching

### Concept
**Batch fetching** loads multiple lazy associations in **batches** instead of one-by-one.

Reduces N+1 to N/batch_size + 1

### Configuration

#### On Entity
```java
@Entity
public class Order {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @BatchSize(size = 10)  // Load up to 10 customers at once
    private Customer customer;
}
```

#### Global Configuration
```properties
# Hibernate property
spring.jpa.properties.hibernate.default_batch_fetch_size=10
```

### How It Works

**Without batch fetching:**
```
Query 1: SELECT * FROM customers WHERE id = 1
Query 2: SELECT * FROM customers WHERE id = 2
Query 3: SELECT * FROM customers WHERE id = 3
... (100 queries for 100 orders)
```

**With batch fetching (size = 10):**
```
Query 1: SELECT * FROM customers WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
Query 2: SELECT * FROM customers WHERE id IN (11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
... (10 queries for 100 orders)
```

### Batch Size Recommendations

| Scenario | Recommended Size |
|----------|-----------------|
| Small datasets | 10-20 |
| Medium datasets | 25-50 |
| Large datasets | 50-100 |

---

## Query Optimization Techniques

### 1. Projection (DTO)

**Concept:** Select only needed columns instead of full entities

```java
// Instead of full entity
@Query("SELECT o FROM Order o")
List<Order> findAll();  // Loads ALL columns

// Use DTO projection
@Query("SELECT new com.example.OrderDTO(o.id, o.total, c.name) " +
       "FROM Order o JOIN o.customer c")
List<OrderDTO> findAllSummary();  // Loads only needed columns
```

**Benefits:**
- Less data transferred
- Less memory used
- Faster queries

---

### 2. Pagination

**Concept:** Load data in chunks instead of all at once

```java
Page<Order> orders = orderRepo.findAll(PageRequest.of(0, 20));
// Load 20 orders at a time
```

---

### 3. Read-Only Queries

**Concept:** Mark queries as read-only to skip dirty checking

```java
@Transactional(readOnly = true)
public List<Order> getAllOrders() {
    return orderRepo.findAll();
}
```

**Benefits:**
- No change tracking overhead
- Better performance for read operations

---

### 4. Index Database Columns

**Concept:** Add database indices on frequently queried columns

```java
@Entity
@Table(indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_order_date", columnList = "order_date")
})
public class Order {
    // ...
}
```

---

### 5. Query Result Caching

**Concept:** Cache query results to avoid repeated database calls

```java
@Cacheable("orders")
public List<Order> findAll() {
    return orderRepo.findAll();
}
```

---

### 6. Select Only Required Fields

```java
// ❌ Bad - loads all fields
@Query("SELECT o FROM Order o")

// ✅ Good - loads specific fields
@Query("SELECT o.id, o.total, o.status FROM Order o")
```

---

### 7. Avoid Cartesian Products

**Problem:** Multiple collection fetch joins create Cartesian product

```java
// ❌ BAD - Cartesian product
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.items " +      // 5 items
       "JOIN FETCH o.payments")     // 2 payments
// Returns 5 * 2 = 10 rows for 1 order!
```

**Solution:** Use multiple queries or @EntityGraph

---

### 8. Use EXISTS Instead of COUNT

```java
// ❌ Slower - counts all
@Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.customer.id = :id")
boolean hasOrders(Long id);

// ✅ Faster - stops at first match
@Query("SELECT CASE WHEN EXISTS " +
       "(SELECT 1 FROM Order o WHERE o.customer.id = :id) " +
       "THEN true ELSE false END")
boolean hasOrders(Long id);
```

---

## Best Practices

### 1. Use Lazy Loading by Default

```java
// ✅ Default to LAZY
@OneToMany(fetch = FetchType.LAZY)
private List<OrderItem> items;

// Override with fetch join/entity graph when needed
```

### 2. Solve N+1 with Fetch Joins or Entity Graphs

```java
// ❌ BAD - N+1 problem
List<Order> orders = orderRepo.findAll();
orders.forEach(o -> o.getCustomer().getName());

// ✅ GOOD - Fetch join
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();
```

### 3. Use Pagination for Large Datasets

```java
// ✅ Don't load all at once
Page<Order> page = orderRepo.findAll(PageRequest.of(0, 50));
```

### 4. Profile Your Queries

```properties
# Enable query logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Log slow queries (Hibernate)
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=100
```

### 5. One Fetch Join per Query (for collections)

```java
// ❌ Avoid multiple collection fetch joins
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.items " +
       "JOIN FETCH o.payments")

// ✅ Use separate queries or entity graph
```

### 6. Use Batch Fetching for Consistent N+1 Patterns

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=25
```

---

## Quick Reference

### Fetch Types
```java
FetchType.LAZY        // Load when accessed (default for collections)
FetchType.EAGER       // Load immediately (default for single entities)
```

### Entity Graph
```java
// Ad-hoc
@EntityGraph(attributePaths = {"customer", "items"})

// Named
@EntityGraph("Order.withCustomer")

// Type
type = EntityGraph.EntityGraphType.FETCH  // Specified EAGER, rest default
type = EntityGraph.EntityGraphType.LOAD   // Specified EAGER, rest LAZY
```

### Fetch Join
```java
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items")
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.customer")
```

### Batch Fetching
```java
@BatchSize(size = 10)  // On entity field
spring.jpa.properties.hibernate.default_batch_fetch_size=10  // Global
```

---

## Summary

### Core Concepts

**Lazy Loading** = Load when accessed (saves memory, risk N+1)

**Eager Loading** = Load immediately (wastes memory, prevents N+1)

**N+1 Problem** = 1 query + N queries for relationships (performance killer)

**Entity Graph** = Specify eager fetching per query (flexible)

**Fetch Join** = JOIN to load in single query (powerful)

**Batch Fetching** = Load multiple in batches (reduces queries)

### The N+1 Solution Hierarchy

```
Problem: N+1 queries

Solutions (in order of preference):
1. Fetch Join (JPQL)         - Best for custom queries
2. @EntityGraph              - Best for repository methods
3. Batch Fetching            - When fetch join not possible
4. Eager Loading             - Last resort (affects all queries)
```

### Performance Checklist

- [ ] Default relationships to LAZY
- [ ] Use fetch joins or @EntityGraph for known N+1 patterns
- [ ] Enable query logging in development
- [ ] Profile slow queries
- [ ] Add database indices
- [ ] Use pagination for large results
- [ ] Use projections/DTOs when full entity not needed
- [ ] Set batch fetch size globally
- [ ] Mark read queries as @Transactional(readOnly = true)
- [ ] Cache frequently accessed data

### Decision Tree

```
Loading associated entities?
├─ Always need them?
│  └─ Use Fetch Join or @EntityGraph
│
├─ Sometimes need them?
│  └─ Use LAZY + conditional fetch join
│
├─ N+1 problem?
│  ├─ Custom query? → Fetch Join
│  ├─ Repository method? → @EntityGraph
│  └─ Consistent pattern? → Batch Fetching
│
└─ Large dataset?
   └─ Use Pagination + Fetch strategy
```

### Remember

1. **Lazy is default for collections** - for good reason
2. **N+1 is the most common performance problem** - learn to spot it
3. **Fetch joins solve N+1** - use liberally
4. **Multiple collection joins = Cartesian product** - avoid
5. **Batch fetching is a safety net** - reduces N+1 impact
6. **Profile in production** - development data too small

---

**Fetch smart, query fast! 🚀**
