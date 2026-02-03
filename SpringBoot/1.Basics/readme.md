# Spring Boot Internals - Complete Guide

> **From User to Expert**: Understanding what happens under the hood

---

## Table of Contents
1. [Introduction](#introduction)
2. [Spring Boot Auto-Configuration Mechanism](#spring-boot-auto-configuration-mechanism)
3. [EnableAutoConfiguration Deep Dive](#enableautoconfiguration-deep-dive)
4. [spring.factories File](#springfactories-file)
5. [Conditional Annotations](#conditional-annotations)
6. [How Starters Work](#how-starters-work)
7. [Custom Starter Creation](#custom-starter-creation)
8. [Boot Startup Lifecycle](#boot-startup-lifecycle)
9. [ApplicationContext vs WebApplicationContext](#applicationcontext-vs-webapplicationcontext)
10. [Best Practices](#best-practices)

---

## Introduction

Spring Boot's "magic" isn't magic at all - it's a well-designed system of auto-configuration, conditional bean creation, and intelligent defaults. Understanding these internals transforms you from a Spring Boot user to an expert.

**Key Concept**: Spring Boot = Spring Framework + Auto-Configuration + Embedded Server + Opinionated Defaults

---

## Spring Boot Auto-Configuration Mechanism

### What is Auto-Configuration?

Auto-configuration is Spring Boot's way of automatically configuring your application based on:
- Jars present in the classpath
- Beans already defined
- Properties you've set

### Simple Example

```java
// WITHOUT Spring Boot (Traditional Spring)
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("password");
        return ds;
    }
}

// WITH Spring Boot (Auto-configured)
// Just add this to application.properties:
// spring.datasource.url=jdbc:mysql://localhost:3306/mydb
// spring.datasource.username=root
// spring.datasource.password=password

// Spring Boot automatically creates DataSource bean!
```

### How It Works (Step-by-Step)

1. **Application starts** with `@SpringBootApplication`
2. **@EnableAutoConfiguration** triggers auto-configuration
3. **AutoConfigurationImportSelector** loads all auto-configuration classes
4. **Conditional annotations** evaluate if configuration should apply
5. **Beans are created** only if conditions are met

---

## EnableAutoConfiguration Deep Dive

### The Annotation

```java
@SpringBootApplication
// This is equivalent to:
@Configuration
@EnableAutoConfiguration  // ← The magic starts here
@ComponentScan
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### What @EnableAutoConfiguration Does

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)  // ← Key import
public @interface EnableAutoConfiguration {
    // ...
}
```

### The Process

**Step 1**: `AutoConfigurationImportSelector` is triggered

**Step 2**: It calls `getCandidateConfigurations()`

**Step 3**: Loads configurations from `META-INF/spring.factories`

**Step 4**: Filters based on conditions

**Step 5**: Creates beans

### Simple Example - Behind the Scenes

```java
// When you add spring-boot-starter-data-jpa dependency

// 1. AutoConfigurationImportSelector finds this:
@Configuration
@ConditionalOnClass(DataSource.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        // Creates HikariCP DataSource
        return new HikariDataSource();
    }
}

// 2. Checks: Is DataSource.class in classpath? YES
// 3. Checks: Is DataSource bean already defined? NO
// 4. Creates the bean automatically!
```

---

## spring.factories File

### What is spring.factories?

A special file that lists all auto-configuration classes. Located at:
```
META-INF/spring.factories
```

### Example spring.factories File

```properties
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.autoconfigure.DatabaseAutoConfiguration,\
com.example.autoconfigure.CacheAutoConfiguration,\
com.example.autoconfigure.SecurityAutoConfiguration
```

### Real Example from Spring Boot

```properties
# From spring-boot-autoconfigure jar
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
```

### How Spring Boot Reads It

```java
// Simplified version of what happens internally
public class AutoConfigurationLoader {
    
    public List<String> loadConfigurations() {
        // 1. Find all spring.factories files in classpath
        Enumeration<URL> urls = classLoader.getResources("META-INF/spring.factories");
        
        // 2. Read EnableAutoConfiguration entries
        List<String> configurations = new ArrayList<>();
        
        // 3. Load each configuration class
        return configurations;
    }
}
```

### Location in Your Project

```
my-custom-starter/
└── src/
    └── main/
        └── resources/
            └── META-INF/
                └── spring.factories  ← Here!
```

---

## Conditional Annotations

These are the "decision makers" - they determine IF a configuration should be applied.

### @ConditionalOnClass

**Meaning**: Apply configuration only if specified class is in classpath

```java
// Example: Only configure JPA if EntityManager is available
@Configuration
@ConditionalOnClass(EntityManager.class)
public class JpaConfiguration {
    
    @Bean
    public EntityManagerFactory entityManagerFactory() {
        // Only created if JPA is in classpath
        return // ... configuration
    }
}
```

**Real-World Example**:
```java
// MongoDB configuration only if MongoDB driver is present
@Configuration
@ConditionalOnClass(MongoClient.class)
public class MongoAutoConfiguration {
    
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }
}
```

### @ConditionalOnMissingBean

**Meaning**: Create bean only if it doesn't already exist

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource defaultDataSource() {
        // Only created if user hasn't defined their own DataSource
        return new HikariDataSource();
    }
}
```

**Why This is Powerful**:
```java
// Spring Boot provides default DataSource
// But if you define your own:

@Configuration
public class MyCustomConfig {
    
    @Bean
    public DataSource dataSource() {
        // Your custom configuration
        return new MyCustomDataSource();
    }
}

// Spring Boot's default won't be created!
// Your bean takes precedence
```

### @ConditionalOnProperty

**Meaning**: Apply configuration based on property values

```java
@Configuration
@ConditionalOnProperty(
    name = "app.cache.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        // Only created if app.cache.enabled=true
        return new ConcurrentMapCacheManager();
    }
}
```

**Advanced Example**:
```java
@Configuration
public class FeatureConfiguration {
    
    // Enable feature A
    @Bean
    @ConditionalOnProperty(name = "features.a.enabled", havingValue = "true")
    public FeatureA featureA() {
        return new FeatureA();
    }
    
    // Enable feature B (default is true)
    @Bean
    @ConditionalOnProperty(
        name = "features.b.enabled", 
        havingValue = "true",
        matchIfMissing = true  // ← Enabled by default!
    )
    public FeatureB featureB() {
        return new FeatureB();
    }
}
```

### Other Common Conditional Annotations

```java
// Only if bean exists
@ConditionalOnBean(DataSource.class)

// Only in web applications
@ConditionalOnWebApplication

// Only if expression is true
@ConditionalOnExpression("${my.property} > 10")

// Only on specific OS
@ConditionalOnOS(OS.LINUX)

// Only if resource exists
@ConditionalOnResource(resources = "classpath:config.properties")
```

### Combining Conditions

```java
@Configuration
@ConditionalOnClass(RedisTemplate.class)  // Redis in classpath
@ConditionalOnProperty(name = "cache.type", havingValue = "redis")  // Property set
public class RedisCacheConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // User hasn't defined their own
    public RedisTemplate<String, Object> redisTemplate() {
        // All conditions must be true!
        return new RedisTemplate<>();
    }
}
```

---

## How Starters Work

### What is a Starter?

A starter is a dependency descriptor that brings in:
- Required libraries (dependencies)
- Auto-configuration classes
- Default properties

### Anatomy of a Starter

```
spring-boot-starter-web
├── Dependencies
│   ├── spring-web
│   ├── spring-webmvc
│   ├── tomcat-embed-core
│   └── jackson-databind
└── Auto-Configuration
    ├── WebMvcAutoConfiguration
    ├── DispatcherServletAutoConfiguration
    └── EmbeddedServletContainerAutoConfiguration
```

### Example: What Happens When You Add a Starter

```xml
<!-- 1. You add this dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

```java
// 2. Spring Boot auto-configures:

// Embedded Tomcat server
@Bean
@ConditionalOnClass(Tomcat.class)
public TomcatServletWebServerFactory tomcatFactory() {
    return new TomcatServletWebServerFactory();
}

// DispatcherServlet
@Bean
@ConditionalOnMissingBean
public DispatcherServlet dispatcherServlet() {
    return new DispatcherServlet();
}

// Jackson for JSON
@Bean
@ConditionalOnClass(ObjectMapper.class)
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}
```

### Common Starters and What They Include

```java
// spring-boot-starter-data-jpa
// Brings: Hibernate, Spring Data JPA, JDBC
@ConditionalOnClass({ EntityManager.class, JpaRepository.class })

// spring-boot-starter-security
// Brings: Spring Security, authentication filters
@ConditionalOnClass({ DefaultAuthenticationEventPublisher.class })

// spring-boot-starter-actuator
// Brings: Health checks, metrics endpoints
@ConditionalOnClass({ HealthEndpoint.class })
```

---

## Custom Starter Creation

Let's create a custom starter from scratch!

### Project Structure

```
my-custom-starter/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/starter/
│       │       ├── config/
│       │       │   └── MyServiceAutoConfiguration.java
│       │       └── service/
│       │           └── MyService.java
│       └── resources/
│           └── META-INF/
│               └── spring.factories
└── pom.xml
```

### Step 1: Create the Service

```java
// MyService.java
package com.example.starter.service;

public class MyService {
    
    private String message;
    
    public MyService(String message) {
        this.message = message;
    }
    
    public void doSomething() {
        System.out.println("MyService says: " + message);
    }
}
```

### Step 2: Create Properties Class

```java
// MyServiceProperties.java
package com.example.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {
    
    private String message = "Hello from MyService!";
    private boolean enabled = true;
    
    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

### Step 3: Create Auto-Configuration Class

```java
// MyServiceAutoConfiguration.java
package com.example.starter.config;

import com.example.starter.service.MyService;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnClass(MyService.class)  // Only if MyService in classpath
@ConditionalOnProperty(
    name = "myservice.enabled", 
    havingValue = "true",
    matchIfMissing = true  // Enabled by default
)
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // User can override
    public MyService myService(MyServiceProperties properties) {
        return new MyService(properties.getMessage());
    }
}
```

### Step 4: Create spring.factories

```properties
# src/main/resources/META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.starter.config.MyServiceAutoConfiguration
```

### Step 5: pom.xml

```xml
<project>
    <groupId>com.example</groupId>
    <artifactId>my-custom-starter</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### Step 6: Using Your Custom Starter

```xml
<!-- In another project's pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-custom-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```properties
# application.properties
myservice.enabled=true
myservice.message=Custom message from properties!
```

```java
// Using the auto-configured service
@RestController
public class MyController {
    
    @Autowired
    private MyService myService;  // Auto-injected!
    
    @GetMapping("/test")
    public String test() {
        myService.doSomething();
        return "Check console!";
    }
}
```

---

## Boot Startup Lifecycle

Understanding the exact order of events during Spring Boot startup.

### The Complete Lifecycle

```
1. main() method starts
   ↓
2. SpringApplication.run() called
   ↓
3. SpringApplicationRunListeners notified (starting)
   ↓
4. Environment prepared
   ↓
5. Banner printed
   ↓
6. ApplicationContext created
   ↓
7. ApplicationContext prepared
   ↓
8. @Configuration classes processed
   ↓
9. Auto-configuration classes loaded
   ↓
10. Conditional annotations evaluated
   ↓
11. Beans created and injected
   ↓
12. ApplicationContext refreshed
   ↓
13. ApplicationRunner & CommandLineRunner executed
   ↓
14. SpringApplicationRunListeners notified (started)
   ↓
15. Application ready!
```

### Detailed Step-by-Step

```java
// 1. Application Entry Point
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        // 2. SpringApplication.run() starts everything
        SpringApplication.run(MyApplication.class, args);
    }
}
```

```java
// 3. What happens inside SpringApplication.run()
public class SpringApplication {
    
    public ConfigurableApplicationContext run(String... args) {
        
        // Step 1: Prepare environment
        ConfigurableEnvironment environment = prepareEnvironment(listeners, args);
        
        // Step 2: Print banner
        printBanner(environment);
        
        // Step 3: Create ApplicationContext
        context = createApplicationContext();
        
        // Step 4: Prepare context
        prepareContext(context, environment, listeners, args);
        
        // Step 5: Refresh context (load beans)
        refreshContext(context);
        
        // Step 6: After refresh
        afterRefresh(context, args);
        
        // Step 7: Call runners
        callRunners(context, args);
        
        return context;
    }
}
```

### Hooking into Lifecycle Events

```java
// 1. ApplicationRunner - runs after context is ready
@Component
public class MyApplicationRunner implements ApplicationRunner {
    
    @Override
    public void run(ApplicationArguments args) {
        System.out.println("Application started! Arguments: " + args);
    }
}
```

```java
// 2. CommandLineRunner - alternative to ApplicationRunner
@Component
public class MyCommandLineRunner implements CommandLineRunner {
    
    @Override
    public void run(String... args) {
        System.out.println("Command line args: " + String.join(", ", args));
    }
}
```

```java
// 3. ApplicationListener - listen to context events
@Component
public class MyApplicationListener 
    implements ApplicationListener<ApplicationReadyEvent> {
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("Application is ready!");
    }
}
```

```java
// 4. @EventListener - alternative approach
@Component
public class StartupEventHandler {
    
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        System.out.println("Ready event received!");
    }
    
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefresh() {
        System.out.println("Context refreshed!");
    }
}
```

### Execution Order Example

```java
@SpringBootApplication
public class LifecycleDemo {
    
    public static void main(String[] args) {
        System.out.println("1. Main method started");
        SpringApplication.run(LifecycleDemo.class, args);
        System.out.println("6. Main method ending");
    }
    
    @Bean
    public CommandLineRunner runner() {
        return args -> System.out.println("4. CommandLineRunner executed");
    }
}

@Component
class MyListener implements ApplicationListener<ApplicationReadyEvent> {
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("5. Application ready!");
    }
}

@Configuration
class Config {
    
    @PostConstruct
    public void init() {
        System.out.println("2. @PostConstruct called");
    }
    
    @Bean
    public MyBean myBean() {
        System.out.println("3. Bean created");
        return new MyBean();
    }
}
```

**Output**:
```
1. Main method started
2. @PostConstruct called
3. Bean created
4. CommandLineRunner executed
5. Application ready!
6. Main method ending
```

---

## ApplicationContext vs WebApplicationContext

### ApplicationContext

**What**: The core container for Spring beans in non-web applications

**Use Case**: Standalone applications, batch jobs, scheduled tasks

```java
// Creating ApplicationContext manually
public class StandaloneApp {
    
    public static void main(String[] args) {
        ApplicationContext context = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        MyService service = context.getBean(MyService.class);
        service.doWork();
    }
}
```

**Characteristics**:
- No servlet-related beans
- No HTTP request/response handling
- Simpler bean lifecycle
- Used for background processing

### WebApplicationContext

**What**: Extended ApplicationContext with web-specific features

**Use Case**: Web applications, REST APIs, MVC applications

```java
// Spring Boot automatically creates WebApplicationContext
@SpringBootApplication
public class WebApp {
    public static void main(String[] args) {
        // Creates WebApplicationContext automatically
        SpringApplication.run(WebApp.class, args);
    }
}
```

**Characteristics**:
- Contains servlet-related beans (DispatcherServlet, etc.)
- Manages web scopes (request, session)
- Has access to ServletContext
- Handles HTTP requests

### Key Differences

```java
// 1. Bean Scopes Available

// ApplicationContext scopes:
@Scope("singleton")  ✓
@Scope("prototype")  ✓
@Scope("request")    ✗
@Scope("session")    ✗

// WebApplicationContext scopes:
@Scope("singleton")  ✓
@Scope("prototype")  ✓
@Scope("request")    ✓  // New!
@Scope("session")    ✓  // New!
@Scope("application") ✓  // New!
```

```java
// 2. Request-Scoped Bean (WebApplicationContext only)
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, 
       proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean {
    
    private String requestId = UUID.randomUUID().toString();
    
    public String getRequestId() {
        return requestId;
    }
}

// New instance for each HTTP request!
```

```java
// 3. Accessing ServletContext (WebApplicationContext only)
@Component
public class MyWebComponent {
    
    @Autowired
    private ServletContext servletContext;  // Only in web context
    
    public void doSomething() {
        String realPath = servletContext.getRealPath("/");
    }
}
```

### Determining Context Type

```java
@Component
public class ContextChecker {
    
    @Autowired
    private ApplicationContext context;
    
    public void checkContextType() {
        if (context instanceof WebApplicationContext) {
            System.out.println("Running in WEB context");
            WebApplicationContext webContext = (WebApplicationContext) context;
            ServletContext servletContext = webContext.getServletContext();
        } else {
            System.out.println("Running in STANDALONE context");
        }
    }
}
```

### Hierarchy Example

```
Root ApplicationContext
    │
    ├── Common Beans (Services, Repositories)
    │
    └── WebApplicationContext (Child)
        │
        ├── Web Beans (Controllers, Filters)
        ├── DispatcherServlet
        └── View Resolvers
```

```java
// Parent-child context setup
@Configuration
public class RootConfig {
    // Shared beans (database, services)
}

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
    // Web-specific beans (controllers)
}
```

---

## Best Practices

### 1. Understanding Auto-Configuration

```java
// ✓ GOOD: Know what's being auto-configured
// Use debug mode to see what's happening
// application.properties:
// debug=true

