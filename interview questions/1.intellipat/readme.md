# Spring Boot Interview Questions & Answers

## 📚 Basic Level Questions

### 1. What is Spring Boot?
**Answer:** Spring Boot is a Java framework that makes it easy to create web applications. It removes the complex setup and configuration that traditional Java web development requires. Think of it as a "ready-to-use" version of Spring framework.

**Key Benefits:**
- Less code to write
- Faster development
- Built-in server (no need to install Tomcat separately)
- Easy to test and deploy

### 2. How is Spring Boot different from traditional Spring Framework?
| Traditional Spring | Spring Boot |
|-------------------|-------------|
| Lots of XML configuration | Minimal configuration |
| Manual dependency management | Auto-configuration |
| Need separate server setup | Built-in embedded server |
| Complex project setup | Simple project creation |

### 3. What is Auto-Configuration in Spring Boot?
**Answer:** Auto-configuration is Spring Boot's "smart" feature that automatically sets up your application based on the dependencies you include.

**Example:** If you add a database dependency, Spring Boot automatically:
- Creates database connection
- Sets up transaction management
- Configures data source

### 4. What are Spring Boot Starters?
**Answer:** Starters are pre-packaged bundles of dependencies that work well together.

**Common Starters:**
- `spring-boot-starter-web` - For web applications
- `spring-boot-starter-data-jpa` - For database operations
- `spring-boot-starter-security` - For security features
- `spring-boot-starter-test` - For testing

### 5. What is an Embedded Server?
**Answer:** An embedded server is a web server (like Tomcat) that comes built into your Spring Boot application. You don't need to install or configure a separate server.

**Benefits:**
- Easy to run: Just use `java -jar myapp.jar`
- No server installation needed
- Consistent across different environments

### 6. What is Spring Boot Actuator?
**Answer:** Actuator provides ready-made endpoints to monitor and manage your application in production.

**Useful Endpoints:**
- `/health` - Check if app is running
- `/metrics` - Performance statistics
- `/info` - Application information
- `/env` - Environment details

### 7. What is Spring Boot DevTools?
**Answer:** DevTools makes development faster by automatically restarting your application when you make changes.

**Features:**
- Automatic restart when code changes
- Live reload in browser
- Better development experience

### 8. How do you create a Spring Boot application?
**Answer:** Use Spring Initializer website (start.spring.io):

**Steps:**
1. Go to start.spring.io
2. Choose project type (Maven/Gradle)
3. Select Java version
4. Add dependencies you need
5. Download and extract the project
6. Open in your IDE

## 🔧 Intermediate Level Questions

### 9. Explain @SpringBootApplication annotation
**Answer:** This is a meta-annotation that combines three important annotations:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**It includes:**
- `@Configuration` - Marks class as configuration source
- `@EnableAutoConfiguration` - Enables auto-configuration
- `@ComponentScan` - Scans for Spring components

### 10. What is @RestController?
**Answer:** `@RestController` is used to create REST APIs. It combines `@Controller` and `@ResponseBody`.

```java
@RestController
public class UserController {
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
}
```

### 11. How do you handle configuration in Spring Boot?
**Answer:** Spring Boot uses `application.properties` or `application.yml` files for configuration.

**application.properties example:**
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=password
```

**application.yml example:**
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
```

### 12. What is @Value annotation?
**Answer:** `@Value` injects property values from configuration files into your Java variables.

```java
@Component
public class DatabaseConfig {
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${app.name:MyApp}")  // Default value is MyApp
    private String appName;
}
```

### 13. Explain Spring Boot's layered architecture annotations
**Answer:**
- `@Component` - Generic Spring component
- `@Service` - Business logic layer
- `@Repository` - Data access layer
- `@Controller` - Web layer (handles HTTP requests)

```java
@Service
public class UserService {
    // Business logic here
}

@Repository
public class UserRepository {
    // Database operations here
}

@Controller
public class UserController {
    // Handle web requests here
}
```

### 14. How do you handle exceptions globally in Spring Boot?
**Answer:** Use `@ControllerAdvice` and `@ExceptionHandler`:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404).body("User not found: " + ex.getMessage());
    }
}
```

### 15. What are the different ways to run a Spring Boot application?
**Answer:**
1. **IDE:** Right-click and run main method
2. **Command line:** `java -jar myapp.jar`
3. **Maven:** `mvn spring-boot:run`
4. **Gradle:** `gradle bootRun`

## 🚀 Advanced Level Questions

### 16. How do you secure a Spring Boot application?
**Answer:** Use Spring Security starter and implement security configurations:

```java
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .antMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .httpBasic();
        return http.build();
    }
}
```

**Security Best Practices:**
- Use HTTPS in production
- Implement JWT for stateless authentication
- Use bcrypt for password hashing
- Validate all inputs to prevent injection attacks

### 17. What are the different packaging options in Spring Boot?
**Answer:**

1. **Executable JAR:** 
   - Default option
   - Contains embedded server
   - Run with `java -jar app.jar`

2. **WAR file:**
   - For traditional application servers
   - Deploy to external Tomcat/WebLogic

3. **Docker Image:**
   - For containerized deployment
   - Easy scaling and management

4. **Native Image:**
   - Compiled to native code
   - Faster startup, lower memory usage

### 18. How do you test Spring Boot applications?
**Answer:** Spring Boot provides several testing annotations:

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private UserRepository userRepository;
    
    @Test
    void testFindUser() {
        // Test implementation
    }
}
```

