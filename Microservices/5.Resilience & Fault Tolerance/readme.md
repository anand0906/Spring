# Resilience & Fault Tolerance in Microservices

## Table of Contents
1. [Introduction to Resilience](#introduction-to-resilience)
2. [Circuit Breaker Pattern](#circuit-breaker-pattern)
3. [Retry Mechanisms](#retry-mechanisms)
4. [Timeouts](#timeouts)
5. [Bulkhead Pattern](#bulkhead-pattern)
6. [Rate Limiting](#rate-limiting)
7. [Resilience4j Integration](#resilience4j-integration)
8. [Combining Patterns](#combining-patterns)

---

## Introduction to Resilience

### Why Resilience Matters

**The Reality of Distributed Systems:**
```
In microservices:
- Services WILL fail
- Networks WILL be slow
- Databases WILL timeout
- Servers WILL crash

This is not "if" but "WHEN"
```

**Without Resilience:**
```
Payment Service down (1 service)
    ↓
Order Service fails (waiting for payment)
    ↓
User Service fails (depends on orders)
    ↓
Entire system crashes (cascading failure)

One small failure → Complete outage
```

**With Resilience:**
```
Payment Service down
    ↓
Circuit breaker opens
    ↓
Order Service uses fallback (deferred payment)
    ↓
User Service continues working
    ↓
System degraded but functional

Graceful degradation, not total failure
```

---

### Key Resilience Principles

**1. Fail Fast**
```
Don't wait 30 seconds for timeout
Detect failure quickly
Return error or fallback immediately
```

**2. Isolate Failures**
```
One service failure shouldn't crash others
Compartmentalize problems
Limit blast radius
```

**3. Provide Fallbacks**
```
Have Plan B, C, D
Cached data
Default responses
Degraded functionality
```

**4. Recover Automatically**
```
Retry transient failures
Test if service recovered
Resume normal operation
```

**5. Monitor Everything**
```
Track failures
Measure response times
Alert on anomalies
Learn from failures
```

---

### Common Failure Scenarios

**1. Service Unavailable**
```
Service crashed or not responding
Network partition
Server down for maintenance
```

**2. Slow Response**
```
Service overloaded
Database query slow
Network congestion
```

**3. Partial Failure**
```
Some instances down
Some requests succeed, others fail
Intermittent errors
```

**4. Cascading Failure**
```
Service A slow → Service B waits → Service B slow
Service B slow → Service C waits → Service C slow
All services become slow/unavailable
```

---

## Circuit Breaker Pattern

### What is Circuit Breaker?

**Definition:** Automatically stop calling a failing service and provide fallback response.

**Simple Analogy:**
```
Circuit Breaker = Electrical Circuit Breaker

Normal operation → Electricity flows
Too much current → Breaker trips (opens)
Electricity stops → Prevents fire
Reset breaker → Try again

Same concept for service calls
```

---

### Three States Explained

**1. CLOSED (Normal Operation)**
```
State: Working fine
Behavior: Requests pass through
Monitoring: Count failures
Transition: If failure rate > threshold → OPEN

Example:
10 calls: 9 success, 1 failure (10% failure rate)
Threshold: 50%
Action: Stay CLOSED (below threshold)
```

**2. OPEN (Service Failing)**
```
State: Service is down/slow
Behavior: Reject requests immediately (no waiting)
Fallback: Return cached data or default response
Duration: Wait for timeout period (e.g., 10 seconds)
Transition: After timeout → HALF_OPEN

Example:
10 calls: 4 success, 6 failures (60% failure rate)
Threshold: 50%
Action: Open circuit
Next requests: Fail fast with fallback (don't call service)
```

**3. HALF_OPEN (Testing Recovery)**
```
State: Testing if service recovered
Behavior: Allow limited test requests
Monitoring: Check success rate
Transition: Success → CLOSED | Failure → OPEN

Example:
After 10 seconds in OPEN:
Allow 3 test requests
If all 3 succeed → Service recovered → CLOSED
If any fails → Service still down → OPEN for another 10s
```

---

### Circuit Breaker Flow

```
Request comes in
    ↓
Check circuit state
    ↓
CLOSED? → Call service → Success? → Continue
                       → Failure? → Record failure
                                 → Failure rate > threshold? → OPEN
    ↓
OPEN? → Don't call service → Return fallback immediately
        Wait for timeout
        After timeout → HALF_OPEN
    ↓
HALF_OPEN? → Allow test request → Success? → CLOSED
                                → Failure? → OPEN
```

---

### Resilience4j Circuit Breaker Setup

**Step 1: Add Dependency (pom.xml)**

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**Step 2: Configuration (application.yml)**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10                    # Track last 10 calls
        failure-rate-threshold: 50                 # Open if 50% fail
        slow-call-rate-threshold: 50               # Open if 50% slow
        slow-call-duration-threshold: 2s           # Call is "slow" if > 2s
        wait-duration-in-open-state: 10s           # Stay OPEN for 10s
        permitted-number-of-calls-in-half-open-state: 3  # Test with 3 calls
        automatic-transition-from-open-to-half-open-enabled: true
        sliding-window-type: COUNT_BASED           # Or TIME_BASED
```

**Step 3: Use Circuit Breaker**

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        // Call payment service
        // If this fails repeatedly, circuit opens
        return paymentClient.process(request);
    }
    
    // Fallback method - must have same signature + Exception parameter
    private PaymentResponse paymentFallback(PaymentRequest request, Exception e) {
        // Circuit is OPEN or service failed
        // Return fallback response
        
        logger.warn("Payment service unavailable, using fallback: {}", e.getMessage());
        
        return PaymentResponse.builder()
            .status("PENDING")
            .message("Payment will be processed later")
            .transactionId("DEFERRED-" + UUID.randomUUID())
            .build();
    }
}
```

---

### Circuit Breaker Scenarios

**Scenario 1: Normal Operation**
```
Call 1: processPayment() → Success → Record success
Call 2: processPayment() → Success → Record success
Call 3: processPayment() → Success → Record success
Call 4: processPayment() → Failure → Record failure
Call 5: processPayment() → Success → Record success

Last 5 calls: 4 success, 1 failure (20% failure rate)
Threshold: 50%
Circuit: CLOSED (normal operation)
```

**Scenario 2: Service Degrades**
```
Call 6: processPayment() → Failure → Record failure
Call 7: processPayment() → Failure → Record failure
Call 8: processPayment() → Failure → Record failure
Call 9: processPayment() → Failure → Record failure
Call 10: processPayment() → Failure → Record failure

Last 10 calls: 4 success, 6 failures (60% failure rate)
Threshold: 50%
Circuit: OPEN (stop calling service)
```

**Scenario 3: Fast Fail**
```
Call 11: Circuit is OPEN → Don't call paymentClient.process()
                        → Call paymentFallback() immediately
                        → Return "PENDING" status

Call 12: Circuit is OPEN → paymentFallback() immediately
Call 13: Circuit is OPEN → paymentFallback() immediately

No waiting for timeouts!
Fast response with fallback
```

**Scenario 4: Recovery Test**
```
After 10 seconds:
Circuit: HALF_OPEN (test if recovered)

Call 14: processPayment() → Success → 1/3 test calls
Call 15: processPayment() → Success → 2/3 test calls
Call 16: processPayment() → Success → 3/3 test calls

All test calls succeeded
Circuit: CLOSED (back to normal)
```

---

### Advanced Circuit Breaker Configuration

**Count-Based Sliding Window:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 100           # Last 100 calls
        minimum-number-of-calls: 10        # Need 10 calls before calculating rate
        failure-rate-threshold: 60         # Open if 60% of 100 fail
```

**Time-Based Sliding Window:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      orderService:
        sliding-window-type: TIME_BASED
        sliding-window-size: 60            # Last 60 seconds
        minimum-number-of-calls: 5         # Need 5 calls in 60s
        failure-rate-threshold: 50         # Open if 50% in window fail
```

---

### Slow Call Detection

**Treat Slow Calls as Failures:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      searchService:
        slow-call-duration-threshold: 3s      # > 3s is "slow"
        slow-call-rate-threshold: 80          # Open if 80% slow
        failure-rate-threshold: 50            # Also open if 50% fail
```

**Example:**
```
Call 1: 5 seconds (slow)
Call 2: 4 seconds (slow)
Call 3: 6 seconds (slow)
Call 4: 2 seconds (fast)
Call 5: 7 seconds (slow)

4 out of 5 calls slow (80% slow rate)
Threshold: 80%
Circuit: OPEN (service too slow)
```

---

### Circuit Breaker Events

**Listen to State Changes:**

```java
@Component
public class CircuitBreakerEventListener {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @PostConstruct
    public void registerEventListener() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("paymentService");
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                logger.info("Circuit Breaker state changed: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()
                );
            })
            .onSuccess(event -> {
                logger.debug("Call succeeded");
            })
            .onError(event -> {
                logger.warn("Call failed: {}", event.getThrowable().getMessage());
            })
            .onCallNotPermitted(event -> {
                logger.warn("Call not permitted - circuit is OPEN");
            });
    }
}
```

---

## Retry Mechanisms

### What is Retry?

**Definition:** Automatically retry a failed operation, hoping it succeeds on subsequent attempt.

**Theory:**
Some failures are transient (temporary):
- Network hiccup
- Service briefly overloaded
- Database lock timeout

Retrying often succeeds!

---

### When to Retry vs When NOT to Retry

**DO Retry:**
```
✓ Network timeouts
✓ Service temporarily unavailable (503)
✓ Database deadlock
✓ Rate limit exceeded (429) - with backoff
✓ Connection refused
✓ Temporary server errors (502, 503, 504)
```

**DON'T Retry:**
```
✗ Bad request (400) - will always fail
✗ Unauthorized (401) - need to fix auth
✗ Forbidden (403) - don't have permission
✗ Not found (404) - resource doesn't exist
✗ Business logic errors - need to fix code
✗ Validation errors - need correct input
```

---

### Resilience4j Retry Setup

**Configuration (application.yml):**

```yaml
resilience4j:
  retry:
    instances:
      paymentService:
        max-attempts: 3                    # Try 3 times total (1 initial + 2 retries)
        wait-duration: 1s                  # Wait 1 second between retries
        retry-exceptions:                  # Retry on these exceptions
          - org.springframework.web.client.ResourceAccessException
          - java.net.SocketTimeoutException
        ignore-exceptions:                 # Don't retry on these
          - com.example.exceptions.ValidationException
```

**Using Retry:**

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @Retry(name = "paymentService", fallbackMethod = "paymentRetryFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info("Attempting payment processing");
        return paymentClient.process(request);
    }
    
    private PaymentResponse paymentRetryFallback(PaymentRequest request, Exception e) {
        logger.error("All retry attempts failed: {}", e.getMessage());
        
        return PaymentResponse.builder()
            .status("FAILED")
            .message("Payment processing failed after retries")
            .build();
    }
}
```

**Execution Flow:**
```
Attempt 1: Call paymentClient.process()
           → Fails with timeout
           → Wait 1 second

Attempt 2: Call paymentClient.process() again
           → Fails with timeout
           → Wait 1 second

Attempt 3: Call paymentClient.process() again
           → Fails with timeout
           → All attempts exhausted
           → Call paymentRetryFallback()
```

---

### Exponential Backoff

**Problem with Fixed Delay:**
```
Retry after 1s, 1s, 1s
If service is recovering, constant bombardment doesn't help
```

**Solution: Exponential Backoff**
```
Retry after 1s, 2s, 4s, 8s
Give service more time to recover
```

**Configuration:**

```yaml
resilience4j:
  retry:
    instances:
      orderService:
        max-attempts: 5
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2    # 1s, 2s, 4s, 8s, 16s
        exponential-max-wait-duration: 10s   # Cap at 10s
```

**Execution:**
```
Attempt 1: Fail → Wait 1s
Attempt 2: Fail → Wait 2s
Attempt 3: Fail → Wait 4s
Attempt 4: Fail → Wait 8s
Attempt 5: Fail → Wait 10s (capped)
Attempt 6: Fail → Fallback
```

---

### Retry with Jitter

**Problem with Exponential Backoff:**
```
Multiple clients retry at same time
All retry after 1s → Thundering herd
All retry after 2s → Thundering herd
Service can't recover
```

**Solution: Add Randomness (Jitter)**
```
Random wait time: 0.5s to 1.5s
Different clients retry at different times
Service has breathing room
```

**Configuration:**

```yaml
resilience4j:
  retry:
    instances:
      userService:
        max-attempts: 4
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        enable-random-wait: true              # Add jitter
        random-wait-factor: 0.5               # 50% randomness
```

**Execution:**
```
Base delays: 1s, 2s, 4s

With jitter (50% random):
Attempt 1: 0.5s to 1.5s (random)
Attempt 2: 1s to 3s (random)
Attempt 3: 2s to 6s (random)
```

---

### Conditional Retry

**Retry Only on Specific Conditions:**

```java
@Service
public class ProductService {
    
    @Autowired
    private ExternalApiClient apiClient;
    
    @Retry(name = "productApi", fallbackMethod = "apiFallback")
    public Product getProduct(Long productId) {
        try {
            return apiClient.getProduct(productId);
        } catch (HttpClientErrorException e) {
            // Don't retry 4xx errors
            if (e.getStatusCode().is4xxClientError()) {
                throw new NonRetryableException("Client error", e);
            }
            throw e;  // Retry 5xx errors
        }
    }
    
    private Product apiFallback(Long productId, Exception e) {
        // Return cached product or default
        return getCachedProduct(productId);
    }
}
```

**Configuration:**
```yaml
resilience4j:
  retry:
    instances:
      productApi:
        max-attempts: 3
        wait-duration: 2s
        retry-exceptions:
          - org.springframework.web.client.HttpServerErrorException
        ignore-exceptions:
          - com.example.exceptions.NonRetryableException
```

---

### Retry Events

**Monitor Retry Attempts:**

```java
@Component
public class RetryEventListener {
    
    @Autowired
    private RetryRegistry retryRegistry;
    
    @PostConstruct
    public void registerEventListener() {
        Retry retry = retryRegistry.retry("paymentService");
        
        retry.getEventPublisher()
            .onRetry(event -> {
                logger.warn("Retry attempt {} for {}: {}", 
                    event.getNumberOfRetryAttempts(),
                    event.getName(),
                    event.getLastThrowable().getMessage()
                );
            })
            .onSuccess(event -> {
                logger.info("Call succeeded after {} attempts", 
                    event.getNumberOfRetryAttempts()
                );
            })
            .onError(event -> {
                logger.error("All retry attempts failed: {}", 
                    event.getLastThrowable().getMessage()
                );
            });
    }
}
```

---

## Timeouts

### Why Timeouts Matter

**Problem Without Timeout:**
```
Service calls another service
Other service is slow (or hanging)
Calling service waits forever
Thread blocked indefinitely
Eventually: All threads blocked
Result: Calling service crashes

One slow service can crash entire system!
```

**Solution With Timeout:**
```
Service calls another service with 2-second timeout
Other service doesn't respond in 2 seconds
Timeout exception thrown
Thread released
Service stays healthy
```

---

### Resilience4j TimeLimiter

**Configuration (application.yml):**

```yaml
resilience4j:
  timelimiter:
    instances:
      paymentService:
        timeout-duration: 3s           # Wait max 3 seconds
        cancel-running-future: true    # Cancel task if timeout
```

**Using TimeLimiter:**

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @TimeLimiter(name = "paymentService", fallbackMethod = "timeoutFallback")
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            paymentClient.process(request)
        );
    }
    
    private CompletableFuture<PaymentResponse> timeoutFallback(
            PaymentRequest request, TimeoutException e) {
        
        logger.warn("Payment service timeout: {}", e.getMessage());
        
        return CompletableFuture.completedFuture(
            PaymentResponse.builder()
                .status("TIMEOUT")
                .message("Request timed out, will retry later")
                .build()
        );
    }
}
```

---

### Connection vs Read Timeout

**Connection Timeout:**
```
Time to establish connection
Example: 5 seconds to connect to server

If server unreachable: Timeout after 5s
```

**Read Timeout:**
```
Time to wait for response after connection established
Example: 10 seconds for response

Server connected but not responding: Timeout after 10s
```

**RestTemplate Configuration:**

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        factory.setConnectTimeout(5000);  // 5 seconds to connect
        factory.setReadTimeout(10000);    // 10 seconds to read response
        
        return new RestTemplate(factory);
    }
}
```

**WebClient Configuration:**

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // Connection timeout
            .responseTimeout(Duration.ofSeconds(10));             // Read timeout
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

---

### Timeout Best Practices

**1. Set Realistic Timeouts**
```
Too short: Requests fail unnecessarily
Too long: Resources blocked too long

Measure actual response times
Set timeout = 95th percentile + buffer
Example: 95th percentile = 500ms → Set timeout = 2s
```

**2. Different Timeouts for Different Operations**
```
Fast operations (user lookup): 1s timeout
Medium operations (order creation): 5s timeout
Slow operations (report generation): 30s timeout
```

**3. Chain Timeouts Properly**
```
API Gateway timeout: 10s
Order Service timeout: 8s
Payment Service timeout: 5s

Gateway > Order > Payment
Prevents gateway timeout before services respond
```

---

### Timeout Configuration Example

```yaml
resilience4j:
  timelimiter:
    instances:
      # Fast service
      userService:
        timeout-duration: 2s
      
      # Medium service
      orderService:
        timeout-duration: 5s
      
      # Slow service
      reportService:
        timeout-duration: 30s
      
      # External API (can be very slow)
      externalApi:
        timeout-duration: 10s
        cancel-running-future: true
```

---

## Bulkhead Pattern

### What is Bulkhead?

**Definition:** Isolate resources to prevent one failing component from consuming all resources.

**Ship Bulkhead Analogy:**
```
Ship without bulkheads:
Hull breach → Water floods entire ship → Ship sinks

Ship with bulkheads (compartments):
Hull breach → Water floods one compartment
Other compartments intact → Ship stays afloat

Same for microservices!
```

---

### Problem Without Bulkhead

**Scenario:**
```
Order Service has 100 threads
Payment Service is slow (takes 30s per request)

Request 1: Call Payment → Thread 1 waiting (30s)
Request 2: Call Payment → Thread 2 waiting (30s)
Request 3: Call Payment → Thread 3 waiting (30s)
...
Request 100: Call Payment → Thread 100 waiting (30s)

All 100 threads stuck waiting for Payment Service!

Meanwhile:
Request 101: Get user profile → No threads available → FAIL
Request 102: Create order → No threads available → FAIL

One slow service consumed all threads
Entire Order Service unavailable
```

---

### Solution With Bulkhead

```
Allocate limited threads per dependency:

Payment Service calls: Max 10 threads
User Service calls: Max 20 threads
Product Service calls: Max 15 threads
Other operations: 55 threads

Payment Service slow:
- 10 threads stuck
- 90 threads still available
- Other operations continue working!
```

---

### Resilience4j Bulkhead Types

**1. Semaphore Bulkhead (Thread Pool)**
```
Limit concurrent calls
Like a parking lot with limited spots
```

**2. ThreadPool Bulkhead**
```
Separate thread pool for each service
Complete isolation
```

---

### Semaphore Bulkhead Setup

**Configuration (application.yml):**

```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10        # Max 10 concurrent calls
        max-wait-duration: 100ms        # Wait max 100ms for slot
```

**Using Bulkhead:**

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentClient.process(request);
    }
    
    private PaymentResponse bulkheadFallback(PaymentRequest request, BulkheadFullException e) {
        logger.warn("Bulkhead full for payment service");
        
        return PaymentResponse.builder()
            .status("QUEUED")
            .message("Too many concurrent requests, queued for later")
            .build();
    }
}
```

**How It Works:**
```
Max concurrent calls: 10

