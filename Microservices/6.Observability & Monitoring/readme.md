# Observability & Monitoring in Microservices

## Table of Contents
1. [Introduction to Observability](#introduction-to-observability)
2. [Centralized Logging](#centralized-logging)
   - [ELK Stack](#elk-stack)
3. [Distributed Tracing](#distributed-tracing)
   - [OpenTelemetry](#opentelemetry)
   - [Zipkin](#zipkin)
   - [Jaeger](#jaeger)
4. [Metrics](#metrics)
   - [Micrometer](#micrometer)
   - [Prometheus](#prometheus)
5. [Health Checks & Alerts](#health-checks--alerts)

---

## Introduction to Observability

### What is Observability?

**Definition:** The ability to understand the internal state of your system by examining its outputs.

**Simple Explanation:**
```
Like a car dashboard:
- Speed meter (metrics)
- Warning lights (alerts)
- GPS path history (distributed tracing)
- Error codes in computer (logs)

Without dashboard: Car breaks, you don't know why
With dashboard: See problem early, fix before breakdown
```

---

### The Three Pillars of Observability

**1. Logs**
```
What: Detailed records of events
Example: "User 123 created order 456 at 10:30:00"
Use: Debug specific issues, audit trail
```

**2. Metrics**
```
What: Numerical measurements over time
Example: "CPU usage: 75%, Memory: 2GB, Requests: 1000/sec"
Use: Monitor health, capacity planning, alerts
```

**3. Traces**
```
What: Journey of a single request across services
Example: Request → Gateway → Order Service → Payment Service
Use: Find bottlenecks, understand dependencies
```

---

### Why Observability Matters in Microservices

**Problem Without Observability:**

```
User reports: "Order failed"

Where did it fail?
- Gateway? Order Service? Payment Service? Database?

When did it fail?
- 10 minutes ago? 1 hour ago?

Why did it fail?
- Timeout? Network error? Business logic error?

Searching through logs on 50 servers manually
Hours to find the problem
Customer already angry
```

**Solution With Observability:**

```
User reports: "Order failed"

Look at dashboard:
1. Logs: Find error message instantly
2. Trace: See request traveled Gateway → Order → Payment (failed here)
3. Metrics: Payment service CPU at 100%, memory exhausted

Problem found in 2 minutes
Root cause: Payment service memory leak
Quick fix deployed
Customer happy
```

---

### Observability vs Monitoring

**Monitoring (Traditional):**
```
Know WHAT is happening
- Server is down
- CPU is high
- Error rate increased

Pre-defined dashboards
Known failure modes
```

**Observability (Modern):**
```
Understand WHY it's happening
- Why did this specific request fail?
- Why is this service slow?
- What caused this cascade?

Ask new questions without predicting them
Unknown unknowns
```

**Both are Needed!**

---

## Centralized Logging

### Why Centralized Logging?

**Problem: Distributed Logs**

```
50 microservices
Each service logs to its own file
Each service on different server

User request touches 5 services:
1. API Gateway (logs on server1:/var/log/gateway.log)
2. Order Service (logs on server2:/var/log/order.log)
3. Payment Service (logs on server3:/var/log/payment.log)
4. Inventory Service (logs on server4:/var/log/inventory.log)
5. Email Service (logs on server5:/var/log/email.log)

To trace one request:
- SSH to 5 different servers
- Grep through 5 different log files
- Correlate timestamps manually
- Takes hours!
```

**Solution: Centralized Logging**

```
All services → Central Log Storage
Search all logs from one place
Filter by request ID
Find related logs instantly
Takes seconds!
```

---

### Centralized Logging Architecture

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Service 1  │  │   Service 2  │  │   Service 3  │
│   (logs)     │  │   (logs)     │  │   (logs)     │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ↓
                 ┌───────────────┐
                 │ Log Collector │ (Logstash/Fluentd)
                 └───────┬───────┘
                         ↓
                 ┌───────────────┐
                 │  Log Storage  │ (Elasticsearch)
                 └───────┬───────┘
                         ↓
                 ┌───────────────┐
                 │     Search    │ (Kibana)
                 │  & Visualize  │
                 └───────────────┘
```

---

## ELK Stack

### What is ELK?

**E**lasticsearch + **L**ogstash + **K**ibana

**Elasticsearch:**
- Stores logs (database)
- Full-text search engine
- Fast queries across millions of logs

**Logstash:**
- Collects logs from services
- Transforms and enriches logs
- Sends to Elasticsearch

**Kibana:**
- Web UI for searching logs
- Visualizations and dashboards
- Real-time log streaming

---

### Setting Up Logging in Spring Boot

**Step 1: Add Dependency (pom.xml)**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Step 2: Configure Logback (logback-spring.xml)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console appender for local development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Logstash appender for production -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"order-service"}</customFields>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="LOGSTASH" />
    </root>
</configuration>
```

---

### Structured Logging

**Bad Logging (Unstructured):**

```java
@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public void createOrder(OrderRequest request) {
        // Hard to parse, hard to search
        logger.info("Creating order for user " + request.getUserId() + 
                    " with amount " + request.getAmount());
    }
}
```

**Good Logging (Structured - JSON):**

```java
@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public void createOrder(OrderRequest request) {
        // Structured logging with key-value pairs
        logger.info("Creating order", 
            kv("userId", request.getUserId()),
            kv("orderId", request.getOrderId()),
            kv("amount", request.getAmount()),
            kv("currency", request.getCurrency())
        );
    }
}
```

**Output (JSON):**
```json
{
  "timestamp": "2024-02-04T10:30:00Z",
  "level": "INFO",
  "service": "order-service",
  "message": "Creating order",
  "userId": 123,
  "orderId": "ORD-456",
  "amount": 99.99,
  "currency": "USD",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.example.OrderService"
}
```

**Benefits:**
- Easy to search: `userId:123`
- Easy to filter: `amount > 100`
- Easy to aggregate: `sum(amount) by userId`

---

### Correlation ID (Request Tracking)

**Problem:**

```
User makes request
Request goes through 5 services
Each service logs independently
How to find all logs for this specific request?
```

**Solution: Correlation ID**

```
Generate unique ID for each request
Pass ID through all services
Log ID with every log entry
Search by ID to see complete flow
```

**Implementation:**

**Step 1: Create Correlation ID Filter**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Get correlation ID from header or generate new one
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Store in MDC (Mapped Diagnostic Context)
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Add to response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

**Step 2: Update Logback Configuration**

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%X{correlationId}] [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"service":"order-service"}</customFields>
        <includeMdcKeyName>correlationId</includeMdcKeyName>
    </encoder>
</appender>
```

**Step 3: Pass Correlation ID to Other Services**

```java
@Component
public class CorrelationIdPropagator implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        // Get correlation ID from MDC
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            // Add to outgoing request
            template.header("X-Correlation-Id", correlationId);
        }
    }
}

@Configuration
public class FeignConfig {
    @Bean
    public CorrelationIdPropagator correlationIdPropagator() {
        return new CorrelationIdPropagator();
    }
}
```

**Example Flow:**

```
1. User request arrives at Gateway
   Gateway generates: correlationId = "abc-123"
   Gateway logs: [abc-123] Received request

2. Gateway calls Order Service with header: X-Correlation-Id: abc-123
   Order Service logs: [abc-123] Creating order

3. Order Service calls Payment Service with header: X-Correlation-Id: abc-123
   Payment Service logs: [abc-123] Processing payment

4. Search Kibana for "abc-123"
   See complete flow:
   - [abc-123] Received request (Gateway)
   - [abc-123] Creating order (Order Service)
   - [abc-123] Processing payment (Payment Service)
```

---

### Log Levels

**Understanding Log Levels:**

```java
@Service
public class OrderService {
    
    public void createOrder(OrderRequest request) {
        // TRACE: Very detailed, use sparingly
        logger.trace("Entering createOrder method with request: {}", request);
        
        // DEBUG: Debugging information, disabled in production
        logger.debug("Validating order request for user {}", request.getUserId());
        
        // INFO: Important business events, enabled in production
        logger.info("Creating order for user {} with amount {}", 
            request.getUserId(), request.getAmount());
        
        // WARN: Something unexpected but handled
        if (request.getAmount() > 10000) {
            logger.warn("Large order amount detected: {} for user {}", 
                request.getAmount(), request.getUserId());
        }
        
        try {
            processPayment(request);
        } catch (PaymentException e) {
            // ERROR: Something went wrong
            logger.error("Payment processing failed for order {}", 
                request.getOrderId(), e);
            throw e;
        }
    }
}
```

**Configuration (application.yml):**

```yaml
logging:
  level:
    root: INFO                          # Default level
    com.example.OrderService: DEBUG     # Debug for specific class
    com.example.PaymentService: WARN    # Only warnings and errors
```

---

### Searching Logs in Kibana

**Common Search Queries:**

```
1. Find all errors:
   level: ERROR

2. Find logs for specific user:
   userId: 123

3. Find logs in time range:
   timestamp: [2024-02-04T10:00:00 TO 2024-02-04T11:00:00]

4. Find logs by correlation ID:
   correlationId: "abc-123"

5. Find slow requests:
   duration > 5000

6. Combine filters:
   service: "order-service" AND level: ERROR AND userId: 123

7. Search in message:
   message: "payment failed"
```

---

### Best Practices for Logging

**1. Log at Right Level**
```
✓ INFO: Business events (order created, payment processed)
✓ WARN: Unexpected but handled (high order amount, retry attempt)
✓ ERROR: Failures (payment failed, database timeout)
✗ Don't log everything at INFO level
```

**2. Include Context**
```
✓ logger.info("Order created", kv("orderId", id), kv("userId", userId))
✗ logger.info("Order created")
```

**3. Don't Log Sensitive Data**
```
✗ logger.info("Payment processed", kv("creditCard", cardNumber))
✓ logger.info("Payment processed", kv("lastFourDigits", last4))
```

**4. Use Structured Logging**
```
✓ JSON format with key-value pairs
✗ String concatenation
```

**5. Include Stack Traces for Errors**
```
✓ logger.error("Processing failed", exception)
✗ logger.error("Processing failed: " + exception.getMessage())
```

---

## Distributed Tracing

### What is Distributed Tracing?

**Definition:** Track a single request as it flows through multiple services.

**Simple Example:**

```
Without Tracing:
User: "My order is slow"
You: "Which service is slow? No idea, have to check all 10 services"

With Tracing:
User: "My order is slow"
You: Look at trace → See Payment Service took 5 seconds (should be 500ms)
     Root cause found instantly!
```

---

### How Distributed Tracing Works

**Key Concepts:**

**1. Trace**
```
Complete journey of one request
Contains multiple spans
Has unique Trace ID
```

**2. Span**
```
Single operation in the trace
Has start time and duration
Parent-child relationship with other spans
```

**Example:**

```
Trace ID: abc-123

Span 1 (API Gateway): 0ms to 1000ms
  ├─ Span 2 (Order Service): 50ms to 800ms
  │   ├─ Span 3 (Database Query): 100ms to 200ms
  │   └─ Span 4 (Payment Service): 300ms to 750ms
  │       └─ Span 5 (External API): 350ms to 700ms
  └─ Span 6 (Logging): 850ms to 900ms

Total request time: 1000ms
Bottleneck: Payment Service → External API (400ms)
```

---

### Trace Visualization

```
Timeline View:

API Gateway     ████████████████████████████████  1000ms
  Order Service   ███████████████████████         750ms
    Database        ██                            100ms
    Payment         ████████████████              450ms
      External API    ████████████                350ms
  Logging                             █           50ms

Waterfall shows:
- Which service is slowest (Payment Service)
- Which call took longest (External API)
- Parallel vs sequential operations
```

---

## OpenTelemetry

### What is OpenTelemetry?

**Definition:** Vendor-neutral standard for collecting telemetry data (traces, metrics, logs).

**Benefits:**
- One library for all observability
- Works with any backend (Jaeger, Zipkin, etc.)
- Industry standard
- Future-proof

---

### OpenTelemetry Setup

**Step 1: Add Dependencies (pom.xml)**

```xml
<dependencies>
    <!-- OpenTelemetry Spring Boot Starter -->
    <dependency>
        <groupId>io.opentelemetry.instrumentation</groupId>
        <artifactId>opentelemetry-spring-boot-starter</artifactId>
        <version>2.0.0</version>
    </dependency>
    
    <!-- Exporter (send to Jaeger/Zipkin) -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
</dependencies>
```

**Step 2: Configuration (application.yml)**

```yaml
spring:
  application:
    name: order-service

otel:
  service:
    name: ${spring.application.name}
  traces:
    exporter: otlp
  exporter:
    otlp:
      endpoint: http://localhost:4317  # OpenTelemetry Collector
  instrumentation:
    spring-webmvc:
      enabled: true
    jdbc:
      enabled: true
    logback-appender:
      enabled: true
```

**Step 3: Auto-Instrumentation**

Most common frameworks auto-instrumented:
- HTTP requests/responses
- Database calls (JDBC)
- HTTP clients (RestTemplate, WebClient, Feign)
- Messaging (Kafka, RabbitMQ)

**No code changes needed!** OpenTelemetry automatically creates spans.

---

### Custom Spans

**Add Manual Spans for Business Logic:**

```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;  // Inject OpenTelemetry tracer
    
    public Order createOrder(OrderRequest request) {
        // Create custom span
        Span span = tracer.spanBuilder("createOrder")
            .setAttribute("userId", request.getUserId())
            .setAttribute("amount", request.getAmount())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Business logic
            Order order = new Order(request);
            orderRepository.save(order);
            
            // Add more attributes
            span.setAttribute("orderId", order.getId());
            span.addEvent("Order saved to database");
            
            // Call payment service (auto-instrumented)
            PaymentResponse payment = paymentService.processPayment(order);
            
            span.addEvent("Payment processed");
            
            return order;
            
        } catch (Exception e) {
            // Record error
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Order creation failed");
            throw e;
            
        } finally {
            span.end();
        }
    }
}
```

---

### Span Attributes and Events

**Attributes (Metadata):**
```java
span.setAttribute("userId", 123);
span.setAttribute("orderAmount", 99.99);
span.setAttribute("currency", "USD");
span.setAttribute("paymentMethod", "credit_card");
```

**Events (Significant Moments):**
```java
span.addEvent("Validation started");
span.addEvent("Order created");
span.addEvent("Payment initiated");
span.addEvent("Email sent");
```

**Searching:**
```
Find traces where:
- userId = 123
- orderAmount > 100
- currency = "USD"
- Has event "Payment initiated"
```

---

## Zipkin

### What is Zipkin?

**Definition:** Distributed tracing system for troubleshooting latency problems.

**Features:**
- Visualize service dependencies
- Find slow operations
- Trace request flow
- Lightweight and fast

---

### Zipkin Setup

**Step 1: Run Zipkin Server**

```bash
# Using Docker
docker run -d -p 9411:9411 openzipkin/zipkin

# Access UI: http://localhost:9411
```

**Step 2: Add Dependencies (pom.xml)**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Step 3: Configuration (application.yml)**

```yaml
spring:
  application:
    name: order-service

management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (use 0.1 for 10% in production)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

**That's it!** Automatic instrumentation enabled.

---

### Using Zipkin UI

**1. Search Traces**
```
Service Name: order-service
Time Range: Last 1 hour
Min Duration: 1000ms (find slow requests)
```

**2. View Trace Details**
```
Click on trace → See complete flow
- Which services were called
- How long each took
- Which failed
- Request/response details
```

**3. Dependency Graph**
```
Visual map of service dependencies
- Which services talk to which
- Call frequency
- Error rates
```

---

## Jaeger

### What is Jaeger?

**Definition:** Distributed tracing platform from Uber, now CNCF project.

**Differences from Zipkin:**
- More features (adaptive sampling, root cause analysis)
- Better for large scale
- More complex setup

---

### Jaeger Setup

**Step 1: Run Jaeger**

```bash
# Using Docker (all-in-one)
docker run -d --name jaeger \
  -p 6831:6831/udp \
  -p 16686:16686 \
  jaegertracing/all-in-one:latest

# Access UI: http://localhost:16686
```

**Step 2: Add Dependencies (pom.xml)**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>
```

**Step 3: Configuration (application.yml)**

```yaml
spring:
  application:
    name: order-service

management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

---

### Jaeger Features

**1. Service Performance Monitoring**
```
See latency percentiles:
- P50 (median): 100ms
- P95: 500ms
- P99: 2000ms
```

**2. Root Cause Analysis**
```
Automatically identifies:
- Slowest operations
- Error hotspots
- Service dependencies causing issues
```

**3. Adaptive Sampling**
```
Sample 100% of errors
Sample 100% of slow requests (> 1s)
Sample 1% of normal requests

Reduces storage while keeping important traces
```

---

### Tracing Best Practices

**1. Meaningful Span Names**
```
✓ "GET /api/orders/{id}"
✓ "save-order-to-database"
✓ "process-payment-with-stripe"
✗ "doStuff"
✗ "process"
```

**2. Add Useful Attributes**
```
✓ userId, orderId, amount, status
✗ Entire object JSON (too big)
✗ Sensitive data (passwords, credit cards)
```

**3. Sample Appropriately**
```
Development: 100% (see everything)
Production: 10-20% (reduce overhead)
Always: 100% of errors
```

**4. Don't Over-Instrument**
```
✓ Service boundaries (HTTP calls)
✓ Database queries
✓ External API calls
✓ Important business logic
✗ Every method call
✗ Getter/setter methods
```

---

## Metrics

### What are Metrics?

**Definition:** Numerical measurements that change over time.

**Types:**

**1. Counter**
```
Value only increases
Examples: total requests, total errors, total orders
```

**2. Gauge**
```
Value goes up and down
Examples: CPU usage, memory, active connections
```

**3. Histogram**
```
Distribution of values
Examples: request duration, request size
Provides: min, max, average, percentiles
```

**4. Summary**
```
Similar to histogram
Pre-calculated percentiles
```

---

## Micrometer

### What is Micrometer?

**Definition:** Metrics facade for JVM applications (like SLF4J for logging).

**Benefits:**
- Vendor-neutral API
- Works with many backends (Prometheus, Graphite, etc.)
- Built into Spring Boot
- Automatic JVM metrics

---

### Micrometer Setup

**Step 1: Add Dependencies (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Step 2: Configuration (application.yml)**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

**Step 3: Access Metrics**

```bash
# All metrics
GET http://localhost:8080/actuator/metrics

# Specific metric
GET http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus format
GET http://localhost:8080/actuator/prometheus
```

---

### Built-in Metrics

**Automatic Metrics (No Code Needed):**

```
JVM Metrics:
- jvm.memory.used
- jvm.memory.max
- jvm.gc.pause
- jvm.threads.live
- jvm.classes.loaded

System Metrics:
- system.cpu.usage
- system.cpu.count
- process.uptime

HTTP Metrics:
- http.server.requests (count, duration)
- http.server.requests.max
- Response codes: 2xx, 4xx, 5xx

Database Metrics:
- jdbc.connections.active
- jdbc.connections.max
- hikaricp.connections.usage
```

---

### Custom Metrics

**Creating Custom Metrics:**

```java
@Service
public class OrderService {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCounter;
    private final Timer orderProcessingTimer;
    private final Gauge activeOrdersGauge;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Counter: Total orders created
        this.orderCounter = Counter.builder("orders.created")
            .description("Total number of orders created")
            .tag("service", "order-service")
            .register(meterRegistry);
        
        // Timer: Order processing duration
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Time taken to process orders")
            .tag("service", "order-service")
            .register(meterRegistry);
        
        // Gauge: Active orders
        this.activeOrdersGauge = Gauge.builder("orders.active", 
                this::getActiveOrderCount)
            .description("Number of active orders")
            .tag("service", "order-service")
            .register(meterRegistry);
    }
    
    public Order createOrder(OrderRequest request) {
        // Record time taken
        return orderProcessingTimer.record(() -> {
            Order order = new Order(request);
            orderRepository.save(order);
            
            // Increment counter
            orderCounter.increment();
            
            // Add tags dynamically
            meterRegistry.counter("orders.created.by.country", 
                "country", request.getCountry()).increment();
            
            return order;
        });
    }
    
    private long getActiveOrderCount() {
        return orderRepository.countByStatus("ACTIVE");
    }
}
```

---

### Metric Tags

**Adding Context with Tags:**

```java
// Without tags (not useful)
counter.increment();  // Just a number

// With tags (very useful)
meterRegistry.counter("orders.created",
    "country", "USA",
    "paymentMethod", "credit_card",
    "status", "completed"
).increment();

// Query examples:
// - Total orders by country
// - Total orders by payment method
// - Success rate by country
```

---

### Distribution Metrics

**Histogram Example:**

```java
@Service
public class OrderService {
    
    private final DistributionSummary orderAmountSummary;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.orderAmountSummary = DistributionSummary.builder("order.amount")
            .description("Distribution of order amounts")
            .baseUnit("USD")
            .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
            .register(meterRegistry);
    }
    
    public void createOrder(OrderRequest request) {
        // Record amount
        orderAmountSummary.record(request.getAmount());
    }
}
```

**Metrics Generated:**
```
order.amount.count = 1000 (total orders)
order.amount.sum = 50000 (total revenue)
order.amount.max = 500 (largest order)
order.amount.percentile.0.5 = 45 (median)
order.amount.percentile.0.95 = 250 (95th percentile)
order.amount.percentile.0.99 = 450 (99th percentile)
```

---

## Prometheus

### What is Prometheus?

**Definition:** Time-series database and monitoring system.

**Features:**
- Pull-based metrics collection
- Powerful query language (PromQL)
- Built-in alerting
- Multi-dimensional data model

---

### Prometheus Setup

**Step 1: Run Prometheus**

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s  # Scrape metrics every 15 seconds

scrape_configs:
  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
  
  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
```

**Docker:**
```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# Access UI: http://localhost:9090
```

---

### PromQL Queries

**Basic Queries:**

```promql
# Current value
http_server_requests_seconds_count

# Filter by tag
http_server_requests_seconds_count{uri="/api/orders"}

# Rate (requests per second)
rate(http_server_requests_seconds_count[5m])

# Total requests in last 5 minutes
increase(http_server_requests_seconds_count[5m])

# Average response time
rate(http_server_requests_seconds_sum[5m]) / 
rate(http_server_requests_seconds_count[5m])
```

**Advanced Queries:**

```promql
# Requests per second by URI
sum(rate(http_server_requests_seconds_count[5m])) by (uri)

# Error rate (4xx and 5xx)
sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m])) by (status)

# P95 response time
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)

# CPU usage
system_cpu_usage

# Memory usage percentage
jvm_memory_used_bytes / jvm_memory_max_bytes * 100
```

---

### Grafana Integration

**Step 1: Run Grafana**

```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana

# Access: http://localhost:3000
# Default login: admin/admin
```

**Step 2: Add Prometheus Data Source**
```
Configuration → Data Sources → Add Prometheus
URL: http://localhost:9090
```

**Step 3: Create Dashboards**

**Example Dashboard:**
```
Panel 1: Request Rate
Query: rate(http_server_requests_seconds_count[5m])
Visualization: Graph

Panel 2: Error Rate
Query: sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m]))
Visualization: Stat (with thresholds)

