# Advanced Microservices Patterns

## Table of Contents
1. [Introduction to Advanced Patterns](#introduction-to-advanced-patterns)
2. [Saga Pattern](#saga-pattern)
3. [CQRS (Command Query Responsibility Segregation)](#cqrs-command-query-responsibility-segregation)
4. [Event Sourcing](#event-sourcing)
5. [API Composition](#api-composition)
6. [Strangler Pattern](#strangler-pattern)
7. [Anti-Corruption Layer](#anti-corruption-layer)

---

## Introduction to Advanced Patterns

### Why Advanced Patterns?

**Basic Microservices:**
```
Simple service-to-service calls
Single database per service
CRUD operations
Works for small systems
```

**Real-World Challenges:**
```
❌ Distributed transactions across services
❌ Data consistency across databases
❌ Complex queries spanning multiple services
❌ Migrating from monolith to microservices
❌ Integrating with legacy systems
```

**Advanced Patterns Solve These:**
```
✓ Saga: Distributed transactions
✓ CQRS: Separate reads and writes
✓ Event Sourcing: Complete audit trail
✓ API Composition: Multi-service queries
✓ Strangler: Gradual migration
✓ Anti-Corruption Layer: Legacy integration
```

---

### When to Use Advanced Patterns

**Don't Use If:**
- Small, simple application
- Learning microservices
- Low complexity
- Team inexperienced with patterns

**Use When:**
- Complex business workflows
- Need strong consistency guarantees
- High read/write different requirements
- Migrating legacy systems
- Need complete audit trail
- Complex reporting across services

**Remember:** Advanced patterns add complexity. Only use when benefits outweigh costs.

---

## Saga Pattern

### What is Saga Pattern?

**Definition:** Managing distributed transactions across multiple services without traditional ACID transactions.

**The Problem:**

```
Scenario: E-commerce order

Traditional Monolith (ACID Transaction):
BEGIN TRANSACTION
  1. Create order
  2. Process payment
  3. Reserve inventory
  4. Create shipment
COMMIT TRANSACTION

If any step fails → Rollback everything
Easy!

Microservices Challenge:
Order Service → Create order (separate DB)
Payment Service → Process payment (separate DB)
Inventory Service → Reserve inventory (separate DB)
Shipping Service → Create shipment (separate DB)

Can't use single database transaction!
How to maintain consistency?
```

---

### Saga Solution

**Two Types of Sagas:**

**1. Choreography-based Saga**
```
Services coordinate through events
No central coordinator
Each service listens and reacts
Decentralized
```

**2. Orchestration-based Saga**
```
Central orchestrator controls flow
Orchestrator tells each service what to do
Centralized control
Easier to understand and debug
```

---

### Choreography-based Saga

**How It Works:**

```
Services communicate via events:

1. Order Service
   ↓ publishes "OrderCreated" event
   
2. Payment Service (listening)
   ↓ processes payment
   ↓ publishes "PaymentCompleted" event
   
3. Inventory Service (listening)
   ↓ reserves inventory
   ↓ publishes "InventoryReserved" event
   
4. Shipping Service (listening)
   ↓ creates shipment
   ↓ publishes "ShipmentCreated" event

Success flow complete!
```

**Compensation (Rollback) Flow:**

```
If Inventory Service fails:

1. Inventory Service
   ↓ publishes "InventoryReservationFailed" event
   
2. Payment Service (listening)
   ↓ refunds payment
   ↓ publishes "PaymentRefunded" event
   
3. Order Service (listening)
   ↓ cancels order
   ↓ publishes "OrderCancelled" event

Saga compensated (rolled back)!
```

---

### Choreography Implementation

**Step 1: Define Events**

```java
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private Double totalAmount;
    // Getters, setters, constructors
}

public class PaymentCompletedEvent {
    private String orderId;
    private String paymentId;
    private Double amount;
}

public class PaymentFailedEvent {
    private String orderId;
    private String reason;
}
```

**Step 2: Order Service**

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. Create order (pending state)
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(request.getUserId());
        order.setItems(request.getItems());
        order.setTotalAmount(calculateTotal(request.getItems()));
        order.setStatus(OrderStatus.PENDING);
        
        orderRepository.save(order);
        
        // 2. Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getItems(),
            order.getTotalAmount()
        );
        
        kafkaTemplate.send("order-created", event);
        
        return order;
    }
    
    // Listen for payment completed
    @KafkaListener(topics = "payment-completed")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        orderRepository.save(order);
    }
    
    // Listen for payment failed - COMPENSATION
    @KafkaListener(topics = "payment-failed")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(event.getReason());
        orderRepository.save(order);
        
        // Publish order cancelled event
        kafkaTemplate.send("order-cancelled", new OrderCancelledEvent(order.getId()));
    }
}
```

**Step 3: Payment Service**

```java
@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    // Listen for order created
    @KafkaListener(topics = "order-created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            // Process payment
            Payment payment = processPayment(event);
            
            // Publish success event
            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                event.getOrderId(),
                payment.getId(),
                payment.getAmount()
            );
            kafkaTemplate.send("payment-completed", completedEvent);
            
        } catch (PaymentException e) {
            // Publish failure event
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("payment-failed", failedEvent);
        }
    }
    
    // Listen for inventory failed - COMPENSATION
    @KafkaListener(topics = "inventory-failed")
    public void handleInventoryFailed(InventoryFailedEvent event) {
        // Refund payment
        Payment payment = paymentRepository.findByOrderId(event.getOrderId()).orElseThrow();
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        
        // Publish refund event
        kafkaTemplate.send("payment-refunded", 
            new PaymentRefundedEvent(event.getOrderId(), payment.getId())
        );
    }
    
    private Payment processPayment(OrderCreatedEvent event) {
        // Payment processing logic
        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setAmount(event.getTotalAmount());
        payment.setStatus(PaymentStatus.COMPLETED);
        return paymentRepository.save(payment);
    }
}
```

**Step 4: Inventory Service**

```java
@Service
public class InventoryService {
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    // Listen for payment completed
    @KafkaListener(topics = "payment-completed")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            // Reserve inventory
            reserveInventory(event.getOrderId());
            
