# Inter-Service Communication in Java Microservices

## Table of Contents
1. [Introduction to Inter-Service Communication](#introduction-to-inter-service-communication)
2. [Synchronous Communication](#synchronous-communication)
   - [REST with RestTemplate](#rest-with-resttemplate)
   - [WebClient (Reactive)](#webclient-reactive)
   - [OpenFeign](#openfeign)
3. [Asynchronous Communication](#asynchronous-communication)
   - [Message Brokers Overview](#message-brokers-overview)
   - [Apache Kafka](#apache-kafka)
   - [RabbitMQ](#rabbitmq)
4. [Event Schemas](#event-schemas)
5. [Idempotency](#idempotency)
6. [Delivery Guarantees](#delivery-guarantees)

---

## Introduction to Inter-Service Communication

### What is Inter-Service Communication?

**Definition:** The way microservices **exchange data and coordinate** with each other.

**Why Important:**
In microservices, different services need to work together to complete business tasks.

**Example:**
```
User places an order:

Order Service needs to:
1. Talk to Product Service (check availability)
2. Talk to Payment Service (process payment)
3. Talk to Inventory Service (reduce stock)
4. Talk to Email Service (send confirmation)

All these "talks" = Inter-Service Communication
```

---

### Two Main Types

**1. Synchronous Communication**
Service waits for response.
```
Order Service → "Is product available?" → Product Service
Order Service ← "Yes, 10 in stock" ← Product Service
Order Service continues...
```

**2. Asynchronous Communication**
Service doesn't wait, uses messages.
```
Order Service → "Order Created" → Message Queue
Order Service continues immediately...
Email Service ← picks message later ← Message Queue
```

---

## Synchronous Communication

Synchronous means **request-response** pattern. Service A calls Service B and waits for answer.

**When to Use:**
- Need immediate response
- Simple operations
- User is waiting
- Data needed to continue

---

## REST with RestTemplate

### What is RestTemplate?

**Definition:** Old Spring class for making HTTP calls to other services.

**Status:** Legacy (old), still works but not recommended for new projects.

**Theory:**
RestTemplate provides methods to call REST APIs easily. It's **blocking** - your code stops and waits for response.

---

### Basic Setup

**Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Create RestTemplate Bean:**
```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

### Simple GET Request

**Scenario:** Order Service calls Product Service to get product details.

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Product getProduct(Long productId) {
        // URL of Product Service
        String url = "http://product-service/api/products/" + productId;
        
        // Make GET request and get Product object
        Product product = restTemplate.getForObject(url, Product.class);
        
        return product;
    }
}
```

**What Happens:**
```
1. Code calls getForObject()
2. Code STOPS and WAITS
3. HTTP GET request sent to product-service
4. Product Service processes request
5. Product Service sends response
6. restTemplate converts JSON to Product object
7. Code continues with product data
```

---

### Simple POST Request

**Scenario:** Order Service creates a payment.

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public PaymentResponse processPayment(PaymentRequest request) {
        String url = "http://payment-service/api/payments";
        
        // Make POST request with request body
        PaymentResponse response = restTemplate.postForObject(
            url, 
            request,  // This becomes JSON in request body
            PaymentResponse.class
        );
        
        return response;
    }
}
```

---

### All RestTemplate Methods

**GET Requests:**
```java
// Get as object
Product product = restTemplate.getForObject(url, Product.class);

// Get as ResponseEntity (includes status code, headers)
ResponseEntity<Product> response = restTemplate.getForEntity(url, Product.class);
Product product = response.getBody();
int status = response.getStatusCodeValue(); // 200, 404, etc.
```

**POST Requests:**
```java
// Post and get object
Payment payment = restTemplate.postForObject(url, request, Payment.class);

// Post and get ResponseEntity
ResponseEntity<Payment> response = restTemplate.postForEntity(url, request, Payment.class);
```

**PUT Request (Update):**
```java
// Update - no return value
restTemplate.put(url, updatedProduct);
```

**DELETE Request:**
```java
// Delete - no return value
restTemplate.delete(url);
```

---

### Handling Errors

**Problem:** What if service is down or returns error?

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Product getProduct(Long productId) {
        try {
            String url = "http://product-service/api/products/" + productId;
            Product product = restTemplate.getForObject(url, Product.class);
            return product;
            
        } catch (HttpClientErrorException e) {
            // 4xx errors (400, 404, etc.)
            System.out.println("Client error: " + e.getStatusCode());
            return null;
            
        } catch (HttpServerErrorException e) {
            // 5xx errors (500, 503, etc.)
            System.out.println("Server error: " + e.getStatusCode());
            return null;
            
        } catch (ResourceAccessException e) {
            // Network errors (service down, timeout)
            System.out.println("Service unavailable");
            return null;
        }
    }
}
```

---

### Adding Timeout

**Problem:** Service is slow, don't want to wait forever.

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        // Create factory with timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds to connect
        factory.setReadTimeout(5000);     // 5 seconds to read response
        
        return new RestTemplate(factory);
    }
}
```

---

### Why RestTemplate is Legacy

**Problems:**
1. **Blocking:** Thread waits doing nothing (wastes resources)
2. **Not reactive:** Can't handle high load efficiently
3. **Limited features:** No built-in retry, circuit breaker
4. **Synchronous only:** Can't do async operations

**Replacement:** Use WebClient instead (explained next).

---

## WebClient (Reactive)

### What is WebClient?

**Definition:** Modern Spring class for making HTTP calls. It's **non-blocking** and **reactive**.

**Status:** Preferred for new projects.

**Theory:**
WebClient doesn't block threads. While waiting for response, thread can do other work. Much more efficient.

---

### Blocking vs Non-Blocking

**RestTemplate (Blocking):**
```
Thread 1: Call Service → WAIT → Get Response → Continue
Thread 2: Call Service → WAIT → Get Response → Continue
Thread 3: Call Service → WAIT → Get Response → Continue

3 threads waiting, doing nothing
```

**WebClient (Non-Blocking):**
```
Thread 1: Call Service → Do other work → Get Response
Thread 1: Call Service → Do other work → Get Response
Thread 1: Call Service → Do other work → Get Response

1 thread handles multiple requests
```

**Result:** WebClient can handle more requests with fewer threads.

---

### Basic Setup

**Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Create WebClient Bean:**
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("http://product-service")  // Base URL
            .build();
    }
}
```

---

### Simple GET Request

**Blocking Style (like RestTemplate):**
```java
@Service
public class OrderService {
    
    @Autowired
    private WebClient webClient;
    
    public Product getProduct(Long productId) {
        Product product = webClient
            .get()  // GET request
            .uri("/api/products/{id}", productId)  // URL path
            .retrieve()  // Execute request
            .bodyToMono(Product.class)  // Convert to Product
            .block();  // WAIT for response (blocking)
        
        return product;
    }
}
```

**Non-Blocking Style (Reactive):**
```java
@Service
public class OrderService {
    
    @Autowired
    private WebClient webClient;
    
    public Mono<Product> getProduct(Long productId) {
        Mono<Product> productMono = webClient
            .get()
            .uri("/api/products/{id}", productId)
            .retrieve()
            .bodyToMono(Product.class);  // No .block()
        
        return productMono;  // Returns immediately
    }
}
```

**Understanding Mono:**
- `Mono` = Promise of single value in future
- Like a subscription: "Tell me when data arrives"
- Doesn't wait, returns immediately

---

### Simple POST Request

```java
@Service
public class OrderService {
    
    @Autowired
    private WebClient webClient;
    
    public PaymentResponse processPayment(PaymentRequest request) {
        PaymentResponse response = webClient
            .post()  // POST request
            .uri("/api/payments")
            .bodyValue(request)  // Request body
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .block();  // Wait for response
        
        return response;
    }
}
```

---

### Handling Errors

```java
public Product getProduct(Long productId) {
    Product product = webClient
        .get()
        .uri("/api/products/{id}", productId)
        .retrieve()
        .onStatus(
            HttpStatus::is4xxClientError,  // 404, 400, etc.
            response -> Mono.error(new ProductNotFoundException())
        )
        .onStatus(
            HttpStatus::is5xxServerError,  // 500, 503, etc.
            response -> Mono.error(new ServiceUnavailableException())
        )
        .bodyToMono(Product.class)
        .block();
    
    return product;
}
```

---

### Adding Timeout

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(5));  // 5 second timeout
        
        return WebClient.builder()
            .baseUrl("http://product-service")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

---

### Adding Retry Logic

```java
public Product getProduct(Long productId) {
    Product product = webClient
        .get()
        .uri("/api/products/{id}", productId)
        .retrieve()
        .bodyToMono(Product.class)
        .retry(3)  // Retry 3 times if fails
        .block();
    
    return product;
}
```

**Advanced Retry (with delay):**
```java
public Product getProduct(Long productId) {
    Product product = webClient
        .get()
        .uri("/api/products/{id}", productId)
        .retrieve()
        .bodyToMono(Product.class)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))  // 3 retries, wait 2 seconds
        .block();
    
    return product;
}
```

---

### Comparison: RestTemplate vs WebClient

| Feature | RestTemplate | WebClient |
|---------|-------------|-----------|
| Blocking | Yes | Optional |
| Reactive | No | Yes |
| Resource Usage | High (one thread per request) | Low (threads reused) |
| Performance | Good | Excellent |
| Scalability | Limited | High |
| Modern APIs | No | Yes |
| Recommended | No (legacy) | Yes |

**When to Use WebClient:**
- New projects (always)
- High traffic applications
- Need reactive programming
- Want better performance

**When RestTemplate is OK:**
- Legacy projects
- Low traffic
- Simple use cases
- Team not familiar with reactive

---

## OpenFeign

### What is OpenFeign?

**Definition:** Declarative REST client. Write interface, Feign creates implementation automatically.

**Theory:**
Instead of writing HTTP calls manually, you define interface methods. Feign generates the actual HTTP code.

**Key Benefit:** Less boilerplate code, cleaner, easier to read.

---

### Basic Setup

**Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**Enable Feign in Application:**
```java
@SpringBootApplication
@EnableFeignClients  // Enable Feign clients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

### Creating Feign Client

**Define Interface:**
```java
@FeignClient(name = "product-service", url = "http://localhost:8081")
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{id}")
    Product getProduct(@PathVariable("id") Long productId);
    
    @GetMapping("/api/products")
    List<Product> getAllProducts();
    
    @PostMapping("/api/products")
    Product createProduct(@RequestBody Product product);
}
```

**That's it!** Feign creates the implementation automatically.

---

### Using Feign Client

```java
@Service
public class OrderService {
    
    @Autowired
    private ProductServiceClient productClient;
    
    public void createOrder(OrderRequest request) {
        // Just call the method - looks like local method!
        Product product = productClient.getProduct(request.getProductId());
        
        if (product.getStock() > 0) {
            // Create order logic
        }
    }
}
```

**What Happens:**
```
1. Call productClient.getProduct(123)
2. Feign converts to: GET http://localhost:8081/api/products/123
3. Feign sends HTTP request
4. Feign receives response
5. Feign converts JSON to Product object
6. Returns Product
```

---

### Service Discovery with Feign

**Without Hardcoded URL:**
```java
@FeignClient(name = "product-service")  // No URL!
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{id}")
    Product getProduct(@PathVariable("id") Long productId);
}
```

**How it Works:**
```
1. Feign asks Service Discovery: "Where is product-service?"
2. Service Discovery: "product-service is at 192.168.1.50:8081"
3. Feign calls: http://192.168.1.50:8081/api/products/123

If product-service moves to different IP, Feign automatically uses new IP
```

**Need:** Service Discovery (Eureka, Consul) must be configured.

---

### Handling Errors

**Default Behavior:**
Feign throws exceptions for errors.

**Custom Error Handling:**
```java
@Component
public class ProductClientErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ProductNotFoundException("Product not found");
        }
        if (response.status() == 500) {
            return new ServiceUnavailableException("Product service down");
        }
        return new Exception("Unknown error");
    }
}
```

**Register Error Decoder:**
```java
@FeignClient(
    name = "product-service",
    configuration = ProductClientConfig.class
)
public interface ProductServiceClient {
    // methods...
}

@Configuration
class ProductClientConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ProductClientErrorDecoder();
    }
}
```

---

### Adding Timeout

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000,  // Connection timeout (5 seconds)
            5000   // Read timeout (5 seconds)
        );
    }
}
```

**Apply to Client:**
```java
@FeignClient(
    name = "product-service",
    configuration = FeignConfig.class
)
public interface ProductServiceClient {
    // methods...
}
```

---

### Adding Retry

**Add Dependency:**
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

**Configure Retry:**
```java
@Configuration
public class FeignConfig {
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100,   // Initial wait time (ms)
            1000,  // Max wait time (ms)
            3      // Max retry attempts
        );
    }
}
```

---

### Request/Response Logging

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // Log everything
    }
}
```

**Log Levels:**
- `NONE`: No logging
- `BASIC`: Request method, URL, response status
- `HEADERS`: Basic + headers
- `FULL`: Headers + body

**Enable in application.properties:**
```properties
logging.level.com.example.ProductServiceClient=DEBUG
```

---

### Feign with Headers

**Pass Custom Headers:**
```java
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{id}")
    Product getProduct(
        @PathVariable("id") Long productId,
        @RequestHeader("Authorization") String authToken
    );
}
```

**Usage:**
```java
Product product = productClient.getProduct(123, "Bearer xyz123");
```

**Add Headers to All Requests:**
```java
@Component
public class FeignClientInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer token123");
        template.header("X-Request-ID", UUID.randomUUID().toString());
    }
}
```

---

### Advantages of Feign

✓ **Less Code:** Just interface, no implementation
✓ **Readable:** Method signatures are self-documenting
✓ **Integration:** Works with Spring Cloud (Service Discovery, Load Balancer)
✓ **Declarative:** Like Spring Data JPA for REST calls
✓ **Built-in Features:** Retry, timeout, logging

---

### Disadvantages of Feign

✗ **Blocking Only:** Not reactive (can't use with WebFlux efficiently)
✗ **Less Control:** Can't customize as much as WebClient
✗ **Learning Curve:** Need to understand Feign configuration
✗ **Dependency:** Adds another library to project

---

### When to Use Each Tool

**Use RestTemplate:**
- Legacy projects only
- Already using it
- Team familiar with it

**Use WebClient:**
- Reactive applications
- High performance needs
- Modern projects
- Need async operations
- Fine-grained control needed

**Use OpenFeign:**
- Service-to-service calls
- Using Spring Cloud
- Want clean, readable code
- Don't need reactive
- Team prefers declarative style

---

## Asynchronous Communication

Asynchronous means **fire-and-forget** or **event-based**. Service sends message and continues immediately without waiting.

**When to Use:**
- Long-running operations
- Don't need immediate response
- Decoupling services
- Event-driven workflows

---

## Message Brokers Overview

### What is a Message Broker?

**Definition:** Middleware that receives, stores, and delivers messages between services.

**Theory:**
Instead of services calling each other directly, they send messages through a broker. The broker ensures messages are delivered.

**Simple Analogy:**
```
Direct Call (Synchronous):
Person A → calls directly → Person B

