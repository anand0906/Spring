# JPA Specifications & Criteria API - Concepts

## Table of Contents
- [What is the Criteria API?](#what-is-the-criteria-api)
- [What are Specifications?](#what-are-specifications)
- [JpaSpecificationExecutor](#jpaspecificationexecutor)
- [Specification Pattern](#specification-pattern)
- [Dynamic Query Building](#dynamic-query-building)
- [Predicate & CriteriaBuilder](#predicate--criteriabuilder)
- [Best Practices](#best-practices)
- [Quick Reference](#quick-reference)

---

## What is the Criteria API?

### Concept
The **Criteria API** lets you build type-safe database queries using **Java code** instead of strings.

### Why Use It?
- **Type Safety** - Compiler checks field names (no runtime errors)
- **Dynamic Queries** - Build queries based on conditions at runtime
- **Refactoring Support** - IDE can track field usage

### Basic Example
```java
// Instead of string JPQL
String jpql = "SELECT u FROM User u WHERE u.age > 18";

// Use type-safe Criteria API
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> user = query.from(User.class);
query.where(cb.gt(user.get("age"), 18));
```

---

## What are Specifications?

### Concept
A **Specification** is a reusable piece of query logic that defines filtering conditions.

Think of it as: **One specification = One filter rule**

### The Interface
```java
@FunctionalInterface
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
```

**Parameters:**
- `root` - The entity you're querying
- `query` - The overall query
- `cb` - Tool to build conditions (CriteriaBuilder)

### Simple Example
```java
// A specification
public static Specification<Product> priceLessThan(BigDecimal maxPrice) {
    return (root, query, cb) -> 
        cb.lessThan(root.get("price"), maxPrice);
}

// Use it
List<Product> products = productRepo.findAll(priceLessThan(new BigDecimal("100")));
```

---

## JpaSpecificationExecutor

### Concept
An interface that adds specification query methods to your repository.

### Setup
```java
public interface ProductRepository extends JpaRepository<Product, Long>, 
                                           JpaSpecificationExecutor<Product> {
    // No methods needed - you get specification methods automatically
}
```

### Available Methods
```java
List<T> findAll(Specification<T> spec)
Optional<T> findOne(Specification<T> spec)
long count(Specification<T> spec)
Page<T> findAll(Specification<T> spec, Pageable pageable)
```

---

## Specification Pattern

### Concept
Separate query logic into **small, reusable, combinable** pieces.

### Benefits
1. **Reusability** - Write once, use everywhere
2. **Composability** - Combine with AND/OR/NOT
3. **Testability** - Test each piece independently
4. **Readability** - Clear business logic

### Combining Specifications
```java
// AND
Specification<Product> spec = Specification
    .where(hasCategory("Electronics"))
    .and(inStock());

// OR
spec = Specification
    .where(hasCategory("Electronics"))
    .or(hasCategory("Computers"));

// NOT
spec = Specification.not(isExpensive());
```

---

## Dynamic Query Building

### Concept
Build queries **at runtime** based on user input or conditions.

### The Problem
```java
// Need many methods for different combinations
List<Product> findByCategory(String category);
List<Product> findByCategoryAndPrice(String category, BigDecimal price);
// ... dozens more
```

### The Solution
```java
// One method handles all combinations
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

### Key Pattern
1. Create empty list of predicates
2. Add predicates conditionally based on input
3. Combine all predicates with AND/OR
4. Return as specification

---

## Predicate & CriteriaBuilder

### What are Predicates?
A **Predicate** is a condition (true/false) in a WHERE clause.

```java
age > 18                    → Predicate
status = 'ACTIVE'           → Predicate
price BETWEEN 100 AND 500   → Predicate
```

### What is CriteriaBuilder?
**CriteriaBuilder** (`cb`) is a factory that creates predicates.

### Common Operations

#### Comparison
```java
cb.equal(x, y)              // x = y
cb.notEqual(x, y)           // x != y
cb.greaterThan(x, y)        // x > y
cb.lessThan(x, y)           // x < y
cb.between(x, min, max)     // x BETWEEN min AND max
```

#### Logical
```java
cb.and(p1, p2)              // p1 AND p2
cb.or(p1, p2)               // p1 OR p2
cb.not(p)                   // NOT p
```

#### String
```java
cb.like(x, pattern)         // LIKE
cb.lower(x)                 // LOWER(x)
```

#### Null
```java
cb.isNull(x)                // IS NULL
cb.isNotNull(x)             // IS NOT NULL
```

#### Collections
```java
x.in(values)                // IN (list)
```

---

## Best Practices

### 1. Always Handle Null Values
```java
public static Specification<Product> nameContains(String keyword) {
    return (root, query, cb) -> {
        if (keyword == null) return cb.conjunction(); // always true
        return cb.like(root.get("name"), "%" + keyword + "%");
    };
}
```

### 2. Create Small, Reusable Specifications
```java
// Small pieces
public static Specification<Product> isActive() {
    return (root, query, cb) -> cb.equal(root.get("active"), true);
}

public static Specification<Product> inStock() {
    return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
}

// Combine them
public static Specification<Product> available() {
    return Specification.where(isActive()).and(inStock());
}
```

### 3. Use Distinct with Fetch Joins
```java
public static Specification<Order> withItems() {
    return (root, query, cb) -> {
        root.fetch("items", JoinType.LEFT);
        query.distinct(true);  // Important: prevent duplicates
        return cb.conjunction();
    };
}
```

### 4. Keep Specifications Simple
Break complex logic into smaller, focused specifications.

---

## Quick Reference

### CriteriaBuilder Methods
```java
// Comparison
cb.equal(x, y)              cb.notEqual(x, y)
cb.greaterThan(x, y)        cb.lessThan(x, y)
cb.between(x, min, max)

// Logical
cb.and(p1, p2)              cb.or(p1, p2)              cb.not(p)

// String
cb.like(x, pattern)         cb.lower(x)

// Null
cb.isNull(x)                cb.isNotNull(x)

// Collections
x.in(values)
```

### Specification Combination
```java
Specification.where(spec1).and(spec2)       // AND
Specification.where(spec1).or(spec2)        // OR
Specification.not(spec)                     // NOT
```

### Repository Methods
```java
findAll(Specification<T> spec)
findOne(Specification<T> spec)
count(Specification<T> spec)
findAll(Specification<T> spec, Pageable pageable)
```

---

## Summary

### Core Concepts

**Criteria API** = Type-safe query building with Java code

**Specification** = Reusable filter rule (one condition or set of conditions)

**JpaSpecificationExecutor** = Interface that adds specification methods to repository

**CriteriaBuilder** = Factory for creating predicates (conditions)

**Predicate** = A WHERE clause condition (true/false)

### Key Benefits

1. **Type Safety** - Compiler checks instead of runtime errors
2. **Reusability** - Write once, use everywhere
3. **Composability** - Combine small specs into complex queries
4. **Dynamic** - Build queries based on runtime conditions
5. **Clean Code** - No need for dozens of repository methods

### When to Use

| Scenario | Solution |
|----------|----------|
| Simple static query | Method name or @Query |
| 2+ dynamic filters | Specification |
| Complex search with many optional filters | Specification |
| Reusable query logic | Specification |

### The Basic Flow

1. **Define specifications** - One per filter condition
2. **Combine specifications** - Use AND/OR/NOT
3. **Execute query** - Call repository with combined spec
4. **Get results** - Type-safe, filtered data

---

**Remember:** Specifications = Clean, Reusable, Type-Safe Dynamic Queries! 🎯
