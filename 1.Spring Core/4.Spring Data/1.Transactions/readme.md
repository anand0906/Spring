# Transactions - Complete Guide

## Table of Contents
- [What is a Transaction?](#what-is-a-transaction)
- [@Transactional Annotation](#transactional-annotation)
- [Propagation Types](#propagation-types)
- [Isolation Levels](#isolation-levels)
- [Read-Only Transactions](#read-only-transactions)
- [Transaction Boundaries in Repositories](#transaction-boundaries-in-repositories)
- [Rollback Rules](#rollback-rules)
- [Best Practices](#best-practices)
- [Common Pitfalls](#common-pitfalls)

---

## What is a Transaction?

### Definition
A transaction is a **sequence of operations** performed as a **single logical unit of work**. Either ALL operations succeed, or ALL fail together.

### Real-Life Example
Think of transferring money from your bank account:
1. Deduct $100 from your account
2. Add $100 to friend's account

**Without Transaction:** If step 1 succeeds but step 2 fails, you lose $100!  
**With Transaction:** If step 2 fails, step 1 is also cancelled (rolled back).

### ACID Properties

| Property | Meaning | Example |
|----------|---------|---------|
| **Atomicity** | All or nothing | Money transfer: both debit and credit must happen, or neither |
| **Consistency** | Data remains valid | Total money in bank remains same before/after transfer |
| **Isolation** | Transactions don't interfere | Two people can't withdraw last $100 simultaneously |
| **Durability** | Changes are permanent | Once confirmed, transaction survives system crash |

---

## @Transactional Annotation

### Basic Usage

```java
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    // Method-level transaction
    @Transactional
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        Account fromAccount = accountRepository.findById(fromId).orElseThrow();
        Account toAccount = accountRepository.findById(toId).orElseThrow();
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        // If any exception occurs, BOTH saves are rolled back
    }
}
```

### Class-Level Transaction

```java
@Service
@Transactional  // Applies to ALL public methods in this class
public class OrderService {
    
    public void createOrder(Order order) {
        // Automatically transactional
    }
    
    public void updateOrder(Order order) {
        // Automatically transactional
    }
    
    @Transactional(readOnly = true)  // Override for specific method
    public Order getOrder(Long id) {
        // Read-only transaction
    }
}
```

### Where to Use @Transactional

✅ **Use on:**
- Service layer methods (recommended)
- Repository custom methods
- Methods that modify multiple entities

❌ **Don't use on:**
- Private methods (won't work!)
- Controller methods (wrong layer)
- Simple read operations (unless needed)

### Simple Example

```java
@Service
public class StudentService {
    
    @Autowired
    private StudentRepository studentRepo;
    
    @Autowired
    private EnrollmentRepository enrollmentRepo;
    
    // WITHOUT @Transactional - DANGEROUS!
    public void enrollStudentBad(Long studentId, Long courseId) {
        Student student = studentRepo.findById(studentId).orElseThrow();
        Enrollment enrollment = new Enrollment(student, courseId);
        enrollmentRepo.save(enrollment);
        
        student.setEnrolledCourses(student.getEnrolledCourses() + 1);
        studentRepo.save(student);
        // If this fails, enrollment is saved but student count is wrong!
    }
    
    // WITH @Transactional - SAFE!
    @Transactional
    public void enrollStudentGood(Long studentId, Long courseId) {
        Student student = studentRepo.findById(studentId).orElseThrow();
        Enrollment enrollment = new Enrollment(student, courseId);
        enrollmentRepo.save(enrollment);
        
        student.setEnrolledCourses(student.getEnrolledCourses() + 1);
        studentRepo.save(student);
        // If this fails, enrollment is also rolled back!
    }
}
```

---

## Propagation Types

### What is Propagation?
Propagation defines **how transactions relate** when one transactional method calls another transactional method.

### All Propagation Types

#### 1. REQUIRED (Default)

**Meaning:** Use existing transaction, or create new one if none exists.

```java
@Service
public class OrderService {
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void placeOrder(Order order) {
        saveOrder(order);
        processPayment(order);  // Uses SAME transaction
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void processPayment(Order order) {
        // Joins parent transaction
    }
}
```

**Visual:**
```
placeOrder() starts Transaction-1
  └─ processPayment() joins Transaction-1
     └─ If processPayment() fails, placeOrder() also rolls back
```

#### 2. REQUIRES_NEW

**Meaning:** Always create a NEW transaction, suspend existing one.

```java
@Service
public class NotificationService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmail(String email, String message) {
        // Runs in its OWN transaction
        // Even if parent transaction fails, this email is still sent!
    }
}

@Service
public class OrderService {
    
    @Autowired
    private NotificationService notificationService;
    
    @Transactional
    public void placeOrder(Order order) {
        saveOrder(order);
        notificationService.sendEmail(order.getEmail(), "Order placed");
        // Email is sent in separate transaction
        throw new RuntimeException("Order failed");
        // Order rolls back, but email was already sent!
    }
}
```

**Visual:**
```
placeOrder() starts Transaction-1
  └─ sendEmail() starts Transaction-2 (independent)
     Transaction-2 commits immediately
Transaction-1 rolls back (doesn't affect Transaction-2)
```

**Use Case:** Audit logging, notifications that should succeed regardless.

#### 3. MANDATORY

**Meaning:** MUST have existing transaction, or throw exception.

```java
@Service
public class PaymentService {
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void processPayment(Order order) {
        // This method REQUIRES a transaction to already exist
    }
}

@Service
public class OrderService {
    
    @Autowired
    private PaymentService paymentService;
    
    // This will FAIL - no transaction exists
    public void badExample(Order order) {
        paymentService.processPayment(order);
        // Throws: No existing transaction found
    }
    
    // This works - transaction exists
    @Transactional
    public void goodExample(Order order) {
        paymentService.processPayment(order);  // OK!
    }
}
```

**Use Case:** Ensure method is never called outside transaction context.

#### 4. SUPPORTS

**Meaning:** Use transaction if exists, run without transaction if none exists.

```java
@Service
public class ReportService {
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public Report generateReport(Long id) {
        // Flexible: works with or without transaction
    }
}
```

**Use Case:** Methods that work fine with or without transactions.

#### 5. NOT_SUPPORTED

**Meaning:** Always run WITHOUT transaction, suspend existing one.

```java
@Service
public class CacheService {
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void clearCache() {
        // Runs outside any transaction
        // Cache operations don't need transactions
    }
}
```

**Use Case:** Operations that shouldn't be part of transaction (cache, logging).

#### 6. NEVER

**Meaning:** MUST NOT have transaction, throw exception if exists.

```java
@Service
public class ExternalApiService {
    
    @Transactional(propagation = Propagation.NEVER)
    public void callExternalApi() {
        // Throws exception if called within a transaction
    }
}
```

**Use Case:** Enforce that method runs outside transaction.

#### 7. NESTED

**Meaning:** Create nested transaction (savepoint) within existing transaction.

```java
@Service
public class OrderService {
    
    @Transactional
    public void placeOrder(Order order) {
        saveOrder(order);
        
        try {
            applyDiscount(order);  // Nested transaction
        } catch (Exception e) {
            // Discount failed, but order is still saved
        }
    }
    
    @Transactional(propagation = Propagation.NESTED)
    public void applyDiscount(Order order) {
        // Runs in nested transaction
        // Can rollback independently
    }
}
```

**Visual:**
```
placeOrder() - Transaction-1
  └─ applyDiscount() - Nested (savepoint)
     If nested fails → rollback to savepoint
     If parent fails → rollback everything
```

### Propagation Comparison Table

| Type | Existing Transaction | No Transaction | Use Case |
|------|---------------------|----------------|----------|
| **REQUIRED** | Join it | Create new | Default, most common |
| **REQUIRES_NEW** | Suspend, create new | Create new | Independent operations |
| **MANDATORY** | Join it | Throw exception | Must have transaction |
| **SUPPORTS** | Join it | Run without | Optional transaction |
| **NOT_SUPPORTED** | Suspend it | Run without | Non-transactional ops |
| **NEVER** | Throw exception | Run without | Must be non-transactional |
| **NESTED** | Create nested | Create new | Partial rollback |

---

## Isolation Levels

### What is Isolation?
Isolation determines **how transaction changes are visible** to other concurrent transactions.

### Common Problems Isolation Solves

#### 1. Dirty Read
Reading data that another transaction hasn't committed yet.

```java
// Transaction 1
@Transactional
public void updateBalance() {
    account.setBalance(1000);  // Not committed yet
}

// Transaction 2 - might read 1000 (dirty read)
@Transactional
public void readBalance() {
    return account.getBalance();  // Reads uncommitted 1000
}
```

#### 2. Non-Repeatable Read
Reading same data twice in transaction gives different results.

```java
@Transactional
public void processOrder() {
    int stock = product.getStock();  // Reads 10
    
    // Another transaction updates stock to 5
    
    int stockAgain = product.getStock();  // Reads 5 (different!)
}
```

#### 3. Phantom Read
Query returns different rows in same transaction.

```java
@Transactional
public void generateReport() {
    List<Order> orders = orderRepo.findAll();  // Returns 10 orders
    
    // Another transaction adds new order
    
    List<Order> ordersAgain = orderRepo.findAll();  // Returns 11 orders
}
```

### Isolation Levels Explained

#### 1. READ_UNCOMMITTED (Lowest Isolation)

**Allows:** Dirty reads, non-repeatable reads, phantom reads  
**Performance:** Fastest  
**Safety:** Least safe

```java
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
    // Can read uncommitted changes from other transactions
    // VERY DANGEROUS - rarely used
}
```

**Example Problem:**
```
Transaction-A: Sets balance = 1000 (not committed)
Transaction-B: Reads balance = 1000 (DIRTY READ!)
Transaction-A: Rolls back
Transaction-B: Used wrong data!
```

#### 2. READ_COMMITTED (Default for most databases)

**Prevents:** Dirty reads  
**Allows:** Non-repeatable reads, phantom reads

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void processOrder() {
    // Can only read committed data
    // But data might change if you read it again
}
```

**Example:**
```
Transaction-A: Reads balance = 1000
Transaction-B: Updates balance = 500 and COMMITS
Transaction-A: Reads balance again = 500 (changed!)
```

#### 3. REPEATABLE_READ

**Prevents:** Dirty reads, non-repeatable reads  
**Allows:** Phantom reads

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void calculateTotal() {
    Product product = productRepo.findById(1L);
    int price = product.getPrice();  // 100
    
    // Another transaction changes price to 200
    
    product = productRepo.findById(1L);
    int priceAgain = product.getPrice();  // Still 100 (repeatable!)
}
```

**Example:**
```
Transaction-A: SELECT * FROM orders WHERE status = 'PENDING' (returns 5 rows)
Transaction-B: INSERT new pending order and COMMITS
Transaction-A: SELECT * FROM orders WHERE status = 'PENDING' (returns 6 rows - PHANTOM!)
```

#### 4. SERIALIZABLE (Highest Isolation)

**Prevents:** All problems (dirty, non-repeatable, phantom)  
**Performance:** Slowest  
**Safety:** Most safe

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void criticalOperation() {
    // Complete isolation - transactions run as if sequential
    // Safest but slowest
}
```

**Example:**
```
Transaction-A and Transaction-B cannot interfere at all
They execute as if one finished completely before the other started
```

### Isolation Level Comparison

| Level | Dirty Read | Non-Repeatable | Phantom | Performance | Use Case |
|-------|-----------|----------------|---------|-------------|----------|
| **READ_UNCOMMITTED** | ❌ Allowed | ❌ Allowed | ❌ Allowed | ⚡⚡⚡ Fastest | Approximate reports |
| **READ_COMMITTED** | ✅ Prevented | ❌ Allowed | ❌ Allowed | ⚡⚡ Fast | Default choice |
| **REPEATABLE_READ** | ✅ Prevented | ✅ Prevented | ❌ Allowed | ⚡ Slower | Financial calculations |
| **SERIALIZABLE** | ✅ Prevented | ✅ Prevented | ✅ Prevented | 🐌 Slowest | Critical operations |

### Real-World Example

```java
@Service
public class InventoryService {
    
    // Low isolation - approximate stock check
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int getApproximateStock(Long productId) {
        return productRepo.findById(productId).getStock();
    }
    
    // Medium isolation - price calculation
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal calculateOrderTotal(Long orderId) {
        Order order = orderRepo.findById(orderId);
        // Ensure prices don't change during calculation
        return order.getItems().stream()
            .map(item -> item.getPrice().multiply(item.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // High isolation - prevent overselling
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void purchaseLastItem(Long productId) {
        Product product = productRepo.findById(productId);
        if (product.getStock() == 1) {
            product.setStock(0);
            productRepo.save(product);
            // Ensures only ONE transaction can buy the last item
        }
    }
}
```

---

## Read-Only Transactions

### What is Read-Only?
Marks transaction as **only reading data**, not modifying it.

### Benefits

1. **Performance Optimization**
   - Database can optimize query execution
   - No need to maintain undo logs
   - Flush mode can be skipped

2. **Clear Intent**
   - Documents that method doesn't modify data
   - Helps prevent accidental modifications

3. **Database Optimizations**
   - Some databases provide read-only replicas
   - Can route to replica servers

### Basic Usage

```java
@Service
public class ProductService {
    
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepo.findById(id).orElseThrow();
    }
    
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepo.findByNameContaining(keyword);
    }
}
```

### Important Notes

```java
@Service
public class OrderService {
    
    // This will WORK but shouldn't - violates read-only!
    @Transactional(readOnly = true)
    public void updateOrderBad(Long id) {
        Order order = orderRepo.findById(id);
        order.setStatus("SHIPPED");
        orderRepo.save(order);
        // May work but unpredictable - don't do this!
    }
    
    // Correct - remove readOnly for modifications
    @Transactional
    public void updateOrderGood(Long id) {
        Order order = orderRepo.findById(id);
        order.setStatus("SHIPPED");
        orderRepo.save(order);
    }
}
```

### Combined with Other Attributes

```java
@Service
public class ReportService {
    
    @Transactional(
        readOnly = true,
        isolation = Isolation.READ_COMMITTED,
        timeout = 30
    )
    public Report generateMonthlyReport(int month, int year) {
        // Read-only
        // Allows some inconsistency (READ_COMMITTED)
        // Times out after 30 seconds
        List<Order> orders = orderRepo.findByMonthAndYear(month, year);
        return new Report(orders);
    }
}
```

### When to Use

✅ **Use for:**
- GET/fetch operations
- Search queries
- Report generation
- Dashboard data
- Analytics queries

❌ **Don't use for:**
- Create operations
- Update operations
- Delete operations
- Any data modification

---

## Transaction Boundaries in Repositories

### Default Repository Behavior

Spring Data JPA repositories have **built-in transaction management**:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Each method runs in its own transaction by default
    // If called outside @Transactional context
}
```

### Single Repository Method

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepo;
    
    // No @Transactional - each repo method gets its own transaction
    public void saveUser(User user) {
        userRepo.save(user);  // Transaction starts and ends here
    }
}
```

### Multiple Repository Calls - Problem

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepo;
    
    @Autowired
    private InventoryRepository inventoryRepo;
    
    // NO @Transactional - DANGER!
    public void placeOrderBad(Order order) {
        orderRepo.save(order);           // Transaction 1
        
        Inventory inv = inventoryRepo.findByProduct(order.getProduct());
        inv.decreaseStock(order.getQuantity());
        inventoryRepo.save(inv);         // Transaction 2
        
        // If this fails, order is saved but inventory is NOT updated!
    }
}
```

### Multiple Repository Calls - Solution

```java
@Service
public class OrderService {
    
    @Transactional  // ONE transaction for all operations
    public void placeOrderGood(Order order) {
        orderRepo.save(order);
        
        Inventory inv = inventoryRepo.findByProduct(order.getProduct());
        inv.decreaseStock(order.getQuantity());
        inventoryRepo.save(inv);
        
        // If ANY operation fails, ALL are rolled back
    }
}
```

### Custom Repository Methods

```java
@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional  // Need explicit transaction for custom methods
    public void batchUpdateUsers(List<User> users) {
        for (User user : users) {
            entityManager.merge(user);
        }
    }
}
```

### Transaction Boundaries Best Practices

```java
@Service
@Transactional  // Class-level: all methods are transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepo;
    
    @Autowired
    private CategoryRepository categoryRepo;
    
    // Inherits class-level @Transactional
    public void createProduct(Product product, Long categoryId) {
        Category category = categoryRepo.findById(categoryId);
        product.setCategory(category);
        productRepo.save(product);
        // One transaction for all operations
    }
    
    // Override for read-only
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepo.findById(id);
    }
}
```

### Nested Service Calls

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentService paymentService;
    
    @Transactional
    public void placeOrder(Order order) {
        saveOrder(order);
        paymentService.processPayment(order);  // Joins this transaction
    }
}

@Service
public class PaymentService {
    
    @Transactional(propagation = Propagation.REQUIRED)  // Joins parent
    public void processPayment(Order order) {
        // Runs in SAME transaction as placeOrder()
    }
}
```

### Repository Transaction Flow

```
WITHOUT @Transactional on Service:
    Service.method()
        ├─ Repository.save()     [Transaction-1]
        ├─ Repository.findById() [Transaction-2]
        └─ Repository.save()     [Transaction-3]
    Each operation is separate - RISKY!

WITH @Transactional on Service:
    Service.method()           [Transaction-1 START]
        ├─ Repository.save()     [Joins Transaction-1]
        ├─ Repository.findById() [Joins Transaction-1]
        └─ Repository.save()     [Joins Transaction-1]
                                [Transaction-1 COMMIT/ROLLBACK]
    All operations in ONE transaction - SAFE!
```

---

## Rollback Rules

### Default Rollback Behavior

By default, Spring rolls back transaction on **unchecked exceptions** only:

```java
@Service
public class PaymentService {
    
    @Transactional
    public void processPayment(Payment payment) {
        savePayment(payment);
        
        // RuntimeException - WILL ROLLBACK
        throw new RuntimeException("Payment failed");
    }
    
    @Transactional
    public void processPaymentChecked(Payment payment) throws Exception {
        savePayment(payment);
        
        // Checked Exception - WILL NOT ROLLBACK by default!
        throw new Exception("Payment failed");
    }
}
```

### Exception Types

```java
// Unchecked exceptions (rollback by default)
RuntimeException
  ├─ NullPointerException
  ├─ IllegalArgumentException
  ├─ IllegalStateException
  └─ Custom runtime exceptions

// Checked exceptions (NO rollback by default)
Exception
  ├─ IOException
  ├─ SQLException
  └─ Custom checked exceptions
```

### Custom Rollback Rules

#### Rollback on Specific Exceptions

```java
@Service
public class OrderService {
    
    // Rollback on checked exception
    @Transactional(rollbackFor = Exception.class)
    public void placeOrder(Order order) throws Exception {
        saveOrder(order);
        
        if (order.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Invalid order total");
            // Will rollback even though it's a checked exception
        }
    }
    
    // Rollback on specific exceptions
    @Transactional(rollbackFor = {
        PaymentFailedException.class,
        InsufficientStockException.class
    })
    public void processOrder(Order order) {
        // Will rollback on these specific exceptions
    }
}
```

#### No Rollback on Specific Exceptions

```java
@Service
public class NotificationService {
    
    // Don't rollback on specific exception
    @Transactional(noRollbackFor = EmailException.class)
    public void createOrderWithNotification(Order order) {
        orderRepo.save(order);  // Save order
        
        try {
            sendEmail(order.getCustomer().getEmail());
        } catch (EmailException e) {
            // Order is still committed even though exception occurred
            log.error("Email failed but order saved", e);
        }
    }
}
```

### Rollback vs. No Rollback

```java
@Service
public class UserService {
    
    // Example 1: Rollback on validation failure
    @Transactional(rollbackFor = ValidationException.class)
    public void registerUser(User user) throws ValidationException {
        if (!isValidEmail(user.getEmail())) {
            throw new ValidationException("Invalid email");
            // Rollback - user not saved
        }
        userRepo.save(user);
    }
    
    // Example 2: Don't rollback on logging failure
    @Transactional(noRollbackFor = LoggingException.class)
    public void updateUser(User user) {
        userRepo.save(user);  // Save user
        
        try {
            auditLog.log("User updated: " + user.getId());
        } catch (LoggingException e) {
            // User update is still committed
            // Logging failure doesn't affect user update
        }
    }
}
```

### Programmatic Rollback

```java
@Service
public class PaymentService {
    
    @Transactional
    public void processPayment(Payment payment) {
        try {
            paymentRepo.save(payment);
            callExternalPaymentGateway(payment);
        } catch (Exception e) {
            // Manually mark transaction for rollback
            TransactionAspectSupport.currentTransactionStatus()
                .setRollbackOnly();
            
            log.error("Payment failed, rolling back", e);
            // Transaction will rollback at method end
        }
    }
}
```

### Complex Rollback Scenario

```java
@Service
public class ComplexOrderService {
    
    @Transactional(
        rollbackFor = {PaymentException.class, StockException.class},
        noRollbackFor = NotificationException.class
    )
    public void placeComplexOrder(Order order) {
        // Step 1: Save order
        orderRepo.save(order);
        
        // Step 2: Process payment - ROLLBACK if fails
        try {
            processPayment(order);
        } catch (PaymentException e) {
            throw e;  // Rollback entire transaction
        }
        
        // Step 3: Update inventory - ROLLBACK if fails
        try {
            updateInventory(order);
        } catch (StockException e) {
            throw e;  // Rollback entire transaction
        }
        
        // Step 4: Send notification - NO ROLLBACK if fails
        try {
            sendNotification(order);
        } catch (NotificationException e) {
            log.warn("Notification failed, but order is still placed");
            // Transaction continues and commits
        }
    }
}
```

### Rollback Decision Table

| Exception Type | Default Behavior | Example |
|----------------|------------------|---------|
| RuntimeException | ✅ Rollback | `new NullPointerException()` |
| Error | ✅ Rollback | `new OutOfMemoryError()` |
| Checked Exception | ❌ No Rollback | `new IOException()` |
| Custom Runtime Exception | ✅ Rollback | `class MyException extends RuntimeException` |
| Custom Checked Exception | ❌ No Rollback | `class MyException extends Exception` |

### Real-World Example

```java
@Service
public class EcommerceService {
    
    @Transactional(
        rollbackFor = {
            InsufficientStockException.class,      // Rollback
            PaymentDeclinedException.class,        // Rollback
            InvalidCouponException.class           // Rollback
        },
        noRollbackFor = {
            EmailException.class,                  // Don't rollback
            SmsException.class,                    // Don't rollback
            AnalyticsException.class               // Don't rollback
        }
    )
    public Order purchaseProduct(Long userId, Long productId, String coupon) 
            throws InsufficientStockException, PaymentDeclinedException {
        
        // 1. Create order - must succeed
        Order order = createOrder(userId, productId);
        
        // 2. Apply coupon - rollback if invalid
        if (coupon != null) {
            applyCoupon(order, coupon);  // Throws InvalidCouponException
        }
        
        // 3. Check stock - rollback if insufficient
        checkStock(productId, order.getQuantity());  // Throws InsufficientStockException
        
        // 4. Process payment - rollback if declined
        processPayment(order);  // Throws PaymentDeclinedException
        
        // 5. Send email - don't rollback if fails
        try {
            sendOrderConfirmationEmail(order);
        } catch (EmailException e) {
            log.error("Email failed, but order is placed", e);
            // Order is still committed
        }
        
        // 6. Track analytics - don't rollback if fails
        try {
            trackPurchaseAnalytics(order);
        } catch (AnalyticsException e) {
            log.error("Analytics failed, but order is placed", e);
        }
        
        return order;
    }
}
```

---

## Best Practices

### 1. Transaction Scope

```java
// ❌ BAD - Transaction too broad
@Transactional
public void processOrderBad(Order order) {
    sendEmail();           // External call - slow
    validateOrder();       // Pure logic - no DB
    orderRepo.save(order); // Actual DB operation
    callExternalAPI();     // External call - slow
    // Transaction held for too long!
}

// ✅ GOOD - Transaction only where needed
public void processOrderGood(Order order) {
    sendEmail();           // Before transaction
    validateOrder();       // Before transaction
    
    saveOrderTransactional(order);  // Transactional method
    
    callExternalAPI();     // After transaction
}

@Transactional
private void saveOrderTransactional(Order order) {
    orderRepo.save(order);  // Only DB operation in transaction
}
```

### 2. Use Service Layer

```java
// ❌ BAD - Controller with @Transactional
@RestController
@Transactional  // Don't do this!
public class OrderController {
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody Order order) {
        return orderRepo.save(order);
    }
}

// ✅ GOOD - Service layer with @Transactional
@RestController
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
}

@Service
@Transactional
public class OrderService {
    
    public Order createOrder(Order order) {
        return orderRepo.save(order);
    }
}
```

### 3. Read-Only Optimization

```java
@Service
@Transactional  // Default for write operations
public class ProductService {
    
    // Override with readOnly for read operations
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepo.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }
    
    // Write operation uses class-level @Transactional
    public Product saveProduct(Product product) {
        return productRepo.save(product);
    }
}
```

### 4. Timeout Configuration

```java
@Service
public class ReportService {
    
    // Set timeout for long-running operations
    @Transactional(timeout = 60)  // 60 seconds
    public Report generateAnnualReport(int year) {
        // If takes more than 60 seconds, rollback
        return createReport(year);
    }
}
```

### 5. Isolation Level Selection

```java
@Service
public class ProductService {
    
    // Low isolation for reports - acceptable inconsistency
    @Transactional(
        readOnly = true,
        isolation = Isolation.READ_COMMITTED
    )
    public List<Product> getProductsForReport() {
        return productRepo.findAll();
    }
    
    // High isolation for critical operations
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void sellLastItem(Long productId) {
        Product product = productRepo.findById(productId);
        if (product.getStock() == 1) {
            product.setStock(0);
            productRepo.save(product);
        }
    }
}
```

### 6. Exception Handling

```java
@Service
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void placeOrder(Order order) throws Exception {
        try {
            validateOrder(order);
            orderRepo.save(order);
            processPayment(order);
        } catch (PaymentException e) {
            // Log but still rollback
            log.error("Payment failed for order: " + order.getId(), e);
            throw e;  // Re-throw to trigger rollback
        }
    }
}
```

### 7. Avoid Long Transactions

```java
// ❌ BAD - Long transaction
@Transactional
public void processLargeFileBad(File file) {
    List<String> lines = readFile(file);  // Slow I/O
    for (String line : lines) {
        processLine(line);  // DB operations
    }
    // Transaction held during entire file read!
}

// ✅ GOOD - Short transaction
public void processLargeFileGood(File file) {
    List<String> lines = readFile(file);  // Outside transaction
    processLinesInTransaction(lines);     // Quick transaction
}

@Transactional
private void processLinesInTransaction(List<String> lines) {
    for (String line : lines) {
        processLine(line);
    }
}
```

---

## Common Pitfalls

### 1. @Transactional on Private Methods

```java
@Service
public class UserService {
    
    // ❌ DOESN'T WORK - Private method
    @Transactional
    private void saveUserBad(User user) {
        userRepo.save(user);
        // @Transactional ignored on private methods!
    }
    
    // ✅ WORKS - Public method
    @Transactional
    public void saveUserGood(User user) {
        userRepo.save(user);
    }
}
```

**Why?** Spring uses proxies. Proxies can only intercept public methods.

### 2. Self-Invocation

```java
@Service
public class OrderService {
    
    // This will NOT be transactional!
    public void createOrder(Order order) {
        saveOrder(order);  // Calling own method directly
    }
    
    @Transactional
    public void saveOrder(Order order) {
        orderRepo.save(order);
        // Transaction doesn't work when called from createOrder()
    }
}
```

**Solution:**
```java
@Service
public class OrderService {
    
    @Autowired
    private OrderService self;  // Inject self
    
    public void createOrder(Order order) {
        self.saveOrder(order);  // Call through proxy
    }
    
    @Transactional
    public void saveOrder(Order order) {
        orderRepo.save(order);  // Now works!
    }
}
```

### 3. Catching Exceptions Without Re-throwing

```java
@Service
public class PaymentService {
    
    // ❌ BAD - Swallows exception
    @Transactional
    public void processPaymentBad(Payment payment) {
        try {
            paymentRepo.save(payment);
            externalService.charge(payment);
        } catch (Exception e) {
            log.error("Error", e);
            // Exception caught - NO ROLLBACK!
        }
    }
    
    // ✅ GOOD - Re-throws exception
    @Transactional
    public void processPaymentGood(Payment payment) {
        try {
            paymentRepo.save(payment);
            externalService.charge(payment);
        } catch (Exception e) {
            log.error("Error", e);
            throw e;  // Re-throw for rollback
        }
    }
}
```

### 4. Wrong Propagation

```java
@Service
public class AuditService {
    
    // ❌ BAD - Audit lost if parent transaction rolls back
    @Transactional(propagation = Propagation.REQUIRED)
    public void logAuditBad(String message) {
        auditRepo.save(new AuditLog(message));
        // Rolls back with parent transaction
    }
    
    // ✅ GOOD - Audit saved even if parent rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditGood(String message) {
        auditRepo.save(new AuditLog(message));
        // Independent transaction
    }
}
```

### 5. Forgetting Database Isolation Support

```java
// ❌ May not work if database doesn't support SERIALIZABLE
@Transactional(isolation = Isolation.SERIALIZABLE)
public void criticalOperation() {
    // MySQL InnoDB supports it
    // Some databases might not
}
```

**Check database capabilities before using high isolation levels!**

### 6. Transaction Too Long

```java
// ❌ BAD - Transaction includes slow operations
@Transactional
public void processOrderWithEmailBad(Order order) {
    orderRepo.save(order);
    sendEmail(order);  // Takes 3 seconds
    updateInventory(order);
    // Database connection held for 3+ seconds!
}

// ✅ GOOD - Email sent outside transaction
@Transactional
public Order processOrderGood(Order order) {
    orderRepo.save(order);
    updateInventory(order);
    return order;
}

public void processOrderWithEmail(Order order) {
    Order savedOrder = processOrderGood(order);
    sendEmail(savedOrder);  // After transaction
}
```

---

## Quick Reference

### Common Annotations

```java
// Basic transaction
@Transactional

// Read-only
@Transactional(readOnly = true)

// Custom propagation
@Transactional(propagation = Propagation.REQUIRES_NEW)

// Custom isolation
@Transactional(isolation = Isolation.REPEATABLE_READ)

// Rollback on checked exceptions
@Transactional(rollbackFor = Exception.class)

// Don't rollback on specific exceptions
@Transactional(noRollbackFor = EmailException.class)

// Timeout
@Transactional(timeout = 30)

// Combined
@Transactional(
    readOnly = true,
    isolation = Isolation.READ_COMMITTED,
    timeout = 60
)
```

### Propagation Quick Guide

- **REQUIRED** - Join existing or create new (default, use most often)
- **REQUIRES_NEW** - Always create new (logging, notifications)
- **MANDATORY** - Must have existing (enforce transaction context)
- **NESTED** - Create savepoint (partial rollback needed)

### Isolation Quick Guide

- **READ_COMMITTED** - Default, good for most cases
- **REPEATABLE_READ** - Financial calculations
- **SERIALIZABLE** - Critical operations (inventory, tickets)

### When to Use What

**Use @Transactional when:**
- Modifying multiple entities
- Multiple repository calls that must succeed together
- Need ACID guarantees

**Use readOnly = true when:**
- Only reading data
- Generating reports
- Dashboard queries

**Use REQUIRES_NEW when:**
- Logging operations
- Sending notifications
- Operations that must succeed independently

---

## Summary

### Key Takeaways

1. **Always use @Transactional for multi-step operations**
2. **Put @Transactional on service layer, not controllers**
3. **Use readOnly = true for read operations**
4. **Choose correct propagation based on independence needed**
5. **Choose correct isolation based on consistency needed**
6. **Be explicit about rollback rules**
7. **Keep transactions short**
8. **Avoid @Transactional on private methods**
9. **Re-throw exceptions to trigger rollback**
10. **Test transaction behavior**

### Transaction Checklist

Before deploying code, check:

- [ ] @Transactional on public methods only
- [ ] Transactions in service layer, not controllers
- [ ] readOnly = true for read-only methods
- [ ] Correct propagation for nested calls
- [ ] Appropriate isolation level
- [ ] Rollback rules defined
- [ ] No long-running operations in transaction
- [ ] Exceptions properly handled
- [ ] No self-invocation issues
- [ ] Database supports chosen isolation level

---

**Happy Transacting! 🎯**