Message Broker (Asynchronous):
Person A → leaves voicemail → Answering Machine
Person B → checks voicemail later → Gets message

Answering Machine = Message Broker
```

---

### How Message Broker Works

```
Producer Service                Message Broker                Consumer Service
     |                                |                              |
     |------ Send Message ----------->|                              |
     |                                |                              |
     | Continues work immediately     |                              |
     |                                |                              |
     |                                |<----- Poll for messages -----|
     |                                |                              |
     |                                |------ Deliver Message ------>|
     |                                |                              |
     |                                |                      Process message
```

---

### Key Concepts

**1. Producer**
Service that sends messages.

**2. Consumer**
Service that receives and processes messages.

**3. Queue**
Line where messages wait (FIFO - First In First Out).
```
Message 1 → Message 2 → Message 3 → | Consumer picks from front
```

**4. Topic**
Named channel for messages. Multiple consumers can subscribe.
```
             Topic: "orders"
                   |
        +---------+---------+
        |         |         |
   Consumer1  Consumer2  Consumer3
   
All three receive the same message
```

**5. Message**
Data being sent (usually JSON).
```json
{
  "eventType": "OrderCreated",
  "orderId": "12345",
  "userId": "user-1",
  "amount": 99.99
}
```

---

### Benefits of Message Brokers

**1. Decoupling**
Services don't need to know about each other.
```
Order Service publishes message
It doesn't know:
- Who consumes it
- How many consumers
- When it's consumed
```

**2. Reliability**
Messages stored until consumed.
```
Service A sends message
Service B is down
Message waits in broker
Service B comes back up
Service B receives message

