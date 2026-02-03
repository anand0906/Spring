# Spring Boot Security Integration

> **Security First**: Master Spring Security with Spring Boot

---

## Table of Contents
1. [Introduction](#introduction)
2. [Auto-Configured Spring Security](#auto-configured-spring-security)
3. [Security Filter Chain](#security-filter-chain)
4. [Stateless Authentication](#stateless-authentication)
5. [JWT Integration](#jwt-integration)
6. [OAuth2 / OpenID Connect](#oauth2--openid-connect)
7. [Method-Level Security](#method-level-security)
8. [Securing Actuator Endpoints](#securing-actuator-endpoints)
9. [Best Practices](#best-practices)

---

## Introduction

Spring Boot makes security configuration simpler while maintaining Spring Security's power and flexibility.

**Key Security Concepts**:
- **Authentication**: Who are you? (Login)
- **Authorization**: What can you do? (Permissions)
- **Encryption**: Protecting data in transit and at rest
- **CSRF**: Cross-Site Request Forgery protection
- **CORS**: Cross-Origin Resource Sharing

**Why Security Matters**:
- ✓ Protect user data
- ✓ Prevent unauthorized access
- ✓ Comply with regulations (GDPR, HIPAA)
- ✓ Maintain trust
- ✓ Prevent attacks (XSS, CSRF, SQL injection)

---

## Auto-Configured Spring Security

### Adding Spring Security

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### What Happens Automatically?

**Default Behavior**:
1. All endpoints are secured
2. Default user: `user`
3. Password: Printed in console
4. HTTP Basic authentication enabled
5. CSRF protection enabled
6. Security headers added

**Console Output**:
```
Using generated security password: a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6
```

**Testing Default Security**:
```bash
# Without credentials - 401 Unauthorized
curl http://localhost:8080/api/users

# With credentials - Success
curl -u user:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6 \
  http://localhost:8080/api/users
```

### Customizing Default User

```yaml
# application.yml
spring:
  security:
    user:
      name: admin
      password: secret123
      roles: ADMIN
```

### Disabling Default Security (Development Only)

```java
// ⚠️ DEVELOPMENT ONLY - NEVER IN PRODUCTION
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain disableSecurity(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .csrf().disable();
        
        return http.build();
    }
}
```

---

## Security Filter Chain

### Understanding the Filter Chain

```
HTTP Request
    ↓
┌───────────────────────────────────┐
│   Security Filter Chain           │
├───────────────────────────────────┤
│ 1. SecurityContextPersistenceFilter│
│ 2. LogoutFilter                    │
│ 3. UsernamePasswordAuthFilter      │
│ 4. BasicAuthenticationFilter       │
│ 5. ExceptionTranslationFilter      │
│ 6. FilterSecurityInterceptor       │
└───────────────────────────────────┘
    ↓
Controller
```

### Basic Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configure authorization
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            // Configure login
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );
        
        return http.build();
    }
}
```

### In-Memory User Store

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("USER", "ADMIN")
            .build();
        
        return new InMemoryUserDetailsManager(user, admin);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Database User Store

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> 
                new UsernameNotFoundException("User not found: " + username)
            );
        
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(getAuthorities(user.getRoles()))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
    }
}
```

### Custom Authentication Provider

```java
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {
        
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        UserDetails user = userDetailsService.loadUserByUsername(username);
        
        if (passwordEncoder.matches(password, user.getPassword())) {
            return new UsernamePasswordAuthenticationToken(
                user, 
                password, 
                user.getAuthorities()
            );
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
            .isAssignableFrom(authentication);
    }
}
```

### Multiple Filter Chains

```java
@Configuration
@EnableWebSecurity
public class MultipleFilterChainConfig {
    
    // Chain 1: API endpoints (stateless)
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .httpBasic();
        
        return http.build();
    }
    
    // Chain 2: Web endpoints (stateful)
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
            );
        
        return http.build();
    }
}
```

---

## Stateless Authentication

### Why Stateless?

**Traditional (Stateful)**:
```
Client → Login → Server creates session → Session ID in cookie
Subsequent requests use session ID
Session stored on server (memory, database, Redis)
```

**Stateless (Token-based)**:
```
Client → Login → Server returns token
Subsequent requests include token in header
No session storage on server
```

**Benefits of Stateless**:
- ✓ Horizontal scalability (no session sharing)
- ✓ Better for microservices
- ✓ Reduced server memory
- ✓ Works with mobile apps
- ✓ CORS-friendly

### Basic Stateless Configuration

```java
@Configuration
@EnableWebSecurity
public class StatelessSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf().disable()  // Disable CSRF for stateless APIs
            .httpBasic();  // Or custom token filter
        
        return http.build();
    }
}
```

### Custom Token Filter

```java
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private TokenService tokenService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Extract token from header
        String token = extractToken(request);
        
        if (token != null && tokenService.validateToken(token)) {
            // Get user details from token
            String username = tokenService.getUsernameFromToken(token);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Create authentication
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            
            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Adding Custom Filter to Chain

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(
                tokenAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
    
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter();
    }
}
```

---

## JWT Integration

### What is JWT?

**JWT (JSON Web Token)** = Header + Payload + Signature
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

Header         .    Payload        .    Signature
```

### Adding JWT Dependencies

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### JWT Service

```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;  // milliseconds
    
    // Generate JWT token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // Extract expiration date
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    // Extract any claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### JWT Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);
            
            if (username != null && 
                SecurityContextHolder.getContext().getAuthentication() == null) {
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    
                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log exception
            logger.error("JWT authentication failed", e);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Authentication Controller

```java
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            // Load user details
            UserDetails userDetails = userDetailsService
                .loadUserByUsername(request.getUsername());
            
            // Generate token
            String token = jwtService.generateToken(userDetails);
            
            return ResponseEntity.ok(new AuthResponse(token));
            
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Create user
        User user = userService.createUser(request);
        
        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponse(token));
    }
}

@Data
class LoginRequest {
    private String username;
    private String password;
}

@Data
class RegisterRequest {
    @NotBlank
    private String username;
    
    @Email
    private String email;
    
    @Size(min = 8)
    private String password;
}

@Data
@AllArgsConstructor
class AuthResponse {
    private String token;
}
```

### JWT Security Configuration

```java
@Configuration
@EnableWebSecurity
public class JwtSecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### Using JWT in Client

```javascript
// Login
const response = await fetch('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'user', password: 'pass' })
});

const { token } = await response.json();

// Store token
localStorage.setItem('token', token);

// Use token in subsequent requests
const data = await fetch('/api/users', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

### Refresh Tokens

```java
@Service
public class RefreshTokenService {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = RefreshToken.builder()
            .username(username)
            .token(UUID.randomUUID().toString())
            .expiryDate(Instant.now().plusMillis(604800000))  // 7 days
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token expired");
        }
        return token;
    }
}
```

---

## OAuth2 / OpenID Connect

### What is OAuth2?

**OAuth2** = Authorization framework
- Allows third-party access without sharing passwords
- Used by Google, Facebook, GitHub login

**Flow**:
```
1. User clicks "Login with Google"
2. Redirected to Google login page
3. User logs in and grants permissions
4. Google redirects back with authorization code
5. App exchanges code for access token
6. App uses token to access user info
```

### Adding OAuth2 Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### OAuth2 Configuration

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          # Google OAuth2
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
          
          # GitHub OAuth2
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope:
              - user:email
              - read:user
```

### OAuth2 Security Configuration

```java
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
            );
        
        return http.build();
    }
    
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }
}
```

### Custom OAuth2 User Service

```java
@Service
public class CustomOAuth2UserService 
    extends DefaultOAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) 
            throws OAuth2AuthenticationException {
        
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Process OAuth2 user
        String registrationId = userRequest.getClientRegistration()
            .getRegistrationId();
        
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Extract user info based on provider
        String email = null;
        String name = null;
        
        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        } else if ("github".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        }
        
        // Find or create user
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setProvider(registrationId);
                return userRepository.save(newUser);
            });
        
        return new CustomOAuth2User(oauth2User, user);
    }
}
```

### OpenID Connect (OIDC)

**OpenID Connect** = Identity layer on top of OAuth2
- Provides user information
- Standardized user info endpoint

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          okta:
            client-id: ${OKTA_CLIENT_ID}
            client-secret: ${OKTA_CLIENT_SECRET}
            scope: openid,profile,email
        
        provider:
          okta:
            issuer-uri: https://dev-123456.okta.com/oauth2/default
```