            // Publish success event
            kafkaTemplate.send("inventory-reserved", 
                new InventoryReservedEvent(event.getOrderId())
            );
            
        } catch (InsufficientInventoryException e) {
            // Publish failure event (triggers compensation)
            kafkaTemplate.send("inventory-failed", 
                new InventoryFailedEvent(event.getOrderId(), e.getMessage())
            );
        }
    }
    
    // Listen for order cancelled - COMPENSATION
    @KafkaListener(topics = "order-cancelled")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // Release reserved inventory
        releaseInventory(event.getOrderId());
    }
    
    private void reserveInventory(String orderId) {
        // Reserve inventory logic
    }
    
    private void releaseInventory(String orderId) {
        // Release inventory logic (compensation)
    }
}
```

---

### Orchestration-based Saga

**How It Works:**

```
Saga Orchestrator controls the flow:

1. Orchestrator → Order Service: Create order
   ↓ Success
   
2. Orchestrator → Payment Service: Process payment
   ↓ Success
   
3. Orchestrator → Inventory Service: Reserve inventory
   ↓ FAILURE!
   
4. Orchestrator → Payment Service: Refund payment (compensation)
   ↓ Success
   
5. Orchestrator → Order Service: Cancel order (compensation)
   ↓ Success

Saga compensated!
```

---

### Orchestration Implementation

**Step 1: Define Saga State**

```java
@Entity
public class OrderSaga {
    
    @Id
    private String sagaId;
    
    private String orderId;
    private SagaStatus status;
    private SagaStep currentStep;
    
    @ElementCollection
    private List<SagaStep> completedSteps = new ArrayList<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters, setters
}

public enum SagaStatus {
    STARTED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}

public enum SagaStep {
    CREATE_ORDER,
    PROCESS_PAYMENT,
    RESERVE_INVENTORY,
    CREATE_SHIPMENT
}
```

**Step 2: Saga Orchestrator**

```java
@Service
public class OrderSagaOrchestrator {
    
    @Autowired
    private OrderSagaRepository sagaRepository;
    
    @Autowired
    private OrderServiceClient orderClient;
    
    @Autowired
    private PaymentServiceClient paymentClient;
    
    @Autowired
    private InventoryServiceClient inventoryClient;
    
    @Autowired
    private ShippingServiceClient shippingClient;
    
    public OrderSaga startSaga(OrderRequest request) {
        // Create saga instance
        OrderSaga saga = new OrderSaga();
        saga.setSagaId(UUID.randomUUID().toString());
        saga.setStatus(SagaStatus.STARTED);
        saga.setCurrentStep(SagaStep.CREATE_ORDER);
        sagaRepository.save(saga);
        
        // Execute saga
        executeSaga(saga, request);
        
        return saga;
    }
    
    private void executeSaga(OrderSaga saga, OrderRequest request) {
        try {
            // Step 1: Create Order
            saga.setCurrentStep(SagaStep.CREATE_ORDER);
            Order order = orderClient.createOrder(request);
            saga.setOrderId(order.getId());
            saga.getCompletedSteps().add(SagaStep.CREATE_ORDER);
            sagaRepository.save(saga);
            
            // Step 2: Process Payment
            saga.setCurrentStep(SagaStep.PROCESS_PAYMENT);
            PaymentResponse payment = paymentClient.processPayment(
                order.getId(), 
                order.getTotalAmount()
            );
            saga.getCompletedSteps().add(SagaStep.PROCESS_PAYMENT);
            sagaRepository.save(saga);
            
            // Step 3: Reserve Inventory
            saga.setCurrentStep(SagaStep.RESERVE_INVENTORY);
            inventoryClient.reserveInventory(order.getId(), order.getItems());
            saga.getCompletedSteps().add(SagaStep.RESERVE_INVENTORY);
            sagaRepository.save(saga);
            
            // Step 4: Create Shipment
            saga.setCurrentStep(SagaStep.CREATE_SHIPMENT);
            shippingClient.createShipment(order.getId());
            saga.getCompletedSteps().add(SagaStep.CREATE_SHIPMENT);
            
            // Saga completed successfully
            saga.setStatus(SagaStatus.COMPLETED);
            sagaRepository.save(saga);
            
        } catch (Exception e) {
            // Saga failed - start compensation
            compensateSaga(saga, e);
        }
    }
    
    private void compensateSaga(OrderSaga saga, Exception cause) {
        saga.setStatus(SagaStatus.COMPENSATING);
        sagaRepository.save(saga);
        
        // Compensate in reverse order
        List<SagaStep> stepsToCompensate = new ArrayList<>(saga.getCompletedSteps());
        Collections.reverse(stepsToCompensate);
        
        for (SagaStep step : stepsToCompensate) {
            try {
                compensateStep(saga, step);
            } catch (Exception e) {
                // Log compensation failure
                logger.error("Failed to compensate step: " + step, e);
            }
        }
        
        saga.setStatus(SagaStatus.COMPENSATED);
        sagaRepository.save(saga);
    }
    
    private void compensateStep(OrderSaga saga, SagaStep step) {
        switch (step) {
            case CREATE_SHIPMENT:
                shippingClient.cancelShipment(saga.getOrderId());
                break;
            case RESERVE_INVENTORY:
                inventoryClient.releaseInventory(saga.getOrderId());
                break;
            case PROCESS_PAYMENT:
                paymentClient.refundPayment(saga.getOrderId());
                break;
            case CREATE_ORDER:
                orderClient.cancelOrder(saga.getOrderId());
                break;
        }
    }
}
```

---

### Saga Pattern Trade-offs

**Choreography:**
- ✓ Decentralized (no single point of failure)
- ✓ Loose coupling
- ✓ Scalable
- ✗ Hard to understand flow
- ✗ Difficult to debug
- ✗ Cyclic dependencies risk

**Orchestration:**
- ✓ Centralized control (easier to understand)
- ✓ Easy to track saga state
- ✓ Simple debugging
- ✗ Orchestrator is single point of failure
- ✗ Tight coupling to orchestrator
- ✗ Orchestrator can become complex

---

## CQRS (Command Query Responsibility Segregation)

### What is CQRS?

**Definition:** Separate the write operations (commands) from read operations (queries) using different models.

**Traditional Approach:**

```
Single Model for Everything:

User → Create Order (Write)
       ↓
    Same Database
       ↓
User → Get Order Report (Read)

Problems:
- Write model optimized for transactions
- Read model needs complex joins
- Different scaling needs
- Conflicts between read/write requirements
```

**CQRS Approach:**

```
Separate Models:

User → Create Order (Command)
       ↓
    Write Database (Normalized)
       ↓
    Sync Events
       ↓
    Read Database (Denormalized)
       ↓
User ← Get Order Report (Query)