**Testing Annotations:**
- `@SpringBootTest` - Full application context
- `@MockBean` - Mock Spring beans
- `@DataJpaTest` - Test JPA repositories only
- `@WebMvcTest` - Test web layer only

### 19. How do you monitor Spring Boot applications in production?
**Answer:** Use Spring Boot Actuator with external monitoring tools:

**Enable Actuator:**
```properties
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always
```

**Integration with monitoring tools:**
- **Prometheus** - For metrics collection
- **Grafana** - For visualization
- **ELK Stack** - For log analysis
- **New Relic/AppDynamics** - For APM

### 20. What is the difference between @Component, @Service, @Repository, and @Controller?
**Answer:**

| Annotation | Purpose | Layer |
|------------|---------|-------|
| @Component | Generic Spring component | Any |
| @Service | Business logic | Service layer |
| @Repository | Data access, exception translation | Data layer |
| @Controller | Handle web requests | Presentation layer |

**Note:** `@Service`, `@Repository`, and `@Controller` are specialized versions of `@Component`.

## 🎯 Quick Tips for Interview Success

### Key Points to Remember:
1. **Auto-configuration** is Spring Boot's main advantage
2. **Starters** solve dependency management problems
3. **Embedded servers** make deployment easier
4. **Actuator** is essential for production monitoring
5. **Configuration externalization** supports different environments
6. **Spring Security** integration provides comprehensive security
7. **Testing support** ensures application quality

### Common Pitfalls to Avoid:
- Don't confuse Spring Boot with Spring Framework
- Remember that Spring Boot is opinionated but configurable
- Understand the difference between JAR and WAR packaging
- Know when to use different testing annotations
- Be familiar with both properties and YAML configuration formats

### 21. How do you handle different environments (dev, test, prod) in Spring Boot?
**Answer:** Use Spring Profiles to manage environment-specific configurations:

**application.properties:**
```properties
spring.profiles.active=dev
```

**application-dev.properties:**
```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:devdb
logging.level.com.myapp=DEBUG
```

**application-prod.properties:**
```properties
server.port=80
spring.datasource.url=jdbc:mysql://prod-server:3306/proddb
logging.level.com.myapp=WARN
```

**In Code:**
```java
@Component
@Profile("prod")
public class ProductionService {
    // Production-specific implementation
}
```

### 22. What is dependency injection in Spring Boot?
**Answer:** Dependency injection is a design pattern where Spring automatically provides the dependencies your class needs.

```java
@Service
public class UserService {
    
    // Field injection (not recommended)
    @Autowired
    private UserRepository userRepository;
    
    // Constructor injection (recommended)
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**Types of Injection:**
- **Constructor Injection** - Best practice, ensures required dependencies
- **Setter Injection** - For optional dependencies
- **Field Injection** - Simple but harder to test

### 23. What is the difference between @RequestMapping and @GetMapping?
**Answer:** 

| @RequestMapping | @GetMapping/@PostMapping |
|----------------|-------------------------|
| Generic mapping for any HTTP method | Specific to HTTP method |
| `@RequestMapping(value="/users", method=GET)` | `@GetMapping("/users")` |
| More verbose | Cleaner and more readable |

```java
@RestController
public class UserController {
    
    // Old way
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public List<User> getUsers() { }
    
    // New way (preferred)
    @GetMapping("/users")
    public List<User> getUsers() { }
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) { }
}
```

### 24. How do you handle database operations in Spring Boot?
**Answer:** Spring Boot provides multiple ways to handle database operations:

**1. Spring Data JPA (Most Common):**
```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByName(String name);
    
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    User findByEmail(String email);
}
```

**2. Using @Transactional:**
```java
@Service
@Transactional
public class UserService {
    
    @Transactional(readOnly = true)
    public User findUser(Long id) {
        return userRepository.findById(id);
    }
    
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
```

### 25. What is the purpose of @ConfigurationProperties?
**Answer:** `@ConfigurationProperties` binds external configuration properties to Java objects in a type-safe way.

**application.yml:**
```yaml
app:
  database:
    host: localhost
    port: 3306
    name: myapp
  security:
    jwt-expiration: 86400
    secret-key: mySecretKey
```

**Configuration Class:**
```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
    private Database database = new Database();
    private Security security = new Security();
    
    // getters and setters
    
    public static class Database {
        private String host;
        private int port;
        private String name;
        // getters and setters
    }
    