// Check auto-configuration report in logs
```

### 2. Override Auto-Configuration Carefully

```java
// ✓ GOOD: Override only what you need
@Bean
@ConditionalOnMissingBean
public DataSource dataSource() {
    // Your custom configuration
}

// ✗ BAD: Disabling entire auto-configuration
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

### 3. Use Conditional Annotations Wisely

```java
// ✓ GOOD: Clear, specific conditions
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")

// ✗ BAD: Vague conditions
@ConditionalOnExpression("#{systemProperties['os.name'].contains('Windows')}")
```

### 4. Custom Starter Naming

```
✓ GOOD: mycompany-myfeature-spring-boot-starter
✗ BAD: spring-boot-starter-myfeature (reserved for official starters)
```

### 5. Document Your Starters

```java
// Include README.md with:
// - What it auto-configures
// - Required dependencies
// - Configuration properties
// - Examples
```

### 6. Lifecycle Event Handling

```java
// ✓ GOOD: Use specific events
@EventListener(ApplicationReadyEvent.class)

// ✗ BAD: Using generic events
@EventListener(ContextRefreshedEvent.class) // Fires multiple times
```

### 7. Testing Auto-Configuration

```java
@SpringBootTest
@TestPropertySource(properties = {
    "myservice.enabled=true",
    "myservice.message=test"
})
class MyServiceAutoConfigurationTest {
    
    @Autowired(required = false)
    private MyService myService;
    
    @Test
    void testAutoConfiguration() {
        assertNotNull(myService);
    }
}
```

