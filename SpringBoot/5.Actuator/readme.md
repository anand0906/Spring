# Spring Boot Actuator - Production Critical

> **Observability & Monitoring**: Every production service needs this

---

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started with Actuator](#getting-started-with-actuator)
3. [Core Actuator Endpoints](#core-actuator-endpoints)
4. [Custom Health Indicators](#custom-health-indicators)
5. [Readiness vs Liveness Probes](#readiness-vs-liveness-probes)
6. [Securing Actuator Endpoints](#securing-actuator-endpoints)
7. [Micrometer Integration](#micrometer-integration)
8. [Prometheus Metrics Exposure](#prometheus-metrics-exposure)
9. [Production Best Practices](#production-best-practices)

---

## Introduction

**Spring Boot Actuator** provides production-ready features for monitoring and managing your application.

**Why Actuator is Critical**:
- ✓ Monitor application health
- ✓ Gather metrics (CPU, memory, requests)
- ✓ Troubleshoot issues in production
- ✓ Integrate with monitoring tools (Prometheus, Grafana)
- ✓ Enable Kubernetes health checks
- ✓ Track application state

**Reality**: If you're not using Actuator in production, you're flying blind.

---

## Getting Started with Actuator

### Adding Actuator Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Basic Configuration

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # Expose specific endpoints
        # include: '*'  # Expose all (use cautiously!)
      base-path: /actuator  # Default base path
  
  endpoint:
    health:
      show-details: when-authorized  # Show health details
```

### Accessing Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics
```

### Available Endpoints

| Endpoint | Description | Example |
|----------|-------------|---------|
| `/health` | Application health status | Health checks |
| `/info` | Application information | Version, git info |
| `/metrics` | Application metrics | Memory, CPU, requests |
| `/env` | Environment properties | Config values |
| `/loggers` | Logger configuration | Change log levels |
| `/threaddump` | Thread dump | Debug threading |
| `/heapdump` | Heap dump | Memory analysis |
| `/prometheus` | Prometheus metrics | Monitoring |
| `/beans` | Spring beans | Dependency info |

---

## Core Actuator Endpoints

### /health Endpoint

**Basic Health Check**:
```bash
curl http://localhost:8080/actuator/health
```

**Response**:
```json
{
  "status": "UP"
}
```

**Detailed Health**:
```yaml
management:
  endpoint:
    health:
      show-details: always  # Show all details
```

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
        "total": 499963174912,
        "free": 216054312960,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### /info Endpoint

**Configuration**:
```yaml
info:
  app:
    name: My Application
    description: Production API Service
    version: 1.0.0
    encoding: UTF-8
  
  company:
    name: Tech Corp
    email: support@techcorp.com
```

**Response**:
```json
{
  "app": {
    "name": "My Application",
    "description": "Production API Service",
    "version": "1.0.0",
    "encoding": "UTF-8"
  },
  "company": {
    "name": "Tech Corp",
    "email": "support@techcorp.com"
  }
}
```

**Adding Git Information**:
```xml
<!-- Add plugin to pom.xml -->
<plugin>
    <groupId>pl.project13.maven</groupId>
    <artifactId>git-commit-id-plugin</artifactId>
</plugin>
```

```yaml
management:
  info:
    git:
      mode: full  # Show full git info
```

**Response with Git**:
```json
{
  "git": {
    "branch": "main",
    "commit": {
      "id": "abc123",
      "time": "2024-01-15T10:30:00Z"
    }
  }
}
```

### /metrics Endpoint

**List All Metrics**:
```bash
curl http://localhost:8080/actuator/metrics
```

```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "system.cpu.usage",
    "http.server.requests",
    "jdbc.connections.active",
    "jdbc.connections.idle"
  ]
}
```

**Get Specific Metric**:
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

```json
{
  "name": "jvm.memory.used",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 157286400
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    }
  ]
}
```

**Filter by Tags**:
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap
```

### /env Endpoint

**Configuration**:
```yaml
management:
  endpoint:
    env:
      show-values: when-authorized  # Hide sensitive values
```

**Response**:
```json
{
  "activeProfiles": ["prod"],
  "propertySources": [
    {
      "name": "applicationConfig",
      "properties": {
        "server.port": {
          "value": 8080
        },
        "spring.datasource.url": {
          "value": "******"  // Masked for security
        }
      }
    }
  ]
}
```

### /loggers Endpoint

**View Logger Levels**:
```bash
curl http://localhost:8080/actuator/loggers/com.example.myapp
```

```json
{
  "configuredLevel": "INFO",
  "effectiveLevel": "INFO"
}
```

**Change Logger Level at Runtime**:
```bash
curl -X POST \
  http://localhost:8080/actuator/loggers/com.example.myapp \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Custom Health Indicators

### Why Custom Health Indicators?

Built-in health checks aren't enough. You need to check:
- External APIs
- Database connectivity
- Message queues
- Cache servers
- Third-party services

### Creating a Custom Health Indicator

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            // Test database connection
            boolean valid = conn.isValid(2);  // 2 second timeout
            
            if (valid) {
                return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("validationQuery", "isValid()")
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Connection validation failed")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
```

### External API Health Check

```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String API_URL = "https://api.example.com/health";
    
    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                API_URL, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return Health.up()
                    .withDetail("api", "External API")
                    .withDetail("status", "reachable")
                    .withDetail("responseTime", "50ms")
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", response.getStatusCode())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("api", "External API")
                .build();
        }
    }
}
```

### Redis Health Check

```java
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("redis", "connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("redis", "no response")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Disk Space Health Check (Custom Threshold)

```java
@Component
public class DiskSpaceHealthIndicator implements HealthIndicator {
    
    private final File path = new File(".");
    private final long threshold = 10L * 1024 * 1024 * 1024;  // 10 GB
    
    @Override
    public Health health() {
        long freeSpace = path.getFreeSpace();
        long totalSpace = path.getTotalSpace();
        
        boolean sufficient = freeSpace >= threshold;
        
        Health.Builder builder = sufficient ? Health.up() : Health.down();
        
        return builder
            .withDetail("total", formatBytes(totalSpace))
            .withDetail("free", formatBytes(freeSpace))
            .withDetail("threshold", formatBytes(threshold))
            .withDetail("percentFree", 
                String.format("%.2f%%", (freeSpace * 100.0 / totalSpace)))
            .build();
    }
    
    private String formatBytes(long bytes) {
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
```

### Health Indicator with Caching

```java
@Component
public class CachedHealthIndicator implements HealthIndicator {
    
    private Health cachedHealth = Health.unknown().build();
    private Instant lastCheck = Instant.MIN;
    private final Duration cacheDuration = Duration.ofSeconds(30);
    
    @Override
    public Health health() {
        if (needsRefresh()) {
            cachedHealth = performHealthCheck();
            lastCheck = Instant.now();
        }
        return cachedHealth;
    }
    
    private boolean needsRefresh() {
        return Duration.between(lastCheck, Instant.now())
            .compareTo(cacheDuration) > 0;
    }
    
    private Health performHealthCheck() {
        // Expensive health check here
        try {
            Thread.sleep(1000);  // Simulate slow check
            return Health.up()
                .withDetail("lastCheck", lastCheck)
                .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down().build();
        }
    }
}
```

### Composite Health Indicator

```java
@Component
public class ApplicationHealthIndicator implements HealthIndicator {
    
    @Autowired
    private List<HealthIndicator> healthIndicators;
    
    @Override
    public Health health() {
        Map<String, Health> healths = new HashMap<>();
        Status overallStatus = Status.UP;
        
        for (HealthIndicator indicator : healthIndicators) {
            Health health = indicator.health();
            healths.put(indicator.getClass().getSimpleName(), health);
            
            if (health.getStatus() == Status.DOWN) {
                overallStatus = Status.DOWN;
            }
        }
        
        return Health.status(overallStatus)
            .withDetails(healths)
            .build();
    }
}
```

---

## Readiness vs Liveness Probes

### Understanding Probes

**Liveness Probe**: "Is the application running?"
- If fails → Restart the container

**Readiness Probe**: "Is the application ready to serve traffic?"
- If fails → Remove from load balancer

```
┌─────────────────────────────────────┐
│       Application Lifecycle         │
├─────────────────────────────────────┤
│  Starting → Ready → Running          │
│             ↑       ↑                │
│         Readiness  Liveness          │
└─────────────────────────────────────┘
```

### Enabling Probes

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true  # Enable liveness and readiness
      
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

### Accessing Probes

```bash
# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

**Liveness Response**:
```json
{
  "status": "UP"
}
```

**Readiness Response**:
```json
{
  "status": "UP"
}
```

### Custom Readiness State

```java
@Component
public class DatabaseReadinessCheck {
    
    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private DataSource dataSource;
    
    @EventListener(ApplicationReadyEvent.class)
    public void checkDatabaseOnStartup() {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                // Database not ready - mark app as not ready
                AvailabilityChangeEvent.publish(
                    context,
                    ReadinessState.REFUSING_TRAFFIC
                );
            } else {
                AvailabilityChangeEvent.publish(
                    context,
                    ReadinessState.ACCEPTING_TRAFFIC
                );
            }
        } catch (SQLException e) {
            AvailabilityChangeEvent.publish(
                context,
                ReadinessState.REFUSING_TRAFFIC
            );
        }
    }
}
```

### Custom Liveness Check

```java
@Component
public class ApplicationLivenessCheck {
    
    @Autowired
    private ApplicationContext context;
    
    private volatile boolean alive = true;
    
    @Scheduled(fixedRate = 60000)  // Check every minute
    public void checkApplicationHealth() {
        try {
            // Perform critical health checks
            if (isCriticalComponentsWorking()) {
                alive = true;
                AvailabilityChangeEvent.publish(
                    context,
                    LivenessState.CORRECT
                );
            } else {
                alive = false;
                AvailabilityChangeEvent.publish(
                    context,
                    LivenessState.BROKEN
                );
            }
        } catch (Exception e) {
            alive = false;
            AvailabilityChangeEvent.publish(
                context,
                LivenessState.BROKEN
            );
        }
    }
    
    private boolean isCriticalComponentsWorking() {
        // Check critical components
        return true;
    }
}
```

### Kubernetes Integration

```yaml
# kubernetes-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    spec:
      containers:
      - name: myapp
        image: myapp:latest
        ports:
        - containerPort: 8080
        
        # Liveness probe - restart if fails
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness probe - route traffic if ready
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
```

### Graceful Degradation

```java
@Component
public class GracefulDegradationManager {
    
    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private ExternalServiceClient externalService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void checkExternalDependencies() {
        // If external service is down, still mark as ready
        // but with degraded functionality
        try {
            externalService.ping();
        } catch (Exception e) {
            // Log but don't fail readiness
            System.out.println("External service unavailable - degraded mode");
        }
        
        // Always mark as ready
        AvailabilityChangeEvent.publish(
            context,
            ReadinessState.ACCEPTING_TRAFFIC
        );
    }
}
```

---

## Securing Actuator Endpoints

### Why Secure Actuator?

**Actuator exposes sensitive information**:
- ✗ Application configuration
- ✗ Environment variables
- ✗ Heap dumps
- ✗ Thread dumps
- ✗ Database credentials (in /env)

**Security is mandatory in production!**

### Basic Security with Spring Security

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```yaml
spring:
  security:
    user:
      name: admin
      password: ${ACTUATOR_PASSWORD}  # From environment

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  
  endpoint:
    health:
      show-details: when-authorized
```

### Custom Security Configuration

```java
@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {
    
    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/health/liveness").permitAll()
                .requestMatchers("/actuator/health/readiness").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                
                // Protected endpoints - require authentication
                .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
            )
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("actuator-admin")
            .password("{bcrypt}$2a$10$...")
            .roles("ACTUATOR_ADMIN")
            .build();
        
        return new InMemoryUserDetailsManager(admin);
    }
}
```

### Separate Port for Actuator

```yaml
# Run actuator on different port (internal only)
management:
  server:
    port: 9090  # Actuator runs on 9090
    address: 127.0.0.1  # Only accessible from localhost
  
server:
  port: 8080  # Application runs on 8080
```

**Firewall Rules**:
```bash
# Allow application port from internet
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT

# Allow actuator port only from internal network
iptables -A INPUT -p tcp --dport 9090 -s 10.0.0.0/8 -j ACCEPT
iptables -A INPUT -p tcp --dport 9090 -j DROP
```

### IP Whitelist

```java
@Configuration
public class ActuatorIpWhitelistConfig {
    
    @Bean
    public SecurityFilterChain ipWhitelist(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**")
                .access(new IpAddressAuthorizationManager("10.0.0.0/8"))
            );
        
        return http.build();
    }
}

class IpAddressAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    
    private final IpAddressMatcher ipMatcher;
    
    public IpAddressAuthorizationManager(String cidr) {
        this.ipMatcher = new IpAddressMatcher(cidr);
    }
    
    @Override
    public AuthorizationDecision check(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext context
    ) {
        HttpServletRequest request = context.getRequest();
        String remoteAddr = request.getRemoteAddr();
        
        boolean allowed = ipMatcher.matches(remoteAddr);
        return new AuthorizationDecision(allowed);
    }
}
```

### Environment Variable Masking

```yaml
management:
  endpoint:
    env:
      show-values: when-authorized  # Hide by default
      
      # Keys to always mask
      keys-to-sanitize:
        - password
        - secret
        - key
        - token
        - apikey
        - api-key
```

### Disable Dangerous Endpoints in Production

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        # Only expose safe endpoints
        include: health,info,metrics,prometheus
        
        # Never expose these in production!
        exclude: heapdump,threaddump,env,beans
```

---

## Micrometer Integration

### What is Micrometer?

**Micrometer** = Metrics facade (like SLF4J for metrics)
- Works with multiple monitoring systems
- Prometheus, Grafana, Datadog, New Relic, etc.

### Adding Micrometer

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

### Custom Metrics - Counter

```java
@Service
public class OrderService {
    
    private final Counter orderCounter;
    
    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "purchase")
            .register(registry);
    }
    
    public Order createOrder(OrderDTO orderDTO) {
        Order order = orderRepository.save(orderDTO.toEntity());
        
        // Increment counter
        orderCounter.increment();
        
        return order;
    }
}
```

### Custom Metrics - Gauge

```java
@Component
public class QueueMetrics {
    
    private final Queue<Task> taskQueue = new LinkedList<>();
    
    public QueueMetrics(MeterRegistry registry) {
        // Register gauge
        Gauge.builder("queue.size", taskQueue, Queue::size)
            .description("Current task queue size")
            .tag("queue", "tasks")
            .register(registry);
    }
    
    public void addTask(Task task) {
        taskQueue.add(task);
    }
}
```

### Custom Metrics - Timer

```java
@Service
public class PaymentService {
    
    private final Timer paymentTimer;
    
    public PaymentService(MeterRegistry registry) {
        this.paymentTimer = Timer.builder("payment.processing.time")
            .description("Payment processing time")
            .tag("method", "credit_card")
            .register(registry);
    }
    
    public PaymentResult processPayment(Payment payment) {
        return paymentTimer.record(() -> {
            // Process payment (timed)
            return gateway.process(payment);
        });
    }
}
```

### Custom Metrics - Distribution Summary

```java
@Service
public class OrderMetrics {
    
    private final DistributionSummary orderAmountSummary;
    
    public OrderMetrics(MeterRegistry registry) {
        this.orderAmountSummary = DistributionSummary
            .builder("order.amount")
            .description("Order amounts distribution")
            .baseUnit("USD")
            .register(registry);
    }
    
    public void recordOrder(Order order) {
        orderAmountSummary.record(order.getTotalAmount());
    }
}
```

### Using @Timed Annotation

```java
@Service
public class UserService {
    
    @Timed(
        value = "user.creation.time",
        description = "Time to create user",
        percentiles = {0.5, 0.95, 0.99}
    )
    public User createUser(UserDTO userDTO) {
        return userRepository.save(userDTO.toEntity());
    }
}
```

**Enable @Timed**:
```java
@Configuration
public class TimedConfiguration {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

### Custom Tags

```java
@Service
public class ApiMetrics {
    
    private final MeterRegistry registry;
    
    public ApiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }
    
    public void recordApiCall(String endpoint, String status, long duration) {
        Timer.builder("api.requests")
            .tag("endpoint", endpoint)
            .tag("status", status)
            .tag("method", "GET")
            .register(registry)
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

### Common Tags (Global)

```java
@Configuration
public class MicrometerConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "myapp",
                "environment", "production",
                "region", "us-east-1"
            );
    }
}
```

---

## Prometheus Metrics Exposure

### Adding Prometheus Support

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  
  metrics:
    export:
      prometheus:
        enabled: true
    
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:dev}
```

### Accessing Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

**Output**:
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Eden Space",} 1.57286e+08

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/api/users",} 42.0
http_server_requests_seconds_sum{method="GET",status="200",uri="/api/users",} 0.521
```

### Custom Prometheus Metrics

```java
@Component
public class BusinessMetrics {
    
    private final Counter salesCounter;
    private final Gauge activeUsersGauge;
    private final Timer orderProcessingTimer;
    
    public BusinessMetrics(MeterRegistry registry) {
        // Counter
        this.salesCounter = Counter.builder("business.sales.total")
            .description("Total sales count")
            .tag("department", "retail")
            .register(registry);
        
        // Gauge
        this.activeUsersGauge = Gauge.builder("business.users.active", 
                this::getActiveUserCount)
            .description("Current active users")
            .register(registry);
        
        // Timer
        this.orderProcessingTimer = Timer.builder("business.order.processing")
            .description("Order processing time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
    
    private int getActiveUserCount() {
        // Get from cache or database
        return 150;
    }
}
```

### Prometheus Configuration File

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'myapp'
          environment: 'production'
```

### Kubernetes Service Monitor

```yaml
# servicemonitor.yml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: myapp-metrics
  labels:
    app: myapp
spec:
  selector:
    matchLabels:
      app: myapp
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### Grafana Dashboard

**Example Queries**:
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Response time (95th percentile)
http_server_requests_seconds{quantile="0.95"}

# JVM memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# Active database connections
hikaricp_connections_active{pool="HikariPool"}
```

### Custom Dashboard Metrics

```java
@Component
public class DashboardMetrics {
    
    private final MeterRegistry registry;
    
    public DashboardMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Revenue metric
        Gauge.builder("dashboard.revenue.today", this::getTodayRevenue)
            .description("Today's revenue in USD")
            .baseUnit("USD")
            .register(registry);
        
        // Order fulfillment rate
        Gauge.builder("dashboard.orders.fulfillment.rate", 
                this::getFulfillmentRate)
            .description("Order fulfillment rate")
            .baseUnit("percent")
            .register(registry);
    }
    
    private double getTodayRevenue() {
        // Calculate from database
        return 15000.50;
    }
    
    private double getFulfillmentRate() {
        // Calculate percentage
        return 95.5;
    }
}
```

---

## Production Best Practices

### 1. Always Enable Health Checks

```yaml
# Minimum configuration for production
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
```

### 2. Secure All Endpoints

```java
// Separate security for actuator
@Configuration
public class ActuatorSecurity {
    
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().hasRole("ACTUATOR_ADMIN")
            )
            .httpBasic();
        return http.build();
    }
}
```

### 3. Use Separate Port

```yaml
# Actuator on management port
management:
  server:
    port: 9090
    address: 0.0.0.0  # Internal network only

