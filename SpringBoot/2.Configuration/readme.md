# Spring Boot Configuration - Complete Guide

> **Master Configuration Management**: From basics to advanced techniques

---

## Table of Contents
1. [Introduction](#introduction)
2. [application.yml vs application.properties](#applicationyml-vs-applicationproperties)
3. [Profile-Based Configuration](#profile-based-configuration)
4. [Externalized Configuration](#externalized-configuration)
5. [@ConfigurationProperties vs @Value](#configurationproperties-vs-value)
6. [Type-Safe Configuration](#type-safe-configuration)
7. [Configuration Precedence Order](#configuration-precedence-order)
8. [Dynamic Config Refresh](#dynamic-config-refresh)
9. [Best Practices](#best-practices)

---

## Introduction

Configuration is the backbone of any Spring Boot application. Understanding how to manage configuration properly makes your applications:
- **Flexible**: Easy to change without code modifications
- **Portable**: Run in different environments seamlessly
- **Maintainable**: Clear and organized settings
- **Secure**: Sensitive data properly managed

**Key Concept**: Spring Boot follows "convention over configuration" but provides extensive configuration options when needed.

---

## application.yml vs application.properties

### application.properties Format

```properties
# Simple key-value format
server.port=8080
server.servlet.context-path=/api

# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Lists (indexed)
spring.profiles.active=dev
app.servers[0]=server1.com
app.servers[1]=server2.com
app.servers[2]=server3.com

# Maps
app.config.timeout=5000
app.config.retries=3
```

### application.yml Format

```yaml
# Hierarchical structure
server:
  port: 8080
  servlet:
    context-path: /api

# Database configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver

# Lists (cleaner)
  profiles:
    active: dev

app:
  servers:
    - server1.com
    - server2.com
    - server3.com
  
  config:
    timeout: 5000
    retries: 3
```

### Side-by-Side Comparison

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
```

```yaml
# application.yml (Much cleaner!)
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
```

### When to Use Each

**Use `.properties`**:
- ✓ Simple, flat configuration
- ✓ Legacy projects
- ✓ When team prefers properties format
- ✓ Integration with older systems

**Use `.yml`**:
- ✓ Complex, nested configuration
- ✓ Better readability
- ✓ Lists and maps are common
- ✓ Modern microservices
- ✓ **Recommended for new projects**

### Common Pitfall with YAML

```yaml
# ✗ WRONG: Indentation matters!
spring:
  datasource:
  url: jdbc:mysql://localhost:3306/mydb  # Wrong indent!
    username: root

# ✓ CORRECT:
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
```

### Can You Use Both?

**Yes!** Spring Boot merges them, but...

```properties
# application.properties
server.port=8080
```

```yaml
# application.yml
server:
  servlet:
    context-path: /api
```

**Result**: Both configurations are applied
- Port: 8080 (from .properties)
- Context path: /api (from .yml)

**Warning**: If same property exists in both, `.properties` wins!

---

## Profile-Based Configuration

### What Are Profiles?

Profiles allow different configurations for different environments:
- **dev**: Development environment
- **test**: Testing environment
- **prod**: Production environment

### Creating Profile-Specific Files

```
src/main/resources/
├── application.yml              # Default config
├── application-dev.yml         # Development
├── application-test.yml        # Testing
└── application-prod.yml        # Production
```

### Example Configuration

**application.yml** (Default):
```yaml
# Common settings for all profiles
app:
  name: MyApplication
  version: 1.0.0

logging:
  level:
    root: INFO
```

**application-dev.yml**:
```yaml
# Development settings
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb_dev
    username: dev_user
    password: dev_pass
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    root: DEBUG
    com.example: TRACE

server:
  port: 8080
```

**application-prod.yml**:
```yaml
# Production settings
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb_prod
    username: prod_user
    password: ${DB_PASSWORD}  # From environment variable
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    root: WARN
    com.example: INFO

server:
  port: 80
```

### Activating Profiles

**Method 1: In application.yml**
```yaml
spring:
  profiles:
    active: dev
```

**Method 2: Environment Variable**
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar myapp.jar
```

**Method 3: Command Line**
```bash
java -jar myapp.jar --spring.profiles.active=prod
```

**Method 4: In IDE (IntelliJ)**
```
Run → Edit Configurations → Environment Variables
SPRING_PROFILES_ACTIVE=dev
```

**Method 5: Programmatically**
```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApplication.class);
        app.setAdditionalProfiles("dev");
        app.run(args);
    }
}
```

### Multiple Profiles

```bash
# Activate multiple profiles
java -jar myapp.jar --spring.profiles.active=dev,debug,mysql
```

```yaml
# In YAML
spring:
  profiles:
    active: dev,debug,mysql
```

### Profile-Specific Beans

```java
// Bean only created in 'dev' profile
@Configuration
@Profile("dev")
public class DevConfiguration {
    
    @Bean
    public DataSource devDataSource() {
        // Development database with H2
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}

// Bean only created in 'prod' profile
@Configuration
@Profile("prod")
public class ProdConfiguration {
    
    @Bean
    public DataSource prodDataSource() {
        // Production database with connection pooling
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod-server:3306/mydb");
        return ds;
    }
}
```

### Conditional Profile Logic

```java
@Component
@Profile("!prod")  // All profiles EXCEPT prod
public class DebugTools {
    // Debug utilities
}

@Component
@Profile({"dev", "test"})  // Only in dev OR test
public class TestHelpers {
    // Test helpers
}
```

### Profile-Specific Properties in Same File

```yaml
# application.yml
spring:
  application:
    name: MyApp

---
# Dev profile
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://localhost:3306/mydb_dev

---
# Prod profile
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb_prod
```

---

## Externalized Configuration

Spring Boot allows configuration from multiple sources, making applications portable across environments.

### Configuration Sources (17 Levels!)

Spring Boot loads configuration in this order (later overrides earlier):

1. Default properties
2. @PropertySource annotations
3. Config data (application.yml)
4. Profile-specific config (application-prod.yml)
5. Environment variables
6. Java System properties
7. Command line arguments

### Environment Variables

```bash
# Set environment variable
export SERVER_PORT=9090
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/mydb
export SPRING_DATASOURCE_USERNAME=root
```

```java
// Spring Boot automatically maps:
// SERVER_PORT → server.port
// SPRING_DATASOURCE_URL → spring.datasource.url
```

**Naming Convention**:
```
Property:           server.port
Environment Var:    SERVER_PORT

Property:           spring.datasource.url
Environment Var:    SPRING_DATASOURCE_URL
```

### Command Line Arguments

```bash
# Override any property
java -jar myapp.jar --server.port=9090 --spring.datasource.url=jdbc:mysql://newhost:3306/db
```

```bash
# Multiple properties
java -jar myapp.jar \
  --server.port=9090 \
  --spring.profiles.active=prod \
  --app.feature.enabled=true
```

### System Properties

```bash
# Using -D flag
java -Dserver.port=9090 -Dspring.profiles.active=prod -jar myapp.jar
```

```java
// Programmatically
System.setProperty("server.port", "9090");
```

### External Configuration Files

```bash
# Load from specific location
java -jar myapp.jar --spring.config.location=file:/etc/myapp/application.yml
```

```bash
# Load additional config
java -jar myapp.jar --spring.config.additional-location=file:/etc/myapp/custom.yml
```

### Configuration from Multiple Locations

```
Load Order (later wins):
1. classpath:/application.yml          # In JAR
2. classpath:/application-prod.yml     # In JAR
3. file:./application.yml              # Current directory
4. file:./config/application.yml       # ./config/ subdirectory
5. file:/etc/myapp/application.yml     # External location
6. Environment variables
7. Command line arguments
```

### Real-World Example: Docker

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
COPY target/myapp.jar app.jar

# Environment variables
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
services:
  app:
    image: myapp:latest
    environment:
      - SERVER_PORT=8080
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/mydb
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=secret
    ports:
      - "8080:8080"
```

### Kubernetes ConfigMap

```yaml
# configmap.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/mydb
    server:
      port: 8080
```

```yaml
# deployment.yml
spec:
  containers:
  - name: myapp
    env:
    - name: SPRING_PROFILES_ACTIVE
      value: "prod"
    volumeMounts:
    - name: config
      mountPath: /config
  volumes:
  - name: config
    configMap:
      name: app-config
```

### Random Values

```yaml
# application.yml
app:
  secret: ${random.value}        # Random string
  number: ${random.int}          # Random integer
  uuid: ${random.uuid}           # Random UUID
  port: ${random.int[1024,65535]} # Random port
```

### Property Placeholders

```yaml
app:
  name: MyApplication
  description: ${app.name} is awesome!  # "MyApplication is awesome!"
  
server:
  port: 8080
  url: http://localhost:${server.port}  # "http://localhost:8080"
```

---

## @ConfigurationProperties vs @Value

### @Value Annotation

**Use Case**: Inject individual properties

```java
@Component
public class AppConfig {
    
    @Value("${server.port}")
    private int serverPort;
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.timeout:5000}")  // Default value: 5000
    private int timeout;
    
    @Value("${app.enabled:true}")
    private boolean enabled;
    
    // Constructor injection (recommended)
    public AppConfig(
        @Value("${server.port}") int port,
        @Value("${app.name}") String name
    ) {
        this.serverPort = port;
        this.appName = name;
    }
}
```

**Limitations of @Value**:
- ✗ No validation
- ✗ No type safety for complex objects
- ✗ Scattered across codebase
- ✗ Hard to test
- ✗ No IDE auto-completion

### @ConfigurationProperties

**Use Case**: Bind entire configuration blocks to POJOs

```yaml
# application.yml
app:
  name: MyApplication
  timeout: 5000
  retry:
    max-attempts: 3
    delay: 1000
  servers:
    - host: server1.com
      port: 8080
    - host: server2.com
      port: 8081
  database:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
```

```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private String name;
    private int timeout;
    private Retry retry;
    private List<Server> servers;
    private Database database;
    
    // Nested class
    public static class Retry {
        private int maxAttempts;
        private int delay;
        
        // Getters and setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { 
            this.maxAttempts = maxAttempts; 
        }
        public int getDelay() { return delay; }
        public void setDelay(int delay) { this.delay = delay; }
    }
    
    public static class Server {
        private String host;
        private int port;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
    
    public static class Database {
        private String url;
        private String username;
        private String password;
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    // Main class getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public List<Server> getServers() { return servers; }
    public void setServers(List<Server> servers) { this.servers = servers; }
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
}
```

**Enable Configuration Properties**:
```java
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**Using the Configuration**:
```java
@Service
public class MyService {
    
    private final AppProperties config;
    
    public MyService(AppProperties config) {
        this.config = config;
    }
    
    public void doWork() {
        System.out.println("App name: " + config.getName());
        System.out.println("Timeout: " + config.getTimeout());
        System.out.println("Max retries: " + config.getRetry().getMaxAttempts());
        
        for (AppProperties.Server server : config.getServers()) {
            System.out.println("Server: " + server.getHost() + ":" + server.getPort());
        }
    }
}
```

### Comparison Table

| Feature | @Value | @ConfigurationProperties |
|---------|--------|-------------------------|
| **Use Case** | Single properties | Grouped properties |
| **Type Safety** | Limited | Excellent |
| **Validation** | No | Yes (with @Validated) |
| **Relaxed Binding** | No | Yes |
| **IDE Support** | Limited | Excellent |
| **Testing** | Hard | Easy |
| **Recommended For** | Simple cases | Complex configuration |

### When to Use Which?

**Use @Value**:
```java
// Simple, one-off values
@Value("${app.version}")
private String version;

// Third-party library integration
@Value("${some.external.property}")
private String externalValue;
```

**Use @ConfigurationProperties**:
```java
// Complex, grouped configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    // Multiple related properties
}

// When you need validation
// When you need IDE auto-completion
// When properties are used across multiple classes
```

---

## Type-Safe Configuration

Making configuration bulletproof with validation and type safety.

### Basic Type-Safe Configuration

```java
@Component
@ConfigurationProperties(prefix = "app")
@Validated  // Enable validation
public class AppConfig {
    
    @NotBlank(message = "App name is required")
    private String name;
    
    @Min(value = 1024, message = "Port must be >= 1024")
    @Max(value = 65535, message = "Port must be <= 65535")
    private int port;
    
    @Email(message = "Invalid email format")
    private String adminEmail;
    
    @Pattern(regexp = "^(http|https)://.*", message = "Invalid URL")
    private String baseUrl;
    
    @NotNull
    @Valid  // Validate nested object
    private Database database;
    
    public static class Database {
        @NotBlank
        private String url;
        
        @Min(1)
        @Max(100)
        private int maxConnections = 10;
        
        // Getters and setters
    }
    
    // Getters and setters
}
```

```yaml
# application.yml - If invalid, app won't start!
app:
  name: ""  # ✗ Fails: @NotBlank
  port: 80  # ✗ Fails: @Min(1024)
  admin-email: "invalid-email"  # ✗ Fails: @Email
  base-url: "ftp://example.com"  # ✗ Fails: @Pattern
  database:
    url: ""  # ✗ Fails: @NotBlank
    max-connections: 200  # ✗ Fails: @Max(100)
```

### Custom Validation

```java
@ConfigurationProperties(prefix = "app")
@Validated
public class AppConfig {
    
    @ValidDatabase  // Custom validator
    private Database database;
    
    public static class Database {
        private String url;
        private String username;
        private String password;
        
        // Getters and setters
    }
}

// Custom Validator
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DatabaseValidator.class)
public @interface ValidDatabase {
    String message() default "Invalid database configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator Implementation
public class DatabaseValidator 
    implements ConstraintValidator<ValidDatabase, AppConfig.Database> {
    
    @Override
    public boolean isValid(AppConfig.Database db, ConstraintValidatorContext context) {
        if (db == null) return false;
        
        // Custom validation logic
        if (db.getUrl() == null || db.getUrl().isEmpty()) {
            return false;
        }
        
        if (db.getUrl().contains("localhost") && 
            "root".equals(db.getUsername())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Don't use root on localhost in production!"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
```

### Duration and DataSize Types

```java
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    @DurationMin(seconds = 1)
    @DurationMax(minutes = 10)
    private Duration timeout;
    
    @DataSizeMin(value = 1, unit = DataUnit.MEGABYTES)
    @DataSizeMax(value = 100, unit = DataUnit.MEGABYTES)
    private DataSize maxFileSize;
    
    // Getters and setters
}
```

```yaml
app:
  timeout: 30s        # 30 seconds
  # timeout: 5m      # 5 minutes
  # timeout: 1h      # 1 hour
  
  max-file-size: 10MB
  # max-file-size: 512KB
  # max-file-size: 1GB
```

### Enum Support

```java
public enum Environment {
    DEV, TEST, STAGING, PROD
}

@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private Environment environment;
    
    // Getter and setter
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { 
        this.environment = environment; 
    }
}
```

```yaml
app:
  environment: PROD  # Type-safe!
  # environment: INVALID  # Would fail at startup
```

### Map Configuration

```java
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private Map<String, String> settings;
    private Map<String, DatabaseConfig> databases;
    
    public static class DatabaseConfig {
        private String url;
        private int maxConnections;
        // Getters and setters
    }
    
    // Getters and setters
}
```

```yaml
app:
  settings:
    theme: dark
    language: en
    timezone: UTC
  
  databases:
    primary:
      url: jdbc:mysql://db1:3306/main
      max-connections: 50
    secondary:
      url: jdbc:mysql://db2:3306/backup
      max-connections: 20
```

### Constructor Binding (Immutable Configuration)

```java
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private final String name;
    private final int port;
    private final Database database;
    
    // Constructor binding (immutable)
    public AppConfig(
        String name,
        int port,
        Database database
    ) {
        this.name = name;
        this.port = port;
        this.database = database;
    }
    
    // Only getters (no setters - immutable!)
    public String getName() { return name; }
    public int getPort() { return port; }
    public Database getDatabase() { return database; }
    
    public static class Database {
        private final String url;
        private final String username;
        
        public Database(String url, String username) {
            this.url = url;
            this.username = username;
        }
        
        public String getUrl() { return url; }
        public String getUsername() { return username; }
    }
}
```

Enable with:
```java
@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
@ConfigurationPropertiesScan  // Scans for constructor binding
public class MyApplication {
    // ...
}
```

---

## Configuration Precedence Order

Understanding which configuration source wins when multiple sources define the same property.

### The Complete Order (17 Levels)

**Lower number = Higher priority (wins)**

```
1.  Devtools global settings (~/.spring-boot-devtools.properties)
2.  @TestPropertySource annotations
3.  @SpringBootTest properties
4.  Command line arguments
5.  SPRING_APPLICATION_JSON properties
6.  ServletConfig init parameters
7.  ServletContext init parameters
8.  JNDI attributes (java:comp/env)
9.  Java System properties (System.getProperties())
10. OS environment variables
11. RandomValuePropertySource (random.*)
12. Profile-specific outside JAR (application-{profile}.yml)
13. Profile-specific inside JAR
14. Application properties outside JAR (application.yml)
15. Application properties inside JAR
16. @PropertySource annotations
17. Default properties (SpringApplication.setDefaultProperties)
```

### Practical Example

```yaml
# 15. application.yml (inside JAR)
server:
  port: 8080
app:
  name: MyApp
```

```bash
# 10. Environment variable (overrides YAML)
export SERVER_PORT=9090

# 4. Command line (overrides environment)
java -jar myapp.jar --server.port=7070 --app.name=CustomApp
```

**Result**:
- `server.port` = 7070 (from command line)
- `app.name` = CustomApp (from command line)

### Detailed Examples

**Example 1: All Sources**
```yaml
# application.yml
server:
  port: 8080
```

```bash
# Environment variable
export SERVER_PORT=9090

# System property
java -Dserver.port=7070 -jar myapp.jar

# Command line
java -jar myapp.jar --server.port=6060
```

**Winner**: `server.port=6060` (command line wins all)

**Example 2: Profile Override**
```yaml
# application.yml (default)
app:
  feature:
    enabled: false
    name: Default Feature
```

```yaml
# application-prod.yml
app:
  feature:
    enabled: true
    # name not specified, uses default
```

```bash
java -jar myapp.jar --spring.profiles.active=prod
```

**Result**:
- `app.feature.enabled` = true (from prod profile)
- `app.feature.name` = "Default Feature" (from default, not overridden)

**Example 3: External Configuration**
```
/etc/myapp/
└── application.yml  (external file)
    server:
      port: 5050
```

```bash
java -jar myapp.jar --spring.config.location=file:/etc/myapp/
```

**Priority**:
1. External file: 5050
2. Command line can still override: `--server.port=3000`

### Testing Configuration Precedence

```java
@SpringBootTest(properties = {
    "server.port=9999",  // Overrides application.yml
    "app.test-mode=true"
})
class ConfigTest {
    
    @Value("${server.port}")
    private int port;
    
    @Test
    void testPort() {
        assertEquals(9999, port);  // Test properties win
    }
}
```

### Visualizing Precedence

```
Command Line Args (--server.port=7070)
    ↓ overrides
Environment Vars (SERVER_PORT=9090)
    ↓ overrides
System Properties (-Dserver.port=8080)
    ↓ overrides
Profile Config (application-prod.yml)
    ↓ overrides
Default Config (application.yml)
    ↓ overrides
@PropertySource
    ↓ overrides
Default Properties (hardcoded)
```

---

## Dynamic Config Refresh

Changing configuration at runtime without restarting the application.

### Prerequisites

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Enable Refresh Endpoint

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: refresh  # Enable /actuator/refresh endpoint
```

### Refreshable Configuration

```java
@Component
@RefreshScope  // ← Makes bean refreshable
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String message;
    private int timeout;
    
    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}
```

```java
@RestController
@RefreshScope  // ← Controller also refreshable
public class ConfigController {
    
    @Value("${app.message:Default}")
    private String message;
    
    private final AppConfig config;
    
    public ConfigController(AppConfig config) {
        this.config = config;
    }
    
    @GetMapping("/message")
    public String getMessage() {
        return message;  // Will update after refresh
    }
    
    @GetMapping("/config")
    public AppConfig getConfig() {
        return config;  // Will update after refresh
    }
}
```

### Triggering Refresh

**Step 1**: Update configuration source
```yaml
# Update application.yml or external config
app:
  message: Updated message!
  timeout: 10000
```

**Step 2**: Call refresh endpoint
```bash
curl -X POST http://localhost:8080/actuator/refresh
```

**Response**:
```json
[
  "app.message",
  "app.timeout"
]
```

**Step 3**: Verify changes
```bash
curl http://localhost:8080/message
# Returns: "Updated message!"
```

### With Spring Cloud Config Server

**Config Server Setup**:
```yaml
# bootstrap.yml (in client app)
spring:
  application:
    name: myapp
  cloud:
    config:
      uri: http://localhost:8888  # Config server URL
```

**Git Repository Structure**:
```
config-repo/
├── myapp.yml              # Default config
├── myapp-dev.yml         # Dev environment
└── myapp-prod.yml        # Prod environment
```

**myapp.yml** (in Git):
```yaml
app:
  message: Hello from Config Server!
  timeout: 5000
```

**Refresh Process**:
```bash
# 1. Update Git repository
git commit -am "Update message"
git push

# 2. Refresh client app
curl -X POST http://localhost:8080/actuator/refresh
```

### Automatic Refresh with Spring Cloud Bus

```xml
<!-- Add Spring Cloud Bus -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672

management:
  endpoints:
    web:
      exposure:
        include: busrefresh  # Enable bus refresh
```

**Refresh all instances**:
```bash
curl -X POST http://localhost:8080/actuator/busrefresh
```

**What happens**:
1. POST to /actuator/busrefresh on ONE instance
2. Message sent to RabbitMQ
3. ALL instances receive message
4. ALL instances refresh configuration

### Config Refresh Events

```java
@Component
public class ConfigRefreshListener {
    
    @EventListener
    public void handleRefresh(RefreshScopeRefreshedEvent event) {
        System.out.println("Configuration refreshed!");
        // Perform post-refresh actions
    }
}
```

### What Gets Refreshed?

**✓ Refreshable**:
- `@RefreshScope` beans
- `@ConfigurationProperties` with `@RefreshScope`
- `@Value` in `@RefreshScope` beans

**✗ NOT Refreshable**:
- Regular beans (without `@RefreshScope`)
- `@Value` in singleton beans
- Static values

**Example**:
```java
@Component
@RefreshScope  // ✓ Will refresh
public class RefreshableComponent {
    
    @Value("${app.message}")
    private String message;  // ✓ Will update
}

@Component  // ✗ Won't refresh
public class RegularComponent {
    
    @Value("${app.message}")
    private String message;  // ✗ Won't update
}
```

### Manual Refresh in Code

```java
@Service
public class ConfigRefreshService {
    
    @Autowired
    private RefreshScope refreshScope;
    
    public void refreshConfiguration() {
        refreshScope.refreshAll();  // Refresh all @RefreshScope beans
    }
    
    public void refreshSpecificBean(String beanName) {
        refreshScope.refresh(beanName);  // Refresh specific bean
    }
}
```

---

## Best Practices

### 1. Use YAML for Complex Configuration

```yaml
# ✓ GOOD: Clear hierarchy
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

```properties
# ✗ BAD: Harder to read
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

### 2. Use @ConfigurationProperties Over @Value

```java
// ✓ GOOD: Type-safe, validatable
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    @NotNull
    private Database database;
}

// ✗ BAD: Scattered, no validation
@Value("${app.database.url}")
private String dbUrl;
@Value("${app.database.username}")
private String dbUsername;
```

### 3. Never Hardcode Secrets

```yaml
# ✗ NEVER DO THIS
spring:
  datasource:
    password: mySecretPassword123

# ✓ DO THIS
spring:
  datasource:
    password: ${DB_PASSWORD}  # From environment
```

### 4. Use Profiles Wisely

```
✓ GOOD:
  - dev
  - test
  - staging
  - prod

✗ BAD:
  - dev-alice
  - dev-bob
  - test-feature-x
```

### 5. Document Your Configuration

```java
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    /**
     * Application name displayed in UI.
     * Default: "MyApp"
     */
    private String name = "MyApp";
    
    /**
     * Request timeout in milliseconds.
     * Range: 1000-30000
     * Default: 5000
     */
    @Min(1000)
    @Max(30000)
    private int timeout = 5000;
}
```

### 6. Use Constructor Injection for Immutability

```java
// ✓ GOOD: Immutable
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private final String name;
    
    public AppConfig(String name) {
        this.name = name;
    }
    
    public String getName() { return name; }
}

// ✗ BAD: Mutable
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String name;
    public void setName(String name) { this.name = name; }
}
```

### 7. Validate Configuration at Startup

```java
@ConfigurationProperties(prefix = "app")
@Validated
public class AppConfig {
    
    @NotBlank
    private String name;
    
    @Min(1024)
    @Max(65535)
    private int port;
    
    // App won't start if invalid!
}
```

### 8. Group Related Properties

```yaml
# ✓ GOOD: Grouped
app:
  database:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    max-connections: 10
  cache:
    enabled: true
    ttl: 3600

# ✗ BAD: Scattered
app.database.url: jdbc:mysql://localhost:3306/mydb
app.database.username: root
cache.enabled: true
cache.ttl: 3600
```

### 9. Use Meaningful Property Names

```yaml
# ✓ GOOD: Clear intent
app:
  security:
    jwt:
      expiration-time-in-seconds: 3600
      secret-key: ${JWT_SECRET}

# ✗ BAD: Unclear
app:
  sec:
    jwt:
      exp: 3600
      key: ${JWT_SECRET}
```

### 10. Provide Sensible Defaults

```java
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private int timeout = 5000;  // ✓ Good default
    private int maxRetries = 3;  // ✓ Good default
    private boolean enabled = true;  // ✓ Good default
    
    // Getters and setters
}
```

---

## Configuration Cheat Sheet

### Quick Reference

| Scenario | Solution |
|----------|----------|
| Simple value injection | `@Value("${property}")` |
| Complex configuration | `@ConfigurationProperties` |
| Environment-specific config | Profiles (`application-{profile}.yml`) |
| External configuration | Environment variables or `--spring.config.location` |
| Secrets management | Environment variables, never in files |
| Runtime config changes | `@RefreshScope` + `/actuator/refresh` |
| Validation | `@Validated` + JSR-303 annotations |
| Type safety | `@ConfigurationProperties` classes |

### Common Patterns

**Database Configuration**:
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Feature Flags**:
```yaml
features:
  new-ui: ${FEATURE_NEW_UI:false}
  beta-api: ${FEATURE_BETA_API:false}
```

**Multi-Environment**:
```yaml
# application-${ENVIRONMENT}.yml
spring:
  config:
    activate:
      on-profile: ${ENVIRONMENT}
```

---

## Summary

**Key Takeaways**:

1. **YAML vs Properties**: Use YAML for complex, nested configuration
2. **Profiles**: Separate configuration per environment
3. **Externalization**: Use environment variables for deployment flexibility
4. **Type Safety**: Prefer `@ConfigurationProperties` over `@Value`
5. **Validation**: Validate configuration at startup to fail fast
6. **Precedence**: Command line > Environment > Properties
7. **Dynamic Refresh**: Use `@RefreshScope` for runtime updates
8. **Security**: Never hardcode secrets, use environment variables

**Remember**: Good configuration management makes your application flexible, maintainable, and production-ready! 🚀
