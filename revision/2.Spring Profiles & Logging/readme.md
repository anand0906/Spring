# Spring Profiles & Logging - Revision Notes

## Table of Contents

1. [Spring Profiles](#1-spring-profiles)
   - [Purpose](#purpose)
   - [Configuration Steps](#configuration-steps)
   - [Using @Profile Annotation](#using-profile-annotation)
   - [Environment-Based Properties](#environment-based-properties)
   - [Setting Active Profiles](#setting-active-profiles)
2. [Spring Logging](#2-spring-logging)
   - [What is Logging?](#what-is-logging)
   - [Logging Frameworks](#logging-frameworks)
   - [Logging Levels](#logging-levels)
   - [Logback Configuration](#logback-configuration)
   - [Using Log4J](#using-log4j)

---

## 1. Spring Profiles

### Purpose

Spring Profiles help classify classes and properties files for different environments (dev, test, prod). Based on active profiles, Spring chooses appropriate beans and configurations.

### Configuration Steps

1. Identify beans for specific profiles (optional)
2. Create environment-based properties files
3. Set active profiles

---

### Using @Profile Annotation

#### Class Level (Components, Services, Repositories)

```java
@Profile("dev")
@Component
@Aspect
public class LoggingAspect { 
    // Runs only in 'dev' environment
}
```

#### Method Level (Configuration Classes)

```java
@Configuration
public class SpringConfiguration {
    
    @Bean("customerService")
    @Profile("dev")
    public CustomerService customerServiceDev() {
        CustomerService service = new CustomerService();
        service.setName("Development-Customer");
        return service;
    }
    
    @Bean("customerService")
    @Profile("prod")
    public CustomerService customerServiceProd() {
        CustomerService service = new CustomerService();
        service.setName("Production-Customer");
        return service;
    }
}
```

#### Using NOT Operator

```java
@Profile("!test")  // Runs in all environments EXCEPT test
@Configuration
@ComponentScan(basePackages="com.infy.service")
public class SpringConfiguration { }
```

**Note:** Beans without `@Profile` are available in ALL environments.

---

### Environment-Based Properties

**Naming Convention:** `application-<profile>.properties`

#### application-dev.properties
```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1522:devDB
spring.datasource.username=root
spring.datasource.password=root
logging.level.org.springframework.web=ERROR
```

#### application-test.properties
```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1522:testDB
spring.datasource.username=root
logging.level.org.springframework.web=DEBUG
```

#### application-prod.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/prodDB?useSSL=false
spring.datasource.username=root
logging.level.org.springframework.web=INFO
```

---

### Setting Active Profiles

#### Method 1: application.properties
```properties
# Single profile
spring.profiles.active=dev

# Multiple profiles
spring.profiles.active=dev,prod
```

#### Method 2: JVM System Parameter
```bash
# VM arguments in Run Configuration
-Dspring.profiles.active=dev

# Command line
--spring.profiles.active=dev
```

#### Method 3: Programmatic
```java
public static void main(String[] args) {
    System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "prod");
}
```

---

## 2. Spring Logging

### What is Logging?

**Logging** = Writing log messages to a central location during program execution to track events, exceptions, and application flow.

**Benefits:**
- Record errors and unusual circumstances
- Track application execution
- Quick problem diagnosis and debugging
- Application maintenance

---

### Logging Frameworks

**Popular Frameworks:**
- JDK Logging API
- Apache Log4j
- Commons Logging API

**Spring Boot Default:**
- Uses **Apache Commons Logging**
- Default implementation: **Logback** (when using Spring Boot starters)
- Supports: Java Util Logging, Log4j2, Logback

---

### Logging Levels

| Level   | Purpose | Description |
|---------|---------|-------------|
| **ALL** | Everything | All levels (for custom levels) |
| **TRACE** | Most detailed | Fine-grained debugging info |
| **DEBUG** | Debugging | Debugging information |
| **INFO** | ✅ Default | Informational messages |
| **WARN** | ✅ Default | Potentially harmful situations |
| **ERROR** | ✅ Default | Error messages |
| **FATAL** | Critical | Severe errors leading to abort |
| **OFF** | Disable | Turn off all logging |

**Spring Boot Default:** ERROR, WARN, INFO

---

### Logback Configuration

Configure logging in `application.properties`:

#### 1. Logging Levels
```properties
# Set logging level for specific package
logging.level.com.infy=INFO

# Set root logging level
logging.level.root=WARN
```

#### 2. Logging Pattern - Console
```properties
logging.pattern.console=%d{yyyy-MMM-dd HH:mm:ss a} [%t] %-5level %logger{36} - %msg%n
```

**Output Example:**
```
2025-Jan-31 10:30:45 AM [main] INFO  c.i.service.CustomerService - User logged in
```

#### 3. Logging Pattern - File
```properties
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

#### 4. Logging to File
```properties
# File name (in current directory)
logging.file.name=Error.log

# File path (custom location)
logging.file.path=logs/Error.log
```

**Note:** By default, Spring Boot logs ONLY to console, not files.

---

### Using Log4J

#### Step 1: Exclude Default Logging & Add Log4J2

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Add Log4j2 Dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

#### Step 2: Create log4j2.properties

Place in root classpath - Spring Boot auto-detects it.

#### Step 3: Use Logger in Code

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyService {
    private static final Logger logger = LogManager.getLogger(MyService.class);
    
    public void someMethod() {
        logger.info("Informational message");
        logger.error("Error message");
        logger.debug("Debug message");
    }
}
```

---

## Quick Reference Card

### Profiles Quick Commands
```bash
# Set via properties
spring.profiles.active=dev

# Set via JVM
-Dspring.profiles.active=prod

# Set via command line
--spring.profiles.active=test
```

### Logging Quick Setup
```properties
# Level
logging.level.com.myapp=DEBUG

# Console pattern
logging.pattern.console=%d{HH:mm:ss.SSS} %-5level - %msg%n

# File
logging.file.name=app.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Pattern Placeholders
- `%d` - Date/Time
- `%thread` - Thread name
- `%-5level` - Log level (left-aligned, 5 chars)
- `%logger{36}` - Logger name (max 36 chars)
- `%msg` - Log message
- `%n` - New line

---

## Key Takeaways

✅ **Profiles** = Environment-specific configuration (dev/test/prod)  
✅ **@Profile** can be used at class or method level  
✅ Multiple profiles can be active simultaneously  
✅ **Logback** = Spring Boot's default logging framework  
✅ **Default levels** = ERROR, WARN, INFO  
✅ Logging to files requires explicit configuration  
✅ **Log4J2** requires excluding default logging dependency

---

**Remember:** Proper logging and profile management are essential for production-ready Spring applications!