Request 1-10: Accepted (10 slots used)
Request 11: Wait 100ms for available slot
  - If slot available in 100ms: Proceed
  - If no slot in 100ms: BulkheadFullException → Fallback

When any of 1-10 completes:
Request 11: Gets the freed slot
```

---

### ThreadPool Bulkhead Setup

**Configuration:**

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      reportService:
        max-thread-pool-size: 5         # Max 5 threads in pool
        core-thread-pool-size: 3        # Keep 3 threads always
        queue-capacity: 20              # Queue up to 20 requests
        keep-alive-duration: 20ms       # Thread idle time before removal
```

**Using ThreadPool Bulkhead:**

```java
@Service
public class ReportService {
    
    @Autowired
    private ReportGenerator generator;
    
    @Bulkhead(name = "reportService", type = Bulkhead.Type.THREADPOOL, 
              fallbackMethod = "reportFallback")
    public CompletableFuture<Report> generateReport(ReportRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            generator.generate(request)
        );
    }
    
    private CompletableFuture<Report> reportFallback(
            ReportRequest request, BulkheadFullException e) {
        
        return CompletableFuture.completedFuture(
            Report.cached("Report generation busy, showing cached data")
        );
    }
}
```

**How It Works:**
```
Thread pool size: 5
Queue capacity: 20

Requests 1-5: Executed immediately (all 5 threads busy)
Requests 6-25: Queued (20 queue slots)
Request 26: BulkheadFullException (pool + queue full) → Fallback
```