---

## Summary Cheat Sheet

### Auto-Configuration Flow
```
@SpringBootApplication
  → @EnableAutoConfiguration
    → AutoConfigurationImportSelector
      → Reads META-INF/spring.factories
        → Loads configuration classes
          → Evaluates @Conditional annotations
            → Creates beans
```

### Key Annotations
```java
@EnableAutoConfiguration      // Enable auto-config
@ConditionalOnClass          // If class exists
@ConditionalOnMissingBean    // If bean not defined
@ConditionalOnProperty       // If property matches
@ConfigurationProperties     // Bind properties
```

### Starter Components
```
1. Dependencies (pom.xml)
2. Auto-configuration class
3. spring.factories file
4. Properties class (optional)
5. README.md
```

### Context Types
```
ApplicationContext       → Standalone apps
WebApplicationContext   → Web apps
  ↳ Extra: request/session scopes
  ↳ Extra: ServletContext access
```

---

## Additional Resources

### Debugging Auto-Configuration

```properties
# application.properties
debug=true
logging.level.org.springframework.boot.autoconfigure=DEBUG
```

### Viewing Auto-Configuration Report

Look for this in console output:
```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required classes (DataSource)

Negative matches:
-----------------
   MongoAutoConfiguration did not match:
      - @ConditionalOnClass did not find required class (MongoClient)
```

### Common Commands

```bash
# View effective auto-configuration
mvn dependency:tree

# Check spring.factories content
jar tf myapp.jar | grep spring.factories

# Extract and view
jar xf myapp.jar META-INF/spring.factories
```

---

## Quick Reference

| Concept | Purpose | Example |
|---------|---------|---------|
| Auto-Configuration | Automatically configure beans | DataSource, JPA |
| @ConditionalOnClass | Check if class exists | `@ConditionalOnClass(DataSource.class)` |
| @ConditionalOnMissingBean | Create if not exists | `@ConditionalOnMissingBean(DataSource.class)` |
| spring.factories | List auto-configs | `EnableAutoConfiguration=...` |
| Starter | Bundle dependencies | `spring-boot-starter-web` |
| ApplicationContext | Non-web container | Batch, scheduled jobs |
| WebApplicationContext | Web container | REST APIs, MVC |

---

**Remember**: Spring Boot internals are not magic - they're well-designed patterns that you can understand, extend, and even create yourself!

**Master these concepts, and you'll move from being a Spring Boot user to a Spring Boot expert.** 🚀