### Getting User Info from Token

```java
@RestController
public class UserController {
    
    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }
    
    @GetMapping("/user/info")
    public UserInfo getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return UserInfo.builder()
            .email(principal.getAttribute("email"))
            .name(principal.getAttribute("name"))
            .picture(principal.getAttribute("picture"))
            .build();
    }
}
```

---

## Method-Level Security

### Enabling Method Security

```java
@Configuration
@EnableMethodSecurity  // Spring Security 6+
// @EnableGlobalMethodSecurity(prePostEnabled = true)  // Older versions
public class MethodSecurityConfig {
}
```

### @PreAuthorize - Check Before Method Execution

```java
@Service
public class BankService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAccount(Long accountId) {
        // Only admins can delete accounts
        accountRepository.deleteById(accountId);
    }
    
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Account getAccount(Long accountId) {
        // Users and admins can view accounts
        return accountRepository.findById(accountId).orElseThrow();
    }
    
    @PreAuthorize("#username == authentication.principal.username")
    public User updateProfile(String username, UserDTO dto) {
        // Users can only update their own profile
        return userService.update(username, dto);
    }
}
```

### @PostAuthorize - Check After Method Execution

```java
@Service
public class DocumentService {
    
    @PostAuthorize("returnObject.owner == authentication.principal.username")
    public Document getDocument(Long id) {
        // Method executes, then checks if returned document belongs to user
        return documentRepository.findById(id).orElseThrow();
    }
}
```