---

### Bulkhead Configuration Per Service

**Different Limits for Different Services:**

```yaml
resilience4j:
  bulkhead:
    instances:
      # Critical service - more resources
      paymentService:
        max-concurrent-calls: 50
        max-wait-duration: 500ms
      
      # Normal service
      orderService:
        max-concurrent-calls: 20
        max-wait-duration: 100ms
      
      # Low priority service - fewer resources
      analyticsService:
        max-concurrent-calls: 5
        max-wait-duration: 50ms
      
      # External API - strict limit
      externalApi:
        max-concurrent-calls: 10
        max-wait-duration: 0ms    # Don't wait, fail immediately
```

---

### Bulkhead Benefits

**1. Fault Isolation**
```
Payment Service fails → Only payment threads affected
User Service continues → Uses its own thread pool
System partially degraded, not completely down
```

**2. Resource Management**
```
Low priority services can't starve high priority services
Each service gets its allocation
```

**3. Predictable Performance**
```
Know exactly how many concurrent calls possible
No surprises from resource exhaustion
```

---

## Rate Limiting

### What is Rate Limiting?

**Definition:** Limit number of requests in a time window.

**Why Rate Limit:**
- Prevent abuse
- Protect backend from overload
- Fair usage across clients
- Cost control (for external APIs)