server:
  port: 8080  # Public application port
```

### 4. Monitor Key Metrics

```java
@Component
public class ProductionMetrics {
    
    public ProductionMetrics(MeterRegistry registry) {
        // Database connection pool
        // JVM memory
        // HTTP request latency
        // Error rates
        // Business metrics
    }
}
```

### 5. Set Up Alerts

```yaml
# alerting-rules.yml
groups:
  - name: application_alerts
    rules:
    
    # High error rate
    - alert: HighErrorRate
      expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
      for: 5m
      annotations:
        summary: "High error rate detected"
    
    # High memory usage
    - alert: HighMemoryUsage
      expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
      for: 5m
      annotations:
        summary: "Memory usage above 90%"
    
    # Service down
    - alert: ServiceDown
      expr: up{job="spring-boot-app"} == 0
      for: 1m
      annotations:
        summary: "Service is down"
```

### 6. Implement Custom Health Checks

```java
// Check all critical dependencies
@Component
public class CriticalDependenciesHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Override
    public Health health() {
        // Check database
        if (!isDatabaseHealthy()) {
            return Health.down().withDetail("database", "unreachable").build();
        }
        
        // Check cache
        if (!isCacheHealthy()) {
            return Health.down().withDetail("cache", "unreachable").build();
        }
        
        return Health.up().build();
    }
}
```

### 7. Log Actuator Access

```java
@Component
public class ActuatorAccessLogger {
    