Benefits:
- Optimize each side independently
- Scale reads and writes separately
- Different technologies for each
```

---

### When to Use CQRS

**Use CQRS When:**
```
✓ Complex domain logic
✓ High read/write ratio difference
✓ Read and write have different scaling needs
✓ Complex queries across aggregates
✓ Need to optimize reads separately
```

**Don't Use CQRS When:**
```
✗ Simple CRUD application
✗ Read and write are similar
✗ Small application
✗ Team not experienced with pattern
```

---

### CQRS Implementation

**Architecture:**

```
┌─────────────────────────────────────────────────┐
│                   Client                         │
└───────────────┬─────────────────┬───────────────┘
                │                 │
           Commands            Queries
                │                 │
        ┌───────▼─────┐    ┌─────▼────────┐
        │  Command    │    │   Query      │
        │   Service   │    │   Service    │
        └───────┬─────┘    └─────▲────────┘
                │                 │
        ┌───────▼─────┐          │
        │   Write     │          │
        │   Database  │          │
        └───────┬─────┘          │
                │                 │
             Events              │
                │                 │
        ┌───────▼─────┐    ┌─────┴────────┐
        │Event Handler│───→│    Read      │
        └─────────────┘    │   Database   │
                           └──────────────┘
```

---

### Step 1: Command Side

**Command DTOs:**

```java
public class CreateOrderCommand {
    private String userId;
    private List<OrderItem> items;
    private String shippingAddress;
    // Getters, setters
}

public class UpdateOrderStatusCommand {
    private String orderId;
    private OrderStatus newStatus;
    // Getters, setters
}

public class CancelOrderCommand {
    private String orderId;
    private String reason;
    // Getters, setters
}
```

**Write Model (Normalized):**

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;
    private String userId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLine> orderLines;
    
    // Business logic methods
    public void cancel(String reason) {
        if (this.status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel shipped order");
        }
        this.status = OrderStatus.CANCELLED;
    }
}

@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    private String id;
    
    @ManyToOne
    private Order order;
    
    private String productId;
    private Integer quantity;
    private Double price;
}
```

**Command Handler:**

```java
@Service
public class OrderCommandHandler {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public String handleCreateOrder(CreateOrderCommand command) {
        // 1. Create order (write model)
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(command.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        // Create order lines
        for (OrderItem item : command.getItems()) {
            OrderLine line = new OrderLine();
            line.setProductId(item.getProductId());
            line.setQuantity(item.getQuantity());
            line.setPrice(item.getPrice());
            order.addOrderLine(line);
        }
        
        orderRepository.save(order);
        
        // 2. Publish event for read side
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getOrderLines(),
            order.getCreatedAt()
        );
        eventPublisher.publishEvent(event);
        
        return order.getId();
    }
    
    @Transactional
    public void handleCancelOrder(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException(command.getOrderId()));
        
        order.cancel(command.getReason());
        orderRepository.save(order);
        
        // Publish event
        OrderCancelledEvent event = new OrderCancelledEvent(
            order.getId(),
            command.getReason(),
            LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);
    }
}
```

---

### Step 2: Query Side

**Read Model (Denormalized):**

```java
@Document(collection = "order_summaries")
public class OrderSummary {
    @Id
    private String orderId;
    private String userId;
    private String userName;          // Denormalized from User
    private String userEmail;         // Denormalized from User
    private OrderStatus status;
    private Double totalAmount;
    private Integer totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<OrderItemSummary> items;
    
    // Getters, setters
}

public class OrderItemSummary {
    private String productId;
    private String productName;       // Denormalized from Product
    private Integer quantity;
    private Double price;
    private Double subtotal;
}
```

**Query Handler:**

```java
@Service
public class OrderQueryHandler {
    
    @Autowired
    private OrderSummaryRepository orderSummaryRepository;  // MongoDB
    
    public OrderSummary getOrderById(String orderId) {
        return orderSummaryRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    public List<OrderSummary> getOrdersByUser(String userId) {
        return orderSummaryRepository.findByUserId(userId);
    }
    
    public List<OrderSummary> getOrdersByStatus(OrderStatus status) {
        return orderSummaryRepository.findByStatus(status);
    }
    
    public OrderStatistics getOrderStatistics(String userId) {
        // Complex aggregation query on denormalized data
        return orderSummaryRepository.calculateStatistics(userId);
    }
}
```

---

### Step 3: Event Handler (Synchronization)

**Sync Write to Read:**

```java
@Service
public class OrderEventHandler {
    
    @Autowired
    private OrderSummaryRepository orderSummaryRepository;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 1. Get denormalized data from other services
        User user = userServiceClient.getUser(event.getUserId());
        
        // 2. Create read model
        OrderSummary summary = new OrderSummary();
        summary.setOrderId(event.getOrderId());
        summary.setUserId(event.getUserId());
        summary.setUserName(user.getName());
        summary.setUserEmail(user.getEmail());
        summary.setStatus(OrderStatus.PENDING);
        summary.setCreatedAt(event.getCreatedAt());
        
        // 3. Denormalize order items
        List<OrderItemSummary> items = new ArrayList<>();
        double total = 0;
        
        for (OrderLineDto line : event.getOrderLines()) {
            Product product = productServiceClient.getProduct(line.getProductId());
            
            OrderItemSummary item = new OrderItemSummary();
            item.setProductId(line.getProductId());
            item.setProductName(product.getName());
            item.setQuantity(line.getQuantity());
            item.setPrice(line.getPrice());
            item.setSubtotal(line.getQuantity() * line.getPrice());
            
            items.add(item);
            total += item.getSubtotal();
        }
        
        summary.setItems(items);
        summary.setTotalAmount(total);
        summary.setTotalItems(items.size());
        
        // 4. Save to read database
        orderSummaryRepository.save(summary);
    }
    
    @EventListener
    @Async
    public void handleOrderCancelled(OrderCancelledEvent event) {
        OrderSummary summary = orderSummaryRepository.findById(event.getOrderId())
            .orElseThrow();
        
        summary.setStatus(OrderStatus.CANCELLED);
        summary.setUpdatedAt(event.getCancelledAt());
        
        orderSummaryRepository.save(summary);
    }
}
```

---

### CQRS with Different Databases

**Write Side: PostgreSQL (Transactional)**
```
Strong consistency
ACID transactions
Normalized schema
Optimized for writes
```

**Read Side: MongoDB (Queries)**
```
Eventual consistency
Denormalized documents
Optimized for reads
Fast queries
```

---

### CQRS Benefits and Challenges

**Benefits:**
- ✓ Separate scaling (scale reads independently)
- ✓ Optimized data models (different needs)
- ✓ Performance (denormalized reads)
- ✓ Multiple read models (different views)
- ✓ Security (separate access control)

