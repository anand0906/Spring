## Table of Contents

1. [Data Persistence Overview](#data-persistence-overview)
2. [I/O Streams](#io-streams)
3. [JDBC (Java Database Connectivity)](#jdbc-java-database-connectivity)
4. [ORM (Object-Relational Mapping)](#orm-object-relational-mapping)
5. [Object-Relational Impedance Mismatch](#object-relational-impedance-mismatch)
6. [Hibernate](#hibernate)
7. [Spring Data](#spring-data)
8. [Spring Repository Architecture](#spring-repository-architecture)
9. [Spring Data Annotations](#spring-data-annotations)
10. [Derived Query Methods](#derived-query-methods)
11. [Pagination and Sorting](#pagination-and-sorting)
12. [Projections](#projections)

---

## Data Persistence Overview

**Data persistence** is the process of storing data in external storage systems.

### Three Main Components

1. **What to store?** → Raw data collected from files or objects in structured form
2. **Where to store?** → SSD, HardDrive, Databases, Cloud
3. **How to store?** → Various techniques and frameworks

### Java Persistence Methods

1. **I/O Streams** - For file-based storage
2. **JDBC** - For database connectivity
3. **ORM Frameworks** - For object-relational mapping

---

## I/O Streams

I/O Streams are useful when you want to store/retrieve raw data from files.

### Types of Streams

- **InputStream/OutputStream** → For byte data
- **FileReader/FileWriter** → For character data

### Serialization Example

```java
// Making a class serializable
public class Person implements Serializable {
    private String name;
    private int age;
    // Constructor, getters, setters
}

// Serializing an object
ObjectOutputStream out = new ObjectOutputStream(
    new FileOutputStream("person.dat")
);
out.writeObject(person);

// Deserializing an object
ObjectInputStream in = new ObjectInputStream(
    new FileInputStream("person.dat")
);
Person person = (Person) in.readObject();
```

---

## JDBC (Java Database Connectivity)

**JDBC** is a Java API for storing/retrieving data from databases. It allows you to connect and query databases.

### Example

```java
// Establishing connection
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/mydb", 
    "username", 
    "password"
);

// Executing query
PreparedStatement stmt = conn.prepareStatement(
    "SELECT * FROM users WHERE id = ?"
);
stmt.setInt(1, userId);
ResultSet rs = stmt.executeQuery();
```

---

## ORM (Object-Relational Mapping)

**ORM** is a programming technique for connecting databases and performing operations using object-oriented programming.

### Mapping Structure

| OOP Concept | Database Concept |
|-------------|------------------|
| Class       | Table            |
| Object      | Row              |
| Attribute   | Column           |

### JPA (Java Persistence API)

**JPA** is the standard ORM specification in Java. Every Java ORM implementation must follow JPA specifications.

**Popular Implementations:**
- Hibernate
- OpenJPA
- EclipseLink

---

## Object-Relational Impedance Mismatch

When mapping object-oriented programming with database systems, several mismatches occur:

### 1. Identity Mismatch

| OOP | DBMS |
|-----|------|
| Uses `==` or `equals()` to check equality | Uses primary keys |

### 2. Navigation Mismatch

| OOP | DBMS |
|-----|------|
| Uses `.` (dot) notation to navigate objects | Uses JOINs to navigate tables |

### 3. Association

| OOP | DBMS |
|-----|------|
| Uses object references for relationships | Uses foreign keys |

### 4. Inheritance

| OOP | DBMS |
|-----|------|
| Supports inheritance | Does not support inheritance |

---

## Hibernate

**Hibernate** is a Java-based ORM that implements JPA. It connects to databases and performs operations using Java OOP without writing SQL queries manually.

### Advantages

- No need to write manual SQL queries
- Built-in support for caching and optimizations
- Works with any database
- Easier to maintain

---

## Spring Data

**Spring Data** is part of the Spring Framework for developing data access layers easily by providing a consistent, abstraction-based way to work with any data store.

### Key Feature

Define interfaces only - implementation is done automatically by Spring Data, reducing boilerplate code.

### Architecture Flow

```
Controller
   ↓
Service
   ↓
Spring Data Repository (interface)
   ↓
Spring Data Implementation (auto-generated)
   ↓
ORM / Driver (Hibernate, Mongo driver, etc.)
   ↓
Database
```

### Spring Data Modules

| Module | Purpose |
|--------|---------|
| Spring Data JPA | Relational databases |
| Spring Data MongoDB | NoSQL (MongoDB) |
| Spring Data Redis | Redis in-memory store |
| Spring Data JDBC | JDBC operations |

### Important Clarification

❌ **Spring Data is NOT an ORM**  
✅ **Spring Data is a data access abstraction**

- **ORM** = Hibernate
- **Abstraction** = Spring Data

---

## Spring Repository Architecture

Spring Data provides a hierarchical repository structure:

```
Repository (marker interface)
   ↓
CrudRepository
   ↓
PagingAndSortingRepository
   ↓
JpaRepository
```

### 1. Repository

Marker interface to identify repositories.

```java
public interface Repository<T, ID> {}
```

### 2. CrudRepository

Provides basic CRUD operations.

```java
public interface CrudRepository<T, ID> extends Repository<T, ID>
```

**Methods:**
- `save()`
- `findById()`
- `findAll()`
- `deleteById()`
- `count()`
- `existsById()`

**Use when:** Basic CRUD operations are enough, no pagination or sorting required.

### 3. PagingAndSortingRepository

Adds pagination and sorting capabilities.

```java
public interface PagingAndSortingRepository<T, ID>
```

**Additional Methods:**
- `findAll(Pageable pageable)`
- `findAll(Sort sort)`

**Use when:** Working with large datasets requiring pagination or sorting.

### 4. JpaRepository

Most feature-rich repository interface.

```java
public interface JpaRepository<T, ID>
```

**Additional Methods:**
- `flush()`
- `saveAndFlush()`
- `deleteInBatch()`
- `findAll()` (returns List)
- Batch operations

**Use when:** Building real-world applications with Spring Data JPA.

### Usage Example

```java
// Simple repository interface
public interface UserRepository extends JpaRepository<User, Long> {
    // Inherits all CRUD operations
    // Custom methods can be added here
}

// Custom base repository
@NoRepositoryBean
interface MyBaseRepository<T, ID> extends Repository<T, ID> {
    Optional<T> findById(ID id);
    <S extends T> S save(S entity);
}

interface UserRepository extends MyBaseRepository<User, Long> {
    User findByEmailAddress(String email);
}
```

---

## Spring Data Annotations

### Field Access vs Property Access (IMPORTANT)

#### 🔹 Field Access (Most Common ✅)

Annotations on fields.

```java
@Entity
public class User {
    @Id
    private Long id;
    
    private String name;
}
```

✔ JPA accesses fields directly  
✔ Getters/setters not required

#### 🔹 Property Access

Annotations on getters.

```java
@Entity
public class User {
    private Long id;
    
    @Id
    public Long getId() {
        return id;
    }
}
```

✔ JPA uses getter/setter  
❌ Mixing both causes bugs

**Rule:**
- If `@Id` is on field → field access
- If `@Id` is on getter → property access

### @Column - Column Mapping

Customize column details.

```java
@Column(
    name = "email_address",
    nullable = false,
    unique = true,
    length = 100
)
private String email;
```

**Common Attributes:**
- `name` → column name
- `nullable` → NOT NULL constraint
- `unique` → UNIQUE constraint
- `length` → column length

If not used, column name = field name.

### Primary Key Generation Strategies

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

| Strategy | Meaning |
|----------|---------|
| AUTO | JPA decides |
| IDENTITY | DB auto-increment |
| SEQUENCE | DB sequence |
| TABLE | Separate ID table |

**Most common:** `IDENTITY`

### Relationships

#### @OneToOne

**Example:** User ↔ Profile (One user has one profile)

```java
@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;
}
```

Foreign key stored in `users.profile_id`.

#### @ManyToOne

**Example:** Many Orders → One Customer

```java
@Entity
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

- Many orders belong to one customer
- Foreign key in orders table
- **Most commonly used relationship**

#### @OneToMany

**Example:** One Customer → Many Orders

```java
@Entity
public class Customer {
    @Id
    @GeneratedValue
    private Long id;
    
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;
}
```

**`mappedBy` means:** "Foreign key is managed by the Order entity"  
**Note:** No extra column created here

#### @ManyToMany

**Example:** Students ↔ Courses (Many students → many courses)

```java
@Entity
public class Student {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
}
```

Creates join table:
```
student_course
  - student_id
  - course_id
```

### @JoinColumn vs @JoinTable

#### @JoinColumn

**Used when:** Relationship stored via foreign key

**Applies to:**
- One-to-One
- Many-to-One

```java
@JoinColumn(name = "user_id")
```

➡️ Adds column in table

#### @JoinTable

**Used when:** Many-to-Many or custom join table needed

```java
@JoinTable(name = "user_role")
```

➡️ Creates separate table

---

## Derived Query Methods

**Derived query methods** are repository methods where Spring Data generates the query automatically from the method name.

**Benefits:**
- No SQL
- No JPQL
- Method name = query logic

### Basic Keywords: findBy, readBy, getBy

All three are functionally identical.

```java
findByEmail(String email)
readByEmail(String email)
getByEmail(String email)
```

✔ `findBy` is most commonly used  
✔ `getBy` may throw exception if not found (provider-specific)

### Logical Keywords

#### And

```java
findByEmailAndStatus(String email, String status)
```

➡️ `WHERE email = ? AND status = ?`

#### Or

```java
findByEmailOrUsername(String email, String username)
```

➡️ `WHERE email = ? OR username = ?`

### Comparison Keywords

#### Between

```java
findByAgeBetween(int min, int max)
```

➡️ `age BETWEEN min AND max`

#### LessThan / GreaterThan

```java
findBySalaryGreaterThan(double amount)
findByAgeLessThan(int age)
```

➡️ `>` and `<`

#### LessThanEqual / GreaterThanEqual

```java
findByAgeGreaterThanEqual(int age)
```

### Pattern Matching Keywords

#### Like

```java
findByNameLike(String pattern)
```

**Usage:** `findByNameLike("%john%")`  
➡️ SQL `LIKE`

#### Containing (Most Used 🔥)

```java
findByNameContaining("john")
```

➡️ Automatically adds `%john%`  
✔ Cleaner than `Like`

#### Other Variants

- `StartingWith`
- `EndingWith`
- `IgnoreCase`

**Example:**

```java
findByNameStartingWithIgnoreCase("jo")
```

### Collection Keywords

#### In

```java
findByStatusIn(List<String> statuses)
```

➡️ `WHERE status IN (...)`

#### NotIn

```java
findByIdNotIn(List<Long> ids)
```

### Null Checks

#### IsNull

```java
findByDeletedAtIsNull()
```

➡️ `WHERE deleted_at IS NULL`

#### IsNotNull

```java
findByEmailIsNotNull()
```

### Sorting in Method Names

**Syntax:** `OrderBy<Field><Asc|Desc>`

**Examples:**

```java
findByStatusOrderByCreatedAtDesc(String status)
findByAgeGreaterThanOrderByNameAsc(int age)
```

➡️ Adds `ORDER BY` automatically

**Best practice:** Use `Sort` parameter instead of long method names.

### Pagination in Query Methods

#### Pageable Parameter

```java
Page<User> findByStatus(String status, Pageable pageable);
```

**Usage:** `PageRequest.of(page, size)`

#### Return Types

| Return Type | Meaning |
|-------------|---------|
| `Page<T>` | Total count + data |
| `Slice<T>` | Next page info |
| `List<T>` | No pagination info |

**Interview Tip:**
- Use `Slice` for performance
- Use `Page` when total count needed

### Combined Example (Real-World)

```java
Page<User> findByStatusAndAgeGreaterThanOrderByCreatedAtDesc(
    String status,
    int age,
    Pageable pageable
);
```

➡️ Complex query  
➡️ Zero SQL  
➡️ Fully readable

### Common Rules (IMPORTANT ⚠️)

✔ Property names must match entity fields  
✔ Method name is case-sensitive logically  
✔ Use `@Query` if method name becomes ugly  
✔ Avoid very long method names

---

## Pagination and Sorting

### Why Pagination & Sorting Matter

**Without pagination:**
- Loads too much data
- Slow responses
- High memory usage

Spring Data provides built-in pagination & sorting with almost no code.

### Return Types: Page, Slice, List

#### List<T>

```java
List<User> findByStatus(String status);
```

✔ Simple list  
❌ No pagination metadata  
❌ Loads all matching rows

**Use only for small datasets**

#### Page<T>

```java
Page<User> findByStatus(String status, Pageable pageable);
```

**Contains:**
- Data (`getContent()`)
- Total elements
- Total pages
- Page number
- Page size

✔ Full pagination info  
❌ Executes extra count query

#### Slice<T>

```java
Slice<User> findByStatus(String status, Pageable pageable);
```

**Contains:**
- Data
- `hasNext()`

✔ No count query  
✔ Faster  
❌ No total pages info

### Pageable (The Pagination Contract)

**Pageable defines:**
- Page number
- Page size
- Sorting

```java
Pageable pageable = PageRequest.of(0, 10);
```

**Note:** Page index is 0-based.

### PageRequest (Most Used Implementation)

```java
PageRequest.of(page, size)
```

**With sorting:**

```java
PageRequest.of(0, 10, Sort.by("createdAt").descending())
```

➡️ Page 0  
➡️ 10 records  
➡️ Ordered by `createdAt DESC`

### Sort (Sorting Data)

**Simple sort:**

```java
Sort.by("name")
```

**Descending:**

```java
Sort.by("name").descending()
```

**Multiple fields:**

```java
Sort.by("status").and(Sort.by("createdAt").descending())
```

### Pagination in Repository Methods

```java
Page<User> findByStatus(String status, Pageable pageable);
```

**Spring automatically:**
- Applies `LIMIT`
- Applies `OFFSET`
- Applies `ORDER BY`

**You write no SQL**

### Page vs Slice Comparison (Interview Favorite 🔥)

| Feature | Page | Slice |
|---------|------|-------|
| Total count | ✅ Yes | ❌ No |
| Total pages | ✅ Yes | ❌ No |
| `hasNext()` | ✅ Yes | ✅ Yes |
| Extra count query | ❌ Yes | ✅ No |
| Performance | Slower | Faster |

**Key takeaway:** `Page` runs two queries, `Slice` runs one query

### Performance Considerations (IMPORTANT ⚠️)

#### 🔴 Avoid Page When:

- Large datasets
- Complex joins
- Infinite scrolling

#### ✅ Prefer Slice When:

- Scrolling / "Load more"
- APIs
- Performance matters

#### 🔴 Avoid Huge Page Sizes

❌ Page size > 1000

✔ **Recommended:** 10–100

#### 🔴 Always Sort Large Queries

**Unsorted pagination can cause:**
- Duplicate records
- Missing records

**Best practice:** `Sort.by("id")`

#### 🔴 Offset Pagination Problem

`OFFSET` becomes slower for large page numbers.

**Solutions:**
- Keyset pagination
- Use `WHERE id > lastSeenId`

### Controller-Level Pagination (Real Life)

Spring automatically maps request params:

```
GET /users?page=0&size=10&sort=name,desc
```

No manual parsing required.

### Common Interview Traps ❌

❌ Using `List` for large tables  
❌ Using `Page` everywhere  
❌ Forgetting sort  
❌ Huge page sizes  
❌ Pagination without indexes

---

## Projections

**Projections** let you fetch only the required fields from the database instead of loading the entire entity.

**Benefits:**
- Less data
- Faster queries
- Better performance

### Why Projections Matter (Real Reason 🔥)

**Scenario:** User entity has:
- id, name, email, password, address, roles, audit fields…

**But your API only needs:** id + name

**Without projections:** ❌ Loads everything  
**With projections:** ✅ Loads only what you need

### 1. Interface-Based Projections (Most Common ✅)

An interface with getter methods for required fields.

```java
public interface UserView {
    Long getId();
    String getName();
}
```

**Repository:**

```java
List<UserView> findByStatus(String status);
```

➡️ Spring generates a query like:

```sql
SELECT id, name FROM users WHERE status = ?
```

**✅ Pros:**
- No implementation needed
- Clean & readable
- Best performance
- Most commonly used

**❌ Cons:**
- Read-only
- Limited logic

### 2. Class-Based (DTO) Projections

A DTO class with a constructor.

```java
public class UserDTO {
    private Long id;
    private String name;

    public UserDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

**Repository:**

```java
@Query("SELECT new com.example.UserDTO(u.id, u.name) FROM User u")
List<UserDTO> findActiveUsers();
```

**✅ Pros:**
- Custom logic allowed
- Immutable DTOs
- Good for APIs

**❌ Cons:**
- More boilerplate
- Needs JPQL
- Constructor must match exactly

### 3. Open vs Closed Projections (Interview Favorite 🔥)

#### 🔹 Closed Projections (Default & Faster ✅)

Only entity fields.

```java
public interface UserView {
    String getName();
}
```

✔ Uses SQL projection  
✔ Best performance

#### 🔹 Open Projections (Computed Fields)

Uses SpEL expressions.

```java
public interface UserView {
    String getName();

    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();
}
```

❌ Fetches full entity  
❌ Slower

**Rule to remember:**
- **Open projection** = entity loaded
- **Closed projection** = partial select

### 4. Nested Projections (Very Important)

Used when entity has relationships.

**Example:** User → Address

```java
public interface UserView {
    String getName();
    AddressView getAddress();
}

public interface AddressView {
    String getCity();
}
```

➡️ Fetches:
- User name
- Address city

✔ Clean  
✔ Avoids loading full objects

**Works best with interface-based projections**