    @EventListener
    public void onActuatorAccess(AuditApplicationEvent event) {
        AuditEvent auditEvent = event.getAuditEvent();
        System.out.println("Actuator endpoint accessed: " + 
            auditEvent.getType() + " by " + auditEvent.getPrincipal());
    }
}
```

### 8. Version Your Metrics

```java
@Configuration
public class MetricVersioning {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> versionTags() {
        return registry -> registry.config()
            .commonTags(
                "version", "1.0.0",
                "build", "${BUILD_NUMBER}"
            );
    }
}
```

### 9. Document Available Metrics

```yaml
# Create metrics documentation
info:
  metrics:
    available:
      - name: "orders.created"
        description: "Total orders created"
        type: "counter"
      - name: "payment.processing.time"
        description: "Payment processing duration"
        type: "timer"
```

### 10. Monitor Actuator Performance

```java
@Aspect
@Component
public class ActuatorPerformanceMonitor {
    
    @Around("execution(* org.springframework.boot.actuate..*(..))")
    public Object monitorActuator(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > 1000) {
                System.out.println("Slow actuator endpoint: " + 
                    joinPoint.getSignature() + " took " + duration + "ms");
            }
        }
    }
}
```

---

## Complete Production Configuration

```yaml
# application-prod.yml
spring:
  application:
    name: myapp