No data lost!
```

**3. Load Leveling**
Handle traffic spikes.
```
1000 messages arrive in 1 second
Consumer processes 10 messages/second
Messages wait in queue
Consumer processes gradually
No system overload
```

**4. Scalability**
Add more consumers to process faster.
```
1 consumer: 10 msg/sec
Add 2 more consumers: 30 msg/sec
```

---

## Apache Kafka

### What is Kafka?

**Definition:** Distributed event streaming platform. Think of it as a **high-speed, durable log** of events.

**Theory:**
Kafka stores messages in order on disk. Messages stay even after being read. Multiple consumers can read same messages.

**Key Characteristics:**
- Very fast (millions of messages/second)
- Persistent (messages stored on disk)
- Distributed (runs on multiple servers)
- Scalable (can handle huge volumes)

---

### Kafka Core Concepts

**1. Topic**
Category or feed name where messages are published.
```
Topics in E-Commerce:
- order-created
- payment-processed
- email-notifications
- inventory-updates
```

**2. Partition**
Topics split into partitions for parallel processing.
```
Topic: order-created (3 partitions)

Partition 0: [msg1, msg4, msg7]
Partition 1: [msg2, msg5, msg8]
Partition 2: [msg3, msg6, msg9]

Different consumers can read different partitions simultaneously
```

**3. Offset**
Unique ID for each message in partition.
```
Partition 0:
Offset 0: Message 1
Offset 1: Message 2
Offset 2: Message 3