---

### Resilience4j RateLimiter

**Configuration (application.yml):**

```yaml
resilience4j:
  ratelimiter:
    instances:
      userService:
        limit-for-period: 10           # Allow 10 requests
        limit-refresh-period: 1s       # Per 1 second
        timeout-duration: 0s           # Don't wait if limit exceeded
```

**Using RateLimiter:**

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @RateLimiter(name = "userService", fallbackMethod = "rateLimitFallback")
    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    private List<User> rateLimitFallback(RequestNotPermitted e) {
        logger.warn("Rate limit exceeded for getUsers");
        throw new TooManyRequestsException("Rate limit exceeded, try again later");
    }
}
```

**How It Works:**
```
Time window: 1 second
Limit: 10 requests

Second 1:
Request 1-10: Allowed
Request 11: Rate limit exceeded → Fallback

Second 2:
Limit resets
Request 12-21: Allowed
Request 22: Rate limit exceeded → Fallback
```

---

### Advanced Rate Limiting

**Different Limits for Different Operations:**

```yaml
resilience4j:
  ratelimiter:
    instances:
      # Read operations - higher limit
      userServiceRead:
        limit-for-period: 100
        limit-refresh-period: 1s
      
      # Write operations - lower limit
      userServiceWrite:
        limit-for-period: 10
        limit-refresh-period: 1s
      
      # Expensive operations - very low limit
      reportGeneration:
        limit-for-period: 2
        limit-refresh-period: 1m    # 2 per minute