**Challenges:**
- ✗ Eventual consistency (read lag)
- ✗ Code duplication
- ✗ Complexity
- ✗ Data synchronization overhead
- ✗ Learning curve

---

## Event Sourcing

### What is Event Sourcing?

**Definition:** Store all changes to application state as a sequence of events instead of storing current state.

**Traditional Approach:**

```
Order Table (Current State):
┌────────┬────────┬─────────┬────────┐
│OrderID │ Status │ Amount  │ Items  │
├────────┼────────┼─────────┼────────┤
│ 001    │ SHIPPED│ $100.00 │ 2      │
└────────┴────────┴─────────┴────────┘

Problem: Lost history
- How did we get here?
- When was it shipped?
- Who made changes?
- Can't audit trail
- Can't rebuild state
```

**Event Sourcing Approach:**

```
Event Store (Complete History):
┌────────┬──────────────────┬─────────────┬──────────┐
│EventID │ Type             │ Timestamp   │ Data     │
├────────┼──────────────────┼─────────────┼──────────┤
│ 1      │ OrderCreated     │ 10:00:00    │ {…}      │
│ 2      │ PaymentProcessed │ 10:01:00    │ {…}      │
│ 3      │ ItemsReserved    │ 10:02:00    │ {…}      │
│ 4      │ OrderShipped     │ 11:00:00    │ {…}      │
└────────┴──────────────────┴─────────────┴──────────┘

Current state = Replay all events
Complete audit trail
Can rebuild any point in time
```

---

### Event Sourcing Core Concepts

**1. Event**
```
Immutable fact that happened
Past tense (OrderCreated, PaymentProcessed)
Never modified or deleted
Stored forever
```

**2. Event Store**
```
Append-only log of events
Like a ledger
Never update or delete
Only append new events
```

**3. Aggregate**
```
Entity that produces events
Validates business rules
Current state from events
```

**4. Projection**
```
Read model built from events
Materialized view
Can have multiple projections
Query-optimized
```

---

### Event Sourcing Implementation

**Step 1: Define Events**

```java
public abstract class OrderEvent {
    private String eventId;
    private String orderId;
    private LocalDateTime timestamp;
    private Long version;  // For optimistic locking
    
    // Getters, setters, constructor
}

public class OrderCreatedEvent extends OrderEvent {
    private String userId;
    private List<OrderItem> items;
    private Double totalAmount;
    // Getters, setters
}

public class PaymentProcessedEvent extends OrderEvent {
    private String paymentId;
    private Double amount;
    private String paymentMethod;
}

public class OrderShippedEvent extends OrderEvent {
    private String trackingNumber;
    private String carrier;
    private String shippingAddress;
}

public class OrderCancelledEvent extends OrderEvent {
    private String reason;
}
```

---

**Step 2: Event Store**

```java
@Entity
@Table(name = "event_store")
public class StoredEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String aggregateId;  // Order ID
    
    @Column(nullable = false)
    private String aggregateType;  // "Order"
    
    @Column(nullable = false)
    private String eventType;  // "OrderCreated"
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData;  // JSON
    
    @Column(nullable = false)
    private Long version;  // Event version
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // Getters, setters
}

public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {
    List<StoredEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);
    Optional<StoredEvent> findTopByAggregateIdOrderByVersionDesc(String aggregateId);
}
```

**Step 3: Event Store Service**

```java
@Service
public class EventStoreService {
    
    @Autowired
    private EventStoreRepository eventStoreRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Transactional
    public void saveEvent(OrderEvent event) {
        StoredEvent storedEvent = new StoredEvent();
        storedEvent.setAggregateId(event.getOrderId());
        storedEvent.setAggregateType("Order");
        storedEvent.setEventType(event.getClass().getSimpleName());
        storedEvent.setVersion(event.getVersion());
        storedEvent.setTimestamp(event.getTimestamp());
        
        try {
            storedEvent.setEventData(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
        
        eventStoreRepository.save(storedEvent);
    }
    
    public List<OrderEvent> getEvents(String orderId) {
        List<StoredEvent> storedEvents = eventStoreRepository
            .findByAggregateIdOrderByVersionAsc(orderId);
        
        return storedEvents.stream()
            .map(this::deserializeEvent)
            .collect(Collectors.toList());
    }
    
    private OrderEvent deserializeEvent(StoredEvent storedEvent) {
        try {
            Class<?> eventClass = Class.forName(
                "com.example.events." + storedEvent.getEventType()
            );
            return (OrderEvent) objectMapper.readValue(
                storedEvent.getEventData(), 
                eventClass
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
```

---

**Step 4: Order Aggregate**

```java
public class OrderAggregate {
    
    private String orderId;
    private String userId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Double totalAmount;
    private Long version;
    
    private List<OrderEvent> uncommittedEvents = new ArrayList<>();
    
    // Create new order
    public static OrderAggregate create(String userId, List<OrderItem> items) {
        OrderAggregate order = new OrderAggregate();
        
        // Generate event
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setItems(items);
        event.setTotalAmount(calculateTotal(items));
        event.setTimestamp(LocalDateTime.now());
        event.setVersion(0L);
        
        // Apply event
        order.apply(event);
        
        return order;
    }
    
    // Process payment
    public void processPayment(String paymentId, String paymentMethod) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only process payment for pending orders");
        }
        
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(this.orderId);
        event.setPaymentId(paymentId);
        event.setAmount(this.totalAmount);
        event.setPaymentMethod(paymentMethod);
        event.setTimestamp(LocalDateTime.now());
        event.setVersion(this.version + 1);
        
        apply(event);
    }
    
    // Ship order
    public void ship(String trackingNumber, String carrier) {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Can only ship paid orders");
        }
        
        OrderShippedEvent event = new OrderShippedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(this.orderId);
        event.setTrackingNumber(trackingNumber);
        event.setCarrier(carrier);
        event.setTimestamp(LocalDateTime.now());
        event.setVersion(this.version + 1);
        
        apply(event);
    }
    
    // Apply event (update state)
    private void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent) {
            OrderCreatedEvent e = (OrderCreatedEvent) event;
            this.orderId = e.getOrderId();
            this.userId = e.getUserId();
            this.items = e.getItems();
            this.totalAmount = e.getTotalAmount();
            this.status = OrderStatus.PENDING;
            this.version = e.getVersion();
        }
        else if (event instanceof PaymentProcessedEvent) {
            this.status = OrderStatus.PAID;
            this.version = event.getVersion();
        }
        else if (event instanceof OrderShippedEvent) {
            this.status = OrderStatus.SHIPPED;
            this.version = event.getVersion();
        }
        else if (event instanceof OrderCancelledEvent) {
            this.status = OrderStatus.CANCELLED;
            this.version = event.getVersion();
        }
        
        uncommittedEvents.add(event);
    }
    
    // Load from history
    public static OrderAggregate loadFromHistory(List<OrderEvent> events) {
        OrderAggregate order = new OrderAggregate();
        for (OrderEvent event : events) {
            order.apply(event);
        }
        order.uncommittedEvents.clear();  // Already persisted
        return order;
    }
    
    public List<OrderEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
```