Consumer reads offset 0, then 1, then 2 (in order)
```

**4. Producer**
Publishes messages to topics.

**5. Consumer**
Reads messages from topics.

**6. Consumer Group**
Multiple consumers working together.
```
Consumer Group: "email-senders"
  - Consumer 1: Reads Partition 0
  - Consumer 2: Reads Partition 1
  - Consumer 3: Reads Partition 2

Work divided among consumers
```

---

### Kafka Setup in Spring Boot

**Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Configuration (application.properties):**
```properties
# Kafka server location
spring.kafka.bootstrap-servers=localhost:9092

# Producer configuration
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Consumer configuration
spring.kafka.consumer.group-id=order-consumer-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

---

### Producing Messages (Sending)

**Simple Producer:**
```java
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void createOrder(Order order) {
        // Save order to database
        orderRepository.save(order);
        
        // Create event
        OrderEvent event = new OrderEvent(
            order.getId(),
            order.getUserId(),
            order.getTotal()
        );
        
        // Send to Kafka topic
        kafkaTemplate.send("order-created", event);
        
        // Method returns immediately, sending happens async
    }
}
```

**Event Class:**
```java
public class OrderEvent {
    private Long orderId;
    private Long userId;
    private Double total;
    
    // Constructor, getters, setters
}
```

---

### Consuming Messages (Receiving)

**Simple Consumer:**
```java
@Service
public class EmailService {
    
    @KafkaListener(topics = "order-created", groupId = "email-consumer-group")
    public void handleOrderCreated(OrderEvent event) {
        // This method called automatically when message arrives
        
        System.out.println("Received order: " + event.getOrderId());
        
        // Send email
        sendEmail(event.getUserId(), "Order confirmed: " + event.getOrderId());
    }
    
    private void sendEmail(Long userId, String message) {
        // Email sending logic
    }
}
```

**What Happens:**
```
1. Order Service sends message to "order-created" topic
2. Kafka stores message
3. Email Service listens to "order-created" topic
4. Kafka calls handleOrderCreated() method
5. Email sent
```

---

### Multiple Consumers Example

```java
// Consumer 1: Email Service
@Service
public class EmailService {
    
    @KafkaListener(topics = "order-created", groupId = "email-group")
    public void sendEmail(OrderEvent event) {
        System.out.println("Sending email for order: " + event.getOrderId());
    }
}

// Consumer 2: Analytics Service
@Service
public class AnalyticsService {
    
    @KafkaListener(topics = "order-created", groupId = "analytics-group")
    public void updateAnalytics(OrderEvent event) {
        System.out.println("Updating analytics for order: " + event.getOrderId());
    }
}

// Consumer 3: Inventory Service
@Service
public class InventoryService {
    
    @KafkaListener(topics = "order-created", groupId = "inventory-group")
    public void reduceStock(OrderEvent event) {
        System.out.println("Reducing stock for order: " + event.getOrderId());
    }
}
```

**Result:** All three services receive the same message independently.

---

### Sending with Partition Key

**Why:** Ensure related messages go to same partition (maintains order).

```java
public void createOrder(Order order) {
    OrderEvent event = new OrderEvent(order.getId(), order.getUserId(), order.getTotal());
    
    // Use userId as key
    // All orders from same user go to same partition
    kafkaTemplate.send(
        "order-created",           // topic
        order.getUserId().toString(),  // key (partition key)
        event                      // value
    );
}
```

**Benefit:**
```
User 1's orders: Partition 0 (always in order)
User 2's orders: Partition 1 (always in order)
User 3's orders: Partition 2 (always in order)

Orders from same user processed in order
```

---

### Error Handling in Consumer

```java
@Service
public class EmailService {
    
    @KafkaListener(topics = "order-created", groupId = "email-group")
    public void handleOrderCreated(OrderEvent event) {
        try {
            sendEmail(event.getUserId(), "Order: " + event.getOrderId());
            
        } catch (Exception e) {
            // Log error
            System.err.println("Failed to send email: " + e.getMessage());
            
            // Message acknowledged, won't be retried
            // For retry, throw exception instead of catching
        }
    }
}
```

**Auto Retry:**
```java
@KafkaListener(topics = "order-created", groupId = "email-group")
public void handleOrderCreated(OrderEvent event) {
    // If exception thrown, Kafka retries automatically
    sendEmail(event.getUserId(), "Order: " + event.getOrderId());
}
```

---

### Kafka Use Cases

**Best For:**
- Event streaming (user actions, logs)
- High throughput (millions of events)
- Event sourcing (store all events)
- Log aggregation (collect logs from services)
- Real-time analytics (process data as it comes)

**Example Scenarios:**
```
1. User Activity Tracking
   - User clicks → Kafka → Analytics
   
2. Order Processing
   - Order created → Kafka → Email, Inventory, Analytics
   
3. Log Collection
   - All services → Kafka → Log Storage
   
4. Real-time Notifications
   - Events → Kafka → Notification Service → Push to users
```

---

## RabbitMQ

### What is RabbitMQ?

**Definition:** Message broker that implements AMQP (Advanced Message Queuing Protocol).

**Theory:**
RabbitMQ acts as a **post office**. Producers send messages to exchanges, exchanges route to queues, consumers read from queues.

