# Spring Boot Embedded Server & Web Stack Tuning

> **Performance & Scalability**: Master embedded servers and optimize your web stack

---

## Table of Contents
1. [Introduction](#introduction)
2. [Embedded Servers Overview](#embedded-servers-overview)
3. [Embedded Tomcat Configuration](#embedded-tomcat-configuration)
4. [Embedded Jetty Configuration](#embedded-jetty-configuration)
5. [Embedded Netty (WebFlux)](#embedded-netty-webflux)
6. [Thread Pools Configuration](#thread-pools-configuration)
7. [Connection Pools Tuning](#connection-pools-tuning)
8. [Server Tuning](#server-tuning)
9. [HTTP/2 Support](#http2-support)
10. [Graceful Shutdown](#graceful-shutdown)
11. [Servlet vs Reactive Stack](#servlet-vs-reactive-stack)
12. [Performance Best Practices](#performance-best-practices)

---

## Introduction

Spring Boot includes embedded servers, eliminating the need for separate server deployment. Understanding how to configure and tune these servers is crucial for production applications.

**Key Concept**: Spring Boot = Application + Embedded Server (all in one JAR!)

**Why Embedded Servers?**
- ✓ No external server installation needed
- ✓ Portable deployment (single JAR)
- ✓ Easy configuration
- ✓ Consistent across environments
- ✓ Simplified CI/CD

---

## Embedded Servers Overview

### Available Embedded Servers

Spring Boot supports three main embedded servers:

| Server | Stack | Best For | Protocol |
|--------|-------|----------|----------|
| **Tomcat** | Servlet (blocking) | Traditional apps | HTTP/1.1, HTTP/2 |
| **Jetty** | Servlet (blocking) | WebSocket apps | HTTP/1.1, HTTP/2 |
| **Netty** | Reactive (non-blocking) | High concurrency | HTTP/1.1, HTTP/2 |
| **Undertow** | Servlet or Reactive | Performance | HTTP/1.1, HTTP/2 |

### Default Server

```xml
<!-- Default: Tomcat -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Includes Tomcat by default -->
</dependency>
```

### Switching to Jetty

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- Exclude Tomcat -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Add Jetty -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

### Switching to Undertow

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Add Undertow -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### Using Netty (Reactive)

```xml
<!-- For reactive applications -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <!-- Includes Netty by default -->
</dependency>
```

---

## Embedded Tomcat Configuration

### Basic Configuration

```yaml
# application.yml
server:
  port: 8080
  
  tomcat:
    # Thread configuration
    threads:
      max: 200              # Maximum worker threads
      min-spare: 10         # Minimum spare threads
    
    # Connection configuration
    max-connections: 8192   # Maximum connections
    accept-count: 100       # Queue length for incoming connections
    
    # Keep-alive
    connection-timeout: 20000  # 20 seconds
    keep-alive-timeout: 60000  # 60 seconds
    max-keep-alive-requests: 100
```

### Advanced Tomcat Configuration

```java
@Configuration
public class TomcatConfig {
    
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> 
        tomcatCustomizer() {
        return factory -> {
            
            // Add custom connector
            factory.addConnectorCustomizers(connector -> {
                
                // Get the protocol handler
                Http11NioProtocol protocol = 
                    (Http11NioProtocol) connector.getProtocolHandler();
                
                // Configure thread pool
                protocol.setMaxThreads(200);
                protocol.setMinSpareThreads(10);
                
                // Configure connections
                protocol.setMaxConnections(8192);
                protocol.setAcceptCount(100);
                
                // Configure timeouts
                protocol.setConnectionTimeout(20000);
                protocol.setKeepAliveTimeout(60000);
                
                // Enable compression
                connector.setProperty("compression", "on");
                connector.setProperty("compressionMinSize", "1024");
                connector.setProperty("compressibleMimeType", 
                    "text/html,text/xml,text/plain,text/css,text/javascript,application/json");
            });
        };
    }
}
```

### Tomcat Access Logs

```yaml
server:
  tomcat:
    accesslog:
      enabled: true
      directory: logs
      prefix: access_log
      suffix: .log
      pattern: "%h %l %u %t \"%r\" %s %b %D"
      # %D = Time taken in milliseconds
```

### Custom Error Pages

```java
@Configuration
public class TomcatErrorConfig {
    
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> 
        errorPageCustomizer() {
        return factory -> {
            factory.addErrorPages(
                new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"),
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500")
            );
        };
    }
}
```

### Tomcat HTTPS Configuration

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
    
    # TLS version
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
```

---

## Embedded Jetty Configuration

### Basic Configuration

```yaml
server:
  port: 8080
  
  jetty:
    threads:
      max: 200
      min: 8
      idle-timeout: 60000
    
    max-http-form-post-size: 200000  # 200KB
    accesslog:
      enabled: true
```

### Advanced Jetty Configuration

```java
@Configuration
public class JettyConfig {
    
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> 
        jettyCustomizer() {
        return factory -> {
            
            factory.addServerCustomizers(server -> {
                
                // Configure thread pool
                QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
                threadPool.setMaxThreads(200);
                threadPool.setMinThreads(8);
                threadPool.setIdleTimeout(60000);
                
                // Configure HTTP configuration
                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.setSendServerVersion(false);
                httpConfig.setSendDateHeader(true);
                httpConfig.setOutputBufferSize(32768);
                httpConfig.setRequestHeaderSize(8192);
                httpConfig.setResponseHeaderSize(8192);
                
                // Add configuration to connector
                ServerConnector connector = new ServerConnector(server, 
                    new HttpConnectionFactory(httpConfig));
                connector.setPort(8080);
                connector.setIdleTimeout(30000);
                
                server.addConnector(connector);
            });
        };
    }
}
```

### Jetty WebSocket Configuration

```java
@Configuration
@EnableWebSocket
public class JettyWebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler(), "/ws")
                .setAllowedOrigins("*");
    }
    
    @Bean
    public WebSocketHandler myWebSocketHandler() {
        return new MyWebSocketHandler();
    }
}
```

### Jetty with Custom Handlers

```java
@Configuration
public class JettyHandlerConfig {
    
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> 
        handlerCustomizer() {
        return factory -> {
            factory.addServerCustomizers(server -> {
                
                // Add GZip handler
                GzipHandler gzipHandler = new GzipHandler();
                gzipHandler.setMinGzipSize(1024);
                gzipHandler.setIncludedMimeTypes(
                    "text/html", "text/plain", "text/css",
                    "application/javascript", "application/json"
                );
                
                server.insertHandler(gzipHandler);
            });
        };
    }
}
```

---

## Embedded Netty (WebFlux)

### Basic Configuration

```yaml
server:
  port: 8080
  
  netty:
    # Connection timeout
    connection-timeout: 20000
    
    # Idle timeout
    idle-timeout: 60000
```

### Advanced Netty Configuration

```java
@Configuration
public class NettyConfig {
    
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> 
        nettyCustomizer() {
        return factory -> {
            
            factory.addServerCustomizers(httpServer -> {
                
                // Configure connection pool
                return httpServer
                    // Max connections
                    .tcpConfiguration(tcpServer -> 
                        tcpServer.option(ChannelOption.SO_BACKLOG, 100)
                                 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20000)
                    )
                    // Keep-alive
                    .tcpConfiguration(tcpServer -> 
                        tcpServer.option(ChannelOption.SO_KEEPALIVE, true)
                    )
                    // Enable compression
                    .compress(true)
                    // Access logs
                    .accessLog(true);
            });
        };
    }
}
```

### Event Loop Configuration

```java
@Configuration
public class NettyEventLoopConfig {
    
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> 
        eventLoopCustomizer() {
        return factory -> {
            
            // Custom event loop group
            EventLoopGroup eventLoopGroup = 
                new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
            
            factory.addServerCustomizers(httpServer -> 
                httpServer.runOn(eventLoopGroup)
            );
        };
    }
}
```

### Netty with WebSockets

```java
@Configuration
public class NettyWebSocketConfig {
    
    @Bean
    public HandlerMapping webSocketMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws", new MyWebSocketHandler());
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
```

---

## Thread Pools Configuration

### Understanding Thread Pools

```
┌─────────────────────────────────────┐
│        Incoming Requests            │
└──────────────┬──────────────────────┘
               │
        ┌──────▼──────┐
        │ Accept Queue │  ← acceptCount
        │ (100 requests)│
        └──────┬───────┘
               │
        ┌──────▼─────────┐
        │  Thread Pool   │
        │  (200 threads) │  ← max threads
        │                │
        │  [T1][T2][T3]  │
        │  [T4][T5]...   │
        └────────────────┘
```

### Tomcat Thread Pool

```yaml
server:
  tomcat:
    threads:
      max: 200              # Maximum threads
      min-spare: 10         # Minimum idle threads
    
    accept-count: 100       # Queue size
    max-connections: 10000  # Max simultaneous connections
```

### Calculating Thread Pool Size

```java
/**
 * Formula: Number of threads = Number of CPU cores × (1 + Wait time / Service time)
 * 
 * For CPU-bound tasks: threads = cores
 * For I/O-bound tasks: threads = cores × (1 + wait/service)
 */

public class ThreadPoolCalculator {
    
    public static int calculateOptimalThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        
        // For I/O-bound operations (databases, REST calls)
        // Wait time ~= 90% of total time
        int ioThreads = cores * (1 + 9);  // 10x cores
        
        // For CPU-bound operations
        int cpuThreads = cores;
        
        // For mixed workload (typical web app)
        int mixedThreads = cores * 2;
        
        return mixedThreads;
    }
}
```

### Custom Thread Pool

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core threads (always alive)
        executor.setCorePoolSize(10);
        
        // Maximum threads
        executor.setMaxPoolSize(50);
        
        // Queue capacity
        executor.setQueueCapacity(100);
        
        // Thread name prefix
        executor.setThreadNamePrefix("async-");
        
        // Rejection policy
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        executor.initialize();
        return executor;
    }
}
```

### Async Request Processing

```java
@RestController
public class AsyncController {
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    @GetMapping("/async")
    public CompletableFuture<String> asyncEndpoint() {
        return CompletableFuture.supplyAsync(() -> {
            // Long-running task
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Completed!";
        }, taskExecutor);
    }
}
```

### Monitoring Thread Pool

```java
@Component
public class ThreadPoolMonitor {
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    @Scheduled(fixedRate = 30000)  // Every 30 seconds
    public void monitorThreadPool() {
        if (taskExecutor instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
            ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
            
            System.out.println("=== Thread Pool Stats ===");
            System.out.println("Active threads: " + pool.getActiveCount());
            System.out.println("Pool size: " + pool.getPoolSize());
            System.out.println("Queue size: " + pool.getQueue().size());
            System.out.println("Completed tasks: " + pool.getCompletedTaskCount());
        }
    }
}
```

---

## Connection Pools Tuning

### HikariCP (Default in Spring Boot)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
    
    hikari:
      # Connection pool size
      minimum-idle: 10          # Minimum idle connections
      maximum-pool-size: 20     # Maximum connections
      
      # Connection timeout
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000      # 10 minutes
      max-lifetime: 1800000     # 30 minutes
      
      # Pool name
      pool-name: MyHikariPool
      
      # Leak detection
      leak-detection-threshold: 60000  # 60 seconds
      
      # Connection test query
      connection-test-query: SELECT 1
      
      # Auto-commit
      auto-commit: true
```

### HikariCP Configuration Class

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // JDBC settings
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("secret");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool sizing
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(20);
        
        // Timeouts
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Connection testing
        config.setConnectionTestQuery("SELECT 1");
        
        // Pool behavior
        config.setAutoCommit(true);
        config.setPoolName("MyHikariPool");
        
        // Leak detection
        config.setLeakDetectionThreshold(60000);
        
        // Performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(config);
    }
}
```

### Connection Pool Sizing Formula

```java
/**
 * Formula: connections = ((core_count × 2) + effective_spindle_count)
 * 
 * For example:
 * - Server with 4 cores and 1 disk: (4 × 2) + 1 = 9 connections
 * - Server with 8 cores and 2 disks: (8 × 2) + 2 = 18 connections
 */

public class ConnectionPoolSizer {
    
    public static int calculatePoolSize() {
        int cores = Runtime.getRuntime().availableProcessors();
        int disks = 1;  // Usually 1 for cloud databases
        
        return (cores * 2) + disks;
    }
    
    // Example usage
    public static void main(String[] args) {
        int recommended = calculatePoolSize();
        System.out.println("Recommended pool size: " + recommended);
    }
}
```

### Multiple DataSources

```java
@Configuration
public class MultiDataSourceConfig {
    
    // Primary DataSource
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary.hikari")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }
    
    // Secondary DataSource
    @Bean
    @ConfigurationProperties("spring.datasource.secondary.hikari")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }
}
```

```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:mysql://primary:3306/db1
      username: user1
      password: pass1
      hikari:
        maximum-pool-size: 20
    
    secondary:
      jdbc-url: jdbc:mysql://secondary:3306/db2
      username: user2
      password: pass2
      hikari:
        maximum-pool-size: 10
```

### Monitoring Connection Pool

```java
@Component
public class HikariMonitor {
    
    @Autowired
    private DataSource dataSource;
    
    @Scheduled(fixedRate = 30000)
    public void logPoolStats() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            
            System.out.println("=== HikariCP Stats ===");
            System.out.println("Active connections: " + pool.getActiveConnections());
            System.out.println("Idle connections: " + pool.getIdleConnections());
            System.out.println("Total connections: " + pool.getTotalConnections());
            System.out.println("Threads waiting: " + pool.getThreadsAwaitingConnection());
        }
    }
}
```

---

## Server Tuning

### Connection Timeouts

```yaml
server:
  # Server port
  port: 8080
  
  # Connection timeout (how long to wait for request)
  connection-timeout: 20s
  
  tomcat:
    # Keep-alive timeout
    keep-alive-timeout: 60s
    
    # Max keep-alive requests on single connection
    max-keep-alive-requests: 100
    
    # Connection timeout
    connection-timeout: 20s
```

### Request Size Limits

```yaml
server:
  # Max HTTP header size
  max-http-header-size: 8KB
  
  tomcat:
    # Max HTTP post size
    max-http-form-post-size: 2MB
    
    # Max swallow size
    max-swallow-size: 2MB
    
spring:
  servlet:
    multipart:
      # Max file upload size
      max-file-size: 10MB
      max-request-size: 10MB
```

### Compression

```yaml
server:
  compression:
    enabled: true
    min-response-size: 1024  # 1KB minimum
    mime-types:
      - text/html
      - text/xml
      - text/plain
      - text/css
      - text/javascript
      - application/javascript
      - application/json
      - application/xml
```

### Custom Server Configuration

```java
@Configuration
public class ServerTuningConfig {
    
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> 
        webServerCustomizer() {
        return factory -> {
            
            // Error pages
            factory.addErrorPages(
                new ErrorPage(HttpStatus.NOT_FOUND, "/404"),
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500")
            );
            
            // Session timeout
            factory.setSession(session -> {
                session.setTimeout(Duration.ofMinutes(30));
                session.getCookie().setHttpOnly(true);
                session.getCookie().setSecure(true);
            });
        };
    }
}
```

### Performance Tuning Example

```yaml
# Production-optimized configuration
server:
  port: 8080
  compression:
    enabled: true
    min-response-size: 1024
  
  tomcat:
    # Thread pool
    threads:
      max: 200
      min-spare: 20
    
    # Connections
    max-connections: 10000
    accept-count: 200
    
    # Timeouts
    connection-timeout: 20s
    keep-alive-timeout: 60s
    max-keep-alive-requests: 100
    
    # Buffer sizes
    max-http-form-post-size: 2MB
    
    # Access logs (production)
    accesslog:
      enabled: true
      directory: /var/log/tomcat
      pattern: "%h %l %u %t \"%r\" %s %b %D"
      rotate: true
      max-days: 30
```

---

## HTTP/2 Support

### Why HTTP/2?

**Benefits**:
- ✓ Multiplexing (multiple requests over single connection)
- ✓ Header compression
- ✓ Server push
- ✓ Binary protocol (faster parsing)
- ✓ Stream prioritization

### Enabling HTTP/2 on Tomcat

```yaml
server:
  port: 8443
  
  http2:
    enabled: true
  
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: tomcat
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
```

**Note**: HTTP/2 requires HTTPS (TLS)

### HTTP/2 with Undertow

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

```yaml
server:
  port: 8443
  http2:
    enabled: true
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
```

### HTTP/2 Programmatic Configuration

```java
@Configuration
public class Http2Config {
    
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> 
        http2Customizer() {
        return factory -> {
            
            factory.addConnectorCustomizers(connector -> {
                connector.addUpgradeProtocol(new Http2Protocol());
            });
        };
    }
}
```

### Testing HTTP/2

```bash
# Using curl (HTTP/2)
curl -I --http2 https://localhost:8443/api/test

# Response shows HTTP/2
HTTP/2 200
content-type: application/json
```

### HTTP/2 Server Push

```java
@RestController
public class Http2PushController {
    
    @GetMapping("/index")
    public String index(PushBuilder pushBuilder) {
        if (pushBuilder != null) {
            // Push CSS and JS files before HTML
            pushBuilder.path("/css/style.css").push();
            pushBuilder.path("/js/app.js").push();
        }
        return "index";
    }
}
```

### HTTP/2 vs HTTP/1.1 Comparison

```
HTTP/1.1:
Request 1 →  ━━━━━━━━━━  → Response 1
Request 2 →       ━━━━━━━━━━  → Response 2
Request 3 →             ━━━━━━━━━━  → Response 3

HTTP/2 (Multiplexed):
Request 1 → ━━━━  → Response 1
Request 2 → ━━━━  → Response 2
Request 3 → ━━━━  → Response 3
(All on same connection!)
```

---

## Graceful Shutdown

### Why Graceful Shutdown?

**Problem**: Abrupt shutdown can cause:
- ✗ Lost in-flight requests
- ✗ Incomplete database transactions
- ✗ Broken user experiences
- ✗ Data corruption

**Solution**: Graceful shutdown:
- ✓ Stop accepting new requests
- ✓ Complete in-flight requests
- ✓ Close connections properly
- ✓ Clean up resources

### Enabling Graceful Shutdown

```yaml
# application.yml
server:
  shutdown: graceful  # Default is 'immediate'

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Max wait time
```

### How It Works

```
1. Shutdown signal received (SIGTERM)
   ↓
2. Stop accepting new requests
   ↓
3. Wait for in-flight requests to complete (max 30s)
   ↓
4. Close server
   ↓
5. Application shutdown
```

### Lifecycle Hooks

```java
@Component
public class GracefulShutdownListener {
    
    @PreDestroy
    public void onShutdown() {
        System.out.println("Application is shutting down...");
        // Cleanup resources
    }
}
```

### Custom Shutdown Hook

```java
@Component
public class CustomShutdownHook implements ApplicationListener<ContextClosedEvent> {
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("Context is closing...");
        
        // Wait for background tasks
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Cleanup completed");
    }
}
```

### Shutdown with Thread Pool

```java
@Configuration
public class ShutdownConfig {
    
    @Bean(destroyMethod = "shutdown")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(10);
    }
    
    @PreDestroy
    public void shutdown() throws InterruptedException {
        ExecutorService executor = executorService();
        
        // Stop accepting new tasks
        executor.shutdown();
        
        // Wait for existing tasks to complete
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            // Force shutdown if timeout
            executor.shutdownNow();
        }
    }
}
```

### Health Check During Shutdown

```java
@Component
public class ShutdownHealthIndicator implements HealthIndicator {
    
    private volatile boolean shuttingDown = false;
    
    @Override
    public Health health() {
        if (shuttingDown) {
            return Health.down()
                .withDetail("status", "shutting_down")
                .build();
        }
        return Health.up().build();
    }
    
    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        shuttingDown = true;
    }
}
```

### Kubernetes Integration

```yaml
# Kubernetes deployment
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: myapp
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
        # Gives time for load balancer to remove pod
```

---

## Servlet vs Reactive Stack

### Understanding the Stacks

**Servlet Stack (Traditional)**:
```
Request → Thread → Process → Response
(One thread per request)
```

**Reactive Stack (Non-blocking)**:
```
Request → Event Loop → Process (async) → Response
(Handles many requests with few threads)
```

### Comparison Table

| Feature | Servlet Stack | Reactive Stack |
|---------|--------------|----------------|
| **Server** | Tomcat, Jetty, Undertow | Netty |
| **Concurrency Model** | Thread per request | Event-driven |
| **Blocking** | Yes | No |
| **Scalability** | Limited by threads | High |
| **Programming Model** | Simple (imperative) | Complex (reactive) |
| **Best For** | CRUD apps, simple APIs | High I/O, streaming |
| **Learning Curve** | Easy | Steep |

### Servlet Stack Example

```java
// Spring MVC (Servlet)
@RestController
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // Blocking call - thread waits
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException());
    }
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        // Blocking save
        return userRepository.save(user);
    }
}
```

### Reactive Stack Example

```java
// Spring WebFlux (Reactive)
@RestController
public class UserController {
    
    @Autowired
    private ReactiveUserRepository userRepository;
    
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        // Non-blocking - returns immediately
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException()));
    }
    
    @PostMapping("/users")
    public Mono<User> createUser(@RequestBody User user) {
        // Non-blocking save
        return userRepository.save(user);
    }
    
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        // Stream of users
        return userRepository.findAll();
    }
}
```

### When to Use Servlet Stack

**✓ Use Servlet (Spring MVC) when**:
- Traditional CRUD applications
- Blocking I/O is acceptable
- Team familiar with servlet programming
- Using JDBC (blocking)
- Simple request-response patterns
- Not handling thousands of concurrent connections

**Example Use Cases**:
- Admin dashboards
- Internal tools
- Traditional web applications
- REST APIs with moderate traffic

### When to Use Reactive Stack

**✓ Use Reactive (Spring WebFlux) when**:
- High concurrent connections (10K+)
- Lots of I/O operations (network calls, databases)
- Streaming data
- Microservices with service-to-service calls
- Real-time applications
- Need backpressure handling

**Example Use Cases**:
- Chat applications
- Stock price streaming
- Social media feeds
- API gateways
- IoT data processing

### Side-by-Side Performance

```java
// Servlet: Blocking database call
@GetMapping("/products")
public List<Product> getProducts() {
    List<Product> products = productRepository.findAll();  // Blocks thread
    return products;
    // Thread held for entire duration
}

// Reactive: Non-blocking database call
@GetMapping("/products")
public Flux<Product> getProducts() {
    return productRepository.findAll();  // Returns immediately
    // Thread freed, data emitted when ready
}
```

### Reactive Example: External API Calls

```java
@Service
public class OrderService {
    
    @Autowired
    private WebClient webClient;
    
    // Multiple API calls in parallel (non-blocking)
    public Mono<OrderSummary> getOrderSummary(Long orderId) {
        
        Mono<Order> orderMono = webClient.get()
            .uri("/orders/{id}", orderId)
            .retrieve()
            .bodyToMono(Order.class);
        
        Mono<Customer> customerMono = webClient.get()
            .uri("/customers/{id}", orderId)
            .retrieve()
            .bodyToMono(Customer.class);
        
        Mono<Payment> paymentMono = webClient.get()
            .uri("/payments/{id}", orderId)
            .retrieve()
            .bodyToMono(Payment.class);
        
        // Combine all three - execute in parallel!
        return Mono.zip(orderMono, customerMono, paymentMono)
            .map(tuple -> new OrderSummary(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3()
            ));
    }
}
```

### Migration Considerations

**Don't migrate to Reactive if**:
- ✗ Team not familiar with reactive programming
- ✗ Using blocking libraries (JDBC, etc.)
- ✗ Current performance is acceptable
- ✗ Simple CRUD application

**Consider migration if**:
- ✓ Scaling challenges
- ✓ High I/O operations
- ✓ Need better resource utilization
- ✓ Team ready to learn reactive

### Hybrid Approach

```java
// You can use reactive in servlet stack!
@RestController
public class HybridController {
    
    @Autowired
    private WebClient webClient;
    
    @GetMapping("/data")
    public CompletableFuture<String> getData() {
        // Async in servlet stack
        return webClient.get()
            .uri("/external-api")
            .retrieve()
            .bodyToMono(String.class)
            .toFuture();  // Convert to CompletableFuture
    }
}
```

---

## Performance Best Practices

### 1. Choose the Right Server

```yaml
# For traditional apps → Tomcat (default)
# For WebSockets → Jetty or Undertow
# For reactive apps → Netty
# For best performance → Undertow
```

### 2. Tune Thread Pool Properly

```java
// Rule: threads = cores × (1 + wait_time/service_time)
int cores = Runtime.getRuntime().availableProcessors();
int threads = cores * 2;  // Good starting point
```

### 3. Enable Compression

```yaml
server:
  compression:
    enabled: true
    min-response-size: 1024
```

### 4. Configure Connection Pool Correctly

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Not 100!
      minimum-idle: 10
```

### 5. Use HTTP/2

```yaml
server:
  http2:
    enabled: true
```

### 6. Enable Graceful Shutdown

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 7. Monitor Metrics

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

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### 8. Load Testing

```bash
# Using Apache Bench
ab -n 10000 -c 100 http://localhost:8080/api/test

# Using wrk
wrk -t12 -c400 -d30s http://localhost:8080/api/test
```

### 9. Profile Your Application

```bash
# Enable JMX
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -jar myapp.jar
```

### 10. Review Access Logs

```yaml
server:
  tomcat:
    accesslog:
      enabled: true
      pattern: "%h %l %u %t \"%r\" %s %b %D"
      # %D shows request processing time
```

---

## Complete Production Configuration Example

```yaml
# application-prod.yml
server:
  port: 8080
  shutdown: graceful
  
  # HTTP/2
  http2:
    enabled: true
  
  # Compression
  compression:
    enabled: true
    min-response-size: 1024
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/plain
      - text/css
      - application/javascript
  
  # Tomcat
  tomcat:
    # Threads
    threads:
      max: 200
      min-spare: 20
    
    # Connections
    max-connections: 10000
    accept-count: 200
    
    # Timeouts
    connection-timeout: 20s
    keep-alive-timeout: 60s
    max-keep-alive-requests: 100
    
    # Limits
    max-http-form-post-size: 2MB
    
    # Access logs
    accesslog:
      enabled: true
      directory: /var/log/tomcat
      prefix: access_log
      suffix: .log
      pattern: "%h %l %u %t \"%r\" %s %b %D"
      rotate: true
      max-days: 30

# Database connection pool
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: HikariPool
      leak-detection-threshold: 60000
      
      # MySQL optimizations
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false

# Lifecycle
  lifecycle:
    timeout-per-shutdown-phase: 30s

# Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# Logging
logging:
  level:
    root: INFO
    com.example: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: /var/log/myapp/application.log
    max-size: 10MB
    max-history: 30
```

---

## Quick Reference Guide

### Server Selection

| Scenario | Recommended Server |
|----------|-------------------|
| Traditional REST API | Tomcat (default) |
| WebSocket heavy | Jetty |
| High performance | Undertow |
| Reactive apps | Netty (WebFlux) |
| Microservices | Undertow or Netty |

### Thread Pool Sizing

```
CPU-bound:     threads = cores
I/O-bound:     threads = cores × 10
Mixed workload: threads = cores × 2
```

### Connection Pool Sizing

```
Formula: (cores × 2) + disks
Example: (4 × 2) + 1 = 9 connections
```

### Key Timeouts

```yaml
connection-timeout: 20s      # How long to wait for connection
keep-alive-timeout: 60s      # Keep connection open
read-timeout: 30s            # Read data timeout
write-timeout: 30s           # Write data timeout
```

### When to Use Reactive

```
✓ High concurrent users (10K+)
✓ Lots of I/O operations
✓ Streaming data
✗ Simple CRUD
✗ Blocking libraries (JDBC)
✗ Team unfamiliar with reactive
```

---

## Summary

**Key Takeaways**:

1. **Server Choice**: Tomcat (default), Jetty (WebSocket), Netty (reactive), Undertow (performance)
2. **Thread Pools**: Size based on workload (CPU vs I/O bound)
3. **Connection Pools**: Keep it small! Formula: (cores × 2) + disks
4. **Timeouts**: Set appropriate timeouts to prevent resource exhaustion
5. **HTTP/2**: Enable for better performance (requires HTTPS)
6. **Graceful Shutdown**: Prevent data loss and broken requests
7. **Servlet vs Reactive**: Choose based on concurrency needs
8. **Monitoring**: Always monitor thread pools and connection pools
9. **Load Test**: Test before production!
10. **Start Simple**: Don't over-optimize prematurely

**Remember**: Good server tuning can dramatically improve application performance and resource utilization! 🚀