```

**Usage:**

```java
@Service
public class UserService {
    
    @RateLimiter(name = "userServiceRead")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    @RateLimiter(name = "userServiceWrite")
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    @RateLimiter(name = "reportGeneration")
    public Report generateReport(ReportRequest request) {
        return reportGenerator.generate(request);
    }
}
```

---

### Rate Limiting with Wait

**Wait for Available Slot:**

```yaml
resilience4j:
  ratelimiter:
    instances:
      searchService:
        limit-for-period: 5
        limit-refresh-period: 1s
        timeout-duration: 500ms    # Wait up to 500ms
```

**Behavior:**
```
Limit: 5 requests/second

Request 1-5: Processed immediately
Request 6: Wait up to 500ms for slot
  - If slot available: Proceed
  - If 500ms elapsed: RequestNotPermitted → Fallback
```

---

## Resilience4j Integration

### Complete Resilience Stack

**Combining All Patterns:**

```yaml
resilience4j:
  # Circuit Breaker
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
  
  # Retry
  retry:
    instances:
      paymentService:
        max-attempts: 3
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
  
  # Timeout
  timelimiter:
    instances:
      paymentService:
        timeout-duration: 5s
        cancel-running-future: true
  
  # Bulkhead
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 20
        max-wait-duration: 100ms
  
  # Rate Limiter
  ratelimiter:
    instances:
      paymentService:
        limit-for-period: 50
        limit-refresh-period: 1s
        timeout-duration: 0s