**Key Characteristics:**
- Message queueing (messages wait in line)
- Flexible routing (route messages based on rules)
- Acknowledgments (confirm message received)
- Dead letter queues (handle failed messages)

---

### RabbitMQ Core Concepts

**1. Producer**
Sends messages.

**2. Exchange**
Receives messages and routes them to queues.
```
Think of Exchange as a mail sorter:
"This message goes to Queue A"
"That message goes to Queue B"
```

**3. Queue**
Stores messages until consumed.
```
FIFO: First In, First Out
[Msg1] [Msg2] [Msg3] → Consumer
```

**4. Binding**
Link between exchange and queue.
```
Exchange "orders" → Binding (routing key: "new") → Queue "new-orders"
```

**5. Consumer**
Receives and processes messages.

---

### Exchange Types

**1. Direct Exchange**
Routes to queue based on exact routing key match.
```
Message with routing key "email" → Queue "email-queue"
Message with routing key "sms" → Queue "sms-queue"

Exact match required
```

**2. Fanout Exchange**
Routes to all bound queues (ignores routing key).
```
Message → Exchange → Queue 1
                  → Queue 2
                  → Queue 3
                  
All queues get same message
```

**3. Topic Exchange**
Routes based on pattern matching.
```
Routing key patterns:
"order.created" → Queue A
"order.*" → Queue B (matches order.created, order.updated)
"*.created" → Queue C (matches order.created, user.created)
```

**4. Headers Exchange**
Routes based on message headers (rarely used).

---

### RabbitMQ Setup in Spring Boot

**Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**Configuration (application.properties):**
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

---

### Creating Queue and Exchange

```java
@Configuration
public class RabbitMQConfig {
    
    // Define Queue
    @Bean
    public Queue orderQueue() {
        return new Queue("order-queue", true);  // true = durable (survives restart)
    }
    
    // Define Exchange
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order-exchange");
    }
    
    // Bind Queue to Exchange
    @Bean
    public Binding binding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder
            .bind(orderQueue)
            .to(orderExchange)
            .with("order.created");  // routing key
    }
}
```

---

### Producing Messages (Sending)

**Simple Producer:**
```java
@Service
public class OrderService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void createOrder(Order order) {
        // Save order
        orderRepository.save(order);
        
        // Create event
        OrderEvent event = new OrderEvent(order.getId(), order.getUserId());
        
        // Send to RabbitMQ
        rabbitTemplate.convertAndSend(
            "order-exchange",    // exchange name
            "order.created",     // routing key
            event                // message
        );
        
        // Returns immediately
    }
}
```

---

### Consuming Messages (Receiving)

**Simple Consumer:**
```java
@Service
public class EmailService {
    
    @RabbitListener(queues = "order-queue")
    public void handleOrderCreated(OrderEvent event) {
        // This method called when message arrives
        
        System.out.println("Processing order: " + event.getOrderId());
        
        // Send email
        sendEmail(event.getUserId(), "Order confirmed");
    }
}
```

---

### Fanout Exchange Example

**Configuration:**
```java
@Configuration
public class RabbitMQConfig {
    
    // Fanout Exchange
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("order-fanout");
    }
    
    // Multiple Queues
    @Bean
    public Queue emailQueue() {
        return new Queue("email-queue");
    }
    
    @Bean
    public Queue smsQueue() {
        return new Queue("sms-queue");
    }
    
    @Bean
    public Queue analyticsQueue() {
        return new Queue("analytics-queue");
    }
    
    // Bind all queues to fanout exchange
    @Bean
    public Binding emailBinding(Queue emailQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(emailQueue).to(fanoutExchange);
    }
    
    @Bean
    public Binding smsBinding(Queue smsQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(smsQueue).to(fanoutExchange);
    }
    
    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(analyticsQueue).to(fanoutExchange);
    }
}
```

**Producer:**
```java
public void createOrder(Order order) {
    OrderEvent event = new OrderEvent(order.getId());
    
    // Send to fanout exchange
    rabbitTemplate.convertAndSend("order-fanout", "", event);
    
    // Message goes to email-queue, sms-queue, and analytics-queue
}
```

**Consumers:**
```java
@Service
public class EmailService {
    @RabbitListener(queues = "email-queue")
    public void sendEmail(OrderEvent event) {
        System.out.println("Sending email");
    }
}

@Service
public class SMSService {
    @RabbitListener(queues = "sms-queue")
    public void sendSMS(OrderEvent event) {
        System.out.println("Sending SMS");
    }
}

@Service
public class AnalyticsService {
    @RabbitListener(queues = "analytics-queue")
    public void updateAnalytics(OrderEvent event) {
        System.out.println("Updating analytics");
    }
}
```

---

### Message Acknowledgment

**Theory:**
Consumer tells RabbitMQ "I processed this message successfully".

**Auto Acknowledgment (default):**
```java
@RabbitListener(queues = "order-queue")
public void handleOrder(OrderEvent event) {
    // Message auto-acknowledged when method returns
    processOrder(event);
}
```

**Manual Acknowledgment:**
```java
@RabbitListener(queues = "order-queue", ackMode = "MANUAL")
public void handleOrder(OrderEvent event, Channel channel, 
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    try {
        processOrder(event);
        
        // Manually acknowledge
        channel.basicAck(tag, false);
        
    } catch (Exception e) {
        // Reject and requeue
        channel.basicNack(tag, false, true);
    }
}
```

**Benefits:**
- If consumer crashes before ack, message requeued
- Ensures no message lost

---

### Dead Letter Queue

**Theory:**
Queue for messages that failed processing.