---

**Step 5: Command Handler with Event Sourcing**

```java
@Service
public class OrderCommandService {
    
    @Autowired
    private EventStoreService eventStore;
    
    @Transactional
    public String createOrder(CreateOrderCommand command) {
        // Create aggregate
        OrderAggregate order = OrderAggregate.create(
            command.getUserId(),
            command.getItems()
        );
        
        // Save events
        for (OrderEvent event : order.getUncommittedEvents()) {
            eventStore.saveEvent(event);
        }
        
        order.markEventsAsCommitted();
        
        return order.getOrderId();
    }
    
    @Transactional
    public void processPayment(ProcessPaymentCommand command) {
        // Load aggregate from events
        List<OrderEvent> events = eventStore.getEvents(command.getOrderId());
        OrderAggregate order = OrderAggregate.loadFromHistory(events);
        
        // Execute command
        order.processPayment(command.getPaymentId(), command.getPaymentMethod());
        
        // Save new events
        for (OrderEvent event : order.getUncommittedEvents()) {
            eventStore.saveEvent(event);
        }
        
        order.markEventsAsCommitted();
    }
}
```

---

### Projections (Read Models)

**Create materialized views from events:**

```java
@Service
public class OrderProjectionService {
    
    @Autowired
    private OrderSummaryRepository orderSummaryRepository;
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        OrderSummary summary = new OrderSummary();
        summary.setOrderId(event.getOrderId());
        summary.setUserId(event.getUserId());
        summary.setStatus(OrderStatus.PENDING);
        summary.setTotalAmount(event.getTotalAmount());
        summary.setCreatedAt(event.getTimestamp());
        
        orderSummaryRepository.save(summary);
    }
    
    @EventListener
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        OrderSummary summary = orderSummaryRepository.findById(event.getOrderId())
            .orElseThrow();
        
        summary.setStatus(OrderStatus.PAID);
        summary.setPaymentId(event.getPaymentId());
        summary.setUpdatedAt(event.getTimestamp());
        
        orderSummaryRepository.save(summary);
    }
    
    @EventListener
    public void handleOrderShipped(OrderShippedEvent event) {
        OrderSummary summary = orderSummaryRepository.findById(event.getOrderId())
            .orElseThrow();
        
        summary.setStatus(OrderStatus.SHIPPED);
        summary.setTrackingNumber(event.getTrackingNumber());
        summary.setUpdatedAt(event.getTimestamp());
        
        orderSummaryRepository.save(summary);
    }
}
```

---

### Event Sourcing Benefits

**Benefits:**
- ✓ Complete audit trail (every change recorded)
- ✓ Time travel (rebuild state at any point)
- ✓ Event replay (fix bugs by replaying)
- ✓ Multiple projections (different views)
- ✓ Debugging (see exact sequence)
- ✓ Compliance (regulatory requirements)

**Challenges:**
- ✗ Complexity (harder to implement)
- ✗ Event versioning (schema evolution)
- ✗ Storage (events grow over time)
- ✗ Query complexity (need projections)
- ✗ Learning curve

---

## API Composition

### What is API Composition?

**Definition:** Implement queries that retrieve data from multiple services by invoking individual services and combining results.

**The Problem:**

```
Query: Get user's complete order history with product details

Data spread across services:
- Order Service: Order info
- Product Service: Product details
- User Service: User info
- Review Service: Product reviews

Can't do JOIN across services!
```

---

### API Composition Pattern

**How It Works:**

```
1. API Composer receives request
2. Calls Order Service → Get orders
3. Calls Product Service → Get product details for each order
4. Calls User Service → Get user info
5. Calls Review Service → Get reviews
6. Combines all data
7. Returns complete response
```

---

### Implementation

**Step 1: API Composer Service**

```java
@Service
public class OrderHistoryComposer {
    
    @Autowired
    private OrderServiceClient orderClient;
    
    @Autowired
    private ProductServiceClient productClient;
    
    @Autowired
    private UserServiceClient userClient;
    
    @Autowired
    private ReviewServiceClient reviewClient;
    
    public OrderHistoryResponse getOrderHistory(String userId) {
        // 1. Get user info
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> 
            userClient.getUser(userId)
        );
        
        // 2. Get orders
        List<Order> orders = orderClient.getOrdersByUser(userId);
        
        // 3. Get product details for all orders (parallel)
        Set<String> productIds = orders.stream()
            .flatMap(order -> order.getItems().stream())
            .map(OrderItem::getProductId)
            .collect(Collectors.toSet());
        
        CompletableFuture<Map<String, Product>> productsFuture = 
            CompletableFuture.supplyAsync(() -> 
                productIds.stream()
                    .collect(Collectors.toMap(
                        id -> id,
                        id -> productClient.getProduct(id)
                    ))
            );
        
        // 4. Get reviews (parallel)
        CompletableFuture<Map<String, List<Review>>> reviewsFuture = 
            CompletableFuture.supplyAsync(() -> 
                productIds.stream()
                    .collect(Collectors.toMap(
                        id -> id,
                        id -> reviewClient.getReviews(id)
                    ))
            );
        
        // 5. Wait for all async calls
        CompletableFuture.allOf(userFuture, productsFuture, reviewsFuture).join();
        
        User user = userFuture.join();
        Map<String, Product> products = productsFuture.join();
        Map<String, List<Review>> reviews = reviewsFuture.join();
        
        // 6. Combine data
        List<EnrichedOrder> enrichedOrders = orders.stream()
            .map(order -> enrichOrder(order, products, reviews))
            .collect(Collectors.toList());
        
        // 7. Build response
        OrderHistoryResponse response = new OrderHistoryResponse();
        response.setUser(user);
        response.setOrders(enrichedOrders);
        response.setTotalOrders(enrichedOrders.size());
        response.setTotalSpent(calculateTotalSpent(enrichedOrders));
        
        return response;
    }
    
    private EnrichedOrder enrichOrder(Order order, 
                                      Map<String, Product> products,
                                      Map<String, List<Review>> reviews) {
        EnrichedOrder enriched = new EnrichedOrder();
        enriched.setOrderId(order.getId());
        enriched.setOrderDate(order.getCreatedAt());
        enriched.setStatus(order.getStatus());
        enriched.setTotalAmount(order.getTotalAmount());
        
        List<EnrichedOrderItem> enrichedItems = order.getItems().stream()
            .map(item -> {
                Product product = products.get(item.getProductId());
                List<Review> productReviews = reviews.get(item.getProductId());
                
                EnrichedOrderItem enrichedItem = new EnrichedOrderItem();
                enrichedItem.setProductId(item.getProductId());
                enrichedItem.setProductName(product.getName());
                enrichedItem.setProductImage(product.getImageUrl());
                enrichedItem.setQuantity(item.getQuantity());
                enrichedItem.setPrice(item.getPrice());
                enrichedItem.setAverageRating(calculateAverageRating(productReviews));
                
                return enrichedItem;
            })
            .collect(Collectors.toList());
        
        enriched.setItems(enrichedItems);
        
        return enriched;
    }
}
```

