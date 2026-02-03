# Query by Example (QBE) - Concepts

## Table of Contents
- [What is Query by Example?](#what-is-query-by-example)
- [Example Class](#example-class)
- [ExampleMatcher](#examplematcher)
- [Limitations of QBE](#limitations-of-qbe)
- [QBE vs Specifications](#qbe-vs-specifications)
- [Quick Reference](#quick-reference)

---

## What is Query by Example?

### Concept
**Query by Example (QBE)** lets you search for entities by providing an **example entity** with the values you want to match.

Think of it as: **"Find me records that look like this example"**

### How It Works
1. Create an instance of your entity
2. Set the fields you want to search by
3. Use it as a search template

### Simple Example
```java
// Create an example entity
Product probe = new Product();
probe.setCategory("Electronics");
probe.setPrice(new BigDecimal("100"));

// Search for matching products
Example<Product> example = Example.of(probe);
List<Product> results = productRepo.findAll(example);
// Finds all products where category = 'Electronics' AND price = 100
```

### Key Benefits
- **No need to write queries** - Just set fields
- **Type-safe** - Uses actual entity objects
- **Simple for basic searches** - Very intuitive
- **Less code** - No specifications or criteria API needed

---

## Example Class

### Concept
The `Example` class wraps your probe entity and optional matching rules.

### Basic Usage
```java
// 1. Create probe (example entity)
User probe = new User();
probe.setUsername("john");
probe.setStatus("ACTIVE");

// 2. Create Example
Example<User> example = Example.of(probe);

// 3. Use in repository
List<User> users = userRepo.findAll(example);
// SELECT * FROM users WHERE username = 'john' AND status = 'ACTIVE'
```

### With ExampleMatcher
```java
// Create Example with custom matching rules
Example<User> example = Example.of(probe, matcher);
```

### Repository Support
Your repository must extend `QueryByExampleExecutor`:

```java
public interface ProductRepository extends JpaRepository<Product, Long>,
                                           QueryByExampleExecutor<Product> {
    // Gets these methods automatically:
    // findAll(Example<T> example)
    // findOne(Example<T> example)
    // count(Example<T> example)
    // exists(Example<T> example)
}
```

---

## ExampleMatcher

### Concept
**ExampleMatcher** defines **how to match** fields in your probe entity.

### Default Behavior (Without Matcher)
- All non-null fields must match (AND logic)
- Exact matching
- Null fields are ignored

### Creating Matchers

#### 1. Match All Fields
```java
ExampleMatcher matcher = ExampleMatcher.matchingAll();
// All non-null fields must match (AND)
```

#### 2. Match Any Field
```java
ExampleMatcher matcher = ExampleMatcher.matchingAny();
// Any non-null field can match (OR)
```

#### 3. Custom Matching
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withIgnoreCase()                              // Case-insensitive
    .withStringMatcher(StringMatcher.CONTAINING)   // LIKE %value%
    .withIgnorePaths("id", "createdDate");         // Ignore these fields
```

### String Matching Options

| StringMatcher | SQL Equivalent | Example |
|---------------|----------------|---------|
| **EXACT** | = 'value' | name = 'John' |
| **STARTING** | LIKE 'value%' | name LIKE 'John%' |
| **ENDING** | LIKE '%value' | name LIKE '%John' |
| **CONTAINING** | LIKE '%value%' | name LIKE '%John%' |

### Common Configurations

#### Case-Insensitive Search
```java
Product probe = new Product();
probe.setName("laptop");

ExampleMatcher matcher = ExampleMatcher.matching()
    .withIgnoreCase();

Example<Product> example = Example.of(probe, matcher);
// Matches: "Laptop", "LAPTOP", "laptop"
```

#### Partial String Matching
```java
Product probe = new Product();
probe.setName("phone");

ExampleMatcher matcher = ExampleMatcher.matching()
    .withStringMatcher(StringMatcher.CONTAINING);

Example<Product> example = Example.of(probe, matcher);
// Matches: "iPhone", "Android Phone", "Smartphone"
```

#### Per-Field Matching
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withMatcher("name", match -> match.contains().ignoreCase())
    .withMatcher("category", match -> match.exact())
    .withIgnorePaths("price", "stock");

// name: case-insensitive partial match
// category: exact match
// price & stock: ignored
```

#### Ignore Null Values (Default)
```java
Product probe = new Product();
probe.setCategory("Electronics");
probe.setPrice(null);  // Will be ignored

Example<Product> example = Example.of(probe);
// Only filters by category, price is not in WHERE clause
```

#### Include Null Values
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withIncludeNullValues();

Product probe = new Product();
probe.setDescription(null);

Example<Product> example = Example.of(probe, matcher);
// WHERE description IS NULL
```

---

## Limitations of QBE

### What QBE Cannot Do

#### 1. No Range Queries
```java
// ❌ CANNOT do: price > 100 or price BETWEEN 100 AND 500
// ✅ CAN only do: price = 100 (exact match)
```

#### 2. No OR Conditions (Across Different Fields)
```java
// ❌ CANNOT do: category = 'Electronics' OR category = 'Computers'
// ✅ CAN only do AND across fields (unless using matchingAny for all fields)
```

#### 3. No Nested Properties (in some cases)
```java
// Limited support for @ManyToOne, @OneToOne
// No support for @OneToMany, @ManyToMany collections
```

#### 4. No Complex Conditions
```java
// ❌ CANNOT do: (price < 100 AND stock > 0) OR featured = true
```

#### 5. No Joins with Filtering
```java
// Limited ability to filter by nested entity properties
```

#### 6. No Aggregations
```java
// ❌ CANNOT do: COUNT, SUM, AVG, GROUP BY
```

#### 7. Primitive Types Always Included
```java
Product probe = new Product();
probe.setStock(0);  // This will be included in query!

// For int, long, boolean, etc., you cannot make them "null"
// Zero values are included in the query
```

### Workarounds

**For primitives:** Use wrapper classes
```java
// Instead of: int stock
// Use: Integer stock (can be null)
```

**For ranges/complex queries:** Use Specifications or @Query

---

## QBE vs Specifications

### When to Use QBE

✅ **Use QBE When:**
- Simple exact matching
- All filters are optional
- Searching by entity properties
- Quick prototyping
- Form-based searches with simple criteria

**Example Scenarios:**
- Search users by username, email, or status
- Filter products by category and brand
- Find orders by customer name or order status

### When to Use Specifications

✅ **Use Specifications When:**
- Need range queries (>, <, BETWEEN)
- Need OR conditions across different fields
- Complex filtering logic
- Need to filter by nested/joined entities
- Need dynamic sorting
- Performance optimization needed

**Example Scenarios:**
- Price range filters (minPrice to maxPrice)
- Date range queries
- Complex multi-table joins
- Advanced search with many optional criteria

### Comparison Table

| Feature | QBE | Specifications |
|---------|-----|----------------|
| **Exact Matching** | ✅ Easy | ✅ Possible |
| **Range Queries** | ❌ No | ✅ Yes |
| **OR Conditions** | ⚠️ Limited | ✅ Full support |
| **Complexity** | Simple | Complex |
| **Code Amount** | Less | More |
| **Type Safety** | ✅ Yes | ✅ Yes |
| **Learning Curve** | Easy | Moderate |
| **Flexibility** | Limited | Very flexible |
| **Performance** | Good | Good |

### Hybrid Approach
You can use both in the same application:
- QBE for simple searches
- Specifications for complex queries

---

## Quick Reference

### Basic QBE Usage
```java
// 1. Create probe
Product probe = new Product();
probe.setCategory("Electronics");

// 2. Create example
Example<Product> example = Example.of(probe);

// 3. Search
List<Product> results = productRepo.findAll(example);
```

### With ExampleMatcher
```java
ExampleMatcher matcher = ExampleMatcher.matching()
    .withIgnoreCase()
    .withStringMatcher(StringMatcher.CONTAINING)
    .withIgnorePaths("id", "createdDate");

Example<Product> example = Example.of(probe, matcher);
List<Product> results = productRepo.findAll(example);
```

### Repository Methods
```java
// QueryByExampleExecutor provides:
<S extends T> Optional<S> findOne(Example<S> example)
<S extends T> Iterable<S> findAll(Example<S> example)
<S extends T> Page<S> findAll(Example<S> example, Pageable pageable)
<S extends T> long count(Example<S> example)
<S extends T> boolean exists(Example<S> example)
```

### ExampleMatcher Methods
```java
// Global settings
.withIgnoreCase()
.withIgnoreNullValues()
.withIncludeNullValues()
.withStringMatcher(StringMatcher.CONTAINING)

// Per-field settings
.withMatcher("fieldName", matcher -> matcher.exact())
.withMatcher("fieldName", matcher -> matcher.contains())
.withMatcher("fieldName", matcher -> matcher.startsWith())
.withMatcher("fieldName", matcher -> matcher.endsWith())
.withMatcher("fieldName", matcher -> matcher.ignoreCase())

// Ignore fields
.withIgnorePaths("field1", "field2")
```

---

## Summary

### Core Concepts

**Query by Example (QBE)** = Search by providing an example entity with desired values

**Example** = Wrapper containing your probe entity and matching rules

**ExampleMatcher** = Defines how to match fields (exact, contains, ignore case, etc.)

**Probe** = Your entity instance with search values set

### Key Points

1. **Simple to Use** - Just set entity fields and search
2. **Limited Power** - Only exact matching and simple string operations
3. **No Ranges** - Cannot do >, <, BETWEEN
4. **Best for Forms** - Perfect for simple search forms
5. **Use Specifications for Complex Queries** - When QBE isn't enough

### The Decision Tree

```
Need range queries (price > X)?
├─ YES → Use Specifications
└─ NO → Continue

Need OR conditions across fields?
├─ YES → Use Specifications  
└─ NO → Continue

Need complex joins/filters?
├─ YES → Use Specifications
└─ NO → Use QBE ✓
```

### Typical Use Case

**Perfect for:** Search forms with optional filters
```java
// User fills out form (some fields empty)
ProductSearchForm form = new ProductSearchForm();
form.setCategory("Electronics");  // User filled this
form.setName("laptop");           // User filled this
form.setBrand(null);              // User left empty

// Convert to probe
Product probe = new Product();
probe.setCategory(form.getCategory());
probe.setName(form.getName());
probe.setBrand(form.getBrand());  // null - will be ignored

// Search
Example<Product> example = Example.of(probe, matcher);
List<Product> results = productRepo.findAll(example);
```

---

**Remember:** QBE = Simple searches made easy! For complex queries, use Specifications. 🎯