    public static class Security {
        private long jwtExpiration;
        private String secretKey;
        // getters and setters
    }
}
```

### 26. How do you implement caching in Spring Boot?
**Answer:** Spring Boot provides built-in caching support:

**1. Enable Caching:**
```java
@SpringBootApplication
@EnableCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**2. Use Caching Annotations:**
```java
@Service
public class UserService {
    
    @Cacheable("users")
    public User findUser(Long id) {
        // This method result will be cached
        return userRepository.findById(id);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        // This will remove the cached entry
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsers() {
        // This will clear all cached entries
    }
}
```

**3. Configure Cache Provider:**
```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

### 27. What is the difference between @Component and @Bean?
**Answer:**

| @Component | @Bean |
|------------|--------|
| Class-level annotation | Method-level annotation |
| Spring manages the entire class | Spring manages the method return value |
| Used for your own classes | Used for third-party classes or complex creation logic |

```java
// @Component example
@Component
public class UserService {
    // Spring manages this class
}

// @Bean example
@Configuration
public class AppConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        // Custom configuration for third-party class
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### 28. How do you handle file uploads in Spring Boot?
**Answer:** Spring Boot makes file uploads simple with `MultipartFile`:

```java
@RestController
public class FileController {
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }
        
        try {
            // Save file to local directory
            String fileName = file.getOriginalFilename();
            Path path = Paths.get("uploads/" + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            
            return ResponseEntity.ok("File uploaded successfully: " + fileName);
            
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Could not upload file: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload-multiple")
    public ResponseEntity<String> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        
        for (MultipartFile file : files) {
            // Process each file
        }
        
        return ResponseEntity.ok("Files uploaded successfully");
    }
}
```

**Configuration:**
```properties
# Maximum file size
spring.servlet.multipart.max-file-size=10MB
# Maximum request size
spring.servlet.multipart.max-request-size=10MB
```

### 29. What is Spring Boot CLI and how is it used?
**Answer:** Spring Boot CLI (Command Line Interface) is a command-line tool that allows you to quickly prototype Spring applications using Groovy scripts.

**Installation:**
```bash
# Using SDKMAN
sdk install springboot

# Using Homebrew (Mac)
brew tap pivotal/tap
brew install springboot
```

**Example Usage:**
```groovy
// hello.groovy
@RestController
class HelloController {
    
    @RequestMapping("/")
    String hello() {
        return "Hello World from Spring Boot CLI!"
    }
}
```

**Run the application:**
```bash
spring run hello.groovy
```

**Benefits:**
- Rapid prototyping
- No need for build tools (Maven/Gradle)
- Automatic dependency resolution
- Great for testing concepts quickly

### 30. How do you deploy Spring Boot applications?
**Answer:** Spring Boot offers multiple deployment options:

**1. Executable JAR (Recommended):**
```bash
# Build the JAR
mvn clean package

# Run the application
java -jar target/myapp-1.0.0.jar

# Run with specific profile
java -jar target/myapp-1.0.0.jar --spring.profiles.active=prod

# Run with JVM options
java -Xmx512m -jar target/myapp-1.0.0.jar
```

**2. WAR Deployment:**
```java
@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**3. Docker Deployment:**
```dockerfile
FROM openjdk:11-jre-slim

COPY target/myapp-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build and run
docker build -t myapp .
docker run -p 8080:8080 myapp
```

**4. Cloud Deployment:**
- **AWS:** Elastic Beanstalk, ECS, Lambda
- **Azure:** App Service, Container Instances
- **Google Cloud:** App Engine, Cloud Run
- **Heroku:** Git-based deployment

**5. Kubernetes Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: myapp:latest
        ports:
        - containerPort: 8080
```

## 🎯 Advanced Tips for Interview Success

### Performance Optimization:
- Use `@Lazy` for expensive beans
- Implement connection pooling for databases
- Use caching strategically
- Profile your application with Spring Boot Actuator

### Production Readiness Checklist:
- ✅ Health checks configured
- ✅ Metrics and monitoring in place
- ✅ Proper logging configuration
- ✅ Security measures implemented
- ✅ Database connection pooling
- ✅ Error handling and graceful degradation
- ✅ Environment-specific configurations

### Common Interview Scenarios:
1. **"How would you optimize a slow Spring Boot application?"**
   - Check database queries and add indexes
   - Implement caching
   - Use async processing for heavy operations
   - Monitor with Actuator and profiling tools

2. **"How do you ensure your Spring Boot app is secure?"**
   - Use Spring Security
   - Implement HTTPS
   - Validate all inputs
   - Use JWT for stateless authentication
   - Keep dependencies updated

3. **"How do you handle microservices communication?"**
   - Use RestTemplate or WebClient
   - Implement circuit breakers (Hystrix/Resilience4j)
   - Use service discovery (Eureka)
   - Implement proper error handling and retries

---

*This comprehensive guide covers all 30 essential Spring Boot interview questions. Master these concepts with hands-on practice to excel in your interviews!*