**Configuration:**
```java
@Bean
public Queue orderQueue() {
    return QueueBuilder.durable("order-queue")
        .withArgument("x-dead-letter-exchange", "dead-letter-exchange")
        .withArgument("x-dead-letter-routing-key", "dead-letter")
        .build();
}

@Bean
public Queue deadLetterQueue() {
    return new Queue("dead-letter-queue");
}

@Bean
public DirectExchange deadLetterExchange() {
    return new DirectExchange("dead-letter-exchange");
}

@Bean
public Binding deadLetterBinding() {
    return BindingBuilder
        .bind(deadLetterQueue())
        .to(deadLetterExchange())
        .with("dead-letter");
}
```

**How it Works:**
```
1. Message fails processing 3 times
2. RabbitMQ moves it to dead-letter-queue
3. Manual investigation and reprocessing
```

---

### RabbitMQ vs Kafka

| Feature | RabbitMQ | Kafka |
|---------|----------|-------|
| Type | Message Broker | Event Streaming |
| Speed | Fast | Very Fast |
| Message Retention | Deleted after consumption | Kept for configured time |
| Ordering | Per queue | Per partition |
| Routing | Complex (exchanges) | Simple (topics) |
| Use Case | Task queues, RPC | Event streaming, logs |
| Replay Messages | No | Yes |
| Acknowledgment | Built-in | Manual offset management |

**Choose RabbitMQ When:**
- Need complex routing
- Task queue patterns
- Request-response needed
- Message acknowledgment important
- Traditional messaging

**Choose Kafka When:**
- High throughput needed
- Event streaming
- Need to replay messages
- Multiple consumers need same data
- Log aggregation

---

## Event Schemas

### What is an Event Schema?

**Definition:** Structure/format of event messages. Defines what fields event contains and their types.

**Theory:**
Like a contract between producer and consumer. Both agree on what data is sent.

---

### Why Event Schemas Matter

**Problem Without Schema:**
```
Producer sends:
{
  "orderId": "123",
  "amount": "99.99"
}

Consumer expects:
{
  "order_id": 123,
  "total": 99.99
}

Field names don't match!
Types don't match!
Consumer breaks!
```

**Solution With Schema:**
Both agree on exact format beforehand.

---

### Simple Event Schema Example

**OrderCreatedEvent.java:**
```java
public class OrderCreatedEvent {
    private Long orderId;        // Required
    private Long userId;         // Required
    private Double totalAmount;  // Required
    private LocalDateTime createdAt;  // Required
    private List<OrderItem> items;    // Optional
    
    // Constructors
    // Getters and Setters
}

public class OrderItem {
    private Long productId;
    private Integer quantity;
    private Double price;
    
    // Constructors, Getters, Setters
}
```

**Producer:**
```java
OrderCreatedEvent event = new OrderCreatedEvent(
    orderId: 123,
    userId: 456,
    totalAmount: 99.99,
    createdAt: LocalDateTime.now(),
    items: [...]
);

kafkaTemplate.send("order-created", event);
```

**Consumer:**
```java
@KafkaListener(topics = "order-created")
public void handle(OrderCreatedEvent event) {
    // event guaranteed to have all required fields
    Long orderId = event.getOrderId();
    // Safe to use
}
```

---

### Schema Versioning

**Problem:**
Need to change schema but old consumers still running.

**Example:**
```
Version 1:
{
  "orderId": 123,
  "amount": 99.99
}

Version 2 (added field):
{
  "orderId": 123,
  "amount": 99.99,
  "currency": "USD"
}

Old consumers don't know about "currency" field
```

**Solution Strategies:**

**1. Backward Compatibility**
New fields are optional.
```java
public class OrderCreatedEvent {
    private Long orderId;        // Required
    private Double amount;       // Required
    private String currency;     // Optional, defaults to "USD"
    
    public String getCurrency() {
        return currency != null ? currency : "USD";
    }
}
```

**2. Version Field**
Include version in event.
```java
public class OrderCreatedEvent {
    private String version = "2.0";
    private Long orderId;
    private Double amount;
    private String currency;
}
```

**Consumer Handles Versions:**
```java
@KafkaListener(topics = "order-created")
public void handle(OrderCreatedEvent event) {
    if ("1.0".equals(event.getVersion())) {
        // Handle old format
    } else if ("2.0".equals(event.getVersion())) {
        // Handle new format
    }
}
```

**3. Separate Topics**
```
Version 1 → Topic: order-created-v1
Version 2 → Topic: order-created-v2

Consumers choose which topic to consume
```

---

### Schema Registry (Advanced)

**What:** Central repository storing event schemas.

**Popular Tools:**
- Confluent Schema Registry (for Kafka)
- AWS Glue Schema Registry

**How it Works:**
```
1. Producer registers schema
2. Schema Registry stores schema with ID
3. Producer sends: [Schema ID][Data]
4. Consumer reads Schema ID
5. Consumer gets schema from Registry
6. Consumer deserializes data using schema

Ensures producer and consumer use same schema
```

**Benefits:**
- Schema validation (reject invalid events)
- Schema evolution (controlled changes)
- Documentation (schemas as documentation)

---

### Best Practices for Schemas

**1. Use Explicit Types**
```java
// Bad
private Object data;

// Good
private String userName;
private Integer age;
```

**2. Required vs Optional Fields**
```java
public class OrderEvent {
    @NotNull
    private Long orderId;  // Required
    
    private String notes;  // Optional
}
```

**3. Include Metadata**
```java
public class OrderEvent {
    // Business data
    private Long orderId;
    private Double amount;
    
    // Metadata
    private String eventId;           // Unique event ID
    private String eventType;         // "OrderCreated"
    private LocalDateTime timestamp;  // When event occurred
    private String version;           // Schema version
}
```

**4. Keep Events Small**
```
Don't include entire order object
Include only what consumers need

Good: { orderId, userId, amount }
Bad: { orderId, userId, amount, allLineItems, customerFullProfile, ... }
```

**5. Use Meaningful Names**
```java
// Bad
private Long id;
private Double amt;

// Good
private Long orderId;
private Double totalAmount;
```

---

## Idempotency

### What is Idempotency?

**Definition:** Performing the same operation multiple times produces the same result.

