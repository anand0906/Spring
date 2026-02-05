# Security in Microservices

## Table of Contents
1. [Introduction to Security](#introduction-to-security)
2. [Spring Security Basics](#spring-security-basics)
3. [Stateless Authentication](#stateless-authentication)
4. [JWT (JSON Web Tokens)](#jwt-json-web-tokens)
5. [OAuth2 / OpenID Connect](#oauth2--openid-connect)
6. [Keycloak Integration](#keycloak-integration)
7. [Auth0 Integration](#auth0-integration)
8. [Securing Inter-Service Communication](#securing-inter-service-communication)
9. [Role-Based & Permission-Based Access](#role-based--permission-based-access)

---

## Introduction to Security

### Why Security is Critical in Microservices

**Security Challenges in Microservices:**

```
Monolith:
├─ Single security perimeter
├─ All code in one place
├─ Easier to secure
└─ One authentication point

Microservices:
├─ Multiple entry points
├─ Services communicate over network
├─ More attack surface
├─ Complex authentication flow
└─ Need to secure every service
```

---

### Security Principles

**1. Defense in Depth**
```
Multiple layers of security:
- Network security (firewalls)
- API Gateway (authentication)
- Service level (authorization)
- Data encryption (TLS)
- Database security (access control)

If one layer fails, others protect
```

**2. Least Privilege**
```
Give minimum necessary permissions:
- User Service → Can only read users
- Admin Service → Full access
- Payment Service → Can only access payment data

Don't give everyone admin access
```

**3. Zero Trust**
```
Never trust, always verify:
- Don't trust internal network
- Verify every request
- Authenticate service-to-service calls
- Encrypt everything
```

**4. Secure by Default**
```
Default state = Secure:
- Endpoints closed by default
- Must explicitly allow access
- Authentication required unless specified
```

---

### Common Security Threats

**1. Unauthorized Access**
```
Problem: User accessing resources without permission
Solution: Authentication + Authorization
```

**2. Man-in-the-Middle (MITM)**
```
Problem: Attacker intercepts communication
Solution: TLS/HTTPS encryption
```

**3. SQL Injection**
```
Problem: Malicious SQL in user input
Solution: Prepared statements, input validation
```

**4. Cross-Site Scripting (XSS)**
```
Problem: Malicious JavaScript in user input
Solution: Input sanitization, output encoding
```

**5. Cross-Site Request Forgery (CSRF)**
```
Problem: Unauthorized commands from trusted user
Solution: CSRF tokens, SameSite cookies
```

**6. Sensitive Data Exposure**
```
Problem: Passwords, tokens in logs or responses
Solution: Never log sensitive data, mask in responses
```

---

## Spring Security Basics

### What is Spring Security?

**Definition:** Comprehensive security framework for Java applications.

**Features:**
- Authentication (who are you?)
- Authorization (what can you do?)
- Protection against common attacks
- Integration with various auth mechanisms
- Highly customizable

---

### Basic Spring Security Setup

**Step 1: Add Dependency (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Step 2: Default Behavior**

```
By default, Spring Security:
- Secures ALL endpoints
- Creates login page at /login
- Generates random password (printed in console)
- Default username: user
- Uses HTTP Basic authentication
```

**Step 3: Access Application**

```
Start application
Try to access any endpoint → Redirected to /login
Login with:
  Username: user
  Password: <check console logs>
Access granted
```

---

### Simple Security Configuration

**In-Memory Users (Development Only):**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()     // Public endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN") // Admin only
                .anyRequest().authenticated()                   // Everything else needs auth
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .permitAll()
            );
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN", "USER")
            .build();
        
        return new InMemoryUserDetailsManager(user, admin);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**What This Does:**

```
/public/** → Anyone can access
/admin/** → Only users with ADMIN role
/login → Custom login page (everyone can access)
Everything else → Must be authenticated
```

---

### Database-Based Authentication

**Step 1: Create User Entity**

```java
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String email;
    
    private boolean enabled = true;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();
    
    // Constructors, getters, setters
}
```

**Step 2: User Repository**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

**Step 3: UserDetailsService Implementation**

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .disabled(!user.isEnabled())
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList())
            )
            .build();
    }
}
```

**Step 4: Security Configuration**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)  // Use our custom service
            .httpBasic();
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Password Encoding

**NEVER Store Plain Text Passwords!**

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(UserRegistrationRequest request) {
        // Check if username exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username taken");
        }
        
        // Create user with ENCODED password
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // ENCODE!
        user.setEmail(request.getEmail());
        user.setRoles(Set.of("USER"));
        
        return userRepository.save(user);
    }
}
```

**How BCrypt Works:**

```
Input: "myPassword123"
Output: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

Same password → Different hash each time (salt)
Verifying is fast
Cracking is very slow (by design)
```

---

## Stateless Authentication

### What is Stateless?

**Stateful (Traditional Sessions):**

```
User logs in
Server creates session → Stores in memory/database
Server returns session ID → Stored in cookie
Next request → Client sends session ID
Server looks up session → Verifies user

Problems in microservices:
- Must share session across services
- Session storage becomes bottleneck
- Horizontal scaling difficult
```

**Stateless (Token-Based):**

```
User logs in
Server generates token → Contains user info
Server returns token → Client stores it
Next request → Client sends token
Server verifies token → Extracts user info (no lookup!)

Benefits:
- No session storage needed
- Easy to scale horizontally
- Works across services
- Stateless servers
```

---

### Token-Based Authentication Flow

```
1. Login Request
   Client → Username/Password → Server

2. Server Validates
   Check database → Credentials valid?

3. Generate Token
   Server creates JWT with user info
   Signs token with secret key

4. Return Token
   Server → JWT → Client
   Client stores token (localStorage, memory)

5. Protected Request
   Client → GET /api/orders (+ JWT in header) → Server

6. Verify Token
   Server validates JWT signature
   Extracts user info from token
   No database lookup needed!

7. Process Request
   Server checks permissions
   Returns data
```

---

## JWT (JSON Web Tokens)

### What is JWT?

**Definition:** Self-contained token that carries information about user.

**Structure:**

```
JWT = Header.Payload.Signature

Example:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

---

### JWT Components

**1. Header**

```json
{
  "alg": "HS256",    // Algorithm used for signing
  "typ": "JWT"       // Token type
}
```

**2. Payload (Claims)**

```json
{
  "sub": "user123",              // Subject (user ID)
  "name": "John Doe",            // User name
  "email": "john@example.com",   // Email
  "roles": ["USER", "ADMIN"],    // Roles
  "iat": 1516239022,             // Issued at (timestamp)
  "exp": 1516242622              // Expiration (timestamp)
}
```

**3. Signature**

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)

Purpose: Verify token hasn't been tampered with
```

---

### JWT Implementation with Spring Security

**Step 1: Add Dependency (pom.xml)**

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**Step 2: JWT Utility Class**

```java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;  // e.g., 86400000 (24 hours in ms)
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList())
        );
        
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
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
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
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

**Step 3: JWT Authentication Filter**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Get JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        
        String username = null;
        String jwt = null;
        
        // Check if header starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);  // Remove "Bearer " prefix
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Unable to get JWT Token or JWT Token has expired");
            }
        }
        
        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**Step 4: Security Configuration with JWT**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Login/register public
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // No sessions!
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**Step 5: Authentication Controller**

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(), 
                    request.getPassword()
                )
            );
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            
            // Generate JWT
            String jwt = jwtUtil.generateToken(userDetails);
            
            return ResponseEntity.ok(new AuthResponse(jwt));
            
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid username or password");
        }
    }
}

// DTOs
class LoginRequest {
    private String username;
    private String password;
    // Getters, setters
}

class AuthResponse {
    private String token;
    
    public AuthResponse(String token) {
        this.token = token;
    }
    // Getter
}
```

---

### Using JWT from Client

**Login Flow:**

```javascript
// 1. Login
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'john',
    password: 'password123'
  })
});

const data = await response.json();
const token = data.token;

// 2. Store token (in memory, localStorage, or sessionStorage)
localStorage.setItem('token', token);

// 3. Use token for protected requests
const ordersResponse = await fetch('http://localhost:8080/api/orders', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const orders = await ordersResponse.json();
```

---

### JWT Best Practices

**1. Use Strong Secret**
```
✗ secret = "mysecret"
✓ secret = "very-long-random-string-at-least-256-bits"

Generate: openssl rand -base64 32
```

**2. Set Reasonable Expiration**
```
Access token: 15 minutes to 1 hour
Refresh token: 7 days to 30 days

Short-lived = More secure
```

**3. Don't Store Sensitive Data in JWT**
```
✗ Password, credit card number
✓ User ID, username, roles, email
```

**4. Use HTTPS**
```
JWT transmitted in plain text (base64 encoded, not encrypted)
HTTPS encrypts communication
```

**5. Implement Token Refresh**
```
Access token expires → Use refresh token to get new access token
User stays logged in without re-entering password
```

---

### Refresh Token Implementation

**Refresh Token Entity:**

```java
@Entity
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private Instant expiryDate;
    
    // Constructors, getters, setters
}
```

**Refresh Token Service:**

```java
@Service
public class RefreshTokenService {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;  // e.g., 2592000000 (30 days)
    
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token expired. Please login again.");
        }
        return token;
    }
}
```

**Refresh Endpoint:**

```java
@PostMapping("/refresh")
public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
    String requestRefreshToken = request.getRefreshToken();
    
    return refreshTokenService.findByToken(requestRefreshToken)
        .map(refreshTokenService::verifyExpiration)
        .map(RefreshToken::getUser)
        .map(user -> {
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String newAccessToken = jwtUtil.generateToken(userDetails);
            
            return ResponseEntity.ok(new RefreshTokenResponse(newAccessToken, requestRefreshToken));
        })
        .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));
}
```

---

## OAuth2 / OpenID Connect

### What is OAuth2?

**Definition:** Authorization framework that enables applications to obtain limited access to user accounts.

**Simple Explanation:**

```
Scenario: You want to use "Photo Printing App"

Without OAuth2:
You give Photo Printing App your Google password
App has full access to your Google account
Very risky!

With OAuth2:
You click "Login with Google"
Google asks: "Photo Printing App wants to access your photos. Allow?"
You say yes
Google gives app a token (not your password!)
Token allows access ONLY to photos (not email, contacts, etc.)
You can revoke access anytime
```

---

### OAuth2 Roles

**1. Resource Owner**
```
The user who owns the data
Example: You (own your Google photos)
```

**2. Client**
```
Application requesting access
Example: Photo Printing App
```

**3. Resource Server**
```
Server hosting the protected resources
Example: Google Photos API
```

**4. Authorization Server**
```
Server that issues access tokens
Example: Google OAuth Server
```

---

### OAuth2 Flow (Authorization Code)

```
1. User clicks "Login with Google" in Photo App
   Photo App → Redirect to Google Login

2. User enters credentials at Google
   Google authenticates user

3. Google asks for consent
   "Photo App wants to access your photos. Allow?"
   User clicks "Allow"

4. Google redirects back to Photo App with Authorization Code
   https://photoapp.com/callback?code=AUTH_CODE_123

5. Photo App exchanges code for access token
   Photo App → POST to Google → Authorization Code
   Google → Verifies code → Returns Access Token

6. Photo App uses access token to access photos
   Photo App → GET photos with token → Google Photos API
   Google → Returns photos

Token never exposed to user's browser (more secure)
```

---

### OpenID Connect (OIDC)

**Definition:** Identity layer on top of OAuth2.

**Difference:**

```
OAuth2:
Purpose: Authorization (what can you access?)
Returns: Access token
Use: "Let app access my photos"

OpenID Connect (OIDC):
Purpose: Authentication (who are you?)
Returns: ID token (JWT) + Access token
Use: "Let app know who I am"
```

**ID Token Example:**

```json
{
  "sub": "user123",                    // User ID
  "name": "John Doe",
  "email": "john@example.com",
  "email_verified": true,
  "picture": "https://example.com/photo.jpg",
  "iss": "https://accounts.google.com", // Issuer
  "aud": "photo-app-client-id",        // Audience (your app)
  "iat": 1516239022,                   // Issued at
  "exp": 1516242622                    // Expiration
}
```

---

### Spring Security OAuth2 Client Setup

**Step 1: Add Dependency (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

**Step 2: Configuration (application.yml)**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
            scope:
              - openid
              - profile
              - email
          github:
            client-id: your-github-client-id
            client-secret: your-github-client-secret
            scope:
              - user:email
              - read:user
```

**Step 3: Security Configuration**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
            );
        
        return http.build();
    }
}
```

**Step 4: Controller**

```java
@Controller
public class HomeController {
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal OAuth2User principal) {
        // Get user info from OAuth2 provider
        String name = principal.getAttribute("name");
        String email = principal.getAttribute("email");
        
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        
        return "dashboard";
    }
}
```

**That's it!** Spring handles OAuth2 flow automatically.

---

## Keycloak Integration

### What is Keycloak?

**Definition:** Open-source Identity and Access Management solution.

**Features:**
- Single Sign-On (SSO)
- Social login (Google, Facebook, etc.)
- User federation (LDAP, Active Directory)
- Identity brokering
- User management UI
- Fine-grained authorization

---

### Keycloak Setup

**Step 1: Run Keycloak**

```bash
# Using Docker
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev

# Access: http://localhost:8180
# Login: admin/admin
```

**Step 2: Create Realm**

```
1. Login to Keycloak Admin Console
2. Hover over "master" dropdown → Click "Create Realm"
3. Realm name: "microservices-realm"
4. Click "Create"
```

**Step 3: Create Client**

```
1. Click "Clients" → "Create client"
2. Client ID: "order-service"
3. Client Protocol: "openid-connect"
4. Click "Next"
5. Client authentication: ON
6. Valid redirect URIs: "http://localhost:8080/*"
7. Click "Save"
8. Go to "Credentials" tab
9. Copy "Client Secret"
```

**Step 4: Create User**

```
1. Click "Users" → "Add user"
2. Username: "john"
3. Email: "john@example.com"
4. Click "Create"
5. Go to "Credentials" tab
6. Set password: "password123"
7. Temporary: OFF
8. Click "Set Password"
```

**Step 5: Create Roles**

```
1. Click "Realm roles" → "Create role"
2. Role name: "USER"
3. Click "Save"
4. Create another role: "ADMIN"
5. Go back to Users → john → "Role mapping"
6. Assign "USER" role
```

---

### Spring Boot with Keycloak

**Step 1: Add Dependencies (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

**Step 2: Configuration (application.yml)**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/microservices-realm
          jwk-set-uri: http://localhost:8180/realms/microservices-realm/protocol/openid-connect/certs

keycloak:
  auth-server-url: http://localhost:8180
  realm: microservices-realm
  resource: order-service
  credentials:
    secret: your-client-secret-from-keycloak
```

**Step 3: Security Configuration**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = 
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }
}
```

---

### Getting Token from Keycloak

**Using Password Grant (Testing Only):**

```bash
curl -X POST 'http://localhost:8180/realms/microservices-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=order-service' \
  -d 'client_secret=your-client-secret' \
  -d 'username=john' \
  -d 'password=password123'
```

**Response:**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer"
}
```

**Using Token:**

```bash
curl -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI..." \
  http://localhost:8080/api/orders
```

---

## Auth0 Integration

### What is Auth0?

**Definition:** Cloud-based identity platform (Identity-as-a-Service).

**Features:**
- Easy setup (no infrastructure)
- Social logins (Google, Facebook, etc.)
- Multi-factor authentication
- Passwordless authentication
- User management dashboard
- Enterprise connections (SAML, LDAP)

---

### Auth0 Setup

**Step 1: Create Auth0 Account**

```
1. Go to https://auth0.com
2. Sign up (free tier available)
3. Create tenant: "myapp"
```

**Step 2: Create Application**

```
1. Dashboard → Applications → Create Application
2. Name: "Order Service"
3. Type: "Regular Web Applications"
4. Technology: "Java Spring Boot"
5. Click "Create"
```

**Step 3: Configure Application**

```
Settings:
- Allowed Callback URLs: http://localhost:8080/login/oauth2/code/auth0
- Allowed Logout URLs: http://localhost:8080
- Click "Save Changes"

Copy:
- Domain: myapp.auth0.com
- Client ID: abc123...
- Client Secret: xyz789...
```

---

### Spring Boot with Auth0

**Step 1: Add Dependencies (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

**Step 2: Configuration (application.yml)**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          auth0:
            client-id: your-client-id
            client-secret: your-client-secret
            scope:
              - openid
              - profile
              - email
        provider:
          auth0:
            issuer-uri: https://myapp.auth0.com/
      resourceserver:
        jwt:
          issuer-uri: https://myapp.auth0.com/
```

**Step 3: Security Configuration**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login()
            .and()
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        
        return http.build();
    }
}
```

---

## Securing Inter-Service Communication

### Why Secure Inter-Service Communication?

**Problem:**

```
External threats are not the only concern:

Scenario:
1. Attacker compromises one microservice
2. That service can call ALL other services
3. No authentication between services
4. Attacker gains access to entire system

Internal network ≠ Secure network
```

**Solution:**

```
Zero Trust:
- Services authenticate each other
- Use mutual TLS or service tokens
- Verify every request
```

---

### Method 1: Service-to-Service JWT

**Theory:**
Each service has its own credentials to get tokens.

**Implementation:**

**Step 1: Service Authentication**

```java
@Service
public class ServiceAuthenticationService {
    
    @Value("${service.username}")
    private String serviceUsername;
    
    @Value("${service.password}")
    private String servicePassword;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private String cachedToken;
    private Instant tokenExpiry;
    
    public String getServiceToken() {
        // Check if token is still valid
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        
        // Get new token
        LoginRequest request = new LoginRequest(serviceUsername, servicePassword);
        AuthResponse response = restTemplate.postForObject(
            "http://auth-service/api/auth/service-login",
            request,
            AuthResponse.class
        );
        
        cachedToken = response.getToken();
        tokenExpiry = Instant.now().plusSeconds(3600);  // Token expires in 1 hour
        
        return cachedToken;
    }
}
```

**Step 2: Add Token to Outgoing Requests**

```java
@Component
public class ServiceTokenInterceptor implements ClientHttpRequestInterceptor {
    
    @Autowired
    private ServiceAuthenticationService authService;
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                       ClientHttpRequestExecution execution) throws IOException {
        
        // Add service token to Authorization header
        String token = authService.getServiceToken();
        request.getHeaders().set("Authorization", "Bearer " + token);
        
        return execution.execute(request, body);
    }
}

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(
            Collections.singletonList(new ServiceTokenInterceptor())
        );
        return restTemplate;
    }
}
```

---

### Method 2: Mutual TLS (mTLS)

**Theory:**
Both client and server verify each other's certificates.

**Normal TLS:**
```
Client verifies server certificate
Server doesn't verify client
```

**Mutual TLS:**
```
Client verifies server certificate
Server verifies client certificate
Both parties authenticated
```

**Configuration (application.yml):**

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: password
    key-store-type: PKCS12
    trust-store: classpath:truststore.p12
    trust-store-password: password
    client-auth: need  # Require client certificate

spring:
  cloud:
    gateway:
      httpclient:
        ssl:
          use-insecure-trust-manager: false
          trust-store: classpath:client-truststore.p12
          trust-store-password: password
          key-store: classpath:client-keystore.p12
          key-store-password: password
```

---

### Method 3: API Keys for Internal Services

**Simple approach for internal services:**

```java
@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {
    
    @Value("${internal.api.keys}")
    private Set<String> validApiKeys;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Get API key from header
        String apiKey = request.getHeader("X-API-Key");
        
        // Verify API key
        if (apiKey == null || !validApiKeys.contains(apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API key");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## Role-Based & Permission-Based Access

### Role-Based Access Control (RBAC)

**Definition:** Permissions assigned to roles, users assigned to roles.

**Example:**

```
Roles:
- USER: Can view own orders
- MANAGER: Can view all orders
- ADMIN: Can view/edit/delete orders

Users:
- john: USER role
- alice: MANAGER role
- bob: ADMIN role
```

---

### RBAC Implementation

**Step 1: Define Roles in Code**

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<Order> getOrders(@AuthenticationPrincipal UserDetails user) {
        // All authenticated users can view orders
        return orderService.getOrders(user.getUsername());
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<Order> getAllOrders() {
        // Only managers and admins can view all orders
        return orderService.getAllOrders();
    }
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Order createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }
}
```

**Step 2: Enable Method Security**

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
    // Configuration here
}
```

---

### Permission-Based Access Control

**Definition:** Fine-grained permissions instead of broad roles.

**Example:**

```
Permissions:
- order:read
- order:create
- order:update
- order:delete
- user:read
- user:create
- user:update
- user:delete