```

---

### Using Multiple Patterns Together

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    @TimeLimiter(name = "paymentService")
    @Bulkhead(name = "paymentService")
    @RateLimiter(name = "paymentService")
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            paymentClient.process(request)
        );
    }
    
    private CompletableFuture<PaymentResponse> paymentFallback(
            PaymentRequest request, Exception e) {
        
        logger.error("Payment processing failed: {}", e.getMessage());
        
        return CompletableFuture.completedFuture(
            PaymentResponse.builder()
                .status("FAILED")
                .message("Payment service unavailable")
                .build()
        );
    }
}
```

**Execution Order:**
```
1. RateLimiter: Check if within rate limit
2. Bulkhead: Check if slot available
3. TimeLimiter: Start timeout timer
4. CircuitBreaker: Check circuit state
5. Retry: Execute with retry logic
6. Actual call: paymentClient.process()
```

---

### Monitoring & Metrics

**Enable Actuator:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configuration:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```

**Access Metrics:**

```bash
# Circuit breaker state
GET /actuator/circuitbreakers

# Circuit breaker events
GET /actuator/circuitbreakerevents

# Retry events
GET /actuator/retryevents

# Overall health
GET /actuator/health
```

**Response Example:**

```json
{
  "circuitBreakers": {
    "paymentService": {
      "state": "CLOSED",
      "failureRate": "15.0%",
      "slowCallRate": "5.0%",
      "bufferedCalls": 10,
      "failedCalls": 1,
      "slowCalls": 0,
      "notPermittedCalls": 0
    }
  }
}
```

---

### Custom Metrics

**Expose to Prometheus:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Access Prometheus Metrics:**

```bash
GET /actuator/prometheus
```

**Metrics Available:**

```
resilience4j_circuitbreaker_state
resilience4j_circuitbreaker_calls_total
resilience4j_circuitbreaker_failure_rate
resilience4j_retry_calls_total
resilience4j_bulkhead_available_concurrent_calls
resilience4j_ratelimiter_available_permissions
```

---

## Combining Patterns

### Pattern Interaction

**How Patterns Work Together:**

```
Request Flow:

1. Rate Limiter
   ↓ (within limit?)
   Yes → Continue
   No → 429 Too Many Requests

2. Bulkhead
   ↓ (slot available?)
   Yes → Continue
   No → 503 Service Unavailable

3. Circuit Breaker
   ↓ (circuit closed?)
   Yes → Continue
   No → Fallback response

4. Timeout
   ↓ (start timer)
   
5. Retry
   ↓ (try call)
   
6. Actual Service Call
   ↓
   Success → Return response
   Failure → Retry or Fallback
```

---

### Best Practices

**1. Order Matters**

```java
// Good order
@RateLimiter       // Check first (cheap)
@Bulkhead          // Check second (cheap)
@CircuitBreaker    // Check third (cheap)
@Retry             // Execute with retry (expensive)
@TimeLimiter       // Wrap with timeout (expensive)
public Response callService() { }

// Bad order
@Retry             // Retry everything including rate limiting!
@RateLimiter       // Checked 3 times if max-attempts=3
public Response callService() { }
```

---

**2. Configure Timeouts Properly**

```yaml
# Service chain: Gateway → Order → Payment

# Gateway timeout: Longest (outer layer)
gateway:
  timelimiter:
    timeout-duration: 10s

# Order service timeout: Medium
orderService:
  timelimiter:
    timeout-duration: 8s

# Payment service timeout: Shortest (innermost)
paymentService:
  timelimiter:
    timeout-duration: 5s

# Rule: Outer timeout > Inner timeout
```

---

**3. Appropriate Fallbacks**

```java
@Service
public class OrderService {
    
    // Critical operation - fail if payment fails
    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    public Order createOrder(OrderRequest request) {
        PaymentResponse payment = processPayment(request);
        return saveOrder(request, payment);
    }
    
    private Order paymentFallback(OrderRequest request, Exception e) {
        // Don't create order if payment fails
        throw new PaymentFailedException("Cannot create order without payment");
    }
    
    // Non-critical operation - return cached data
    @CircuitBreaker(name = "product", fallbackMethod = "productFallback")
    public List<Product> getRecommendations(Long userId) {
        return recommendationService.getRecommendations(userId);
    }
    
    private List<Product> productFallback(Long userId, Exception e) {
        // Return popular products if recommendation service down
        return productService.getPopularProducts();
    }
}
```

---

**4. Monitor Everything**

```java
@Component
public class ResilienceMetrics {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RetryRegistry retryRegistry;
    
    @Scheduled(fixedRate = 60000)  // Every minute
    public void logMetrics() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            logger.info("CircuitBreaker {}: State={}, FailureRate={}, SlowCallRate={}", 
                cb.getName(),
                cb.getState(),
                metrics.getFailureRate(),
                metrics.getSlowCallRate()
            );
        });
        
        retryRegistry.getAllRetries().forEach(retry -> {
            Retry.Metrics metrics = retry.getMetrics();
            logger.info("Retry {}: Successful={}, Failed={}, SuccessWithoutRetry={}", 
                retry.getName(),
                metrics.getNumberOfSuccessfulCallsWithRetryAttempt(),
                metrics.getNumberOfFailedCallsWithRetryAttempt(),
                metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()
            );
        });
    }
}
```

---

**5. Test Failure Scenarios**

```java
@SpringBootTest
public class ResilienceTest {
    
    @Autowired
    private OrderService orderService;
    