**Theory:**
In distributed systems, messages can be delivered multiple times. Your consumer should handle this safely.

**Mathematical Example:**
```
Idempotent:
SET balance = 100
Call 1: balance becomes 100
Call 2: balance stays 100
Call 3: balance stays 100
Result: Always 100 (safe)

Non-Idempotent:
ADD 100 to balance
Call 1: balance = 0 + 100 = 100
Call 2: balance = 100 + 100 = 200
Call 3: balance = 200 + 100 = 300
Result: Different each time (dangerous!)
```

---

### Why Idempotency Matters

**Duplicate Messages Scenario:**
```
1. Order Service sends "ProcessPayment" message
2. Payment Service receives message
3. Payment Service processes payment
4. Payment Service crashes before acknowledging
5. Message broker resends message (thinks it wasn't processed)
6. Payment Service processes AGAIN
7. Customer charged twice! 💸💸

Without idempotency: Customer charged twice
With idempotency: Second processing is safe, no double charge
```

---

### Implementing Idempotency

**Strategy 1: Unique Message ID**

```java
@Service
public class PaymentService {
    
    @Autowired
    private ProcessedMessageRepository processedRepo;
    
    @KafkaListener(topics = "payment-requests")
    public void processPayment(PaymentEvent event) {
        String messageId = event.getMessageId();
        
        // Check if already processed
        if (processedRepo.existsById(messageId)) {
            System.out.println("Message already processed, skipping");
            return;  // Idempotent: safe to skip
        }
        
        // Process payment
        chargeCustomer(event.getAmount());
        
        // Record that we processed this message
        processedRepo.save(new ProcessedMessage(messageId));
    }
}
```

**ProcessedMessage Entity:**
```java
@Entity
public class ProcessedMessage {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
    
    // Constructor, getters, setters
}
```

---

**Strategy 2: Business ID (Natural Idempotency)**

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepo;
    
    @KafkaListener(topics = "order-events")
    public void createOrder(OrderEvent event) {
        // Use orderId from event
        Long orderId = event.getOrderId();
        
        // Check if order already exists
        if (orderRepo.existsById(orderId)) {
            System.out.println("Order already created");
            return;  // Safe to skip
        }
        
        // Create order
        Order order = new Order(orderId, event.getUserId(), event.getAmount());
        orderRepo.save(order);
    }
}
```

---

**Strategy 3: Update Operations (Naturally Idempotent)**

```java
@Service
public class UserService {
    
    @KafkaListener(topics = "user-updates")
    public void updateUserEmail(UserUpdateEvent event) {
        // UPDATE is naturally idempotent
        // Running multiple times produces same result
        
        User user = userRepo.findById(event.getUserId());
        user.setEmail(event.getNewEmail());
        userRepo.save(user);
        
        // Safe to call multiple times
        // Email will be set to same value each time
    }
}
```

---

**Strategy 4: Database Constraints**

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "orderId"))
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(unique = true)  // Database enforces uniqueness
    private String orderId;
    
    // Other fields
}

@Service
public class OrderService {
    
    @KafkaListener(topics = "orders")
    public void createOrder(OrderEvent event) {
        try {
            Order order = new Order(event.getOrderId(), event.getAmount());
            orderRepo.save(order);
            
        } catch (DataIntegrityViolationException e) {
            // Duplicate orderId - already processed
            System.out.println("Order already exists, skipping");
            // Idempotent: exception is expected and handled
        }
    }
}
```

---

### Idempotent vs Non-Idempotent Examples

**Idempotent Operations:**
```java
// SET operations
user.setName("John");           // ✓ Safe to repeat

// UPDATE with final value
UPDATE users SET status='ACTIVE' WHERE id=1;  // ✓ Safe

// DELETE
DELETE FROM orders WHERE id=123;  // ✓ Safe (second delete does nothing)

// PUT in REST
PUT /users/123 { name: "John" }   // ✓ Safe
```

**Non-Idempotent Operations:**
```java
// INCREMENT operations
balance = balance + 100;        // ✗ Dangerous to repeat

// APPEND operations
list.add(item);                 // ✗ Adds duplicate

// INSERT without check
INSERT INTO orders ...          // ✗ Creates duplicate

// POST in REST (usually)
POST /orders { ... }            // ✗ Creates duplicate order
```

---

### Best Practices for Idempotency

**1. Always Include Unique ID**
```java
public class OrderEvent {
    private String eventId;      // UUID.randomUUID()
    private Long orderId;
    private Double amount;
}
```

**2. Track Processed Messages**
```
Keep table of processed message IDs
Check before processing
```

**3. Use Database Transactions**
```java
@Transactional
public void processPayment(PaymentEvent event) {
    // Check and process in single transaction
    // Ensures atomic operation
}
```

**4. Design for Idempotency**
```
Prefer:
- SET over ADD
- UPDATE over INSERT
- Final state over delta changes
```

---

## Delivery Guarantees

### What are Delivery Guarantees?

**Definition:** How message brokers ensure messages are delivered to consumers.

**Three Levels:**
1. At-most-once
2. At-least-once
3. Exactly-once

---

### At-Most-Once Delivery

**Guarantee:** Message delivered **zero or one time** (might be lost).

**Theory:**
Send message and forget. Don't retry if fails.

**Example:**
```
Producer → Message Broker → Consumer
           ↓ (network fails)
           ✗ Message lost

Producer doesn't retry
Consumer never receives message
Message delivered: 0 times
```

**When to Use:**
- Logging (losing few logs acceptable)
- Metrics (missing few data points OK)
- Non-critical notifications

**Trade-off:** Fast, low overhead, but data loss possible.

**Implementation:**
```java
// Kafka
properties.put("acks", "0");  // Don't wait for acknowledgment

// RabbitMQ
rabbitTemplate.convertAndSend(exchange, routingKey, message);
// No confirmation
```

---

### At-Least-Once Delivery

**Guarantee:** Message delivered **one or more times** (might duplicate).