---

### API Composition with Resilience

**Handle failures gracefully:**

```java
@Service
public class ResilientOrderHistoryComposer {
    
    @Autowired
    private OrderServiceClient orderClient;
    
    @Autowired
    private ProductServiceClient productClient;
    
    @CircuitBreaker(name = "orderHistory", fallbackMethod = "getOrderHistoryFallback")
    @Bulkhead(name = "orderHistory")
    public OrderHistoryResponse getOrderHistory(String userId) {
        User user = getUserWithFallback(userId);
        List<Order> orders = orderClient.getOrdersByUser(userId);
        
        List<EnrichedOrder> enrichedOrders = orders.stream()
            .map(order -> enrichOrderWithFallback(order))
            .collect(Collectors.toList());
        
        return buildResponse(user, enrichedOrders);
    }
    
    private User getUserWithFallback(String userId) {
        try {
            return userClient.getUser(userId);
        } catch (Exception e) {
            // Fallback: basic user info
            logger.warn("Failed to get user details, using fallback", e);
            User fallback = new User();
            fallback.setId(userId);
            fallback.setName("User " + userId);
            return fallback;
        }
    }
    
    private EnrichedOrder enrichOrderWithFallback(Order order) {
        EnrichedOrder enriched = new EnrichedOrder();
        enriched.setOrderId(order.getId());
        enriched.setOrderDate(order.getCreatedAt());
        enriched.setStatus(order.getStatus());
        
        List<EnrichedOrderItem> items = order.getItems().stream()
            .map(item -> enrichItemWithFallback(item))
            .collect(Collectors.toList());
        
        enriched.setItems(items);
        return enriched;
    }
    
    private EnrichedOrderItem enrichItemWithFallback(OrderItem item) {
        EnrichedOrderItem enriched = new EnrichedOrderItem();
        enriched.setProductId(item.getProductId());
        enriched.setQuantity(item.getQuantity());
        enriched.setPrice(item.getPrice());
        
        try {
            // Try to get product details
            Product product = productClient.getProduct(item.getProductId());
            enriched.setProductName(product.getName());
            enriched.setProductImage(product.getImageUrl());
        } catch (Exception e) {
            // Fallback: basic info
            logger.warn("Failed to get product details for " + item.getProductId(), e);
            enriched.setProductName("Product " + item.getProductId());
            enriched.setProductImage("/images/placeholder.png");
        }
        
        return enriched;
    }
    
    private OrderHistoryResponse getOrderHistoryFallback(String userId, Exception e) {
        logger.error("Failed to get order history, returning cached data", e);
        return orderHistoryCache.get(userId);
    }
}
```

---

### API Composition Challenges

**Challenges:**
- ✗ Multiple network calls (latency)
- ✗ Partial failures (some services down)
- ✗ Complex error handling
- ✗ Memory overhead (combining data)
- ✗ Difficult to optimize

**Solutions:**
- ✓ Parallel calls (reduce latency)
- ✓ Circuit breakers (handle failures)
- ✓ Caching (reduce calls)
- ✓ Fallbacks (graceful degradation)

---

## Strangler Pattern

### What is Strangler Pattern?

**Definition:** Incrementally migrate from legacy monolith to microservices by gradually replacing functionality.

**Analogy:**
```
Like a strangler fig tree:
- Grows around host tree
- Gradually takes over
- Eventually replaces host
- Host tree can be removed

Same for migration:
- New microservices grow around monolith
- Gradually route traffic to new services
- Eventually replace all functionality
- Monolith can be removed
```

---

### Why Strangler Pattern?

**Big Bang Migration (Risky):**
```
❌ Rewrite everything at once
❌ Long development (6-12 months)
❌ No revenue during rewrite
❌ High risk (might fail)
❌ Can't rollback easily
```

**Strangler Pattern (Safe):**
```
✓ Migrate piece by piece
✓ Continuous delivery (weeks)
✓ Keep earning revenue
✓ Low risk (rollback easily)
✓ Learn and adjust
```

---

### Strangler Pattern Implementation

**Architecture:**

```
┌─────────────────────────────────────────────┐
│            Strangler Facade                  │
│           (Routing Layer)                    │
└────────┬──────────────────────────┬─────────┘
         │                          │
         │                          │
    Old Routes               New Routes
         │                          │
    ┌────▼─────┐             ┌──────▼──────┐
    │ Monolith │             │Microservice │
    │          │             │             │
    │ User Mgmt│             │User Service │
    │ Orders   │             │Order Service│
    │ Products │             │             │
    │ Payments │             │             │
    └──────────┘             └─────────────┘
```

---

### Step-by-Step Migration

**Phase 1: Setup Facade**

```java
@RestController
@RequestMapping("/api")
public class StranglerFacade {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${monolith.url}")
    private String monolithUrl;
    
    @Value("${user-service.url}")
    private String userServiceUrl;
    
    // User endpoints - MIGRATED
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        // Route to new microservice
        return restTemplate.getForEntity(
            userServiceUrl + "/users/" + id,
            User.class
        );
    }
    
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
        // Route to new microservice
        return restTemplate.postForEntity(
            userServiceUrl + "/users",
            request,
            User.class
        );
    }
    
    // Order endpoints - NOT YET MIGRATED
    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        // Route to monolith
        return restTemplate.getForEntity(
            monolithUrl + "/api/orders/" + id,
            Order.class
        );
    }
    
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        // Route to monolith
        return restTemplate.postForEntity(
            monolithUrl + "/api/orders",
            request,
            Order.class
        );
    }
}
```

