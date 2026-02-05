# API Gateway & Routing in Microservices

## Table of Contents
1. [Introduction to API Gateway](#introduction-to-api-gateway)
2. [Spring Cloud Gateway](#spring-cloud-gateway)
3. [Request Routing](#request-routing)
4. [Filters (Pre/Post)](#filters-prepost)
5. [Rate Limiting](#rate-limiting)
6. [Authentication at Gateway](#authentication-at-gateway)
7. [Circuit Breakers at Gateway](#circuit-breakers-at-gateway)

---

## Introduction to API Gateway

### What is an API Gateway?

**Definition:** Single entry point for all client requests to microservices.

**Simple Analogy:**
```
API Gateway = Hotel Reception Desk

Client (Guest) → Reception → Directs to correct room (service)

Without Reception:
Guest finds room themselves → Confusing, inefficient

With Reception:
Guest asks reception → Reception directs → Easy, organized
```

---

### Problem Without API Gateway

**Direct Client-to-Service Communication:**

```
Mobile App                    Microservices
    |                              |
    |----------- User Service      | (http://192.168.1.10:8081)
    |----------- Order Service     | (http://192.168.1.11:8082)
    |----------- Payment Service   | (http://192.168.1.12:8083)
    |----------- Product Service   | (http://192.168.1.13:8084)
```

**Problems:**
- ❌ Client must know all service URLs
- ❌ Client handles authentication for each service
- ❌ Client makes multiple requests (slow)
- ❌ No centralized logging/monitoring
- ❌ CORS issues (cross-origin requests)
- ❌ Security vulnerabilities (services exposed directly)
- ❌ If service moves, all clients must update

---

### Solution With API Gateway

```
Mobile App                API Gateway              Microservices
    |                         |                         |
    |                         |                         |
    |---- Single Request ---> | -----> User Service     |
                              | -----> Order Service    |
                              | -----> Payment Service  |
                              | -----> Product Service  |
```

**Benefits:**
- ✓ Single entry point (one URL for clients)
- ✓ Centralized authentication/authorization
- ✓ Request routing to correct service
- ✓ Load balancing across instances
- ✓ Rate limiting and throttling
- ✓ Request/response transformation
- ✓ Monitoring and logging
- ✓ Circuit breaking and fallbacks
- ✓ API versioning

---

### API Gateway Responsibilities

**1. Routing**
```
/api/users/* → User Service
/api/orders/* → Order Service
/api/products/* → Product Service
```

**2. Authentication & Authorization**
```
Check JWT token → Validate → Allow/Deny
```

**3. Load Balancing**
```
Request → Gateway → Distribute across 3 instances
```

**4. Rate Limiting**
```
User can make 100 requests/minute
Exceeded → Return 429 Too Many Requests
```

**5. Request/Response Transformation**
```
Add headers, modify body, aggregate responses
```

**6. Caching**
```
Cache frequent requests → Faster responses
```

**7. Monitoring & Logging**
```
Log all requests → Track performance → Detect issues
```

---

## Spring Cloud Gateway

### What is Spring Cloud Gateway?

**Definition:** Modern, non-blocking API Gateway built on Spring WebFlux.

**Theory:**
- Built on Spring Boot 3 and Spring WebFlux (reactive)
- Non-blocking I/O (handles many requests with few threads)
- Replaces older Zuul gateway
- Highly performant and scalable

**Key Features:**
- Route matching (path, headers, query params)
- Filters (pre and post processing)
- Integration with Spring Cloud ecosystem
- Built-in rate limiting
- Circuit breaker integration
- WebSocket support

---

### Setting Up Spring Cloud Gateway

**Step 1: Create Gateway Project**

**Dependencies (pom.xml):**
```xml
<dependencies>
    <!-- Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    
    <!-- Service Discovery (Optional) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```

**Step 2: Main Application Class**

```java
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**Step 3: Basic Configuration (application.yml):**

```yaml
server:
  port: 8080  # Gateway runs on port 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/users/**
        
        - id: order-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/orders/**
```

**What This Does:**
```
Client Request: GET http://localhost:8080/api/users/123

Gateway matches: Path=/api/users/**
Gateway routes to: http://localhost:8081/api/users/123
```

---

### Route Components Explained

**Route Configuration Structure:**
```yaml
routes:
  - id: unique-route-id          # Unique identifier
    uri: http://target-service   # Where to route
    predicates:                  # Matching conditions
      - Path=/api/users/**
    filters:                     # Pre/post processing
      - AddRequestHeader=X-Custom-Header, Value
```

**Components:**
1. **ID:** Unique name for the route
2. **URI:** Destination service URL
3. **Predicates:** Conditions to match (path, headers, etc.)
4. **Filters:** Modify request/response

---

## Request Routing

### Path-Based Routing

**Basic Path Matching:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Route 1: User Service
        - id: user-service-route
          uri: http://localhost:8081
          predicates:
            - Path=/api/users/**
        
        # Route 2: Order Service
        - id: order-service-route
          uri: http://localhost:8082
          predicates:
            - Path=/api/orders/**
        
        # Route 3: Product Service
        - id: product-service-route
          uri: http://localhost:8083
          predicates:
            - Path=/api/products/**
```

**Examples:**
```
Request: GET /api/users/123
→ Routes to: http://localhost:8081/api/users/123

Request: POST /api/orders
→ Routes to: http://localhost:8082/api/orders

Request: GET /api/products/456
→ Routes to: http://localhost:8083/api/products/456
```

---

### Path Rewriting

**Strip Path Prefix:**

```yaml
routes:
  - id: user-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/**
    filters:
      - StripPrefix=1  # Remove first path segment
```

**Example:**
```
Request: GET /api/users/123
After StripPrefix: GET /users/123
Routes to: http://localhost:8081/users/123
```

**Custom Rewrite:**
```yaml
routes:
  - id: user-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/v1/users/**
    filters:
      - RewritePath=/api/v1/users/(?<segment>.*), /users/${segment}
```

**Example:**
```
Request: GET /api/v1/users/123
After Rewrite: GET /users/123
Routes to: http://localhost:8081/users/123
```

---

### Service Discovery Integration

**Using Eureka for Dynamic Routing:**

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  # Auto-create routes from Eureka
          lower-case-service-id: true  # Use lowercase service names
```

**Auto-Generated Routes:**
```
If "user-service" registered in Eureka:
http://localhost:8080/user-service/** → Routed to user-service instances

Example:
GET /user-service/api/users/123
→ Gateway finds user-service in Eureka
→ Routes to one of user-service instances
→ Load balances automatically
```

**Manual Route with Service Discovery:**
```yaml
routes:
  - id: user-service
    uri: lb://user-service  # lb = load balanced, from Eureka
    predicates:
      - Path=/api/users/**
```

**How It Works:**
```
1. Request comes to gateway
2. Gateway checks Eureka: "Where is user-service?"
3. Eureka returns: [instance1, instance2, instance3]
4. Gateway picks one instance (load balancing)
5. Routes request to chosen instance
```

---

### Header-Based Routing

**Route Based on Headers:**

```yaml
routes:
  - id: mobile-route
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/**
      - Header=X-Client-Type, mobile
  
  - id: web-route
    uri: http://localhost:8082
    predicates:
      - Path=/api/users/**
      - Header=X-Client-Type, web
```

**Example:**
```
Request: GET /api/users/123
Header: X-Client-Type: mobile
→ Routes to: http://localhost:8081 (mobile service)

Request: GET /api/users/123
Header: X-Client-Type: web
→ Routes to: http://localhost:8082 (web service)
```

---

### Method-Based Routing

**Route Based on HTTP Method:**

```yaml
routes:
  - id: read-service
    uri: http://localhost:8081  # Read-only replica
    predicates:
      - Path=/api/users/**
      - Method=GET
  
  - id: write-service
    uri: http://localhost:8082  # Write-enabled primary
    predicates:
      - Path=/api/users/**
      - Method=POST,PUT,DELETE
```

---

### Query Parameter Routing

```yaml
routes:
  - id: premium-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/products/**
      - Query=tier, premium
  
  - id: standard-service
    uri: http://localhost:8082
    predicates:
      - Path=/api/products/**
```

**Example:**
```
Request: GET /api/products?tier=premium
→ Routes to: http://localhost:8081

Request: GET /api/products
→ Routes to: http://localhost:8082
```

---

### Weight-Based Routing (Canary Deployment)

**Route Percentage of Traffic:**

```yaml
routes:
  - id: order-service-v1
    uri: http://localhost:8081
    predicates:
      - Path=/api/orders/**
      - Weight=order-service, 90  # 90% traffic
  
  - id: order-service-v2
    uri: http://localhost:8082
    predicates:
      - Path=/api/orders/**
      - Weight=order-service, 10  # 10% traffic (new version)
```

**Use Case:**
```
Testing new version:
- 90% users get old version (stable)
- 10% users get new version (testing)
- Monitor metrics
- Gradually increase to 50/50, then 100% new version
```

---

## Filters (Pre/Post)

### What are Filters?

**Definition:** Components that modify requests before routing (pre-filters) or responses before returning to client (post-filters).

**Flow:**
```
Client → Pre-Filters → Route to Service → Service → Post-Filters → Client
```

---

### Built-in Gateway Filters

**1. AddRequestHeader**
```yaml
filters:
  - AddRequestHeader=X-Request-Id, ${random.uuid}
  - AddRequestHeader=X-Request-Time, ${T(java.time.Instant).now()}
```

**Example:**
```
Client sends: GET /api/users/123
Gateway adds headers:
  X-Request-Id: 550e8400-e29b-41d4-a716-446655440000
  X-Request-Time: 2024-02-04T10:30:00Z
Service receives request with added headers
```

---

**2. AddRequestParameter**
```yaml
filters:
  - AddRequestParameter=source, gateway
```

**Example:**
```
Client sends: GET /api/users/123
Gateway modifies to: GET /api/users/123?source=gateway
```

---

**3. AddResponseHeader**
```yaml
filters:
  - AddResponseHeader=X-Response-Time, ${T(java.time.Instant).now()}
  - AddResponseHeader=X-Powered-By, Spring Cloud Gateway
```

**Example:**
```
Service returns response
Gateway adds headers before sending to client
Client receives response with added headers
```

---

**4. RemoveRequestHeader / RemoveResponseHeader**
```yaml
filters:
  - RemoveRequestHeader=Cookie
  - RemoveResponseHeader=X-Internal-Info
```

**Use Case:**
```
Remove sensitive headers before routing
Remove internal headers before returning to client
```

---

**5. SetPath**
```yaml
filters:
  - SetPath=/users/{segment}
```

**Example:**
```
Request: GET /api/v1/users/123
After SetPath: GET /users/123
```

---

**6. SetStatus**
```yaml
filters:
  - SetStatus=404
```

**Use Case:**
```
Return custom status code
```

---

**7. Retry**
```yaml
filters:
  - name: Retry
    args:
      retries: 3
      statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
      methods: GET,POST
      backoff:
        firstBackoff: 100ms
        maxBackoff: 500ms
```

**Behavior:**
```
Request fails with 502 or 503
Gateway retries 3 times
Wait 100ms, then 200ms, then 400ms between retries
```

---

**8. RequestRateLimiter (covered in Rate Limiting section)**

---

### Custom Global Filters

**Create Custom Filter for All Routes:**

```java
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomGlobalFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // PRE-FILTER: Before routing
        ServerHttpRequest request = exchange.getRequest();
        logger.info("Request Path: {}", request.getPath());
        logger.info("Request Method: {}", request.getMethod());
        logger.info("Request Headers: {}", request.getHeaders());
        
        // Continue to next filter
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // POST-FILTER: After receiving response
            ServerHttpResponse response = exchange.getResponse();
            logger.info("Response Status: {}", response.getStatusCode());
        }));
    }
    
    @Override
    public int getOrder() {
        return -1;  // Higher priority (executes first)
    }
}
```

**Order of Execution:**
```
Lower number = Higher priority = Executes first

Order -1: Custom logging filter
Order 0: Authentication filter
Order 1: Rate limiting filter
```

---

### Custom Gateway Filter Factory

**Reusable Filter for Specific Routes:**

```java
@Component
public class CustomHeaderGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<CustomHeaderGatewayFilterFactory.Config> {
    
    public CustomHeaderGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Add custom header
            ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(config.getHeaderName(), config.getHeaderValue())
                .build();
            
            return chain.filter(exchange.mutate().request(request).build());
        };
    }
    
    public static class Config {
        private String headerName;
        private String headerValue;
        
        // Getters and Setters
    }
}
```

**Use in Configuration:**
```yaml
routes:
  - id: user-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/**
    filters:
      - CustomHeader=X-Custom-Header, MyValue
```

---

### Request/Response Modification Example

**Add Request Timestamp:**

```java
@Component
public class RequestTimestampFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put("requestTime", System.currentTimeMillis());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long requestTime = exchange.getAttribute("requestTime");
            long duration = System.currentTimeMillis() - requestTime;
            
            exchange.getResponse().getHeaders()
                .add("X-Response-Time", duration + "ms");
        }));
    }
}
```

---

## Rate Limiting

### What is Rate Limiting?

**Definition:** Restrict number of requests a client can make in a time period.

**Why Needed:**
- Prevent abuse and DDoS attacks
- Ensure fair usage across clients
- Protect backend services from overload
- Monetization (different tiers, different limits)

**Example:**
```
Free tier: 100 requests/hour
Premium tier: 10,000 requests/hour
Exceeded → Return 429 Too Many Requests
```

---

### Setting Up Rate Limiting

**Step 1: Add Redis Dependency (for distributed rate limiting)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

**Step 2: Configure Redis (application.yml):**

```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

**Step 3: Configure Rate Limiter:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/users/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # Tokens per second
                redis-rate-limiter.burstCapacity: 20  # Max burst size
                redis-rate-limiter.requestedTokens: 1 # Tokens per request
```

**How It Works:**
```
Token Bucket Algorithm:

Bucket capacity: 20 tokens
Refill rate: 10 tokens/second

Request 1: 20 tokens → Take 1 → 19 left → Allow
Request 2: 19 tokens → Take 1 → 18 left → Allow
...
Request 21 (same second): 0 tokens → Deny → 429 Too Many Requests

After 1 second: Bucket refilled with 10 tokens
Requests allowed again
```

---

### User-Based Rate Limiting

**Limit Per User ID:**

```java
@Component
public class UserKeyResolver implements KeyResolver {
    
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        // Extract user ID from request (e.g., from JWT token)
        return Mono.just(
            exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id")
        );
    }
}
```

**Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/users/**
          filters:
            - name: RequestRateLimiter
              args:
                key-resolver: "#{@userKeyResolver}"  # Use custom resolver
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

**Result:**
```
User A: 100 requests/minute → Allowed
User B: 100 requests/minute → Allowed
User A tries 101st request → Denied (exceeded limit)
User B still has quota
```

---

### IP-Based Rate Limiting

```java
@Component
public class IpKeyResolver implements KeyResolver {
    
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(
            exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress()
        );
    }
}
```

**Result:**
```
Each IP address has its own limit
```

---

### Path-Based Rate Limiting

**Different Limits for Different Endpoints:**

```yaml
routes:
  # Stricter limit for expensive operations
  - id: search-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/search/**
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 5   # Only 5/second
          redis-rate-limiter.burstCapacity: 10
  
  # Relaxed limit for simple operations
  - id: user-profile
    uri: http://localhost:8081
    predicates:
      - Path=/api/users/profile
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 100  # 100/second
          redis-rate-limiter.burstCapacity: 200
```

---

### Custom Rate Limit Response

```java
@Component
public class CustomRateLimiterGatewayFilterFactory 
    extends RequestRateLimiterGatewayFilterFactory {
    
    public CustomRateLimiterGatewayFilterFactory(
        RateLimiter rateLimiter, KeyResolver resolver) {
        super(rateLimiter, resolver);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return super.apply(config).filter(exchange, chain)
                .onErrorResume(ex -> {
                    // Custom error response
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    
                    String errorMessage = "{ \"error\": \"Rate limit exceeded. Try again later.\" }";
                    DataBuffer buffer = response.bufferFactory()
                        .wrap(errorMessage.getBytes());
                    
                    return response.writeWith(Mono.just(buffer));
                });
        };
    }
}
```

---

### Tiered Rate Limiting

**Different Limits Based on User Tier:**

```java
@Component
public class TierBasedKeyResolver implements KeyResolver {
    
    @Autowired
    private UserService userService;
    
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        
        // Get user tier from database or cache
        String tier = userService.getUserTier(userId);  // "free", "premium", "enterprise"
        
        return Mono.just(userId + ":" + tier);
    }
}
```

**Configuration:**
```yaml
routes:
  - id: free-tier
    uri: http://localhost:8081
    predicates:
      - Path=/api/**
      - Header=X-User-Tier, free
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20
  
  - id: premium-tier
    uri: http://localhost:8081
    predicates:
      - Path=/api/**
      - Header=X-User-Tier, premium
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 100
          redis-rate-limiter.burstCapacity: 200
```

---

## Authentication at Gateway

### Why Authenticate at Gateway?

**Without Gateway Authentication:**
```
Client → Service 1 → Authenticate
Client → Service 2 → Authenticate
Client → Service 3 → Authenticate

Every service duplicates authentication logic
```

**With Gateway Authentication:**
```
Client → Gateway → Authenticate once
       ↓ (if valid)
       Service 1, 2, 3 (trust gateway)

Centralized authentication
Services simplified
```

---

### JWT Token Validation

**Step 1: Add Dependencies:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
</dependency>
```

**Step 2: Create JWT Validation Filter:**

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    private static final String SECRET_KEY = "your-secret-key-min-256-bits";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip authentication for public endpoints
        if (isPublicPath(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        
        // Get Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Validate JWT token
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            // Extract user info from token
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            
            // Add user info to request headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (JwtException e) {
            return onError(exchange, "Invalid JWT token");
        }
    }
    
    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/login") || 
               path.equals("/api/auth/register");
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        String errorJson = String.format("{\"error\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory()
            .wrap(errorJson.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -100;  // High priority - execute before other filters
    }
}
```

---

### Role-Based Access Control

**Restrict Access Based on Roles:**

```java
@Component
public class RoleAuthorizationFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        String role = request.getHeaders().getFirst("X-User-Role");
        
        // Admin endpoints
        if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            return onError(exchange, "Forbidden: Admin access required");
        }
        
        // Premium endpoints
        if (path.startsWith("/api/premium/") && 
            !("PREMIUM".equals(role) || "ADMIN".equals(role))) {
            return onError(exchange, "Forbidden: Premium access required");
        }
        
        return chain.filter(exchange);
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        
        String errorJson = String.format("{\"error\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory()
            .wrap(errorJson.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -99;  // After JWT validation, before routing
    }
}
```

---

### OAuth2 / OpenID Connect Integration

**Using Spring Security OAuth2:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

**Configuration:**
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
        provider:
          google:
            issuer-uri: https://accounts.google.com
```

**Security Configuration:**
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/public/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2Login()
            .and()
            .build();
    }
}
```

---

### API Key Authentication

**Simple API Key Validation:**

```java
@Component
public class ApiKeyAuthenticationFilter implements GlobalFilter, Ordered {
    
    private static final Set<String> VALID_API_KEYS = Set.of(
        "api-key-123456",
        "api-key-789012"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        
        if (apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
```

---

### Combining Multiple Authentication Methods

**Support Both JWT and API Key:**

```java
@Component
public class MultiAuthFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Try JWT first
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return validateJwt(exchange, chain, authHeader.substring(7));
        }
        
        // Try API Key
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        if (apiKey != null) {
            return validateApiKey(exchange, chain, apiKey);
        }
        
        // No valid authentication
        return onError(exchange, "Authentication required");
    }
    
    private Mono<Void> validateJwt(ServerWebExchange exchange, 
                                    GatewayFilterChain chain, String token) {
        // JWT validation logic
        return chain.filter(exchange);
    }
    
    private Mono<Void> validateApiKey(ServerWebExchange exchange, 
                                       GatewayFilterChain chain, String apiKey) {
        // API Key validation logic
        return chain.filter(exchange);
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
```

---

## Circuit Breakers at Gateway

### Why Circuit Breaker at Gateway?

**Problem:**
```
Service is down or slow
Gateway keeps sending requests
Gateway waits for timeout
Gateway resources exhausted
Gateway crashes
Entire system down!
```

**Solution:**
```
Circuit Breaker monitors failures
Too many failures → Circuit opens
Stop sending requests to failing service
Return fallback response immediately
Periodically test if service recovered
```

---

### Circuit Breaker States

```
CLOSED (Normal):
  ├─ Requests pass through
  ├─ Monitor failures
  └─ If failure rate > threshold → OPEN

OPEN (Service failing):
  ├─ Reject requests immediately
  ├─ Return fallback response
  ├─ Wait for timeout period
  └─ After timeout → HALF_OPEN

HALF_OPEN (Testing):
  ├─ Allow limited requests
  ├─ Test if service recovered
  └─ Success → CLOSED | Failure → OPEN
```

---

### Setting Up Circuit Breaker

**Step 1: Add Resilience4j Dependency:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

**Step 2: Configure Circuit Breaker:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: CircuitBreaker
              args:
                name: orderServiceCB
                fallbackUri: forward:/fallback/orders

resilience4j:
  circuitbreaker:
    instances:
      orderServiceCB:
        sliding-window-size: 10           # Track last 10 calls
        failure-rate-threshold: 50        # Open if 50% fail
        wait-duration-in-open-state: 10s  # Wait 10s before retry
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
```

**Step 3: Create Fallback Controller:**

```java
@RestController
public class FallbackController {
    
    @GetMapping("/fallback/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Order service is temporarily unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }
    
    @PostMapping("/fallback/orders")
    public ResponseEntity<Map<String, Object>> ordersPostFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Order service is temporarily unavailable");
        response.put("message", "Your order will be processed when service is restored");
        response.put("requestQueued", true);
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }
}
```

---

### How Circuit Breaker Works

**Normal Operation (CLOSED):**
```
Request 1 → Order Service → Success → Response
Request 2 → Order Service → Success → Response
Request 3 → Order Service → Success → Response

Circuit: CLOSED (0% failure rate)
```

**Service Starts Failing:**
```
Request 4 → Order Service → Timeout → Error
Request 5 → Order Service → Timeout → Error
Request 6 → Order Service → Success → Response
Request 7 → Order Service → Timeout → Error
Request 8 → Order Service → Timeout → Error
Request 9 → Order Service → Timeout → Error
Request 10 → Order Service → Timeout → Error

Last 10 calls: 6 failures, 4 success
Failure rate: 60% > 50% threshold
Circuit: OPEN
```

**Circuit Open:**
```
Request 11 → Circuit Breaker → Fallback (immediate)
Request 12 → Circuit Breaker → Fallback (immediate)
Request 13 → Circuit Breaker → Fallback (immediate)

No calls to Order Service
Fast fail with fallback
Wait 10 seconds
```

**Testing Recovery (HALF_OPEN):**
```
After 10 seconds:
Circuit: HALF_OPEN

Request 14 → Order Service → Success
Request 15 → Order Service → Success
Request 16 → Order Service → Success

3 successful calls → Service recovered
Circuit: CLOSED (back to normal)
```

---

### Circuit Breaker with Retry

**Retry Before Circuit Breaking:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**
          filters:
            - name: Retry
              args:
                retries: 3
                statuses: SERVICE_UNAVAILABLE
            - name: CircuitBreaker
              args:
                name: paymentServiceCB
                fallbackUri: forward:/fallback/payments

resilience4j:
  circuitbreaker:
    instances:
      paymentServiceCB:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

**Flow:**
```
1. Request fails
2. Retry 3 times
3. All retries fail
4. Circuit breaker counts as 1 failure
5. If failure rate > 50% → Circuit opens
```

---

### Custom Circuit Breaker Filter

**Advanced Circuit Breaker with Custom Logic:**

```java
@Component
public class CustomCircuitBreakerFilter implements GlobalFilter, Ordered {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public CustomCircuitBreakerFilter(CircuitBreakerRegistry registry) {
        this.circuitBreakerRegistry = registry;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = getServiceName(exchange);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker(serviceName);
        
        return Mono.defer(() -> {
            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                // Circuit is open - return cached response or fallback
                return getCachedResponse(exchange, serviceName);
            }
            
            // Circuit closed/half-open - try request
            return chain.filter(exchange)
                .doOnSuccess(v -> circuitBreaker.onSuccess(0, TimeUnit.SECONDS))
                .doOnError(e -> circuitBreaker.onError(0, TimeUnit.SECONDS, e));
        });
    }
    
    private String getServiceName(ServerWebExchange exchange) {
        // Extract service name from path
        String path = exchange.getRequest().getPath().toString();
        return path.split("/")[2];  // e.g., /api/orders → orders
    }
    
    private Mono<Void> getCachedResponse(ServerWebExchange exchange, String serviceName) {
        // Return cached response or default fallback
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        
        String fallbackJson = String.format(
            "{\"error\":\"%s service unavailable\",\"cached\":true}", 
            serviceName
        );
        
        DataBuffer buffer = response.bufferFactory()
            .wrap(fallbackJson.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -50;  // After auth, before routing
    }
}
```

---

### Circuit Breaker Metrics & Monitoring

**Expose Circuit Breaker Metrics:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,circuitbreakerevents
  health:
    circuitbreakers:
      enabled: true
```

**Access Metrics:**
```
GET /actuator/circuitbreakers
GET /actuator/circuitbreakerevents
GET /actuator/health
```

**Response Example:**
```json
{
  "circuitBreakers": {
    "orderServiceCB": {
      "state": "CLOSED",
      "failureRate": "10.0%",
      "slowCallRate": "5.0%",
      "bufferedCalls": 10,
      "failedCalls": 1,
      "slowCalls": 0
    }
  }
}
```

---

## Complete Gateway Example

**Putting It All Together:**

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  redis:
    host: localhost
    port: 6379
  
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      
      routes:
        # Public routes (no auth)
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
        
        # Protected user routes
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
            - name: CircuitBreaker
              args:
                name: userServiceCB
                fallbackUri: forward:/fallback/users
        
        # Order service with strict rate limit
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
            - name: Retry
              args:
                retries: 3
                statuses: SERVICE_UNAVAILABLE
            - name: CircuitBreaker
              args:
                name: orderServiceCB
                fallbackUri: forward:/fallback/orders
        
        # Admin routes (role-based)
        - id: admin-service
          uri: lb://admin-service
          predicates:
            - Path=/api/admin/**
          filters:
            - StripPrefix=1

resilience4j:
  circuitbreaker:
    instances:
      userServiceCB:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
      
      orderServiceCB:
        sliding-window-size: 20
        failure-rate-threshold: 60
        wait-duration-in-open-state: 30s

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

---

## Summary

### Key Takeaways

**API Gateway Benefits:**
- ✓ Single entry point for all clients
- ✓ Centralized authentication/authorization
- ✓ Request routing and load balancing
- ✓ Rate limiting and throttling
- ✓ Circuit breaking for resilience
- ✓ Request/response transformation
- ✓ Monitoring and logging

**Spring Cloud Gateway:**
- Modern, reactive, non-blocking
- Route based on path, headers, methods, etc.
- Pre/post filters for request processing
- Integration with Spring Cloud ecosystem

**Routing:**
- Path-based, header-based, method-based
- Service discovery integration (Eureka)
- Load balancing across instances
- Dynamic routing with weights

**Filters:**
- Pre-filters: Modify requests before routing
- Post-filters: Modify responses before returning
- Global filters: Apply to all routes
- Custom filters: Reusable components

**Rate Limiting:**
- Prevent abuse and overload
- User-based, IP-based, path-based
- Token bucket algorithm
- Tiered limits for different users

**Authentication:**
- JWT token validation
- Role-based access control
- OAuth2 / OpenID Connect
- API key authentication
- Centralized security

**Circuit Breaker:**
- Prevent cascading failures
- Fast fail with fallbacks
- Automatic recovery testing
- Three states: Closed, Open, Half-Open

---

### Best Practices

**1. Authentication at Gateway Only**
```
Gateway validates token
Downstream services trust gateway
Simpler service code
```

**2. Use Circuit Breakers**
```
Protect against cascading failures
Always provide fallbacks
Monitor circuit states
```

**3. Implement Rate Limiting**
```
Protect backend services
Different limits for different endpoints
User-based limits for fairness
```

**4. Enable Monitoring**
```
Log all requests
Track response times
Monitor circuit breaker states
Alert on anomalies
```

**5. Design Good Fallbacks**
```
Meaningful error messages
Cached responses when possible
Queue requests for retry
Don't just return generic errors
```

**6. Security First**
```
Validate all inputs at gateway
Remove sensitive headers
Use HTTPS in production
Rotate API keys regularly
```

**Remember:** API Gateway is critical infrastructure. It's the front door to your microservices. Make it secure, resilient, and performant!
