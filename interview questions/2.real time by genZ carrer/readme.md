# Spring Boot Interview Questions - Complete Guide

## Table of Contents
- [Microservices Architecture](#microservices-architecture)
- [Caching](#caching)
- [Performance Optimization](#performance-optimization)
- [API Design & Versioning](#api-design--versioning)
- [Data Access Layer](#data-access-layer)
- [Configuration & Annotations](#configuration--annotations)
- [Security](#security)
- [Testing](#testing)
- [Profiles & Configuration](#profiles--configuration)
- [Aspect-Oriented Programming](#aspect-oriented-programming)
- [Spring Cloud](#spring-cloud)
- [Deployment & DevOps](#deployment--devops)
- [Error Handling](#error-handling)
- [Advanced Topics](#advanced-topics)

---

## Microservices Architecture

### 1. How would you handle inter-service communication in a microservice architecture using Spring Boot?

**Answer:**
- **Simple direct communication**: Use RestTemplate for synchronous request-response communication
- **Complex interactions**: Use Feign Client for declarative REST client with cleaner code
- **Asynchronous communication**: Use message brokers like RabbitMQ or Kafka for non-blocking communication
- **Benefits**: Ensures robust, flexible communication system between microservices

### 2. What is Spring Cloud and how is it useful for building microservices?

**Answer:**
Spring Cloud is a component of the Spring framework that helps manage microservices. It provides:
- Service discovery and registration
- Load balancing
- Circuit breakers
- Configuration management
- API Gateway functionality
- Security management across services

---

## Caching

### 3. Can you explain the caching mechanism available in Spring Boot?

**Answer:**
Spring Boot provides Spring Cache Abstraction:
- **Purpose**: Memory layer for storing frequently used data
- **Benefits**: Saves time and resources by avoiding expensive operations
- **How it works**: Stores results of database queries or computations for quick retrieval
- **Implementation**: Uses annotations like `@Cacheable`, `@CacheEvict`, `@CachePut`

### 4. How would you implement caching in a Spring Boot application?

**Answer:**
```java
// 1. Add dependency
// spring-boot-starter-cache

// 2. Enable caching
@EnableCaching
@SpringBootApplication
public class Application {}

// 3. Use cacheable annotation
@Cacheable("products")
public Product findProduct(Long id) {
    return productRepository.findById(id);
}

// 4. Configure cache provider (optional)
// EHCache, Hazelcast, or default ConcurrentHashMap
```

### 5. Explain the difference between cache eviction and cache expiration

**Answer:**
- **Cache Eviction**: Removes data to free up space based on policies (LRU, FIFO)
- **Cache Expiration**: Removes data based on predetermined Time-To-Live (TTL)
- **Purpose**: Eviction manages cache size, expiration ensures data freshness

---

## Performance Optimization

### 6. Your Spring Boot application is experiencing performance issues under high load. What steps would you take?

**Answer:**
1. **Identify issues**: Use Spring Boot Actuator and monitoring tools
2. **Analyze logs**: Look for patterns and errors under load
3. **Performance testing**: Replicate issues and use profilers
4. **Optimization strategies**:
   - Database query optimization
   - Implement caching
   - Use horizontal scaling
   - Code optimization
5. **Continuous monitoring**: Prevent future issues

### 7. What strategies would you use to optimize the performance of a Spring Boot application?

**Answer:**
- Implement caching for frequently accessed data
- Optimize database queries and use connection pooling
- Use asynchronous methods for non-critical operations
- Implement load balancing for high traffic
- Use WebFlux for handling concurrent connections
- Enable HTTP response compression
- Configure stateless sessions

---

## API Design & Versioning

### 8. What are the best practices for versioning REST APIs in a Spring Boot application?

**Answer:**
1. **URL Versioning**: `/api/v1/products`
2. **Header Versioning**: Custom header to specify version
3. **Media Type Versioning**: Content negotiation using Accept header
4. **Parameter Versioning**: Version as request parameter

### 9. How would you handle multiple beans of the same type?

**Answer:**
```java
// Using @Qualifier
@Autowired
@Qualifier("primaryDataSource")
private DataSource dataSource;

// Using @Primary
@Primary
@Bean
public DataSource primaryDataSource() {
    return new DataSource();
}
```

---

## Data Access Layer

### 10. How does Spring Boot simplify the data access layer implementation?

**Answer:**
- **Auto-configuration**: Automatically configures DataSource and JPA
- **Repository support**: Built-in CRUD operations without boilerplate code
- **Database initialization**: Automatic schema creation and data seeding
- **Exception translation**: Converts SQL exceptions to Spring's DataAccessException
- **Multiple database support**: Easy integration with various databases

### 11. How can you implement pagination in a Spring Boot application?

**Answer:**
```java
// Repository method
Page<Product> findAll(Pageable pageable);

// Service layer
PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
Page<Product> products = productRepository.findAll(pageRequest);

// Controller
@GetMapping("/products")
public Page<Product> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
    return productService.findAll(page, size);
}
```

---

## Configuration & Annotations

### 12. What are conditional annotations and their purpose in Spring Boot?

**Answer:**
Conditional annotations create beans only when certain conditions are met:
- `@ConditionalOnClass`: Create bean only if specific class is present
- `@ConditionalOnMissingBean`: Create bean only if it doesn't exist
- `@ConditionalOnProperty`: Based on configuration properties
- **Benefits**: Makes applications flexible and adaptable

### 13. Explain the role of @EnableAutoConfiguration annotation

**Answer:**
- Tells Spring Boot to automatically configure the application based on dependencies
- Uses conditional evaluation to examine classpath, beans, and properties
- Relies on conditional annotations in auto-configuration classes
- Simplifies setup and speeds up development

### 14. What does @SpringBootApplication annotation do internally?

**Answer:**
Combines three annotations:
1. `@Configuration`: Indicates configuration class with beans
2. `@EnableAutoConfiguration`: Enables automatic configuration
3. `@ComponentScan`: Scans for components in current package

### 15. How to disable a specific auto-configuration?

**Answer:**
```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Application {
    // Application code
}
```

---

## Security

### 16. How can you secure the actuator endpoints?

**Answer:**
1. **Limit exposure**: Control which endpoints are web-accessible
2. **Use Spring Security**: Require authentication for actuator endpoints
3. **Use HTTPS**: Secure data transmission
4. **Role-based access**: Create specific roles like `ACTUATOR_ADMIN`

### 17. Explain the difference between authentication and authorization in Spring Security

**Answer:**
- **Authentication**: Verifying user identity (who you are) - like showing ID
- **Authorization**: Determining user permissions (what you can do) - access rights
- **Implementation**: Authentication comes first, then authorization based on roles

### 18. How would you secure a Spring Boot application using JSON Web Tokens?

**Answer:**
1. Generate JWT upon successful login with user details and permissions
2. Include JWT in subsequent requests (usually in Authorization header)
3. Validate JWT on each request using Spring Security filters
4. Extract user information from valid tokens
5. Benefits: Stateless, scalable, and secure

### 19. How to implement Spring Security in a Spring Boot application?

**Answer:**
```java
// 1. Add dependency
// spring-boot-starter-security

// 2. Configuration class
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .formLogin();
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user").password("{noop}password").roles("USER");
    }
}
```

---

## Testing

### 20. How do you approach testing in Spring Boot applications?

**Answer:**
Two main approaches:
1. **Unit Testing**: Test individual components in isolation
2. **Integration Testing**: Test how components work together with Spring context

### 21. Discuss the use of @SpringBootTest and @MockBean annotations

**Answer:**
- **@SpringBootTest**: Loads full Spring context for integration testing
- **@MockBean**: Creates mock version of a bean for isolated testing
- **Usage**: Use @SpringBootTest when testing component interactions, @MockBean for isolation

### 22. How can you mock external services in a Spring Boot test?

**Answer:**
```java
@SpringBootTest
class ServiceTest {
    
    @MockBean
    private ExternalService externalService;
    
    @Test
    void testServiceLogic() {
        // Define mock behavior
        when(externalService.getData()).thenReturn("mocked data");
        
        // Test your service
        String result = myService.processData();
        
        assertEquals("expected result", result);
    }
}
```

---

## Profiles & Configuration

### 23. Explain how Spring Boot profiles work

**Answer:**
Profiles allow different configurations for different environments:
- **Purpose**: Separate settings for development, testing, production
- **Files**: `application-{profile}.properties`
- **Activation**: `spring.profiles.active=dev`
- **Benefits**: Environment-specific configuration without code changes

### 24. What advantages does YAML offer over properties files?

**Answer:**
**Advantages:**
- Hierarchical configuration structure
- More readable for complex configurations
- Supports comments and documentation
- Better for nested properties

**Limitations:**
- More error-prone (space/indentation sensitive)
- Less familiar to some developers
- Parsing overhead

### 25. What does it mean that Spring Boot supports relaxed binding?

**Answer:**
Flexible property name formats:
- `server.port`
- `server-port`
- `server_port`
- `SERVER_PORT`

All are recognized as the same property, making configuration more tolerant to variations.

---

## Aspect-Oriented Programming

### 26. What is Aspect-Oriented Programming in the Spring framework?

**Answer:**
AOP helps separate cross-cutting concerns:
- **Purpose**: Handle common tasks like logging, security, transactions
- **Benefits**: Keeps main business logic clean and focused
- **Implementation**: Define aspects once and apply across multiple methods
- **Example**: Logging aspect applied to all service methods

---

## Spring Cloud

### 27. How does Spring Boot make the decision on which server to use?

**Answer:**
Based on classpath dependencies:
- Checks for specific server dependencies (Tomcat, Jetty, Undertow)
- Auto-configures the found server
- Defaults to Tomcat if no specific server dependency found
- Included in `spring-boot-starter-web`

### 28. Can you override or replace embedded Tomcat server in Spring Boot?

**Answer:**
```xml
<!-- Exclude Tomcat -->
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

<!-- Add Jetty -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

---

## Deployment & DevOps

### 29. How to deploy Spring Boot web application as JAR and WAR files?

**Answer:**
**JAR Deployment:**
```bash
mvn package
java -jar target/application.jar
```

**WAR Deployment:**
```xml
<packaging>war</packaging>
```
```java
@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }
}
```

### 30. Explain the process of creating a Docker image for a Spring Boot application

**Answer:**
```dockerfile
FROM openjdk:11-jre-slim
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build image
docker build -t my-spring-app .

# Run container
docker run -p 8080:8080 my-spring-app
```

---

## Error Handling

### 31. How to resolve white label error page in Spring Boot?

**Answer:**
1. Check URL mappings in controllers
2. Add missing request mappings
3. Create custom error pages
4. Use `@ControllerAdvice` for global error handling
5. Configure custom error controller

### 32. How to handle a 404 error in Spring Boot?

**Answer:**
```java
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == 404) {
            return "error/404";
        }
        return "error/generic";
    }
}
```

---

## Advanced Topics

### 33. How does Spring Boot make DI easier compared to traditional Spring?

**Answer:**
- **Auto-configuration**: Automatically discovers and registers beans
- **Component scanning**: Finds components based on classpath
- **Reduced XML configuration**: Minimal manual wiring required
- **Convention over configuration**: Intelligent defaults
- **Focus on business logic**: Less boilerplate configuration

### 34. How does Spring Boot support internationalization?

**Answer:**
```properties
# messages_en.properties
welcome.message=Welcome

# messages_es.properties
welcome.message=Bienvenido
```

```java
@Autowired
private MessageSource messageSource;

public String getWelcomeMessage(Locale locale) {
    return messageSource.getMessage("welcome.message", null, locale);
}
```

### 35. Can we create a non-web application in Spring Boot?

**Answer:**
Yes, by:
- Not including web starter dependencies
- Implementing `CommandLineRunner` or `ApplicationRunner`
- Using `SpringApplication.run()` without web context
- Perfect for batch processing, data migration, or CLI tools

### 36. What is Spring Boot CLI and how to execute projects using Boot CLI?

**Answer:**
Command-line tool for rapid Spring Boot development:
```bash
# Install CLI
spring --version

# Run Groovy script
spring run app.groovy

# Create project
spring init --dependencies=web,jpa my-project
```

### 37. How would you build a non-blocking reactive REST API using Spring WebFlux?

**Answer:**
```java
@RestController
public class ReactiveController {
    
    @GetMapping("/products")
    public Flux<Product> getAllProducts() {
        return productService.findAll(); // Returns Flux<Product>
    }
    
    @GetMapping("/products/{id}")
    public Mono<Product> getProduct(@PathVariable String id) {
        return productService.findById(id); // Returns Mono<Product>
    }
}
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 38. How to get the list of all beans in your Spring Boot application?

**Answer:**
```java
@Autowired
private ApplicationContext applicationContext;

public void listBeans() {
    String[] beanNames = applicationContext.getBeanDefinitionNames();
    for (String beanName : beanNames) {
        System.out.println(beanName);
    }
}
```

---

## Best Practices

### 39. What are some best practices for managing transactions in Spring Boot?

**Answer:**
1. **Use @Transactional**: Apply on service methods for automatic transaction management
2. **Service layer transactions**: Handle transactions at the service layer
3. **Proper exception handling**: Ensure rollback on exceptions
4. **Transaction boundaries**: Keep transactions as short as possible
5. **Read-only transactions**: Use for query-only operations

### 40. How would you manage externalized configuration in a microservices architecture?

**Answer:**
Use Spring Cloud Config:
1. **Config Server**: Centralized configuration management
2. **External storage**: Git repository or file system
3. **Environment-specific**: Different configs for dev/test/prod
4. **Security**: Encrypt sensitive properties
5. **Dynamic refresh**: Update configs without restart

---

*This guide covers essential Spring Boot interview questions with practical examples and detailed explanations. Each section builds upon core concepts to help you understand and implement Spring Boot applications effectively.*