---

**Phase 2: Feature Toggle**

```java
@RestController
public class StranglerFacadeWithToggles {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private FeatureToggleService featureToggle;
    
    @GetMapping("/api/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        if (featureToggle.isEnabled("use-user-microservice")) {
            // Route to microservice
            return restTemplate.getForEntity(
                userServiceUrl + "/users/" + id,
                User.class
            );
        } else {
            // Route to monolith (fallback)
            return restTemplate.getForEntity(
                monolithUrl + "/api/users/" + id,
                User.class
            );
        }
    }
}
```

**Feature Toggle Config:**
```yaml
features:
  use-user-microservice: true  # Enable new service
  use-order-microservice: false # Still use monolith
```

---

**Phase 3: Gradual Rollout**

```java
@Component
public class GradualRolloutRouter {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public ResponseEntity<?> routeUserRequest(String id, String operation) {
        // Route 10% to new service, 90% to monolith
        double random = Math.random();
        
        if (random < 0.1) {  // 10% to microservice
            logger.info("Routing to microservice");
            return routeToMicroservice(id, operation);
        } else {  // 90% to monolith
            logger.info("Routing to monolith");
            return routeToMonolith(id, operation);
        }
    }
}
```

**Gradually increase percentage:**
```
Week 1: 10% microservice, 90% monolith
Week 2: 25% microservice, 75% monolith
Week 3: 50% microservice, 50% monolith
Week 4: 75% microservice, 25% monolith
Week 5: 100% microservice, 0% monolith
```

---

**Phase 4: Data Synchronization**

**Keep both databases in sync during migration:**

```java
@Service
public class UserSyncService {
    
    @Autowired
    private MonolithUserRepository monolithRepo;
    
    @Autowired
    private UserServiceClient microserviceClient;
    
    @Transactional
    public User createUser(UserRequest request) {
        // 1. Create in monolith (primary during migration)
        User monolithUser = monolithRepo.save(new User(request));
        
        // 2. Sync to microservice (async)
        CompletableFuture.runAsync(() -> {
            try {
                microserviceClient.createUser(request);
            } catch (Exception e) {
                logger.error("Failed to sync user to microservice", e);
                // Could queue for retry
            }
        });
        
        return monolithUser;
    }
}
```

---

**Phase 5: Complete Migration**

```
1. All traffic goes to microservice
2. Verify microservice working correctly
3. Stop syncing data
4. Remove monolith code
5. Delete old database tables
```

---

### Strangler Pattern Best Practices

**1. Start with Independent Features**
```
Good first migrations:
✓ User authentication
✓ Email notifications
✓ Report generation

Bad first migrations:
✗ Core business logic
✗ Highly coupled features
✗ Complex workflows
```

**2. Dual Writes During Migration**
```
Write to both monolith and microservice
Read from microservice only
Verify data consistency
```

**3. Monitor Both Systems**
```
Compare:
- Response times
- Error rates
- Data consistency
- User experience
```

**4. Have Rollback Plan**
```
Always able to route back to monolith
Keep monolith running until confident
Don't delete code prematurely
```

---

## Anti-Corruption Layer

### What is Anti-Corruption Layer?

**Definition:** A layer that translates between your clean domain model and a legacy/external system.

**The Problem:**

```
Your Modern Microservice:
- Clean domain model
- RESTful APIs
- JSON
- Modern practices

Legacy System:
- Complex data structures
- SOAP/XML
- Stored procedures
- Confusing naming
- Business logic in database

If you directly integrate:
❌ Your code becomes polluted
❌ Legacy complexity spreads
❌ Hard to maintain
❌ Can't evolve independently
```

---

### Anti-Corruption Layer Solution

```
Your Service
    ↓
Anti-Corruption Layer
    ↓ (translates)
Legacy System

ACL acts as translator:
- Converts data formats
- Maps domain concepts
- Hides legacy complexity
- Protects your domain model
```

---

### ACL Implementation

**Step 1: Define Your Clean Domain Model**

```java
// Your clean, modern model
public class Customer {
    private String customerId;
    private String fullName;
    private String email;
    private Address address;
    private LocalDate registrationDate;
    
    // Clean, simple model
}

public class Address {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
```

---