# Actuator configuration
management:
  # Separate management port
  server:
    port: 9090
    address: 127.0.0.1  # Only localhost
  
  # Endpoints
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
        exclude: heapdump,threaddump,env
      base-path: /actuator
  
  # Health endpoint
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
      show-components: when-authorized
  
  # Health checks
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
    db:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10GB
  
  # Metrics
  metrics:
    export:
      prometheus:
        enabled: true
    
    distribution:
      percentiles-histogram:
        http.server.requests: true
    
    tags:
      application: ${spring.application.name}
      environment: production
      region: us-east-1
      version: ${VERSION:1.0.0}

# Application info
info:
  app:
    name: ${spring.application.name}
    version: ${VERSION:1.0.0}
    description: Production API Service
  
  company:
    name: Tech Corp
    email: ops@techcorp.com

# Security
spring:
  security:
    user:
      name: actuator-admin
      password: ${ACTUATOR_PASSWORD}
```

---

## Monitoring Architecture

```
┌─────────────────────────────────────────────────┐
│              Spring Boot App                     │
│  ┌──────────────────────────────────────┐       │
│  │     Micrometer Metrics               │       │
│  │  - Counters, Timers, Gauges          │       │
│  └──────────────┬───────────────────────┘       │
│                 │                                │
│  ┌──────────────▼───────────────────────┐       │
│  │   Actuator Endpoints                 │       │
│  │  - /health, /metrics, /prometheus    │       │
│  └──────────────┬───────────────────────┘       │
└─────────────────┼───────────────────────────────┘
                  │
                  │ HTTP
                  │
