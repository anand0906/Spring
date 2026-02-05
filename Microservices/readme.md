# Java Microservices - Complete Revision Notes

## Table of Contents
1. [Microservices Architecture](#microservices-architecture)
2. [Inter-Service Communication](#inter-service-communication)
3. [Service Discovery & Configuration](#service-discovery--configuration)
4. [API Gateway & Routing](#api-gateway--routing)
5. [Resilience & Fault Tolerance](#resilience--fault-tolerance)
6. [Observability & Monitoring](#observability--monitoring)
7. [Security](#security)
8. [Advanced Patterns](#advanced-patterns)

---

## Microservices Architecture

### Monolith vs Microservices

**Monolith:**
- Single codebase, single deployment
- One database, tightly coupled
- ✓ Simple, easy to test
- ✗ Hard to scale, technology lock-in

**Microservices:**
- Multiple services, independent deployment
- Database per service, loosely coupled
- ✓ Independent scaling, technology freedom
- ✗ Complex infrastructure, network latency

**When to Use:** Large teams, complex apps, need independent scaling

---

### Service Decomposition Strategies

1. **By Business Capability:** Each service = business function (order, payment, user)
2. **By Subdomain (DDD):** Core, supporting, generic domains
3. **By Data Ownership:** Each service owns its data
4. **By Use Case:** User journey-based services
5. **By Scalability:** Different scaling needs

**Key Principles:** Single responsibility, loose coupling, high cohesion

---

### 12-Factor App

1. **Codebase:** One repo, many deploys
2. **Dependencies:** Explicit declaration
3. **Config:** Environment variables
4. **Backing Services:** Attached resources
5. **Build, Release, Run:** Separate stages
6. **Processes:** Stateless
7. **Port Binding:** Self-contained
8. **Concurrency:** Scale via processes
9. **Disposability:** Fast startup, graceful shutdown
10. **Dev/Prod Parity:** Same environment
11. **Logs:** Event streams
12. **Admin Processes:** One-off tasks

---

### Stateless Services

**Stateless:** No session data in memory, use external storage (Redis)
**Why:** Easy horizontal scaling, any instance handles any request

---

### Event-Driven Architecture

**Flow:** Service A → Publish Event → Message Broker → Service B consumes
**Benefits:** Loose coupling, scalability, flexibility
**Challenges:** Eventual consistency, debugging

---

### Synchronous vs Asynchronous

**Synchronous (REST):**
- Request-response, wait for answer
- Use when: Need immediate response

**Asynchronous (Messaging):**
- Fire-and-forget, don't wait
- Use when: Long-running, can process later

---

### CAP Theorem

**Can only have 2 of 3:**
- **C**onsistency: All nodes see same data
- **A**vailability: Always responds
- **P**artition Tolerance: Works despite network issues

**CP:** Banking (consistent but might be unavailable)
**AP:** Social media (available but eventually consistent)

---

### Distributed Systems Fundamentals

**Challenges:**
- Network unreliable
- Partial failures
- Latency
- Data consistency
- Clock sync issues

**Solutions:**
- Replication (copies on multiple servers)
- Partitioning (split data across servers)
- Consensus (agreement algorithms)
- Leader election
- Idempotency

---

## Inter-Service Communication

### Synchronous Communication

**1. RestTemplate (Legacy):**
```java
RestTemplate restTemplate = new RestTemplate();
Product product = restTemplate.getForObject(url, Product.class);
```
- ✗ Blocking, old

**2. WebClient (Modern):**
```java
Product product = webClient.get()
    .uri("/products/{id}", id)
    .retrieve()
    .bodyToMono(Product.class)
    .block();
```
- ✓ Non-blocking, reactive

**3. OpenFeign (Declarative):**
```java
@FeignClient(name = "product-service")
interface ProductClient {
    @GetMapping("/products/{id}")
    Product getProduct(@PathVariable Long id);
}
```
- ✓ Clean, simple

---

### Asynchronous Communication

**Apache Kafka:**
- Event streaming platform
- Topics and partitions
- High throughput

**Producer:**
```java
kafkaTemplate.send("order-created", orderEvent);
```

**Consumer:**
```java
@KafkaListener(topics = "order-created")
public void handleOrder(OrderEvent event) {
    // Process event
}
```

**RabbitMQ:**
- Message broker
- Exchanges, queues, bindings
- Flexible routing

**Producer:**
```java
rabbitTemplate.convertAndSend("order-exchange", "order.created", event);
```

**Consumer:**
```java
@RabbitListener(queues = "order-queue")
public void handleOrder(OrderEvent event) {
    // Process
}
```

---

### Event Schemas

Define structure of events, version carefully
```java
{
  "eventId": "123",
  "eventType": "OrderCreated",
  "orderId": "456",
  "userId": "789",
  "timestamp": "2024-02-04T10:30:00Z"
}
```

---

### Idempotency

Same operation multiple times = same result
```java
// Check if already processed
if (processedRepo.existsById(messageId)) return;
// Process
processMessage();
// Mark as processed
processedRepo.save(messageId);
```

---

### Delivery Guarantees

1. **At-most-once:** 0 or 1 (might lose)
2. **At-least-once:** 1+ (might duplicate) - Most common
3. **Exactly-once:** Exactly 1 (complex)

---

## Service Discovery & Configuration

### Service Discovery

**Problem:** Services need to find each other
**Solution:** Registry where services register and discover

**Eureka (Netflix):**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Consul (HashiCorp):**
```yaml
spring:
  cloud:
    consul:
      host: localhost
      port: 8500
```

**Usage with Feign:**
```java
@FeignClient(name = "user-service") // Auto-discovers
interface UserClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);
}
```

---

### Centralized Configuration

**Spring Cloud Config Server:**
- Store configs in Git
- Services fetch on startup
- Environment-specific configs

**Config Server:**
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/yourname/config-repo
```

**Client:**
```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
```

---

### Dynamic Refresh

**Without Restart:**
```java
@RefreshScope
@RestController
public class OrderController {
    @Value("${app.max-order-value}")
    private int maxOrderValue; // Refreshable
}
```

**Trigger:** `POST /actuator/refresh`

**Auto-refresh:** Spring Cloud Bus + RabbitMQ

---

### Failover Strategies

1. Multiple server instances
2. Service discovery (auto-failover)
3. Retry pattern
4. Circuit breaker
5. Local config fallback
6. Config caching

---

## API Gateway & Routing

### What is API Gateway?

Single entry point for all client requests
- Routing
- Authentication
- Rate limiting
- Load balancing
- Request/response transformation

---

### Spring Cloud Gateway

**Basic Route:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
```

**With Service Discovery:**
```yaml
uri: lb://user-service  # Load balanced, from Eureka
```

---

### Filters

**Pre-filters:** Modify request before routing
**Post-filters:** Modify response before returning to client

**Built-in:**
- AddRequestHeader
- AddResponseHeader
- StripPrefix
- Retry
- RequestRateLimiter

**Custom Global Filter:**
```java
@Component
public class CustomFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Pre-filter
        ServerHttpRequest request = exchange.getRequest();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Post-filter
            ServerHttpResponse response = exchange.getResponse();
        }));
    }
}
```

---

### Rate Limiting

**Token Bucket Algorithm:**
```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10  # 10 per second
      redis-rate-limiter.burstCapacity: 20
```

**Per-user:**
```java
@Component
public class UserKeyResolver implements KeyResolver {
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(exchange.getRequest().getHeaders().getFirst("X-User-Id"));
    }
}
```

---

### Authentication at Gateway

**JWT Validation:**
```java
@Component
public class JwtAuthFilter implements GlobalFilter {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = getToken(exchange);
        if (isValidToken(token)) {
            return chain.filter(exchange);
        }
        return unauthorized();
    }
}
```

---

### Circuit Breaker at Gateway

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: orderServiceCB
      fallbackUri: forward:/fallback/orders
```

**Fallback:**
```java
@RestController
public class FallbackController {
    @GetMapping("/fallback/orders")
    public ResponseEntity<?> ordersFallback() {
        return ResponseEntity.ok("Service temporarily unavailable");
    }
}
```

---

## Resilience & Fault Tolerance

### Circuit Breaker Pattern

**Three States:**
1. **CLOSED:** Normal, requests pass
2. **OPEN:** Service failing, reject immediately
3. **HALF_OPEN:** Testing recovery

**Config:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

**Usage:**
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}

private PaymentResponse fallback(PaymentRequest request, Exception e) {
    return PaymentResponse.deferred();
}
```

---

### Retry Mechanism

**Exponential Backoff:**
```yaml
resilience4j:
  retry:
    instances:
      orderService:
        max-attempts: 3
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
```

**Usage:**
```java
@Retry(name = "orderService")
public Order createOrder(OrderRequest request) {
    return orderClient.create(request);
}
```

---

### Timeouts

**Configuration:**
```yaml
resilience4j:
  timelimiter:
    instances:
      paymentService:
        timeout-duration: 3s
```

**RestTemplate:**
```java
factory.setConnectTimeout(5000);  // Connection
factory.setReadTimeout(10000);    // Read
```

---

### Bulkhead Pattern

**Isolate Resources:**
```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10
        max-wait-duration: 100ms
```

**Usage:**
```java
@Bulkhead(name = "paymentService")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}
```

**Prevents:** One slow service from consuming all threads

---

### Rate Limiting

```yaml
resilience4j:
  ratelimiter:
    instances:
      userService:
        limit-for-period: 10
        limit-refresh-period: 1s
```

---

### Combining Patterns

```java
@RateLimiter(name = "payment")
@Bulkhead(name = "payment")
@CircuitBreaker(name = "payment", fallbackMethod = "fallback")
@Retry(name = "payment")
@TimeLimiter(name = "payment")
public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
    return CompletableFuture.supplyAsync(() -> paymentClient.process(request));
}
```

**Order:** RateLimiter → Bulkhead → CircuitBreaker → Retry → TimeLimiter

---

## Observability & Monitoring

### Three Pillars

1. **Logs:** What happened (events)
2. **Metrics:** Numerical measurements
3. **Traces:** Request journey across services

---

### Centralized Logging (ELK)

**Components:**
- **Elasticsearch:** Storage
- **Logstash:** Collection
- **Kibana:** Visualization

**Structured Logging:**
```java
logger.info("Order created", 
    kv("orderId", orderId),
    kv("userId", userId),
    kv("amount", amount)
);
```

**Output (JSON):**
```json
{
  "timestamp": "2024-02-04T10:30:00Z",
  "level": "INFO",
  "message": "Order created",
  "orderId": "123",
  "userId": "456",
  "amount": 99.99
}
```

---

### Correlation ID

**Track request across services:**
```java
// Filter adds correlation ID
MDC.put("correlationId", UUID.randomUUID().toString());

// Pass to other services
request.setHeader("X-Correlation-Id", correlationId);

// All logs include correlation ID
logger.info("Processing order"); // [abc-123] Processing order
```

---

### Distributed Tracing

**OpenTelemetry/Zipkin/Jaeger:**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

**Auto-instrumentation:** HTTP, DB, messaging automatically traced

**Custom Span:**
```java
Span span = tracer.spanBuilder("processOrder")
    .setAttribute("orderId", orderId)
    .startSpan();
try {
    // Business logic
} finally {
    span.end();
}
```

---

### Metrics (Micrometer + Prometheus)

**Built-in Metrics:**
- JVM (memory, GC, threads)
- HTTP (requests, duration, status codes)
- Database (connections)

**Custom Metrics:**
```java
Counter counter = Counter.builder("orders.created")
    .tag("status", "success")
    .register(meterRegistry);
counter.increment();

Timer timer = Timer.builder("order.processing.time")
    .register(meterRegistry);
timer.record(() -> processOrder());
```

**Prometheus Query:**
```promql
rate(http_server_requests_seconds_count[5m])
histogram_quantile(0.95, http_server_requests_seconds_bucket)
```

---

### Health Checks

**Spring Boot Actuator:**
```yaml
management:
  endpoint:
    health:
      show-details: always
```

**Access:** `GET /actuator/health`

**Custom Health:**
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    public Health health() {
        if (isDatabaseUp()) {
            return Health.up().build();
        }
        return Health.down().build();
    }
}
```

---

### Alerting (Prometheus)

**Alert Rule:**
```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests{status=~"5.."}[5m]) > 0.05
  for: 5m
  annotations:
    summary: "High error rate detected"
```

---

## Security

### Spring Security Basics

**Add Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic();
        return http.build();
    }
}
```

---

### JWT Authentication

**Generate Token:**
```java
String token = Jwts.builder()
    .setSubject(username)
    .claim("roles", roles)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 86400000))
    .signWith(key, SignatureAlgorithm.HS256)
    .compact();
```

**Validate Token:**
```java
Claims claims = Jwts.parserBuilder()
    .setSigningKey(key)
    .build()
    .parseClaimsJws(token)
    .getBody();
String username = claims.getSubject();
```

**Filter:**
```java
public class JwtAuthFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = getTokenFromHeader(request);
        if (isValidToken(token)) {
            setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }
}
```

---

### OAuth2 / OpenID Connect

**Google Login:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id
            client-secret: your-client-secret
            scope: openid, profile, email
```

**Spring handles OAuth2 flow automatically**

---

### Keycloak

**Setup:**
1. Run Keycloak
2. Create realm
3. Create client
4. Create users and roles

**Spring Config:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/microservices-realm
```

---

### Securing Inter-Service Communication

**1. Service-to-Service JWT:**
- Each service gets token
- Pass token in requests

**2. Mutual TLS:**
- Both client and server verify certificates

**3. API Keys:**
- Simple internal service authentication

---

### Role-Based Access Control (RBAC)

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteOrder(Long id) {
    orderRepository.deleteById(id);
}

@PreAuthorize("hasAuthority('order:read')")
public Order getOrder(Long id) {
    return orderRepository.findById(id);
}
```

---

## Advanced Patterns

### Saga Pattern

**Problem:** Distributed transactions across services

**Choreography:**
- Services coordinate via events
- Decentralized

**Orchestration:**
- Central coordinator controls flow
- Easier to understand

**Example:**
```java
// Orchestrator
public void executeSaga() {
    try {
        orderService.createOrder();     // Step 1
        paymentService.processPayment(); // Step 2
        inventoryService.reserve();      // Step 3
    } catch (Exception e) {
        // Compensate in reverse order
        inventoryService.release();
        paymentService.refund();
        orderService.cancel();
    }
}
```

---

### CQRS (Command Query Responsibility Segregation)

**Separate read and write models:**

**Command Side (Write):**
```java
@Service
public class OrderCommandHandler {
    public String createOrder(CreateOrderCommand cmd) {
        Order order = new Order(cmd);
        orderRepository.save(order);
        eventPublisher.publish(new OrderCreatedEvent(order));
        return order.getId();
    }
}
```

**Query Side (Read):**
```java
@Service
public class OrderQueryHandler {
    public OrderSummary getOrder(String id) {
        return orderSummaryRepository.findById(id); // Denormalized
    }
}
```

**Event Handler (Sync):**
```java
@EventListener
public void handle(OrderCreatedEvent event) {
    OrderSummary summary = new OrderSummary(event);
    orderSummaryRepository.save(summary); // Update read model
}
```

---

### Event Sourcing

**Store events, not current state:**

**Event Store:**
```
Events:
1. OrderCreated
2. PaymentProcessed
3. OrderShipped

Current state = Replay all events
```

**Aggregate:**
```java
public class OrderAggregate {
    public static OrderAggregate create(CreateOrderCommand cmd) {
        OrderAggregate order = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(cmd);
        order.apply(event);
        return order;
    }
    
    private void apply(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.status = OrderStatus.PENDING;
        uncommittedEvents.add(event);
    }
    
    public static OrderAggregate loadFromHistory(List<Event> events) {
        OrderAggregate order = new OrderAggregate();
        events.forEach(order::apply);
        return order;
    }
}
```

---

### API Composition

**Combine data from multiple services:**

```java
public OrderHistoryResponse getOrderHistory(String userId) {
    // Parallel calls
    CompletableFuture<User> userFuture = 
        CompletableFuture.supplyAsync(() -> userClient.getUser(userId));
    
    List<Order> orders = orderClient.getOrders(userId);
    
    CompletableFuture<Map<String, Product>> productsFuture = 
        CompletableFuture.supplyAsync(() -> getProducts(orders));
    
    // Wait for all
    CompletableFuture.allOf(userFuture, productsFuture).join();
    
    // Combine
    return combine(userFuture.join(), orders, productsFuture.join());
}
```

---

### Strangler Pattern

**Gradually migrate monolith to microservices:**

**Strangler Facade:**
```java
@RestController
public class StranglerFacade {
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable String id) {
        if (featureToggle.isEnabled("use-user-microservice")) {
            return userServiceClient.getUser(id); // New
        } else {
            return monolithClient.getUser(id); // Old
        }
    }
}
```

**Gradual rollout:** 10% → 25% → 50% → 100%

---

### Anti-Corruption Layer

**Protect domain model from legacy systems:**

```java
@Component
public class CustomerAntiCorruptionLayer {
    public Customer getCustomer(String id) {
        // Call legacy system
        LegacyCustomerResponse legacy = legacyClient.getCustomer(id);
        
        // Translate to clean model
        Customer customer = new Customer();
        customer.setId(legacy.getCUST_ID());
        customer.setName(legacy.getF_NAME() + " " + legacy.getL_NAME());
        customer.setEmail(legacy.getEMAIL_ADDR());
        
        return customer;
    }
}
```

**Benefits:** Clean code, easy to test, can replace legacy gradually

---

## Quick Interview Answers

### "What are microservices?"
Architectural style where application is composed of small, independent services that communicate over network. Each service owns its data and can be deployed independently.

### "Monolith vs Microservices?"
**Monolith:** Single deployment, shared database, simple but hard to scale.
**Microservices:** Multiple services, independent deployment, complex but scalable.

### "How do services communicate?"
**Sync:** REST (Feign, WebClient) for immediate response.
**Async:** Message brokers (Kafka, RabbitMQ) for decoupling and eventual consistency.

### "What is service discovery?"
Registry where services register and find each other dynamically. Examples: Eureka, Consul.

### "Explain circuit breaker"
Prevents cascading failures. Three states: Closed (normal), Open (failing, reject fast), Half-Open (testing recovery). Uses fallback when open.

### "What is API Gateway?"
Single entry point for clients. Handles routing, authentication, rate limiting, and load balancing.

### "CAP Theorem?"
Can't have all three: Consistency, Availability, Partition Tolerance. Choose CP (banking) or AP (social media).

### "What is CQRS?"
Separate read and write models. Optimize each independently. Write to normalized DB, read from denormalized.

### "Saga pattern?"
Manage distributed transactions without 2PC. Either choreography (events) or orchestration (coordinator). Includes compensation for rollback.

### "How to handle failures?"
Circuit breaker, retry with backoff, timeouts, bulkhead, fallbacks, monitoring.

### "Security in microservices?"
JWT for stateless auth, OAuth2 for social login, API Gateway for central authentication, service-to-service security (mTLS, tokens).

### "Observability?"
Logs (what happened), Metrics (numbers), Traces (request journey). Use ELK, Prometheus, Zipkin.

---

## Common Pitfalls to Avoid

❌ **Over-engineering:** Start simple, add complexity when needed
❌ **Distributed Monolith:** Services too tightly coupled
❌ **Sharing Database:** Each service should own its data
❌ **Not Handling Failures:** Always expect failures, use resilience patterns
❌ **Ignoring Monitoring:** Can't fix what you can't see
❌ **Premature Microservices:** Start with monolith for MVP
❌ **Too Many Services:** Start with fewer, larger services
❌ **No API Versioning:** Always version your APIs
❌ **Synchronous Everything:** Use async when appropriate
❌ **No Documentation:** Document service contracts

---

## Technology Stack Summary

**Framework:** Spring Boot
**Service Discovery:** Eureka, Consul
**Configuration:** Spring Cloud Config
**API Gateway:** Spring Cloud Gateway
**Resilience:** Resilience4j
**Messaging:** Kafka, RabbitMQ
**Logging:** ELK Stack
**Tracing:** Zipkin, Jaeger, OpenTelemetry
**Metrics:** Micrometer, Prometheus, Grafana
**Security:** Spring Security, JWT, OAuth2, Keycloak
**Database:** PostgreSQL, MongoDB
**Cache:** Redis
**Containers:** Docker, Kubernetes

---

## Study Tips

1. **Understand WHY before HOW:** Know the problems patterns solve
2. **Practice coding:** Don't just read, implement
3. **Draw diagrams:** Visualize architecture
4. **Compare alternatives:** Know trade-offs
5. **Real-world scenarios:** Think about production issues
6. **Interview prep:** Explain to someone else
7. **Keep updated:** Microservices evolve rapidly

---

## Must-Know for Interviews

✓ Monolith vs Microservices trade-offs
✓ Service communication (sync/async)
✓ Service discovery and why it's needed
✓ API Gateway responsibilities
✓ Circuit breaker pattern and states
✓ CAP theorem
✓ Distributed transaction handling (Saga)
✓ Security (JWT, OAuth2)
✓ Observability (logs, metrics, traces)
✓ Resilience patterns
✓ When to use microservices
✓ Common challenges and solutions

---

## Final Checklist

Before claiming microservices expertise:

□ Built at least 3 microservices
□ Implemented inter-service communication
□ Used service discovery
□ Set up API Gateway
□ Implemented circuit breaker
□ Added centralized logging
□ Implemented distributed tracing
□ Secured services with JWT
□ Handled distributed transactions
□ Deployed to production
□ Debugged production issues
□ Refactored monolith to microservices

**Remember:** Microservices are complex. Master the basics before advanced patterns. Real-world experience > theoretical knowledge.

Good luck with your interviews! 🚀