**Theory:**
Retry until confirmed received. Might send duplicates if confirmation lost.

**Example:**
```
Scenario 1 (Success):
Producer → Message → Consumer → Ack → Producer
Message delivered: 1 time ✓

Scenario 2 (Ack lost):
Producer → Message → Consumer (processes)
Consumer → Ack → ✗ (ack lost)
Producer → Message again → Consumer (processes again)
Message delivered: 2 times

Consumer must handle duplicates!
```

**When to Use:**
- Most common choice
- Payment processing (with idempotency)
- Order creation (with idempotency)
- Email sending (duplicate email OK)

**Trade-off:** No data loss, but must handle duplicates.

**Implementation:**
```java
// Kafka (default)
properties.put("acks", "all");  // Wait for confirmation
properties.put("retries", 3);   // Retry on failure

// Consumer must be idempotent
@KafkaListener(topics = "orders")
public void handleOrder(OrderEvent event) {
    // Check if already processed (idempotency)
    if (!alreadyProcessed(event.getId())) {
        processOrder(event);
        markAsProcessed(event.getId());
    }
}
```

---

### Exactly-Once Delivery

**Guarantee:** Message delivered **exactly one time** (no loss, no duplicates).

**Theory:**
Combination of idempotent producers and transactional writes.

**Example:**
```
Producer → Message → Consumer
           (retries if needed)
Consumer processes exactly once
No duplicates, no loss
```

**When to Use:**
- Financial transactions (critical)
- Inventory management (must be accurate)
- Billing systems

**Trade-off:** Complex, slower, but perfectly accurate.

**Implementation in Kafka:**
```java
// Producer Configuration
properties.put("enable.idempotence", "true");
properties.put("transactional.id", "order-producer-1");

// Producer Code
KafkaProducer<String, OrderEvent> producer = new KafkaProducer<>(properties);

producer.initTransactions();

try {
    producer.beginTransaction();
    producer.send(new ProducerRecord<>("orders", orderEvent));
    producer.commitTransaction();
    
} catch (Exception e) {
    producer.abortTransaction();
}
```

**Consumer Configuration:**
```java
properties.put("isolation.level", "read_committed");
// Only read messages from committed transactions
```

---

### Comparison Table

| Guarantee | Delivery Count | Data Loss | Duplicates | Complexity | Speed |
|-----------|---------------|-----------|------------|------------|-------|
| At-most-once | 0 or 1 | Possible | No | Low | Fast |
| At-least-once | 1 or more | No | Possible | Medium | Medium |
| Exactly-once | Exactly 1 | No | No | High | Slow |

---

### Real-World Examples

**At-Most-Once:**
```java
// Logging Service
@Service
public class LoggingService {
    
    @KafkaListener(topics = "application-logs")
    public void logMessage(LogEvent event) {
        // Just log it
        // If lost, not critical
        logger.info(event.getMessage());
    }
}
```

**At-Least-Once (with Idempotency):**
```java
// Payment Service
@Service
public class PaymentService {
    
    @KafkaListener(topics = "payment-requests")
    public void processPayment(PaymentEvent event) {
        // Idempotent check
        if (paymentRepo.existsByTransactionId(event.getTransactionId())) {
            return;  // Already processed
        }
        
        // Process payment
        Payment payment = new Payment(event.getTransactionId(), event.getAmount());
        paymentRepo.save(payment);
        
        // Safe even if message delivered twice
    }
}
```

**Exactly-Once:**
```java
// Critical Transaction Service
@Service
public class TransactionService {
    
    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    
    @Transactional
    public void createTransaction(TransactionRequest request) {
        // Execute in Kafka transaction
        kafkaTemplate.executeInTransaction(operations -> {
            // Save to database
            Transaction txn = new Transaction(request);
            transactionRepo.save(txn);
            
            // Send event
            TransactionEvent event = new TransactionEvent(txn);
            operations.send("transactions", event);
            
            return true;
        });
        
        // Both database save and Kafka send succeed or both fail
        // No partial state
    }
}
```

---

### Choosing the Right Guarantee

**Choose At-Most-Once If:**
- Data loss acceptable
- Speed critical
- High volume, low value data
- Examples: Metrics, logs, tracking

**Choose At-Least-Once If:**
- Cannot lose data
- Can handle duplicates
- Most common scenario
- Examples: Orders, emails, notifications

**Choose Exactly-Once If:**
- Cannot lose data
- Cannot have duplicates
- Critical accuracy needed
- Have resources for complexity
- Examples: Payments, billing, inventory

---

## Summary

### Key Takeaways

**Synchronous Communication:**
- **RestTemplate:** Simple, blocking, legacy
- **WebClient:** Modern, reactive, non-blocking
- **OpenFeign:** Declarative, clean, integrates with Spring Cloud
- Use when: Need immediate response

**Asynchronous Communication:**
- **Kafka:** High throughput, event streaming, message persistence
- **RabbitMQ:** Flexible routing, traditional messaging, task queues
- Use when: Don't need immediate response, want decoupling

**Event Schemas:**
- Define structure of messages
- Version carefully (backward compatibility)
- Include metadata (ID, timestamp, version)

**Idempotency:**
- Handle duplicate messages safely
- Use unique IDs to track processed messages
- Design operations to be idempotent

**Delivery Guarantees:**
- **At-most-once:** Fast, might lose data
- **At-least-once:** No loss, might duplicate (most common)
- **Exactly-once:** Perfect, complex, slower

---

### Decision Guide

**For Simple Request-Response:**
```
Use: OpenFeign (declarative)
Or: WebClient (if need reactive)
```

**For Background Processing:**
```
Low volume, complex routing: RabbitMQ
High volume, event streaming: Kafka
```

**For Critical Data:**
```
Use: At-least-once + Idempotency
Or: Exactly-once (if complexity acceptable)
```

**Remember:** Start simple, add complexity only when needed. Most applications work well with at-least-once delivery and proper idempotency handling.