**Step 2: Legacy System Model (What You're Dealing With)**

```java
// Legacy SOAP response (ugly)
public class LegacyCustomerResponse {
    private String CUST_ID;
    private String F_NAME;
    private String L_NAME;
    private String EMAIL_ADDR;
    private String ADDR_LINE_1;
    private String ADDR_LINE_2;
    private String CITY_NAME;
    private String STATE_CD;
    private String ZIP_CD;
    private String COUNTRY_CD;
    private String REG_DT;  // Format: "YYYYMMDD"
    private String CUST_TYPE_CD;  // "I" = Individual, "B" = Business
    private String STAT_CD;  // "A" = Active, "I" = Inactive
    
    // 50 more fields you don't need...
}
```

---

**Step 3: Anti-Corruption Layer**

```java
@Component
public class CustomerAntiCorruptionLayer {
    
    @Autowired
    private LegacySoapClient legacyClient;
    
    // Translate FROM legacy TO clean model
    public Customer getCustomer(String customerId) {
        // 1. Call legacy system
        LegacyCustomerResponse legacyResponse = legacyClient.getCustomer(customerId);
        
        // 2. Translate to your clean model
        return translateToCustomer(legacyResponse);
    }
    
    // Translate FROM clean model TO legacy
    public void updateCustomer(Customer customer) {
        // 1. Translate to legacy format
        LegacyCustomerUpdateRequest legacyRequest = translateToLegacyRequest(customer);
        
        // 2. Call legacy system
        legacyClient.updateCustomer(legacyRequest);
    }
    
    private Customer translateToCustomer(LegacyCustomerResponse legacy) {
        Customer customer = new Customer();
        
        // Map ID
        customer.setCustomerId(legacy.getCUST_ID());
        
        // Combine first and last name
        String fullName = legacy.getF_NAME() + " " + legacy.getL_NAME();
        customer.setFullName(fullName);
        
        // Map email
        customer.setEmail(legacy.getEMAIL_ADDR());
        
        // Build address from multiple fields
        Address address = new Address();
        address.setStreet(legacy.getADDR_LINE_1() + 
                         (legacy.getADDR_LINE_2() != null ? " " + legacy.getADDR_LINE_2() : ""));
        address.setCity(legacy.getCITY_NAME());
        address.setState(legacy.getSTATE_CD());
        address.setZipCode(legacy.getZIP_CD());
        address.setCountry(legacy.getCOUNTRY_CD());
        customer.setAddress(address);
        
        // Parse date (legacy uses YYYYMMDD)
        String regDate = legacy.getREG_DT();
        customer.setRegistrationDate(LocalDate.parse(regDate, 
            DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        return customer;
    }
    
    private LegacyCustomerUpdateRequest translateToLegacyRequest(Customer customer) {
        LegacyCustomerUpdateRequest request = new LegacyCustomerUpdateRequest();
        
        request.setCUST_ID(customer.getCustomerId());
        
        // Split full name
        String[] nameParts = customer.getFullName().split(" ", 2);
        request.setF_NAME(nameParts[0]);
        request.setL_NAME(nameParts.length > 1 ? nameParts[1] : "");
        
        request.setEMAIL_ADDR(customer.getEmail());
        
        // Map address
        Address address = customer.getAddress();
        request.setADDR_LINE_1(address.getStreet());
        request.setCITY_NAME(address.getCity());
        request.setSTATE_CD(address.getState());
        request.setZIP_CD(address.getZipCode());
        request.setCOUNTRY_CD(address.getCountry());
        
        // Format date
        request.setREG_DT(customer.getRegistrationDate()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        return request;
    }
}
```

---

**Step 4: Use in Your Service**

```java
@Service
public class CustomerService {
    
    @Autowired
    private CustomerAntiCorruptionLayer acl;  // Use ACL, not legacy client directly!
    
    public Customer getCustomer(String customerId) {
        // Clean, simple call
        // ACL handles all legacy complexity
        return acl.getCustomer(customerId);
    }
    
    public void updateCustomer(Customer customer) {
        // Your domain model stays clean
        acl.updateCustomer(customer);
    }
}
```

---

### Complex ACL Example

**Multiple Legacy Systems:**

```java
@Component
public class OrderAntiCorruptionLayer {
    
    @Autowired
    private LegacyOrderSystemClient legacyOrderClient;
    
    @Autowired
    private LegacyInventorySystemClient legacyInventoryClient;
    
    @Autowired
    private LegacyShippingSystemClient legacyShippingClient;
    
    public Order getCompleteOrder(String orderId) {
        // 1. Get order from legacy order system
        LegacyOrder legacyOrder = legacyOrderClient.getOrder(orderId);
        
        // 2. Get inventory status from different legacy system
        List<LegacyInventoryItem> inventory = legacyInventoryClient
            .getInventoryForOrder(orderId);
        
        // 3. Get shipping info from yet another legacy system
        LegacyShipmentInfo shipment = legacyShippingClient
            .getShipmentForOrder(orderId);
        
        // 4. Combine all into clean Order model
        return combineIntoOrder(legacyOrder, inventory, shipment);
    }
    
    private Order combineIntoOrder(LegacyOrder legacyOrder,
                                   List<LegacyInventoryItem> inventory,
                                   LegacyShipmentInfo shipment) {
        Order order = new Order();
        order.setOrderId(legacyOrder.getORDER_NUM());
        order.setCustomerId(legacyOrder.getCUST_ID());
        
        // Map items with inventory status
        List<OrderItem> items = legacyOrder.getItems().stream()
            .map(legacyItem -> {
                OrderItem item = new OrderItem();
                item.setProductId(legacyItem.getPROD_CD());
                item.setQuantity(legacyItem.getQTY());
                item.setPrice(legacyItem.getUNIT_PRC());
                
                // Find inventory status
                LegacyInventoryItem inv = inventory.stream()
                    .filter(i -> i.getPROD_CD().equals(legacyItem.getPROD_CD()))
                    .findFirst()
                    .orElse(null);
                
                if (inv != null) {
                    item.setInStock(inv.getQTY_ON_HAND() > 0);
                }
                
                return item;
            })
            .collect(Collectors.toList());
        
        order.setItems(items);
        
        // Map shipping
        if (shipment != null) {
            ShippingInfo shipping = new ShippingInfo();
            shipping.setTrackingNumber(shipment.getTRK_NUM());
            shipping.setCarrier(shipment.getCARRIER_CD());
            shipping.setEstimatedDelivery(parseDate(shipment.getEST_DELIV_DT()));
            order.setShipping(shipping);
        }
        
        return order;
    }
}
```

---

### ACL Benefits

**Benefits:**
- ✓ Isolates legacy complexity
- ✓ Clean domain model
- ✓ Easy to test (mock ACL)
- ✓ Can replace legacy gradually
- ✓ Single place for translation logic

**When to Use:**
```
✓ Integrating with legacy systems
✓ External systems you don't control
✓ Third-party APIs with bad design
✓ Systems using different paradigms
```

---

## Summary

### Pattern Selection Guide

**Saga Pattern**
```
Use When:
✓ Distributed transactions needed
✓ Multiple services must coordinate
✓ Need rollback capability

Example: E-commerce checkout
```

**CQRS**
```
Use When:
✓ Different read/write requirements
✓ Complex queries needed
✓ High read vs write ratio

Example: Reporting dashboard
```

**Event Sourcing**
```
Use When:
✓ Need complete audit trail
✓ Compliance requirements
✓ Time travel needed
✓ Multiple projections wanted

Example: Financial transactions
```

**API Composition**
```
Use When:
✓ Query spans multiple services
✓ Need aggregated data
✓ Simple read operations

Example: User dashboard
```

**Strangler Pattern**
```
Use When:
✓ Migrating monolith to microservices
✓ Can't do big bang rewrite
✓ Need gradual migration

Example: Legacy modernization
```

**Anti-Corruption Layer**
```
Use When:
✓ Integrating legacy systems
✓ External system has bad design
✓ Want to protect domain model

Example: Legacy ERP integration
```

---

### Key Takeaways

**1. Don't Use All Patterns**
- Start simple
- Add complexity only when needed
- Each pattern has trade-offs

**2. Patterns Often Combine**
```
Saga + Event Sourcing
CQRS + Event Sourcing
Strangler + ACL
```

**3. Team Readiness Matters**
- Advanced patterns require skill
- Good documentation essential
- Training needed

**4. Business Value First**
- Patterns are means, not ends
- Solve real problems
- Don't over-engineer

**Remember:** These are senior-level patterns because they solve complex problems. Use them wisely, not because they're "cool." Simple solutions are often better solutions!