### @Secured - Simple Role Check

```java
@Service
public class AdminService {
    
    @Secured("ROLE_ADMIN")
    public void performAdminTask() {
        // Only ADMIN role allowed
    }
    
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public void performUserTask() {
        // USER or ADMIN role allowed
    }
}
```

### @RolesAllowed - JSR-250 Standard

```java
@Service
public class ProductService {
    
    @RolesAllowed("ADMIN")
    public Product createProduct(ProductDTO dto) {
        return productRepository.save(dto.toEntity());
    }
    
    @RolesAllowed({"USER", "ADMIN"})
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
```

### SpEL Expressions

```java
@Service
public class OrderService {
    
    // Check if user is owner
    @PreAuthorize("#order.userId == authentication.principal.id")
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }
    
    // Complex expression
    @PreAuthorize("hasRole('ADMIN') or (#userId == authentication.principal.id and hasRole('USER'))")
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    // Check collection
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#orderId)")
    public void cancelOrder(Long orderId) {
        orderRepository.deleteById(orderId);
    }
}

@Component("orderSecurity")
public class OrderSecurityService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    public boolean isOwner(Long orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        
        return orderRepository.findById(orderId)
            .map(order -> order.getUserId().equals(userId))
            .orElse(false);
    }
}
```

### @PreFilter and @PostFilter

```java
@Service
public class DocumentService {
    
    // Filter list before processing
    @PreFilter("filterObject.owner == authentication.principal.username")
    public void deleteDocuments(List<Document> documents) {
        // Only processes documents owned by current user
        documentRepository.deleteAll(documents);
    }
    
    // Filter returned list
    @PostFilter("filterObject.owner == authentication.principal.username")
    public List<Document> getAllDocuments() {
        // Returns all documents, then filters to show only user's documents
        return documentRepository.findAll();
    }
}
```

---

## Securing Actuator Endpoints

### Basic Actuator Security

```java
@Configuration
public class ActuatorSecurityConfig {
    
    @Bean
    @Order(1)  // Higher priority than main security
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                // Public health checks for load balancers
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/health/liveness").permitAll()
                .requestMatchers("/actuator/health/readiness").permitAll()
                
                // Info endpoint public
                .requestMatchers("/actuator/info").permitAll()
                
                // All other actuator endpoints require ACTUATOR_ADMIN role
                .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
            )
            .httpBasic();
        
        return http.build();
    }
}
```

### Separate Management Port

```yaml
# application.yml
management:
  server:
    port: 9090  # Different from application port
    address: 127.0.0.1  # Only accessible from localhost
  
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

server:
  port: 8080
```

### Role-Based Actuator Access

```java
@Configuration
public class ActuatorRoleConfig {
    
    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                // Read-only endpoints for OPERATOR
                .requestMatchers(HttpMethod.GET, "/actuator/health/**").hasRole("OPERATOR")
                .requestMatchers(HttpMethod.GET, "/actuator/metrics/**").hasRole("OPERATOR")
                
                // Sensitive endpoints for ADMIN only
                .requestMatchers("/actuator/env/**").hasRole("ADMIN")
                .requestMatchers("/actuator/shutdown").hasRole("ADMIN")
                
                // Default deny
                .anyRequest().hasRole("ADMIN")
            )
            .httpBasic();
        
        return http.build();
    }
}
```

### Actuator with JWT

```java
@Configuration
public class ActuatorJwtConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().hasRole("ACTUATOR_ADMIN")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf().disable();
        
        return http.build();
    }
}
```

### IP Whitelist for Actuator