Panel 3: Response Time (P95)
Query: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
Visualization: Graph

Panel 4: CPU Usage
Query: system_cpu_usage * 100
Visualization: Gauge
```

---

### Key Metrics to Monitor

**RED Method (for Services):**

```
Rate: Requests per second
Error: Error rate (4xx, 5xx)
Duration: Response time (P50, P95, P99)
```

**USE Method (for Resources):**

```
Utilization: % of resource used (CPU, memory)
Saturation: Queue length, thread pool usage
Errors: Error count, error rate
```

**Example Metrics:**

```java
@Service
public class OrderService {
    
    public OrderService(MeterRegistry meterRegistry) {
        // RED metrics (automatically collected by Spring Boot)
        // http.server.requests.count (Rate)
        // http.server.requests{status=~"[45].."} (Error)
        // http.server.requests.duration (Duration)
        
        // Custom business metrics
        Counter.builder("orders.completed")
            .tag("status", "success")
            .register(meterRegistry);
        
        Gauge.builder("orders.pending", this::getPendingOrderCount)
            .register(meterRegistry);
        
        Timer.builder("payment.processing.time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }
}
```

---

## Health Checks & Alerts

### Health Checks

**Purpose:**
- Is service running?
- Can it handle requests?
- Are dependencies available?

---

### Spring Boot Actuator Health

**Built-in Health Indicators:**

```yaml
management:
  endpoint:
    health:
      show-details: always
  health:
    defaults:
      enabled: true
```

**Access:**
```bash
GET http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 250GB,
        "free": 150GB,
        "threshold": 10MB
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

### Custom Health Indicators

**Create Custom Health Check:**

```java
@Component
public class PaymentServiceHealthIndicator implements HealthIndicator {
    
    @Autowired
    private PaymentServiceClient paymentClient;
    
    @Override
    public Health health() {
        try {
            // Check if payment service is reachable
            paymentClient.ping();
            
            return Health.up()
                .withDetail("paymentService", "Available")
                .withDetail("lastCheck", LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("paymentService", "Unavailable")
                .withDetail("error", e.getMessage())
                .withDetail("lastCheck", LocalDateTime.now())
                .build();
        }
    }
}
```

**Response:**
```json
{
  "status": "DOWN",
  "components": {
    "paymentService": {
      "status": "DOWN",
      "details": {
        "paymentService": "Unavailable",
        "error": "Connection refused",
        "lastCheck": "2024-02-04T10:30:00"
      }
    }
  }
}
```

---

### Liveness vs Readiness Probes

**Liveness Probe:**
```
Question: Is the application alive?
Purpose: Should Kubernetes restart the pod?
Example: Application deadlocked, should restart
```

**Readiness Probe:**
```
Question: Is the application ready to serve traffic?
Purpose: Should Kubernetes send traffic to this pod?
Example: Still warming up cache, not ready yet
```

**Configuration:**

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

**Endpoints:**
```
Liveness:  /actuator/health/liveness
Readiness: /actuator/health/readiness
```

**Custom Readiness:**

```java
@Component
public class CacheReadinessIndicator implements HealthIndicator {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    public Health health() {
        if (cacheManager.isWarmedUp()) {
            return Health.up().build();
        } else {
            return Health.down()
                .withDetail("cache", "Not warmed up")
                .build();
        }
    }
}
```

---

### Alerting

### Prometheus Alerting Rules

**alerting-rules.yml:**

```yaml
groups:
  - name: service_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: |
          (sum(rate(http_server_requests_seconds_count{status=~"[45].."}[5m])) 
           / sum(rate(http_server_requests_seconds_count[5m]))) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} for {{ $labels.service }}"
      
      # High response time
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95, 
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "P95 latency is {{ $value }}s"
      
      # Service down
      - alert: ServiceDown
        expr: up{job="order-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "{{ $labels.job }} has been down for more than 1 minute"
      
      # High CPU usage
      - alert: HighCPU
        expr: system_cpu_usage > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value | humanizePercentage }}"
      
      # Low memory
      - alert: LowMemory
        expr: |
          (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Low memory"
          description: "Memory usage is {{ $value | humanizePercentage }}"
```

---

### AlertManager Configuration

**alertmanager.yml:**

```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@example.com'
  smtp_auth_username: 'alerts@example.com'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'email-alerts'
  
  routes:
    # Critical alerts → PagerDuty
    - match:
        severity: critical
      receiver: 'pagerduty'
    
    # Warning alerts → Slack
    - match:
        severity: warning
      receiver: 'slack'

receivers:
  - name: 'email-alerts'
    email_configs:
      - to: 'team@example.com'
        headers:
          Subject: '{{ .GroupLabels.alertname }} - {{ .GroupLabels.severity }}'
  
  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/...'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
  
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: 'your-pagerduty-key'
```

---

### Complete Observability Stack

**Docker Compose:**

```yaml
version: '3.8'

services:
  # Logging
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    ports:
      - "9200:9200"
    environment:
      - discovery.type=single-node
  
  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    ports:
      - "5000:5000"
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
  
  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
  
  # Tracing
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"
      - "4317:4317"
  
  # Metrics
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
  
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
  
  # Alerting
  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
```

---

## Summary

### The Three Pillars

**Logs:**
- What happened and when
- Detailed event records
- Use: Debugging, audit trails
- Tools: ELK Stack (Elasticsearch, Logstash, Kibana)

**Traces:**
- Request journey across services
- Performance bottlenecks
- Use: Find slow operations, understand dependencies
- Tools: OpenTelemetry, Jaeger, Zipkin

**Metrics:**
- Numerical measurements over time
- System health indicators
- Use: Monitoring, alerting, capacity planning
- Tools: Micrometer, Prometheus, Grafana

---

### Best Practices

**1. Use Correlation IDs**
```
Track requests across all services
Include in logs, traces, and errors
```

**2. Structure Everything**
```
Structured logs (JSON)
Tagged metrics
Attributed traces
Easier to query and analyze
```

**3. Sample Intelligently**
```
Logs: All errors, sample normal (10%)
Traces: All errors, all slow, sample normal (10%)
Metrics: Always collect (low overhead)
```

**4. Monitor What Matters**
```
RED metrics for services (Rate, Error, Duration)
USE metrics for resources (Utilization, Saturation, Errors)
Business metrics (orders, revenue, signups)
```

**5. Alert on Symptoms, Not Causes**
```
✓ Alert: Response time > 1s
✗ Alert: CPU > 80%

Users care about slow responses, not CPU
```

**6. Make Alerts Actionable**
```
Include:
- What's wrong
- Why it matters
- How to fix it
- Runbook link
```

**7. Practice Observability-Driven Development**
```
Add logging while coding
Add custom metrics for business logic
Add spans for important operations
Test observability before deploying
```

---

### Quick Reference

**When User Reports Problem:**

```
Step 1: Check Metrics (Is there a spike?)
  → Grafana dashboard

Step 2: Find Request Trace
  → Search Jaeger by time/user

Step 3: Identify Slow/Failed Service
  → Look at trace waterfall

Step 4: Check Service Logs
  → Search Kibana with correlation ID

Step 5: Analyze Root Cause
  → Logs show exact error

Step 6: Fix and Monitor
  → Deploy fix
  → Watch metrics return to normal
```

**Remember:** You can't fix what you can't see. Invest in observability from day one!