Roles with Permissions:
- USER: order:read, order:create
- MANAGER: order:*, user:read
- ADMIN: order:*, user:*
```

---

### Permission-Based Implementation

**Step 1: Permission Entity**

```java
@Entity
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String name;  // e.g., "order:read"
    
    // Getters, setters
}

@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String name;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    // Getters, setters
}

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String password;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    // Getters, setters
}
```

**Step 2: Load Permissions**

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Collect all permissions from all roles
        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(permission -> new SimpleGrantedAuthority(permission.getName()))
            .collect(Collectors.toSet());
        
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            authorities
        );
    }
}
```

**Step 3: Check Permissions**

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public List<Order> getOrders() {
        return orderService.getAllOrders();
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public Order createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('order:update')")
    public Order updateOrder(@PathVariable Long id, @RequestBody OrderRequest request) {
        return orderService.updateOrder(id, request);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('order:delete')")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }
}
```

---

### Custom Authorization Logic

**Complex Business Rules:**

```java
@Service
public class OrderSecurityService {
    
    public boolean canAccessOrder(Authentication authentication, Long orderId) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // Admin can access all orders
        if (userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        
        // Manager can access orders from their department
        if (userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
            Order order = orderRepository.findById(orderId).orElse(null);
            return order != null && order.getDepartment().equals(userDetails.getUsername());
        }
        
        // Regular user can only access their own orders
        Order order = orderRepository.findById(orderId).orElse(null);
        return order != null && order.getUserId().equals(userDetails.getUsername());
    }
}

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderSecurityService securityService;
    
    @GetMapping("/{id}")
    @PreAuthorize("@orderSecurityService.canAccessOrder(authentication, #id)")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}
```

---

## Summary

### Security Checklist

**Authentication:**
- ✓ Use strong password hashing (BCrypt)
- ✓ Implement JWT with reasonable expiration
- ✓ Use refresh tokens for long sessions
- ✓ Support OAuth2 for social login
- ✓ Consider Keycloak/Auth0 for enterprise

**Authorization:**
- ✓ Implement RBAC or permission-based access
- ✓ Use @PreAuthorize for method security
- ✓ Validate ownership (users access only their data)
- ✓ Least privilege principle

**Communication:**
- ✓ Use HTTPS everywhere
- ✓ Secure inter-service communication
- ✓ Validate all inputs
- ✓ Sanitize outputs

**Data Protection:**
- ✓ Never log sensitive data
- ✓ Encrypt sensitive data at rest
- ✓ Use environment variables for secrets
- ✓ Mask sensitive data in responses

**Best Practices:**
- ✓ Keep dependencies updated
- ✓ Regular security audits
- ✓ Implement rate limiting
- ✓ Monitor for suspicious activity
- ✓ Have incident response plan

**Remember:** Security is not a one-time task. It's an ongoing process. Stay updated on security best practices and vulnerabilities!