    @Test
    public void testCircuitBreakerOpens() throws Exception {
        // Simulate payment service failures
        mockPaymentService.failNextCalls(10);
        
        // Make 10 calls - should open circuit
        for (int i = 0; i < 10; i++) {
            try {
                orderService.createOrder(createTestOrder());
            } catch (Exception e) {
                // Expected
            }
        }
        
        // Circuit should be OPEN now
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        
        // Next call should fail fast (no actual call to payment service)
        long startTime = System.currentTimeMillis();
        assertThrows(CallNotPermittedException.class, () -> 
            orderService.createOrder(createTestOrder())
        );
        long duration = System.currentTimeMillis() - startTime;
        
        // Should fail immediately (< 100ms)
        assertTrue(duration < 100);
    }
}
```

---

## Summary

### Key Takeaways

**Circuit Breaker:**
- Prevents cascading failures
- Three states: CLOSED, OPEN, HALF_OPEN
- Provides fallback when service unavailable
- Automatically tests recovery

**Retry:**
- Handles transient failures
- Exponential backoff prevents thundering herd
- Add jitter for distributed retries
- Don't retry business logic errors

**Timeout:**
- Prevents thread exhaustion
- Set realistic timeouts based on SLAs
- Connection timeout vs read timeout
- Chain timeouts properly (outer > inner)

**Bulkhead:**
- Isolates resources per dependency
- Prevents one slow service from blocking all threads
- Semaphore (simple) vs ThreadPool (complete isolation)
- Critical for system stability

**Rate Limiting:**
- Protects from abuse and overload
- Different limits for different operations
- Per-user, per-IP, per-endpoint
- Fair usage across clients

**Resilience4j:**
- Modern resilience library for Java
- Lightweight (no dependencies)
- Works with Spring Boot
- Rich metrics and monitoring
- Combine patterns for robust systems

---

### Design Principles

**1. Fail Fast**
```
Don't wait for inevitable failure
Detect quickly and respond
```

**2. Fail Gracefully**
```
Always provide fallback
Degrade functionality, don't crash
```

**3. Recover Automatically**
```
Test if service recovered
Resume normal operation when possible
```

**4. Isolate Failures**
```
One service failure shouldn't crash others
Use bulkheads and circuit breakers
```

**5. Monitor and Learn**
```
Track all failures
Analyze patterns
Improve resilience over time
```

---

### Real-World Example

**Production-Grade Service:**

```java
@Service
public class PaymentService {
    
    @Autowired
    private ExternalPaymentGateway paymentGateway;
    
    @RateLimiter(name = "payment")              // 1. Rate limit
    @Bulkhead(name = "payment")                 // 2. Isolate resources
    @CircuitBreaker(name = "payment", fallbackMethod = "fallback")  // 3. Circuit break
    @Retry(name = "payment")                    // 4. Retry on failure
    @TimeLimiter(name = "payment")              // 5. Timeout protection
    public CompletableFuture<PaymentResult> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate request
            validatePayment(request);
            
            // Call external gateway
            PaymentResponse response = paymentGateway.charge(request);
            
            // Save to database
            savePayment(response);
            
            return PaymentResult.success(response);
        });
    }
    
    private CompletableFuture<PaymentResult> fallback(PaymentRequest request, Exception e) {
        logger.error("Payment processing failed: {}", e.getMessage());
        
        // Try to get from cache
        Optional<PaymentResult> cached = paymentCache.get(request.getIdempotencyKey());
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }
        
        // Queue for later processing
        paymentQueue.enqueue(request);
        
        return CompletableFuture.completedFuture(
            PaymentResult.queued("Payment queued for processing")
        );
    }
}
```

**Configuration:**

```yaml
resilience4j:
  ratelimiter:
    instances:
      payment:
        limit-for-period: 100
        limit-refresh-period: 1s
  
  bulkhead:
    instances:
      payment:
        max-concurrent-calls: 20
  
  circuitbreaker:
    instances:
      payment:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  
  retry:
    instances:
      payment:
        max-attempts: 3
        wait-duration: 2s
        enable-exponential-backoff: true
  
  timelimiter:
    instances:
      payment:
        timeout-duration: 10s
```

**This gives you:**
- ✓ Protection from overload (rate limiter)
- ✓ Resource isolation (bulkhead)
- ✓ Cascading failure prevention (circuit breaker)
- ✓ Transient failure handling (retry)
- ✓ Hung request protection (timeout)
- ✓ Graceful degradation (fallback)

**Remember:** Distributed systems WILL fail. Design for resilience from day one!
