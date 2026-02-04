# Service Discovery & Configuration Management

## Table of Contents
1. [Introduction](#introduction)
2. [Spring Cloud Fundamentals](#spring-cloud-fundamentals)
3. [Service Discovery](#service-discovery)
   - [Why Service Discovery?](#why-service-discovery)
   - [Eureka Server & Client](#eureka-server--client)
   - [Consul](#consul)
4. [Centralized Configuration](#centralized-configuration)
   - [Spring Cloud Config Server](#spring-cloud-config-server)
   - [Config Client](#config-client)
5. [Dynamic Refresh of Configs](#dynamic-refresh-of-configs)
6. [Failover Strategies](#failover-strategies)

---

## Introduction

### What Problem Are We Solving?

In microservices, you have **many services running on different servers**. This creates challenges:

**Challenge 1: Finding Services**
```
Order Service needs to call Payment Service
Where is Payment Service?
- IP: 192.168.1.50? Or 192.168.1.51?
- Port: 8080? Or 8081?
- What if it moves?

Hardcoding = Breaks when service moves
```

**Challenge 2: Managing Configuration**
```
10 microservices, each needs:
- Database URL
- API keys
- Feature flags
- Business rules

Change database URL = Update 10 config files
Deploy 10 services
Too much work!
```

**Solutions:**
- **Service Discovery:** Services find each other automatically
- **Centralized Configuration:** One place for all configs

---

## Spring Cloud Fundamentals

### What is Spring Cloud?

**Definition:** Set of tools built on Spring Boot for building distributed systems and microservices.

**Theory:**
Spring Cloud provides ready-made solutions for common microservices patterns. You don't build from scratch.

**Core Components:**

```
Spring Cloud Stack:

┌─────────────────────────────────────┐
│   Spring Cloud Netflix (Eureka)    │ ← Service Discovery
├─────────────────────────────────────┤
│   Spring Cloud Config              │ ← Configuration Management
├─────────────────────────────────────┤
│   Spring Cloud Gateway             │ ← API Gateway
├─────────────────────────────────────┤
│   Spring Cloud Circuit Breaker     │ ← Resilience
├─────────────────────────────────────┤
│   Spring Cloud LoadBalancer        │ ← Client-side Load Balancing
└─────────────────────────────────────┘
         Built on Spring Boot
```

---

### Key Features

**1. Service Discovery (Eureka, Consul)**
Services register themselves and discover others.

**2. Configuration Management (Config Server)**
Centralized configuration for all services.

**3. Load Balancing (Spring Cloud LoadBalancer)**
Distribute requests across instances.

**4. API Gateway (Spring Cloud Gateway)**
Single entry point for clients.

**5. Circuit Breaker (Resilience4j)**
Handle service failures gracefully.

**6. Distributed Tracing (Sleuth + Zipkin)**
Track requests across services.

---

### Spring Cloud Dependencies

**Parent POM (Dependency Management):**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Why Needed:**
Manages versions of all Spring Cloud libraries to ensure compatibility.

---

## Service Discovery

### Why Service Discovery?

**Problem Without Service Discovery:**

```java
// Hardcoded URL in code
@Service
public class OrderService {
    
    public void createOrder() {
        String paymentServiceUrl = "http://192.168.1.50:8080";
        // Call payment service
    }
}
```

**Issues:**
- ❌ IP address changes → Code breaks
- ❌ Port changes → Code breaks
- ❌ Multiple instances → Which one to call?
- ❌ Instance goes down → Can't switch automatically
- ❌ New instance added → Can't use it automatically

---

**Solution With Service Discovery:**

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentServiceClient paymentClient;
    
    public void createOrder() {
        // Just use service name, discovery handles the rest
        paymentClient.processPayment();
    }
}

@FeignClient(name = "payment-service")  // Logical name
interface PaymentServiceClient {
    @PostMapping("/payments")
    PaymentResponse processPayment();
}
```

**Benefits:**
- ✓ No hardcoded URLs
- ✓ Automatic failover
- ✓ Load balancing
- ✓ Dynamic scaling

---

### How Service Discovery Works

**Flow:**

```
1. Service Startup:
   Payment Service starts → Registers with Discovery Server
   "Hi, I'm payment-service at 192.168.1.50:8080"

2. Service Registry:
   Discovery Server stores:
   - payment-service: [192.168.1.50:8080, 192.168.1.51:8080]
   - order-service: [192.168.1.60:8081]
   - user-service: [192.168.1.70:8082]

3. Service Discovery:
   Order Service: "Where is payment-service?"
   Discovery Server: "payment-service is at 192.168.1.50:8080 and 192.168.1.51:8080"

4. Service Call:
   Order Service → Calls one instance
   If fails → Automatically tries other instance

5. Health Checks:
   Discovery Server: "Are you alive?"
   Payment Service: "Yes, I'm healthy"
   (Repeated every 30 seconds)
   
6. Deregistration:
   If service doesn't respond → Removed from registry
```

---

### Two Types of Service Discovery

**1. Client-Side Discovery**
```
Client                     Service Registry              Service Instances
  |                              |                              |
  |------ "Where is X?" -------->|                              |
  |<----- "X is at 1.2.3.4" -----|                              |
  |                              |                              |
  |---------------------- Call service ----------------------->|

Client chooses which instance to call
Client does load balancing
```

**Example:** Eureka with Spring Cloud LoadBalancer

**2. Server-Side Discovery**
```
Client              Load Balancer         Service Registry       Service Instances
  |                       |                       |                      |
  |---- Call service ---->|                       |                      |
  |                       |--- "Where is X?" ---->|                      |
  |                       |<-- "X is at 1.2.3.4"--|                      |
  |                       |                       |                      |
  |                       |----------- Call service ------------------>|

Load balancer chooses instance
Load balancer does load balancing
```

**Example:** Consul with NGINX, Kubernetes

---

## Eureka Server & Client

### What is Eureka?

**Definition:** Service registry from Netflix, part of Spring Cloud Netflix.

**Theory:**
Eureka Server = Phone book for microservices
Services register their location, other services look them up.

---

### Setting Up Eureka Server

**Step 1: Create New Spring Boot Project**

**Dependencies (pom.xml):**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>
```

**Step 2: Enable Eureka Server**

```java
@SpringBootApplication
@EnableEurekaServer  // This annotation makes it Eureka Server
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**Step 3: Configure (application.yml):**

```yaml
server:
  port: 8761  # Default Eureka port

eureka:
  client:
    register-with-eureka: false  # Don't register itself
    fetch-registry: false        # Don't fetch registry
  server:
    enable-self-preservation: false  # Disable in dev (enable in prod)
```

**Step 4: Run and Access**
```
Start application
Open browser: http://localhost:8761
See Eureka Dashboard (shows registered services)
```

---

### Setting Up Eureka Client (Service Registration)

**Example: Payment Service registers with Eureka**

**Step 1: Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Step 2: Enable Discovery Client**

```java
@SpringBootApplication
@EnableDiscoveryClient  // Enable service registration
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

**Step 3: Configure (application.yml):**

```yaml
spring:
  application:
    name: payment-service  # Service name in registry

server:
  port: 8080

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # Eureka Server URL
  instance:
    prefer-ip-address: true  # Register with IP instead of hostname
    lease-renewal-interval-in-seconds: 30  # Heartbeat interval
```

**What Happens:**
```
1. Payment Service starts on port 8080
2. Connects to Eureka Server at localhost:8761
3. Registers as "payment-service"
4. Sends heartbeat every 30 seconds
5. Visible in Eureka Dashboard
```

---

### Using Service Discovery (Calling Other Services)

**Order Service calls Payment Service through Eureka**

**Option 1: Using OpenFeign (Recommended)**

```xml
<!-- Add Feign dependency -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

// Feign Client - No URL needed!
@FeignClient(name = "payment-service")  // Uses service name from Eureka
public interface PaymentClient {
    
    @PostMapping("/api/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}

@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    public void createOrder(OrderRequest request) {
        // Call payment service
        PaymentResponse response = paymentClient.processPayment(
            new PaymentRequest(request.getAmount())
        );
        
        if (response.isSuccess()) {
            // Complete order
        }
    }
}
```

**How It Works:**
```
1. Order Service: "I need payment-service"
2. Feign: "Let me check Eureka..."
3. Eureka: "payment-service is at 192.168.1.50:8080"
4. Feign: Calls http://192.168.1.50:8080/api/payments
```

---

**Option 2: Using RestTemplate with LoadBalancer**

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced  // Enable load balancing with Eureka
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public void createOrder(OrderRequest request) {
        // Use service name instead of URL
        String url = "http://payment-service/api/payments";
        
        PaymentResponse response = restTemplate.postForObject(
            url, 
            new PaymentRequest(request.getAmount()),
            PaymentResponse.class
        );
    }
}
```

---

**Option 3: Using WebClient (Reactive)**

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced  // Enable load balancing
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

@Service
public class OrderService {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    public void createOrder(OrderRequest request) {
        PaymentResponse response = webClientBuilder.build()
            .post()
            .uri("http://payment-service/api/payments")  // Service name
            .bodyValue(new PaymentRequest(request.getAmount()))
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .block();
    }
}
```

---

### Multiple Instances & Load Balancing

**Register Multiple Instances:**

```
Start Payment Service:
Instance 1: Port 8080
Instance 2: Port 8081
Instance 3: Port 8082

All register as "payment-service" in Eureka
```

**Eureka Registry:**
```
payment-service:
  - 192.168.1.50:8080 (UP)
  - 192.168.1.50:8081 (UP)
  - 192.168.1.50:8082 (UP)
```

**Automatic Load Balancing:**
```
Order Service makes 6 calls:

Call 1 → Instance 1 (8080)
Call 2 → Instance 2 (8081)
Call 3 → Instance 3 (8082)
Call 4 → Instance 1 (8080)  // Round-robin
Call 5 → Instance 2 (8081)
Call 6 → Instance 3 (8082)

Feign automatically distributes load!
```

---

### Health Checks & Self-Preservation

**Heartbeat Mechanism:**
```
Every 30 seconds:
Payment Service → "I'm alive" → Eureka Server

If Eureka doesn't receive heartbeat for 90 seconds:
Eureka marks service as DOWN
Removes from available instances
```

**Self-Preservation Mode:**
```
If many services suddenly stop sending heartbeats:
Eureka thinks: "Maybe network issue, not all services down"
Eureka: Keeps services in registry (self-preservation)
Prevents mass deregistration during network issues

In development: Disable self-preservation
In production: Enable self-preservation
```

**Configuration:**
```yaml
eureka:
  server:
    enable-self-preservation: true  # Enable in production
    eviction-interval-timer-in-ms: 60000  # Check every 60 seconds
```

---

### Eureka Metadata

**Add Custom Metadata:**

```yaml
eureka:
  instance:
    metadata-map:
      zone: us-east-1
      version: 1.0.0
      environment: production
      team: payments
```

**Access Metadata:**
```java
@Service
public class ServiceInfoService {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    public void printServiceInfo() {
        List<ServiceInstance> instances = 
            discoveryClient.getInstances("payment-service");
        
        for (ServiceInstance instance : instances) {
            String zone = instance.getMetadata().get("zone");
            String version = instance.getMetadata().get("version");
            System.out.println("Zone: " + zone + ", Version: " + version);
        }
    }
}
```

---

### Eureka Advantages & Disadvantages

**Advantages:**
- ✓ Easy Spring Boot integration
- ✓ Built-in dashboard
- ✓ Client-side load balancing
- ✓ Self-preservation mode
- ✓ No external dependencies (pure Java)

**Disadvantages:**
- ✗ Only for JVM applications
- ✗ Eventual consistency (takes time to update)
- ✗ No built-in health checks (needs Spring Actuator)
- ✗ Netflix no longer actively develops (in maintenance mode)

---

## Consul

### What is Consul?

**Definition:** Service discovery and configuration tool from HashiCorp.

**Theory:**
Like Eureka but more powerful. Provides service discovery, health checking, key-value store, and multi-datacenter support.

**Key Differences from Eureka:**

| Feature | Eureka | Consul |
|---------|--------|--------|
| Language | Java only | Any language |
| Health Checks | Basic | Advanced (HTTP, TCP, Script) |
| Configuration | No | Yes (K/V store) |
| Multi-DC | No | Yes |
| DNS Interface | No | Yes |
| Consistency | AP (Available) | CP (Consistent) |

---

### Setting Up Consul

**Step 1: Install Consul**

```bash
# Download Consul binary
# Or using Docker:
docker run -d -p 8500:8500 consul agent -dev -ui -client=0.0.0.0

# Access UI: http://localhost:8500
```

**Step 2: Add Spring Cloud Consul Dependency**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```

---

### Registering Service with Consul

**Payment Service Configuration (application.yml):**

```yaml
spring:
  application:
    name: payment-service
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        register: true
        instance-id: ${spring.application.name}:${random.value}
        health-check-interval: 10s  # Health check every 10 seconds
        health-check-path: /actuator/health  # Health endpoint
        health-check-critical-timeout: 30s
```

**Enable Discovery:**
```java
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

**Add Health Endpoint (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**What Happens:**
```
1. Payment Service starts
2. Registers with Consul at localhost:8500
3. Consul checks /actuator/health every 10 seconds
4. If health check passes: Service marked as healthy
5. If health check fails 3 times: Service marked as unhealthy
6. Visible in Consul UI
```

---

### Using Consul for Service Discovery

**Same as Eureka - Use Feign:**

```java
@FeignClient(name = "payment-service")
public interface PaymentClient {
    @PostMapping("/api/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}
```

**Consul automatically provides:**
- Service location
- Load balancing
- Health-based routing (only calls healthy instances)

---

### Advanced Health Checks

**HTTP Health Check (Default):**
```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-path: /actuator/health
        health-check-interval: 10s
```

**TCP Health Check:**
```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-path: ""
        health-check-interval: 10s
```

**Custom Health Indicator:**
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            // Check database connection
            Connection conn = dataSource.getConnection();
            conn.close();
            return Health.up().build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

### Consul Key-Value Store

**Store Configuration:**

```java
@Service
public class ConfigService {
    
    @Autowired
    private ConsulClient consulClient;
    
    public void saveConfig(String key, String value) {
        consulClient.setKVValue(key, value);
    }
    
    public String getConfig(String key) {
        Response<GetValue> response = consulClient.getKVValue(key);
        return response.getValue().getDecodedValue();
    }
}
```

**Usage:**
```java
configService.saveConfig("feature.payment.enabled", "true");
String enabled = configService.getConfig("feature.payment.enabled");
```

---

### Consul Service Tags

**Add Tags to Service:**
```yaml
spring:
  cloud:
    consul:
      discovery:
        tags:
          - v1.0
          - payments
          - critical
```

**Filter by Tags:**
```java
@Service
public class ServiceDiscoveryService {
    
    @Autowired
    private ConsulDiscoveryClient discoveryClient;
    
    public List<ServiceInstance> getServicesWithTag(String tag) {
        return discoveryClient.getInstances("payment-service")
            .stream()
            .filter(instance -> instance.getMetadata().containsValue(tag))
            .collect(Collectors.toList());
    }
}
```

---

### When to Use Consul vs Eureka

**Use Eureka When:**
- Pure Java/Spring ecosystem
- Simple service discovery needed
- Don't need configuration management
- Single datacenter

**Use Consul When:**
- Multi-language environment (Java, Go, Python)
- Need advanced health checks
- Need configuration management
- Multi-datacenter deployment
- Need DNS-based discovery
- Want stronger consistency guarantees

---

## Centralized Configuration

### Why Centralized Configuration?

**Problem Without Centralized Config:**

```
10 microservices, each has application.yml:

Order Service:
  database.url: jdbc:mysql://db.example.com/orders
  api.key: abc123

Payment Service:
  database.url: jdbc:mysql://db.example.com/payments
  api.key: abc123

User Service:
  database.url: jdbc:mysql://db.example.com/users
  api.key: abc123

... 7 more services

Need to change api.key:
- Update 10 config files
- Rebuild 10 services
- Deploy 10 services
- Risky and time-consuming!
```

**Solution: Centralized Configuration**

```
Single Config Repository (Git):
├── application.yml (common config)
├── order-service.yml
├── payment-service.yml
└── user-service.yml

Change api.key in one place
Services fetch updated config
No rebuild needed!
```

---

## Spring Cloud Config Server

### What is Config Server?

**Definition:** Central server that stores and serves configuration to all microservices.

**Theory:**
- Configurations stored in Git repository (version controlled)
- Config Server reads from Git
- Microservices fetch config from Config Server
- Changes in Git = Changes in all services

**Architecture:**
```
Git Repository          Config Server         Microservices
(Configs stored)       (Serves configs)      (Consume configs)
     |                       |                      |
     |<----- Reads ----------|                      |
     |                       |<----- Fetches -------|
```

---

### Setting Up Config Server

**Step 1: Create Config Server Project**

**Dependencies (pom.xml):**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-server</artifactId>
    </dependency>
</dependencies>
```

**Step 2: Enable Config Server**

```java
@SpringBootApplication
@EnableConfigServer  // Makes this a Config Server
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**Step 3: Configure (application.yml):**

```yaml
server:
  port: 8888  # Config Server port

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/yourname/config-repo  # Git repository
          clone-on-start: true  # Clone on startup
          default-label: main   # Branch name
```

---

### Creating Configuration Repository

**Step 1: Create Git Repository**

```bash
mkdir config-repo
cd config-repo
git init
```

**Step 2: Create Configuration Files**

**application.yml (Common config for all services):**
```yaml
# Common configuration
logging:
  level:
    root: INFO

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

**order-service.yml (Specific to Order Service):**
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orders
    username: orderuser
    password: orderpass

app:
  feature:
    discount: true
  max-order-value: 10000
```

**payment-service.yml (Specific to Payment Service):**
```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payments
    username: paymentuser
    password: paymentpass

app:
  payment:
    gateway: stripe
    api-key: sk_test_123456
```

**Step 3: Commit and Push**

```bash
git add .
git commit -m "Initial configuration"
git push origin main
```

---

### Environment-Specific Configurations

**File Naming Convention:**
```
{application}-{profile}.yml

Examples:
- order-service-dev.yml     (Development)
- order-service-test.yml    (Testing)
- order-service-prod.yml    (Production)
```

**order-service-dev.yml:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orders_dev

app:
  feature:
    discount: true  # Enable in dev
```

**order-service-prod.yml:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db.example.com:3306/orders

app:
  feature:
    discount: false  # Disable in prod
```

---

## Config Client

### Setting Up Config Client (Microservice)

**Order Service connects to Config Server**

**Step 1: Add Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

**Step 2: Configure (application.yml):**
```yaml
spring:
  application:
    name: order-service  # Must match config file name
  profiles:
    active: dev  # Load order-service-dev.yml
  config:
    import: optional:configserver:http://localhost:8888  # Config Server URL
```

**Alternative (bootstrap.yml - older approach):**
```yaml
spring:
  application:
    name: order-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true  # Fail if can't connect to Config Server
```

**Step 3: Use Configuration in Code**

```java
@RestController
@RefreshScope  // Important: Allows dynamic refresh
public class OrderController {
    
    @Value("${app.max-order-value}")
    private int maxOrderValue;
    
    @Value("${app.feature.discount}")
    private boolean discountEnabled;
    
    @GetMapping("/order/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxOrderValue", maxOrderValue);
        config.put("discountEnabled", discountEnabled);
        return config;
    }
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        if (request.getAmount() > maxOrderValue) {
            throw new IllegalArgumentException("Order value too high");
        }
        
        double finalAmount = request.getAmount();
        if (discountEnabled) {
            finalAmount = finalAmount * 0.9;  // 10% discount
        }
        
        // Create order...
        return new Order(finalAmount);
    }
}
```

---

### How Config Loading Works

**Startup Sequence:**

```
1. Order Service starts
   ↓
2. Reads spring.application.name = "order-service"
   Reads spring.profiles.active = "dev"
   ↓
3. Connects to Config Server (http://localhost:8888)
   ↓
4. Requests: GET /order-service/dev
   ↓
5. Config Server reads from Git:
   - application.yml (common)
   - order-service.yml (app-specific)
   - order-service-dev.yml (profile-specific)
   ↓
6. Config Server merges configurations (profile overrides defaults)
   ↓
7. Returns merged config to Order Service
   ↓
8. Order Service uses config to start
```

---

### Configuration Priority (Highest to Lowest)

```
1. order-service-dev.yml     (Most specific)
2. order-service.yml         (App-specific)
3. application-dev.yml       (Profile common)
4. application.yml           (Least specific)

If same property in multiple files:
More specific wins!
```

**Example:**
```
application.yml:
  app.max-order-value: 5000

order-service.yml:
  app.max-order-value: 10000  (Overrides application.yml)

order-service-dev.yml:
  app.max-order-value: 50000  (Overrides both)

Result in dev: 50000
```

---

### Encrypting Sensitive Data

**Config Server Encryption**

**Step 1: Add Encryption Key (Config Server application.yml):**
```yaml
encrypt:
  key: MySecretEncryptionKey123  # Symmetric key
```

**Step 2: Encrypt Value:**
```bash
# Call Config Server encrypt endpoint
curl http://localhost:8888/encrypt -d "mySecretPassword"

# Returns: AQA7xU...encrypted...value
```

**Step 3: Store Encrypted Value:**

**order-service.yml:**
```yaml
spring:
  datasource:
    password: '{cipher}AQA7xU...encrypted...value'
```

**Config Server automatically decrypts when serving config!**

---

### Config Server Security

**Protect Config Server with Basic Auth:**

**Config Server (application.yml):**
```yaml
spring:
  security:
    user:
      name: configuser
      password: configpass
```

**Add Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Client Configuration:**
```yaml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      username: configuser
      password: configpass
```

---

## Dynamic Refresh of Configs

### Why Dynamic Refresh?

**Problem:**
```
Change config in Git → Services need to restart
100 services = 100 restarts
Downtime, slow, risky
```

**Solution:**
```
Change config in Git → Services reload config automatically
No restart needed!
```

---

### Implementing Dynamic Refresh

**Step 1: Add Actuator (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Step 2: Enable Refresh Endpoint (application.yml):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: refresh  # Expose /actuator/refresh endpoint
```

**Step 3: Use @RefreshScope:**

```java
@RestController
@RefreshScope  // Must have this annotation!
public class OrderController {
    
    @Value("${app.max-order-value}")
    private int maxOrderValue;  // Will be refreshed
    
    @Value("${app.feature.discount}")
    private boolean discountEnabled;  // Will be refreshed
    
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxOrderValue", maxOrderValue);
        config.put("discountEnabled", discountEnabled);
        return config;
    }
}
```

**Step 4: Refresh Configuration:**

```bash
# Change config in Git repository
git commit -m "Updated max-order-value"
git push

# Trigger refresh on each service
curl -X POST http://localhost:8081/actuator/refresh
```

**Result:**
```
Service reads new config without restart!
```

---

### Configuration Classes with Refresh

**Using @ConfigurationProperties:**

```java
@Component
@ConfigurationProperties(prefix = "app")
@RefreshScope  // Important!
public class AppConfig {
    
    private int maxOrderValue;
    private Feature feature;
    
    // Getters and Setters
    
    public static class Feature {
        private boolean discount;
        
        // Getter and Setter
    }
}

@RestController
public class OrderController {
    
    @Autowired
    private AppConfig appConfig;
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        if (request.getAmount() > appConfig.getMaxOrderValue()) {
            throw new IllegalArgumentException("Order too large");
        }
        
        double amount = request.getAmount();
        if (appConfig.getFeature().isDiscount()) {
            amount = amount * 0.9;
        }
        
        return new Order(amount);
    }
}
```

---

### Automatic Refresh with Spring Cloud Bus

**Problem:** Manually calling /actuator/refresh on each instance is tedious.

**Solution:** Spring Cloud Bus broadcasts refresh event to all instances.

**Architecture:**
```
Git Repo → Webhook → Config Server → Message Broker → All Services
                                            ↓
                                    (RabbitMQ/Kafka)
                                            ↓
                                      Broadcast Refresh
```

**Setup:**

**Step 1: Add Dependencies (all services):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

**Step 2: Configure RabbitMQ (application.yml):**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

management:
  endpoints:
    web:
      exposure:
        include: busrefresh  # Expose bus-refresh endpoint
```

**Step 3: Trigger Broadcast Refresh:**
```bash
# Change config in Git
git commit -m "Updated config"
git push

# Trigger refresh on ANY ONE instance
curl -X POST http://localhost:8081/actuator/busrefresh

# ALL instances receive refresh event and reload config!
```

---

### Git Webhook for Auto-Refresh

**Fully Automated Refresh:**

**Step 1: Configure Webhook in Git (GitHub example):**
```
Repository Settings → Webhooks → Add Webhook
Payload URL: http://your-config-server:8888/monitor
Content Type: application/json
Events: Just push event
```

**Step 2: Add Monitor Dependency (Config Server):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-monitor</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

**Flow:**
```
1. Developer commits config change to Git
2. Git sends webhook to Config Server
3. Config Server receives webhook
4. Config Server sends refresh event to Message Broker
5. All services receive event and refresh config
6. Zero manual intervention!
```

---

### What Gets Refreshed vs What Doesn't

**Refreshable (with @RefreshScope):**
- ✓ @Value annotated fields
- ✓ @ConfigurationProperties beans
- ✓ Configuration class properties

**NOT Refreshable:**
- ✗ server.port (requires restart)
- ✗ spring.datasource.url (requires restart)
- ✗ @Bean definitions (requires restart)
- ✗ Component scan paths (requires restart)

**Workaround for Database URL:**
Use dynamic datasource that can be reconfigured without restart (advanced topic).

---

## Failover Strategies

### What is Failover?

**Definition:** Automatically switching to a backup when primary system fails.

**Goal:** High availability - system keeps running even when components fail.

---

### Strategy 1: Multiple Config Server Instances

**Problem:**
```
Config Server down → All services can't start or refresh config
Single point of failure!
```

**Solution:**
```
Run multiple Config Server instances

Config Server 1: localhost:8888
Config Server 2: localhost:8889
Config Server 3: localhost:8890
```

**Client Configuration:**
```yaml
spring:
  cloud:
    config:
      uri: http://localhost:8888,http://localhost:8889,http://localhost:8890
      fail-fast: false  # Try next if first fails
```

**Behavior:**
```
1. Try Config Server 1
2. If fails, try Config Server 2
3. If fails, try Config Server 3
4. If all fail, use cached config or start with defaults
```

---

### Strategy 2: Config Server with Eureka

**Register Config Server in Eureka:**

**Config Server (application.yml):**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Client Configuration:**
```yaml
spring:
  cloud:
    config:
      discovery:
        enabled: true
        service-id: config-server  # Find via Eureka
```

**Benefits:**
```
- Multiple Config Server instances auto-discovered
- Automatic load balancing
- Automatic failover
- No hardcoded URLs
```

---

### Strategy 3: Local Config Fallback

**Use Local Config if Config Server Unavailable:**

```yaml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: false  # Don't fail if unreachable
  config:
    import: optional:configserver:http://localhost:8888
```

**Create Local application.yml:**
```yaml
# Fallback configuration used if Config Server down
app:
  max-order-value: 5000  # Default value
  feature:
    discount: false
```

**Behavior:**
```
Config Server available → Use remote config
Config Server down → Use local config
Service still starts!
```

---

### Strategy 4: Config Caching

**Cache Config Locally:**

When service fetches config from Config Server, save it locally. If Config Server fails later, use cached config.

**Implementation:**
```yaml
spring:
  cloud:
    config:
      fail-fast: false
      allow-override: true
```

**Config Server sends cache headers:**
```
Service fetches config → Saves to /tmp/config-cache
Config Server down → Service uses cached config
```

---

### Strategy 5: Service Discovery Failover

**Multiple Eureka Servers:**

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8762/eureka/
```

**Behavior:**
```
1. Connect to eureka1
2. If eureka1 down, connect to eureka2
3. Services still discover each other
```

---

### Strategy 6: Retry Pattern

**Retry Failed Requests:**

**Add Dependency:**
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

**Enable Retry:**
```java
@SpringBootApplication
@EnableRetry
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

**Use @Retryable:**
```java
@Service
public class PaymentService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @Retryable(
        value = {ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public PaymentResponse processPayment(PaymentRequest request) {
        // Try 3 times with 2 second delay between attempts
        return paymentClient.process(request);
    }
}
```

---

### Strategy 7: Circuit Breaker Pattern

**Prevent Cascading Failures:**

**Add Resilience4j (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

**Configuration (application.yml):**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        failure-rate-threshold: 50  # Open circuit if 50% fail
        wait-duration-in-open-state: 10000  # Wait 10s before retry
        sliding-window-size: 10  # Track last 10 calls
```

**Use Circuit Breaker:**
```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentClient paymentClient;
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        // Call payment service
        return paymentClient.process(request);
    }
    
    // Fallback method - called when circuit is open
    public PaymentResponse paymentFallback(PaymentRequest request, Exception e) {
        // Return cached response or default
        return new PaymentResponse("Payment pending - will retry later");
    }
}
```

**Circuit States:**
```
CLOSED → Normal operation, requests go through
    ↓ (50% failures)
OPEN → Stop calling service, return fallback immediately
    ↓ (wait 10 seconds)
HALF-OPEN → Try one request
    ↓ (success)
CLOSED → Resume normal operation
```

---

### Strategy 8: Bulkhead Pattern

**Isolate Resources:**

**Theory:** If one service fails, don't let it consume all threads and crash entire application.

**Configuration:**
```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10  # Max 10 concurrent calls
        max-wait-duration: 100ms  # Wait 100ms for slot
```

**Usage:**
```java
@Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}

public PaymentResponse bulkheadFallback(PaymentRequest request, BulkheadFullException e) {
    return new PaymentResponse("Service busy, try again later");
}
```

**Benefit:**
```
Payment Service slow/down:
- Only 10 threads allocated to it
- Other 90 threads handle other services
- Application stays responsive for other operations
```

---

### Strategy 9: Health Checks & Auto-Recovery

**Continuous Health Monitoring:**

```java
@Component
public class ServiceHealthCheck {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Scheduled(fixedRate = 30000)  // Every 30 seconds
    public void checkServiceHealth() {
        List<ServiceInstance> instances = 
            discoveryClient.getInstances("payment-service");
        
        for (ServiceInstance instance : instances) {
            // Check if instance is healthy
            // Remove unhealthy instances from load balancer
        }
    }
}
```

---

### Complete Failover Architecture Example

```
┌─────────────────────────────────────────────┐
│         Multiple Eureka Servers             │
│  Eureka 1 (8761)    Eureka 2 (8762)        │
└─────────────────────────────────────────────┘
              ↓ Service Discovery
┌─────────────────────────────────────────────┐
│       Multiple Config Servers                │
│  Config 1 (8888)    Config 2 (8889)         │
└─────────────────────────────────────────────┘
              ↓ Configuration
┌─────────────────────────────────────────────┐
│           Order Service Instances            │
│    Instance 1   Instance 2   Instance 3     │
│    (Circuit Breaker + Retry + Bulkhead)     │
└─────────────────────────────────────────────┘
              ↓ Calls with Resilience
┌─────────────────────────────────────────────┐
│         Payment Service Instances            │
│    Instance 1   Instance 2   Instance 3     │
│         (Auto health checks)                 │
└─────────────────────────────────────────────┘
```

---

## Summary

### Key Takeaways

**Service Discovery:**
- **Eureka:** Simple, Java-only, great for Spring Boot
- **Consul:** Advanced, multi-language, includes health checks
- Services register automatically and find each other
- Load balancing and failover built-in

**Configuration Management:**
- **Config Server:** Centralized configuration in Git
- Environment-specific configs (dev, test, prod)
- Encryption for sensitive data
- Version control for configuration changes

**Dynamic Refresh:**
- Use @RefreshScope for refreshable beans
- Manual refresh via /actuator/refresh
- Automatic refresh with Spring Cloud Bus
- Git webhooks for zero-touch updates

**Failover Strategies:**
- Multiple server instances
- Retry patterns
- Circuit breakers
- Bulkhead isolation
- Health checks and auto-recovery
- Config caching and fallbacks

---

### Best Practices

**1. Always Use Service Discovery**
```
Never hardcode URLs in code
Let discovery handle locations
```

**2. Externalize All Configuration**
```
No hardcoded values
Everything in Config Server
```

**3. Implement Multiple Layers of Resilience**
```
Retry + Circuit Breaker + Bulkhead
Defense in depth
```

**4. Monitor Health Continuously**
```
Use health checks
Remove unhealthy instances automatically
```

**5. Test Failure Scenarios**
```
What if Config Server is down?
What if Eureka is unavailable?
What if service doesn't respond?
```

**6. Use Profiles for Environments**
```
dev profile for development
prod profile for production
Different configs, same code
```

**Remember:** Distributed systems fail. Design for failure from the start. Multiple backups, automatic retries, graceful degradation.