┌─────────────────▼───────────────────────────────┐
│              Prometheus                          │
│  - Scrapes metrics every 15s                    │
│  - Stores time-series data                      │
│  - Evaluates alerting rules                     │
└─────────────────┬───────────────────────────────┘
                  │
                  │
         ┌────────┴────────┐
         │                 │
┌────────▼────────┐  ┌────▼──────────┐
│    Grafana      │  │  AlertManager │
│  - Dashboards   │  │  - Alerts     │
│  - Visualize    │  │  - Notify     │
└─────────────────┘  └───────────────┘
```

---

## Summary

**Key Takeaways**:

1. **Actuator is Essential**: Every production app needs it for monitoring
2. **Health Checks**: Implement custom health indicators for dependencies
3. **Probes**: Use liveness/readiness for Kubernetes deployments
4. **Security**: Always secure actuator endpoints in production
5. **Metrics**: Use Micrometer for custom business metrics
6. **Prometheus**: Expose metrics for monitoring and alerting
7. **Separate Port**: Run actuator on management port (internal only)
8. **Monitoring**: Set up Grafana dashboards and alerts
9. **Documentation**: Document available metrics and endpoints
10. **Testing**: Test health checks and monitor metrics regularly

**Remember**: You can't manage what you can't measure. Actuator gives you visibility into your application! 📊🚀
