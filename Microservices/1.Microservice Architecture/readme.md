# Microservices Architecture - Complete Guide

## Table of Contents
1. [Monolith vs Microservices](#monolith-vs-microservices)
2. [Service Decomposition Strategies](#service-decomposition-strategies)
3. [12-Factor App Principles](#12-factor-app-principles)
4. [Stateless Services](#stateless-services)
5. [Event-Driven Architecture](#event-driven-architecture)
6. [Synchronous vs Asynchronous Communication](#synchronous-vs-asynchronous-communication)
7. [CAP Theorem](#cap-theorem)
8. [Distributed Systems Fundamentals](#distributed-systems-fundamentals)

---

## Monolith vs Microservices

### What is Monolith Architecture?

A monolith is a **single, unified application** where all features are combined into one codebase and deployed as one unit.

**Key Characteristics:**
- All code lives in one project
- Single database for entire application
- One deployment unit (single WAR/JAR file)
- All features are tightly connected
- Runs as a single process

**Simple Example:**
```
E-Commerce Monolith Application:
┌─────────────────────────────────┐
│   Single Application (JAR)      │
│                                  │
│  - User Management              │
│  - Product Catalog              │
│  - Shopping Cart                │
│  - Payment Processing           │
│  - Order Management             │
│  - Inventory Management         │
│                                  │
└─────────────────────────────────┘
         ↓
   Single Database
```

**Advantages of Monolith:**
- **Simple to develop**: Everything is in one place
- **Easy to test**: Test the whole application at once
- **Simple deployment**: Deploy one file
- **Easy debugging**: All code is together
- **No network calls**: All functions are local

**Disadvantages of Monolith:**
- **Hard to scale**: Must scale the entire application
- **Slow deployment**: Small change requires full redeployment
- **Technology lock-in**: Stuck with one technology stack
- **Team conflicts**: Everyone works on same codebase
- **Risk of failure**: One bug can crash entire application
- **Large codebase**: Becomes difficult to understand over time

---

### What is Microservices Architecture?

Microservices is an approach where you **break down your application into small, independent services**. Each service handles one specific business function.

**Key Characteristics:**
- Multiple small services
- Each service has its own database
- Independent deployment
- Services communicate over network
- Each service can use different technology

**Simple Example:**
```
E-Commerce Microservices:
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│    User      │  │   Product    │  │   Payment    │
│   Service    │  │   Service    │  │   Service    │
└──────────────┘  └──────────────┘  └──────────────┘
      ↓                 ↓                 ↓
   User DB         Product DB        Payment DB

┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│    Order     │  │  Inventory   │  │   Cart       │
│   Service    │  │   Service    │  │   Service    │
└──────────────┘  └──────────────┘  └──────────────┘
      ↓                 ↓                 ↓
   Order DB        Inventory DB       Cart DB
```

**Advantages of Microservices:**
- **Independent scaling**: Scale only what you need
- **Technology freedom**: Each service can use different tech
- **Faster deployment**: Deploy one service without touching others
- **Team autonomy**: Different teams own different services
- **Fault isolation**: One service crash doesn't kill everything
- **Easy to understand**: Each service is small and focused

**Disadvantages of Microservices:**
- **Complex infrastructure**: Need to manage many services
- **Network latency**: Services talk over network (slower)
- **Data consistency**: Hard to maintain data across services
- **Testing complexity**: Need to test service interactions
- **Monitoring difficulty**: Must monitor many services
- **Initial overhead**: More work to set up initially

---

### When to Use What?

**Use Monolith When:**
- Small team (less than 10 people)
- Simple application
- Starting a new project
- Limited resources
- Need to move fast initially

**Use Microservices When:**
- Large team (multiple teams)
- Complex application
- Need independent scaling
- Different parts need different technologies
- Have resources for infrastructure

---

## Service Decomposition Strategies

Service decomposition means **breaking down a big application into smaller services**. Here are the main strategies:

### 1. Decompose by Business Capability

Split services based on **what the business does**.

**Theory:**
Each service represents a business function or department in a company.

**Example:**
For an E-Commerce company:
```
Business Departments → Services

Marketing Department → Marketing Service
Sales Department → Order Service
HR Department → Employee Service
Finance Department → Payment Service
Warehouse → Inventory Service
Customer Support → Support Service
```

**Why This Works:**
- Matches how business thinks
- Each team owns their business area
- Changes in one business area don't affect others

---

### 2. Decompose by Subdomain (Domain-Driven Design)

Split based on **different problem domains** in your application.

**Theory:**
Identify core domains (most important), supporting domains (help core), and generic domains (common utilities).

**Example:**
```
E-Commerce Domains:

Core Domain (Critical):
- Order Management Service
- Product Catalog Service

Supporting Domain (Important):
- Shipping Service
- Recommendation Service

Generic Domain (Utility):
- Email Service
- SMS Service
```

**Why This Works:**
- Focus resources on core business
- Clear boundaries between domains
- Each domain has its own rules and logic

---

### 3. Decompose by Data Ownership

Each service **owns and manages its own data**.

**Theory:**
Services are responsible for specific data entities and no other service can directly access that data.

**Example:**
```
Data Entity → Service Owner

Customer Data → Customer Service
Product Data → Product Service
Order Data → Order Service
Payment Data → Payment Service
```

**Rule:** 
If Service A needs data from Service B, it must ask Service B through an API call, not access the database directly.

**Why This Works:**
- No shared database problems
- Clear data ownership
- Easy to change database structure

---

### 4. Decompose by Use Case / User Journey

Split based on **user actions or workflows**.

**Theory:**
Each service handles a complete user journey or use case.

**Example:**
```
User Journeys:

User Registration Journey → Registration Service
Product Search Journey → Search Service
Checkout Journey → Checkout Service
Return Product Journey → Return Service
```

**Why This Works:**
- Services align with user needs
- Easy to track user experience
- Changes to one journey don't affect others

---

### 5. Decompose by Scalability Needs

Split services based on **different scaling requirements**.

**Theory:**
Parts of application that need more resources should be separate services.

**Example:**
```
High Traffic:
- Search Service (needs 50 instances)
- Product View Service (needs 30 instances)

Medium Traffic:
- Cart Service (needs 10 instances)

Low Traffic:
- Admin Service (needs 2 instances)
- Report Service (needs 2 instances)
```

**Why This Works:**
- Save money (don't scale what doesn't need it)
- Better performance where needed
- Efficient resource usage

---

### Key Principles for Decomposition

**1. Single Responsibility**
Each service should do ONE thing well.

**2. Loose Coupling**
Services should not depend heavily on each other.

**3. High Cohesion**
Related functionality should be together in same service.

**4. Bounded Context**
Each service has clear boundaries and owns its domain.

---

## 12-Factor App Principles

The 12-Factor App is a **methodology for building modern applications** that are easy to deploy, scale, and maintain.

### Factor 1: Codebase

**Rule:** One codebase tracked in version control, many deployments.

**Theory:**
- Each service has its own Git repository
- Same code runs in dev, staging, and production
- Different configurations for each environment

**Example:**
```
One Service:
Git Repo: order-service
Deployments:
- Dev environment (localhost)
- Test environment (test.company.com)
- Production environment (app.company.com)
```

**Why:** Easy to track changes and maintain consistency.

---

### Factor 2: Dependencies

**Rule:** Explicitly declare and isolate dependencies.

**Theory:**
All dependencies should be listed in a file (like pom.xml for Maven). Never rely on system-wide packages.

**Example:**
```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.1.0</version>
    </dependency>
</dependencies>
```

**Why:** Anyone can build your app without installing extra tools.

---

### Factor 3: Config

**Rule:** Store configuration in environment variables, not in code.

**Theory:**
Things that change between environments (database URL, API keys) should be in environment variables.

**Wrong Way:**
```java
// Bad: Hardcoded in code
String dbUrl = "jdbc:mysql://localhost:3306/mydb";
```

**Right Way:**
```java
// Good: From environment variable
String dbUrl = System.getenv("DATABASE_URL");
```

**Why:** Same code works in all environments, no secrets in code.

---

### Factor 4: Backing Services

**Rule:** Treat backing services as attached resources.

**Theory:**
Databases, message queues, caches are external services. Connect via URL, easy to swap.

**Example:**
```
Today: MySQL database
Tomorrow: PostgreSQL database
Change: Just update DATABASE_URL environment variable
```

**Why:** Easy to switch services without code changes.

---

### Factor 5: Build, Release, Run

**Rule:** Strictly separate build, release, and run stages.

**Theory:**
- **Build:** Convert code to executable (JAR file)
- **Release:** Combine build with config
- **Run:** Execute the application

**Example:**
```
Build Stage: mvn clean package → order-service.jar
Release Stage: order-service.jar + prod-config → release-v1.2
Run Stage: java -jar release-v1.2.jar
```

**Why:** Clear separation, easy rollback to previous releases.

---

### Factor 6: Processes

**Rule:** Execute app as stateless processes.

**Theory:**
Your application should not store data in memory or local disk. Use external services for storage.

**Example:**
```
Wrong: Store user session in application memory
Right: Store user session in Redis cache
```

**Why:** Can scale horizontally (add more instances).

---

### Factor 7: Port Binding

**Rule:** Export services via port binding.

**Theory:**
Your app should be self-contained and listen on a port for requests.

**Example:**
```java
@SpringBootApplication
public class OrderService {
    public static void main(String[] args) {
        SpringApplication.run(OrderService.class, args);
        // App runs on port 8080
    }
}
```

**Why:** No need for separate web server, app is independent.

---

### Factor 8: Concurrency

**Rule:** Scale out via the process model.

**Theory:**
Run multiple instances of your app to handle more load.

**Example:**
```
Low Traffic: 2 instances of order-service
High Traffic: 10 instances of order-service
```

**Why:** Easy horizontal scaling.

---

### Factor 9: Disposability

**Rule:** Fast startup and graceful shutdown.

**Theory:**
App should start quickly and shut down gracefully when needed.

**Example:**
```
Startup: App should be ready in seconds
Shutdown: Complete current requests before stopping
```

**Why:** Easy to scale up/down quickly.

---

### Factor 10: Dev/Prod Parity

**Rule:** Keep development and production as similar as possible.

**Theory:**
Use same database, same services in dev and prod.

**Example:**
```
Wrong: Use H2 in dev, MySQL in prod
Right: Use MySQL in both dev and prod
```

**Why:** Catch bugs early, no surprises in production.

---

### Factor 11: Logs

**Rule:** Treat logs as event streams.

**Theory:**
App writes logs to stdout, external tool collects them.

**Example:**
```java
// App just prints logs
System.out.println("Order created: " + orderId);

// External tool (like ELK) collects and stores logs
```

**Why:** Centralized logging, easy to search and analyze.

---

### Factor 12: Admin Processes

**Rule:** Run admin tasks as one-off processes.

**Theory:**
Database migrations, data cleanup should run as separate processes.

**Example:**
```
Regular App: java -jar order-service.jar
Admin Task: java -jar order-service.jar --migrate-database
```

**Why:** Separate concerns, admin tasks don't interfere with app.

---

## Stateless Services

### What is Stateless?

**Stateless** means the service **does not remember anything** about previous requests. Each request is independent.

**Theory:**
Every request contains all information needed. Service doesn't store any client data between requests.

### Stateless Example

```
Request 1: Get user profile (userId=123)
→ Service looks up user 123 in database
→ Returns profile

Request 2: Get user profile (userId=123)
→ Service looks up user 123 in database again
→ Returns profile

Service doesn't remember Request 1 happened
```

**Key Point:** Service forgets everything after responding.

---

### What is Stateful?

**Stateful** means the service **remembers information** about the client.

### Stateful Example

```
Request 1: Login (username, password)
→ Service creates session in memory
→ Returns session token

Request 2: Get profile (session token)
→ Service checks session in its memory
→ Returns profile

Service remembers the session from Request 1
```

---

### Why Stateless is Better for Microservices

**1. Easy Scaling**
```
Stateless:
Request 1 → Instance A
Request 2 → Instance B (works fine)

Stateful:
Request 1 → Instance A (session stored here)
Request 2 → Instance B (fails - no session)
```

**2. No Memory Overhead**
Stateless services don't fill up memory with session data.

**3. Fault Tolerance**
If one instance crashes, others can handle requests.

**4. Load Balancing**
Any instance can handle any request.

---

### How to Make Services Stateless

**1. Store State Externally**
```
Instead of: Storing session in service memory
Do this: Store session in Redis cache

Request → Service → Check Redis → Response
```

**2. Pass Everything in Request**
```
Bad: Service remembers user preferences
Good: Client sends preferences with each request
```

**3. Use Tokens**
```
Login → Generate JWT token → Send to client
Next Request → Client sends JWT → Service validates → Response

Service doesn't store token, just validates it
```

**4. Database for Persistence**
```
Store user data, orders, products in database
Service queries database for each request
```

---

### Real-World Example

**Stateless Order Service:**
```java
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        // All info comes in request
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProducts(request.getProducts());
        
        // Save to database (external state)
        database.save(order);
        
        // Return response
        return order;
        
        // Service forgets everything after this
    }
}
```

Every request provides all needed information. Service doesn't remember anything.

---

## Event-Driven Architecture

### What is Event-Driven Architecture?

**Event-Driven Architecture (EDA)** is a design pattern where services **communicate by sending and receiving events**.

**Event:** Something that happened (past tense)
- "Order was created"
- "Payment was completed"
- "User was registered"

### How It Works

**Theory:**
1. Service A does something
2. Service A publishes an event
3. Other services listen for events
4. Services react to events they care about

**Simple Example:**
```
User places an order:

1. Order Service creates order
2. Order Service publishes "OrderCreated" event
3. Inventory Service listens, reduces stock
4. Email Service listens, sends confirmation email
5. Analytics Service listens, updates dashboard

Order Service doesn't know who's listening
Listeners don't know who published
```

---

### Key Components

**1. Event Producer**
Service that creates and publishes events.

**2. Event**
Message containing data about what happened.

**3. Event Channel (Message Broker)**
Middleware that delivers events (Kafka, RabbitMQ).

**4. Event Consumer**
Service that listens and reacts to events.

**Diagram:**
```
Producer → Event Channel → Consumer 1
                       → Consumer 2
                       → Consumer 3
```

---

### Types of Events

**1. Domain Events**
Business-related events.
```
- OrderPlaced
- PaymentProcessed
- ProductOutOfStock
- UserRegistered
```

**2. System Events**
Technical events.
```
- ServiceStarted
- DatabaseConnected
- ErrorOccurred
```

---

### Event Structure

**Simple Event Example:**
```json
{
  "eventId": "12345",
  "eventType": "OrderCreated",
  "timestamp": "2024-02-04T10:30:00Z",
  "data": {
    "orderId": "ORD-001",
    "userId": "USER-123",
    "totalAmount": 99.99,
    "items": [
      {"productId": "PROD-1", "quantity": 2}
    ]
  }
}
```

---

### Benefits of Event-Driven Architecture

**1. Loose Coupling**
Services don't know about each other.
```
Order Service publishes event
It doesn't know:
- Who will receive it
- How many services will react
- What they will do with it
```

**2. Scalability**
Add new consumers without changing producers.
```
Today: 3 services listen to OrderCreated
Tomorrow: Add 2 more services
Order Service code: No changes needed
```

**3. Flexibility**
Easy to add new features.
```
New Requirement: Send SMS on order
Solution: Add SMS Service as new consumer
Changes: Zero code changes to existing services
```

**4. Resilience**
If one consumer fails, others continue working.
```
OrderCreated event published
- Email Service: Working ✓
- Analytics Service: Down ✗
- Inventory Service: Working ✓

Order processing continues
```

---

### Challenges

**1. Eventual Consistency**
Data is not immediately consistent across services.
```
Event: OrderCreated
Time 0ms: Order Service has order
Time 50ms: Inventory Service updates stock
Time 100ms: Email Service sends email

Not instant, takes time
```

**2. Debugging Difficulty**
Hard to trace event flow across services.

**3. Event Ordering**
Events might arrive out of order.
```
Event 1: OrderCreated
Event 2: OrderCancelled

If Event 2 arrives before Event 1, problems occur
```

**4. Duplicate Events**
Same event might be delivered twice.
```
Consumer must handle:
- Processing same event multiple times
- Idempotent operations (safe to repeat)
```

---

### Event-Driven Patterns

**1. Event Notification**
Simple notification that something happened.
```
Event: "OrderCreated"
Data: Just orderId
Consumer: Calls Order Service for details
```

**2. Event-Carried State Transfer**
Event contains all needed data.
```
Event: "OrderCreated"
Data: Complete order details
Consumer: Has everything, no need to call Order Service
```

**3. Event Sourcing**
Store all events as sequence of state changes.
```
Events:
1. AccountCreated (balance: $0)
2. MoneyDeposited (amount: $100)
3. MoneyWithdrawn (amount: $30)

Current State = Replay all events
Balance = $0 + $100 - $30 = $70
```

---

### Real-World Example

**E-Commerce Order Flow:**

```java
// Order Service - Producer
public class OrderService {
    
    public void createOrder(OrderRequest request) {
        // 1. Create order
        Order order = new Order(request);
        database.save(order);
        
        // 2. Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getItems(),
            order.getTotal()
        );
        
        eventPublisher.publish("order.created", event);
        // Done! Other services will react
    }
}

// Inventory Service - Consumer
public class InventoryService {
    
    @EventListener("order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Reduce stock for ordered items
        for (Item item : event.getItems()) {
            reduceStock(item.getProductId(), item.getQuantity());
        }
    }
}

// Email Service - Consumer
public class EmailService {
    
    @EventListener("order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Send confirmation email
        sendEmail(event.getUserId(), "Order Confirmed");
    }
}
```

Order Service doesn't call Inventory or Email Service directly. They react independently to the event.

---

## Synchronous vs Asynchronous Communication

### Synchronous Communication

**Definition:** Service waits for response before continuing.

**Theory:**
- Service A calls Service B
- Service A stops and waits
- Service B processes and responds
- Service A continues with response

**Example - HTTP REST API:**
```
User Service calls Payment Service:

1. User Service: "Process payment $100"
2. User Service: Waits...
3. Payment Service: Processing...
4. Payment Service: "Payment successful"
5. User Service: Receives response
6. User Service: Continues work
```

**Code Example:**
```java
public class UserService {
    
    public void checkout(Order order) {
        // Call Payment Service and WAIT
        PaymentResponse response = paymentServiceClient.processPayment(order);
        
        // Can't proceed until response arrives
        if (response.isSuccess()) {
            completeOrder(order);
        }
    }
}
```

**Characteristics:**
- Request-Response pattern
- Immediate response expected
- Service waits (blocked)
- Usually HTTP/REST or gRPC

---

### Asynchronous Communication

**Definition:** Service doesn't wait for response, continues immediately.

**Theory:**
- Service A sends message
- Service A continues immediately (doesn't wait)
- Service B processes when ready
- Service B might respond later (or not at all)

**Example - Message Queue:**
```
User Service sends to Payment Service:

1. User Service: Sends "Process payment $100" to queue
2. User Service: Continues immediately
3. User Service: Does other work
4. Payment Service: Picks message from queue
5. Payment Service: Processes payment
6. Payment Service: Sends result to another queue (optional)
```

**Code Example:**
```java
public class UserService {
    
    public void checkout(Order order) {
        // Send message and DON'T WAIT
        messageQueue.send("payment.queue", order);
        
        // Immediately continues
        logActivity("Payment request sent");
        // Do other work...
    }
}
```

**Characteristics:**
- Fire-and-forget or event-based
- No immediate response
- Service continues working
- Usually message queues (Kafka, RabbitMQ)

---

### Detailed Comparison

**1. Response Time**

Synchronous:
```
Total Time = Processing Time + Network Time
User waits for everything to complete
```

Asynchronous:
```
Total Time = Time to send message (very fast)
Processing happens in background
```

---

**2. Failure Handling**

Synchronous:
```
Payment Service is down
↓
User Service gets error immediately
↓
User sees error message
```

Asynchronous:
```
Payment Service is down
↓
Message stays in queue
↓
User sees "Processing..." 
↓
Payment Service comes back up
↓
Message processed later
```

---

**3. Coupling**

Synchronous:
```
User Service must know:
- Payment Service URL
- Payment Service API
- Payment Service must be running NOW

Tight coupling
```

Asynchronous:
```
User Service must know:
- Queue name
- Message format

Payment Service can be down
Message waits in queue

Loose coupling
```

---

**4. Scalability**

Synchronous:
```
If Payment Service is slow:
- User Service waits longer
- More threads blocked
- Can handle fewer requests
```

Asynchronous:
```
If Payment Service is slow:
- Messages queue up
- User Service unaffected
- Can handle same number of requests
```

---

### When to Use Synchronous

**Use When:**
1. Need immediate response
2. Simple request-response flow
3. User waiting for result
4. Operation must complete before proceeding

**Examples:**
```
- Login (need token now)
- Search products (show results immediately)
- Check stock availability (before adding to cart)
- Validate coupon code (immediate feedback)
```

---

### When to Use Asynchronous

**Use When:**
1. Long-running operations
2. Don't need immediate response
3. Can process later
4. Want loose coupling

**Examples:**
```
- Send email (can happen in background)
- Generate report (can take minutes)
- Process video upload (can take hours)
- Update analytics (not urgent)
- Batch processing
```

---

### Hybrid Approach

Many systems use both!

**Example - Order Processing:**

```
Synchronous Parts:
1. Validate order (need immediate feedback)
2. Check stock (must know before ordering)
3. Calculate price (show to user now)

Asynchronous Parts:
4. Send confirmation email (can wait)
5. Update analytics (not urgent)
6. Notify warehouse (background task)
7. Process loyalty points (can be delayed)
```

**Code Example:**
```java
public class OrderService {
    
    public OrderResponse createOrder(OrderRequest request) {
        // Synchronous - must complete now
        validateOrder(request);
        PaymentResponse payment = paymentClient.processPayment(request);
        
        if (payment.isSuccess()) {
            Order order = saveOrder(request);
            
            // Asynchronous - fire and forget
            messageQueue.send("email.queue", order);
            messageQueue.send("analytics.queue", order);
            messageQueue.send("warehouse.queue", order);
            
            return new OrderResponse(order.getId(), "Success");
        }
    }
}
```

---

### Real-World Trade-offs

**Synchronous:**
- ✓ Simple to implement
- ✓ Easy to debug
- ✓ Immediate feedback
- ✗ Services must be available
- ✗ Slower overall
- ✗ Cascading failures

**Asynchronous:**
- ✓ Better scalability
- ✓ Fault tolerant
- ✓ Loose coupling
- ✗ Complex to implement
- ✗ Harder to debug
- ✗ Eventual consistency

---

## CAP Theorem

### What is CAP Theorem?

**CAP Theorem** states that in a distributed system, you can only have **TWO out of THREE** properties at the same time.

**The Three Properties:**

**C - Consistency**
All nodes see the same data at the same time.

**A - Availability**  
Every request gets a response (success or failure).

**P - Partition Tolerance**
System continues working even if network fails between nodes.

**Key Rule:** You can only choose 2 out of 3 (CA, CP, or AP).

---

### Understanding Each Property

**1. Consistency (C)**

**Theory:**
When you write data, all future reads get that new data. Everyone sees the same value.

**Example:**
```
Time 1: User updates address to "New York"
Time 2: User reads address from ANY server
Result: All servers show "New York"

If one server shows old address, NOT consistent
```

**Simple Scenario:**
```
Bank Account Balance: $100

Transaction: Withdraw $50
↓
All ATMs must show $50 immediately
Not: Some show $100, some show $50
```

---

**2. Availability (A)**

**Theory:**
System always responds to requests, even if some nodes are down.

**Example:**
```
3 servers: Server1, Server2, Server3

Server2 is down
↓
Request still gets response from Server1 or Server3
System stays available
```

**Simple Scenario:**
```
E-commerce website:

One database server crashes
↓
Website still works using other servers
Users can still shop
```

---

**3. Partition Tolerance (P)**

**Theory:**
System continues working even when network splits (servers can't talk to each other).

**Example:**
```
Data Center A (New York) ←→ Data Center B (London)

Network cable breaks
↓
Both data centers work independently
System continues functioning
```

**Simple Scenario:**
```
Server1 and Server2 can't communicate

Server1: Receives requests and responds
Server2: Receives requests and responds
Both work separately
```

---

### The Trade-offs

In distributed systems, **network partition WILL happen** (cables break, routers fail). So, you MUST choose Partition Tolerance (P).

This leaves you choosing between:
- **CP:** Consistency + Partition Tolerance (sacrifice Availability)
- **AP:** Availability + Partition Tolerance (sacrifice Consistency)

---

### CP System (Consistency + Partition Tolerance)

**Theory:**
When network partitions, system becomes unavailable rather than returning wrong data.

**Example:**
```
Bank Database System:

Network partition happens
↓
Can't guarantee all servers have same balance
↓
System STOPS responding (unavailable)
↓
Better than showing wrong balance
```

**Real System: MongoDB (with strong consistency)**
```
3 servers: Primary, Secondary1, Secondary2

Network splits
↓
Secondary1 can't reach Primary
↓
Secondary1 REFUSES read requests
↓
Unavailable, but data is consistent
```

**When to Use:**
- Banking systems (accurate balance critical)
- Inventory management (must know exact stock)
- Booking systems (can't double-book)

**Trade-off:** Some requests fail during network issues.

---

### AP System (Availability + Partition Tolerance)

**Theory:**
When network partitions, system stays available but data might be inconsistent.

**Example:**
```
Social Media Like Counter:

Network partition happens
↓
Server1 shows 100 likes
Server2 shows 102 likes
↓
Both respond (available)
↓
Data is slightly inconsistent (acceptable)
```

**Real System: Cassandra, DynamoDB**
```
3 servers with post data

Network splits
↓
User A posts comment → saved to Server1
User B posts comment → saved to Server2
↓
Both servers accept writes (available)
↓
Comments not immediately visible everywhere
```

**When to Use:**
- Social media (small inconsistencies okay)
- Shopping cart (can sync later)
- User preferences (not critical)
- Analytics (approximate counts okay)

**Trade-off:** Users might see different data temporarily.

---

### Visual Comparison

**CP System - Banking:**
```
User: "What's my balance?"

Normal: "$100" (from any server)

Network Partition:
Server can't verify with other servers
↓
Response: "Service temporarily unavailable"
↓
User frustrated but data is CORRECT
```

**AP System - Social Media:**
```
User: "How many likes on my post?"

Normal: "42 likes" (from any server)

Network Partition:
Server1: "42 likes"
Server2: "44 likes"
↓
Different users see different numbers
↓
System AVAILABLE but data is INCONSISTENT
Eventually syncs to 44 likes
```

---

### Real-World Examples

**CP Systems:**
```
1. Banking: Account Balance
   - Must be correct always
   - Okay to fail temporarily

2. E-Commerce: Inventory Count
   - Can't oversell products
   - Better to show "unavailable" than wrong count

3. Booking: Seat Reservation
   - Can't double-book seat
   - Okay to reject bookings during outage
```

**AP Systems:**
```
1. Facebook: Friend Count
   - 1,042 or 1,044 doesn't matter much
   - Must always be available

2. Twitter: Tweet Count
   - Approximate is fine
   - More important to show page

3. Shopping Cart (Amazon)
   - Can sync later
   - User can keep shopping
```

---

### Eventual Consistency

**Theory:**
In AP systems, data becomes consistent eventually (not immediately).

**Example:**
```
Time 0: User posts comment
Time 1ms: Saved to Server1
Time 100ms: Replicated to Server2
Time 200ms: Replicated to Server3

Between 1ms and 200ms: Inconsistent
After 200ms: Consistent everywhere

This is "eventual consistency"
```

---

### Practical Decision Guide

**Choose CP When:**
- Financial data
- Inventory counts
- Bookings/reservations
- Data must be 100% accurate

**Choose AP When:**
- Social features
- Analytics
- Caching
- Small inconsistencies acceptable

**Note:** Most modern systems use **AP with eventual consistency** because users prefer fast, available systems over temporarily inconsistent data.

---

## Distributed Systems Fundamentals

### What is a Distributed System?

**Definition:** A system where components on different computers communicate and coordinate to achieve a common goal.

**Simple Explanation:**
Instead of one big computer doing everything, multiple computers work together.

**Example:**
```
Monolithic (One Computer):
┌─────────────────────┐
│   One Big Server    │
│   - Database        │
│   - Application     │
│   - Files           │
└─────────────────────┘

Distributed (Multiple Computers):
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Server 1 │  │ Server 2 │  │ Server 3 │
│ Database │  │   App    │  │  Files   │
└──────────┘  └──────────┘  └──────────┘
      ↕️            ↕️            ↕️
         Network Communication
```

---

### Key Characteristics

**1. No Shared Memory**
Each computer has its own memory.
```
Server A knows: User logged in
Server B knows: Nothing (unless told)

Must communicate to share information
```

**2. Concurrent Execution**
Multiple things happen at the same time.
```
Server 1: Processing Order A
Server 2: Processing Order B (simultaneously)
Server 3: Sending Email C (simultaneously)
```

**3. Network Communication**
Computers talk over network (not guaranteed to work).
```
Server A → Network → Server B
Network can: Be slow, fail, lose messages
```

**4. Independent Failures**
One computer can fail without killing others.
```
Server 1: Down ❌
Server 2: Running ✓
Server 3: Running ✓

System partially works
```

---

### Core Challenges

### Challenge 1: Network is Unreliable

**Problem:**
Messages can get lost, delayed, or arrive out of order.

**Example:**
```
Order Service → Payment Service: "Charge $100"

Possible outcomes:
1. Message arrives instantly ✓
2. Message delayed by 5 seconds
3. Message lost completely
4. Message arrives twice (duplicate)
```

**Solution Strategies:**
- Retry failed requests
- Use timeouts
- Handle duplicates (idempotency)
- Use message acknowledgments

---

### Challenge 2: Partial Failures

**Problem:**
Some parts fail while others work.

**Example:**
```
User places order:

Step 1: Order Service ✓ (works)
Step 2: Payment Service ✓ (works)
Step 3: Inventory Service ❌ (fails)
Step 4: Email Service ✓ (works)

Result: Partial success
Order created, payment done, but inventory not updated
```

**Solution Strategies:**
- Implement retry logic
- Use circuit breakers
- Design for graceful degradation
- Use compensation transactions

---

### Challenge 3: Latency

**Problem:**
Network calls are slow compared to local calls.

**Example:**
```
Local Function Call:
Function call: 1 microsecond (0.001ms)

Network Call:
HTTP request: 50 milliseconds (50ms)

Network is 50,000x slower!
```

**Impact:**
```
Calling 10 services sequentially:
10 services × 50ms = 500ms total

User waits half a second
```

**Solution Strategies:**
- Async communication
- Caching
- Parallel calls
- Reduce network hops

---

### Challenge 4: Data Consistency

**Problem:**
Keeping data synchronized across multiple databases.

**Example:**
```
User updates profile:

Time 0: Update User Service database
Time 100ms: Update Cache
Time 200ms: Update Analytics database
Time 300ms: Update Search index

Between 0-300ms: Inconsistent data
Some systems have old data, some have new
```

**Solution Strategies:**
- Event-driven updates
- Eventual consistency
- Distributed transactions (rare)
- Accept temporary inconsistency

---

### Challenge 5: Clock Synchronization

**Problem:**
Each computer has its own clock, clocks drift apart.

**Example:**
```
Server A clock: 10:00:00.000
Server B clock: 10:00:00.050 (50ms ahead)

Event 1 on Server B: Timestamp 10:00:00.040
Event 2 on Server A: Timestamp 10:00:00.045

Which happened first? Can't tell reliably!
```

**Solution Strategies:**
- Use logical clocks (version numbers)
- Use vector clocks
- Don't rely on timestamps for ordering

---

### Fundamental Concepts

### 1. Replication

**Theory:**
Keep copies of data on multiple servers.

**Purpose:** Reliability and performance

**Example:**
```
Primary Database: User table
Replica 1: Copy of user table
Replica 2: Copy of user table

If primary fails, use replica
```

**Types:**

**Master-Slave Replication:**
```
Master: Handles writes
Slaves: Handle reads

Write → Master → Slaves
Read ← Slaves
```

**Master-Master Replication:**
```
Master 1: Handles writes and reads
Master 2: Handles writes and reads

Both sync with each other
```

---

### 2. Partitioning (Sharding)

**Theory:**
Split data across multiple servers.

**Purpose:** Handle more data than one server can store

**Example:**
```
Users A-M → Server 1
Users N-Z → Server 2

Each server stores half the data
```

**Types:**

**Horizontal Partitioning (Sharding):**
```
User 1-1000 → Shard 1
User 1001-2000 → Shard 2
User 2001-3000 → Shard 3
```

**Vertical Partitioning:**
```
User Profile data → Server 1
User Orders data → Server 2
User Payments data → Server 3
```

---

### 3. Consensus

**Theory:**
Multiple servers must agree on a value.

**Purpose:** Make decisions in distributed system

**Example:**
```
3 servers need to decide: Who is the leader?

Server 1 votes: Server 1
Server 2 votes: Server 1
Server 3 votes: Server 2

Majority (2/3) agrees: Server 1 is leader
```

**Common Algorithms:**
- Paxos (complex, proven)
- Raft (simpler, popular)
- Zab (used by ZooKeeper)

---

### 4. Leader Election

**Theory:**
Choose one server as coordinator among many.

**Purpose:** Avoid conflicts, centralize decisions

**Example:**
```
5 database servers

Normal operation:
- Leader: Handles writes
- Followers: Handle reads

Leader fails:
- Followers detect failure
- Hold election
- Choose new leader
- Resume operations
```

---

### 5. Distributed Transactions

**Theory:**
Transaction spans multiple databases/services.

**Purpose:** Maintain consistency across services

**Problem:**
```
Book Flight + Charge Payment

Flight Service: Reserves seat
Payment Service: Charges card

What if payment fails after seat reserved?
Need both to succeed or both to fail
```

**Solution - Two-Phase Commit:**
```
Phase 1 - Prepare:
Coordinator: "Can you commit?"
Flight Service: "Yes, ready"
Payment Service: "Yes, ready"

Phase 2 - Commit:
Coordinator: "Commit now!"
Flight Service: Commits
Payment Service: Commits

All or nothing
```

**Problem with 2PC:** Slow and blocking

**Modern Solution - Saga Pattern:**
```
Step 1: Reserve seat
Step 2: Charge payment
If Step 2 fails: Compensate Step 1 (cancel reservation)

Each step can be undone
```

---

### 6. Idempotency

**Theory:**
Performing same operation multiple times produces same result.

**Purpose:** Handle duplicate messages safely

**Example:**

**Not Idempotent:**
```
addToBalance(100)

Call 1: Balance $0 → $100
Call 2: Balance $100 → $200 (wrong!)
```

**Idempotent:**
```
setBalance(transactionId, 100)

Call 1: Balance $0 → $100
Call 2: Balance $100 → $100 (same result)

Uses transaction ID to detect duplicates
```

---

### 7. Circuit Breaker

**Theory:**
Prevent cascading failures by stopping calls to failing service.

**Purpose:** Protect system from overload

**States:**
```
Closed → Normal operation, requests pass through
Open → Service failing, reject requests immediately  
Half-Open → Test if service recovered
```

**Example:**
```
Payment Service keeps failing

After 5 failures:
- Circuit opens
- Stop calling Payment Service for 30 seconds
- Return error immediately
- After 30 seconds, try one request (half-open)
- If succeeds, close circuit
- If fails, stay open another 30 seconds
```

---

### 8. Service Discovery

**Theory:**
Services find each other dynamically.

**Purpose:** Don't hardcode service locations

**Example:**
```
Without Service Discovery:
Order Service code: "http://payment-service:8080"
(Hardcoded, if IP changes, code breaks)

With Service Discovery:
Order Service: "Where is payment-service?"
Service Registry: "payment-service is at 192.168.1.50:8080"
Order Service: Calls that address

Service Registry keeps updated list
```

**Tools:** Consul, Eureka, ZooKeeper

---

### 9. Load Balancing

**Theory:**
Distribute requests across multiple servers.

**Purpose:** No single server gets overloaded

**Example:**
```
100 requests come in:

Without Load Balancer:
Server 1: 100 requests (overloaded)
Server 2: 0 requests (idle)
Server 3: 0 requests (idle)

With Load Balancer:
Server 1: 33 requests
Server 2: 33 requests
Server 3: 34 requests
```

**Strategies:**
- Round Robin: Rotate between servers
- Least Connections: Send to server with fewest active connections
- Random: Pick random server

---

### 10. Eventual Consistency

**Theory:**
Data will become consistent eventually, not immediately.

**Purpose:** Allow system to stay available

**Example:**
```
User changes profile picture:

Time 0: Updated in User Service
Time 50ms: Old picture in Cache
Time 100ms: Old picture in CDN
Time 500ms: Updated everywhere

Eventually consistent (takes 500ms)
```

**Real Example - Social Media:**
```
Post a comment

Your view: Comment visible immediately
Friend's view: Sees comment after 2 seconds

Eventually everyone sees same comment
```

---

### Putting It All Together

**Real-World Microservices System:**

```
User Request
    ↓
API Gateway (Load Balanced)
    ↓
Service Discovery (Find services)
    ↓
Order Service (Leader + 2 Replicas)
    ↓ (Async Event)
Message Queue (Partitioned)
    ↓
Payment Service (Circuit Breaker)
    ↓
Saga Coordinator (Distributed Transaction)
    ↓
Database (Sharded + Replicated)
```

**This system uses:**
- Load Balancing (API Gateway)
- Service Discovery (dynamic routing)
- Replication (high availability)
- Partitioning (handle large data)
- Leader Election (coordinated writes)
- Circuit Breaker (fault tolerance)
- Saga Pattern (distributed transactions)
- Eventual Consistency (async events)

---

## Summary

### Key Takeaways

**1. Monolith vs Microservices**
- Monolith: One application, simple, hard to scale
- Microservices: Many services, complex, easy to scale
- Choose based on team size and complexity

**2. Service Decomposition**
- Break by business capability
- Break by subdomain
- Break by data ownership
- Keep services focused and independent

**3. 12-Factor App**
- Configuration in environment
- Stateless processes
- Explicit dependencies
- Dev/prod parity
- Makes apps portable and scalable

**4. Stateless Services**
- Don't store data in memory
- Use external storage (Redis, database)
- Easy to scale horizontally
- Any instance can handle any request

**5. Event-Driven Architecture**
- Services communicate via events
- Loose coupling
- Easy to add new features
- Eventual consistency

**6. Sync vs Async**
- Synchronous: Wait for response (HTTP)
- Asynchronous: Don't wait (message queues)
- Use both based on needs

**7. CAP Theorem**
- Can't have all three: Consistency, Availability, Partition Tolerance
- Choose CP (banking) or AP (social media)
- Most choose AP with eventual consistency

**8. Distributed Systems**
- Multiple computers working together
- Network is unreliable
- Partial failures happen
- Use replication, partitioning, consensus
- Design for failure

---

**Remember:** Microservices add complexity. Use them when benefits outweigh costs. Start simple, evolve as needed.