```java
@Configuration
public class ActuatorIpWhitelist {
    
    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().access(
                    new IpAddressAuthorizationManager("10.0.0.0/8", "192.168.0.0/16")
                )
            );
        
        return http.build();
    }
}

class IpAddressAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    
    private final List<IpAddressMatcher> matchers;
    
    public IpAddressAuthorizationManager(String... cidrs) {
        this.matchers = Arrays.stream(cidrs)
            .map(IpAddressMatcher::new)
            .collect(Collectors.toList());
    }
    
    @Override
    public AuthorizationDecision check(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext context
    ) {
        String remoteAddr = context.getRequest().getRemoteAddr();
        
        boolean allowed = matchers.stream()
            .anyMatch(matcher -> matcher.matches(remoteAddr));
        
        return new AuthorizationDecision(allowed);
    }
}
```

---

## Best Practices

### 1. Always Use HTTPS in Production

```yaml
# Force HTTPS
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

```java
@Configuration
public class HttpsConfig {
    
    @Bean
    public SecurityFilterChain forceHttps(HttpSecurity http) throws Exception {
        http.requiresChannel(channel -> channel
            .anyRequest().requiresSecure()
        );
        return http.build();
    }
}
```

### 2. Use Strong Password Encoding

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // BCrypt with strength 12
    return new BCryptPasswordEncoder(12);
    
    // OR use Argon2 (more secure)
    // return new Argon2PasswordEncoder();
}
```

### 3. Implement Account Lockout

```java
@Component
public class LoginAttemptService {
    
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    
    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
    }
    
    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0);
        attemptsCache.put(username, attempts + 1);
    }
    
    public boolean isBlocked(String username) {
        return attemptsCache.getOrDefault(username, 0) >= MAX_ATTEMPTS;
    }
}
```

### 4. Secure Password Reset

```java
@Service
public class PasswordResetService {
    
    public String generateResetToken(User user) {
        String token = UUID.randomUUID().toString();
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        
        resetTokenRepository.save(resetToken);
        
        return token;
    }
    
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new InvalidTokenException());
        
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException();
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        resetTokenRepository.delete(resetToken);
    }
}
```

### 5. CSRF Protection

```java
@Configuration
public class CsrfConfig {
    
    @Bean
    public SecurityFilterChain csrfProtection(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/public/**")
            );
        
        return http.build();
    }
}
```

### 6. CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://app.example.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 7. Security Headers

```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public SecurityFilterChain securityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'")
            )
            .frameOptions().deny()
            .xssProtection().and()
            .contentTypeOptions().and()
            .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN)
            )
        );
        
        return http.build();
    }
}
```

### 8. Audit Login Activity

```java
@Component
public class LoginAuditListener {
    
    @EventListener
    public void auditLoginSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ip = getClientIp();
        
        auditService.logLogin(username, ip, "SUCCESS");
    }
    
    @EventListener
    public void auditLoginFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        String ip = getClientIp();
        
        auditService.logLogin(username, ip, "FAILED");
    }
}
```

### 9. Rate Limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String clientId = getClientId(request);
        RateLimiter limiter = limiters.computeIfAbsent(
            clientId, 
            k -> RateLimiter.create(100.0)  // 100 requests per second
        );
        
        if (!limiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 10. Never Log Sensitive Data

```java
// ✓ GOOD
log.info("User logged in: {}", username);

// ✗ BAD
log.info("Login attempt: {} with password: {}", username, password);
log.debug("JWT token: {}", token);
```

---

## Complete Security Configuration Example

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class CompleteSecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    // Main application security
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors()
            .and()
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/auth/**", "/public/**").permitAll()
                
                // API endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                
                // All other requests
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
            );
        
        return http.build();
    }
    
    // Actuator security
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().hasRole("ACTUATOR_ADMIN")
            )
            .httpBasic();
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://app.example.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## Summary

**Key Takeaways**:

1. **Auto-Configuration**: Spring Boot provides sensible security defaults
2. **Filter Chain**: Customize security with `SecurityFilterChain`
3. **Stateless**: Use JWT for scalable, stateless authentication
4. **OAuth2**: Integrate social login (Google, GitHub, etc.)
5. **Method Security**: Protect methods with `@PreAuthorize`, `@Secured`
6. **Actuator**: Always secure management endpoints
7. **HTTPS**: Always use HTTPS in production
8. **Password Encoding**: Use BCrypt or Argon2
9. **CSRF**: Enable for state-changing operations
10. **Audit**: Log all security events

**Remember**: Security is not optional—it's a fundamental requirement! 🔒
