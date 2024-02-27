<h1>Spring Security</h1>
<p>Spring Security is a powerful authentication and access control framework for Java applications, particularly those developed using the Spring Framework. It is designed to address various security concerns in web and enterprise applications, providing a flexible and customizable way to secure your application against unauthorized access and potential vulnerabilities.</p>
<p>Key Concepts of Spring Security include:</p>
<ol>
	<li>Authentication:</li>
	<p>Spring Security helps verify the identity of users attempting to access an application. It supports various authentication methods, including:</p>
	<ul>
		<li>Username and password (through forms, basic, and digest authentication)</li>
		<li>Integration with external authentication providers like LDAP or CAS</li>
		<li>Social login using services like Facebook, Google, etc.</li>
	</ul>
	<li>Authorization:</li>
	<p>Once a user is authenticated, Spring Security controls what they can access within the application. It allows defining rules and access levels based on roles, permissions, or other attributes.</p>
	<li>Security Features:</li>
	<p>Spring Security protects against common web security vulnerabilities like:</p>
	<ol>
		<li>Cross-site request forgery (CSRF)</li>
		<li>Session fixation</li>
		<li>Clickjacking</li>
		<li>and more</li>
	</ol>
</ol>
<p>Key Features</p>
<ul>
	<li>Extensible: Spring Security provides a modular design, allowing you to customize and integrate various security components based on your application's needs.</li>
	<li>Comprehensive: It offers support for both imperative and reactive applications, making it adaptable to different development styles.</li>
	<li>Integrations: Spring Security seamlessly integrates with other Spring projects like Spring MVC, simplifying security implementation in your application.</li>
</ul>

<h2>Spring Security Architecture</h2>
<p>Spring Security utilizes Servlet filters as a key component in its security architecture. These filters work like layered checkpoints for incoming requests, enforcing various security measures before they reach the desired resources (servlets or JSP pages or controllers) in your application.</p>

<h3>Servelt Filters</h3>
<p>Servlet Filters are components in Java web applications that act as intermediaries between client requests and server responses. They can be used to perform various pre-processing and post-processing tasks on both requests and responses, adding functionality without modifying the core logic of servlets or JSPs.</p>
<p>Servlet filters are like gatekeepers in a web application built with Java technology (specifically using Servlets). They act as an intermediate layer between the client (your browser) and the server-side resources (like servlets) in your application.</p>
<img src="https://dotnettutorials.net/wp-content/uploads/2020/11/word-image-149-768x442.png?ezimgfmt=ng:webp/ngcb8">
<p>What they do:</p>
<ul>
	<li>Intercept incoming requests: Before a request reaches its intended destination (a servlet or JSP page), it passes through one or more filters.</li>
	<li>Perform actions on the request or response: Filters can:</li>
	<ul>
		<li>Modify the request: Change headers, parameters, or content before passing it to the next component.</li>
		<li>Modify the response: Change headers, content, or even completely replace it before sending it back to the client.</li>
		<li>Perform other tasks: Log requests, validate user information, do security checks, compress data, etc.</li>
	</ul>
	<li>Decide whether to continue: Filters have the power to stop a request from reaching its intended destination if they deem it necessary (e.g., due to security concerns).</li>
</ul>

<h3>Security Filters</h3>
<p>In Spring Security, security filters are components responsible for performing various security-related tasks during the processing of HTTP requests</p>
<p>Each filter performs a specific security-related task like authentication (verifying user identity), authorization (checking access rights), or security exception handling.</p>

<p>In Spring Security, filters play a vital role in managing the security of your web applications. They act as checkpoints throughout the request processing pipeline, performing various security tasks like authentication, authorization, and protection against vulnerabilities.</p>
<img src="https://docs.spring.io/spring-security/reference/_images/servlet/architecture/filterchain.png">
<ul>
	<li>Filters intercept incoming HTTP requests before they reach the actual application logic.</li>
	<li>They act like a chain, with each filter potentially modifying the request or response object before passing it to the next filter in the chain.</li>
	<li>Ultimately, only the final filter in the chain delegates the request to the actual application endpoint (servlet) for processing.</li>
</ul>

<h3>SecurityFilterChain</h3>
<p>In Spring Security, SecurityFilterChain is a class used to configure and manage a single security filter chain. It provides a convenient way to define the set of security filters that should be applied to a specific set of requests within your application. Here's its role:</p>
<img src="https://docs.spring.io/spring-security/reference/_images/servlet/architecture/securityfilterchain.png">
<p>Represents a single chain of security filters</p>
<p>Defines the order in which these filters should be executed.</p>
<p>Can be mapped to specific URL patterns using FilterChainProxy.</p>
<p>FilterChainProxy manages a collection of SecurityFilterChains.</p>
<img src="https://docs.spring.io/spring-security/reference/_images/servlet/architecture/multi-securityfilterchain.png">
<p>FilterChainProxy decides which SecurityFilterChain should be used. </p>
<p>It maps incoming requests to the appropriate SecurityFilterChain based on defined URL patterns or custom matching logic.</p>
<p>SecurityFilterChain is typically created and configured using Spring Security's HttpSecurity object.</p>


<p>A filter chain in Spring Security is a structured sequence of filters that act on every incoming HTTP request before it reaches the actual application logic. Each filter performs specific security-related tasks, effectively creating a controlled </p>
<p>Here's how the filter chain works:</p>
<ol>
	<li>Interceptor: The FilterChainProxy intercepts every incoming HTTP request.</li>
	<li>Matching: It then identifies the appropriate filter chain based on predefined rules, such as URL patterns or request type.</li>
	<li>Chain Execution: The identified chain of filters is invoked sequentially. Each filter has the opportunity to modify the request object or response object before passing it to the next filter in the chain.</li>
	<li>Delegation: Only the last filter in the chain delegates the request to the actual application endpoint for processing.</li>
</ol>

<h3>FilterChainProxy</h3>
<p>In Spring Security, the FilterChainProxy is a crucial component responsible for managing and executing a chain of filters that are part of the security processing.</p>
<p>The FilterChainProxy holds a collection of individual security filters and determines the order in which they should be executed.</p>
<p>FilterChainProxy is commonly integrated with the DelegatingFilterProxy in a web application's web.xml configuration.</p>
<p>The DelegatingFilterProxy is responsible for invoking the FilterChainProxy, and subsequently, the FilterChainProxy manages the execution of the configured filter chain.</p>
<img src="https://docs.spring.io/spring-security/reference/_images/servlet/architecture/filterchainproxy.png">

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults());
        return http.build();
    }

}
```

<h3>DeligatingFilterProxy</h3>
<p>In Spring Security, DelegatingFilterProxy plays a crucial role in bridging the gap between the standard servlet filter mechanism and the Spring framework. Here's what it does:</p>
<p>DelegatingFilterProxy is a Servlet Filter provided by Spring Web that acts as a proxy.</p>
<p>It delegates all the work to another Spring bean implementing the standard javax.servlet.Filter interface.</p>
<p>This allows Spring Security to leverage the benefits of the Spring framework within its filter logic.</p>
<p>In the context of Spring Security, DelegatingFilterProxy is often used to integrate Spring Security's FilterChainProxy with the servlet container.</p>
<p>The FilterChainProxy is a core component of Spring Security that manages a chain of filters responsible for various security-related tasks such as authentication and authorization.</p>
<img src="https://docs.spring.io/spring-security/reference/_images/servlet/architecture/delegatingfilterproxy.png">
<p>How it works in Spring Security:</p>
<ol>
	<li>web.xml configuration: You declare the DelegatingFilterProxy in your web.xml file, specifying the filter-name and the filter class as org.springframework.web.filter.DelegatingFilterProxy.</li>
	<li>Spring bean configuration: In your Spring configuration file (e.g., XML or Java configurations), you define the actual security filter as a Spring bean, typically named springSecurityFilterChain. This bean represents the chain of security filters that Spring Security manages internally.</li>
	<li>Delegation: When a request comes in, the DelegatingFilterProxy in web.xml intercepts it.</li>
	<li>The DelegatingFilterProxy then delegates the request processing to the Spring bean named springSecurityFilterChain.</li>
	<li>The springSecurityFilterChain applies the configured security filters in the correct order to perform various security tasks like authentication, authorization, etc.</li>
</ol>

```java
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
	Filter delegate = getFilterBean(someBeanName); 
	delegate.doFilter(request, response); 
}
```



<h2>Authentication Filter</h2>
<p>Spring Authentication filters are the gatekeepers of your Spring applications. They are primarily responsible for intercepting incoming requests, extracting user credentials (e.g., username and password), and verifying if that user is who they claim to be.</p>
<p>Spring Security works with a collection of filters forming a chain. The Authentication Filter is an essential part of this chain.</p>
<p>When a request enters your application, the Authentication Filter jumps into action. It checks for authentication information, which could be:</p>
<ul>
	<li>Traditional username/password in the request body or header</li>
	<li>API token</li>
	<li>Authentication data like certificates</li>
</ul>

<h3>Architecture</h3>
<img src="https://prod-acb5.kxcdn.com/wp-content/uploads/2019/09/Spring-Security-Architecture--1024x607.png.webp">


<h4>Authentication</h4>
<p>The Authentication interface plays a central role in Spring Security by representing the following:</p>
<ul>
	<li>Authentication Request (Before Verification): Before a user's credentials are verified, an Authentication object holds their provided credentials (e.g., raw username and password).</li>
	<li>Authenticated Principal (After Verification): Once the credentials are successfully validated, the Authentication object encapsulates the verified user or principal. This principal often includes details about the user's identity, granted authorities (roles/permissions), and more.</li>
</ul>
<p>ou can obtain the current Authentication from the SecurityContext.</p>
<p>The Authentication contains:</p>
<ul>
	<li>principal: Identifies the user. When authenticating with a username/password this is often an instance of UserDetails.</li>
	<li>credentials: Often a password. In many cases, this is cleared after the user is authenticated, to ensure that it is not leaked.</li>
	<li>authorities: The GrantedAuthority instances are high-level permissions the user is granted. Two examples are roles and scopes</li>
</ul>

```java
public interface Authentication {

    /**
     * Returns the name of the principal (usually the username).
     *
     * @return the principal name
     */
    String getName();

    /**
     * Returns the credentials used to authenticate this principal.
     *
     * @return the credentials
     */
    Object getCredentials();

    /**
     * Returns additional details about the authentication event, such as remote address, session ID, etc.
     *
     * @return the details
     */
    Object getDetails();

    /**
     * Returns the authorities granted to the principal.
     *
     * @return the collection of authorities
     */
    Collection<? extends GrantedAuthority> getAuthorities();

     /**
     * Returns the principal, possibly a UserDetails object.
     *
     * @return the principal
     */
    Object getPrincipal();

    /**
     * Indicates whether the user has been authenticated.
     *
     * @return true if the user has been authenticated, false otherwise
     */
    boolean isAuthenticated();

    /**
     * Sets whether the user has been authenticated.
     *
     * @param isAuthenticated true if the user is authenticated, false otherwise
     */
    void setAuthenticated(boolean isAuthenticated);
}

```

<p>Spring Security provides several built-in implementations of the Authentication interface.</p>
<ul>
	<li>UsernamePasswordAuthenticationToken : This is the workhorse for traditional username and password login forms.</li>
	<li>OAuth2 and JWT: Spring Security provides specific Authentication implementations designed for handling authentication tokens used in OAuth2 and JWT-based authorization flows.</li>
</ul>
<p>You can always create your own custom implementations of the Authentication interface to match the specific needs of a unique authentication mechanism within your application.</p>

<h4>Security Context Holder & Security Context</h4>
<p>The SecurityContextHolder is where Spring Security stores the details of who is authenticated.</p>
<p>The SecurityContextHolder is designed to hold the SecurityContext, which contains information about the current user's authentication and authorization details. The SecurityContext typically includes an Authentication object that represents the authenticated user along with their granted authorities.</p>
<p>This class is responsible for managing the SecurityContext for the current thread. It provides static methods to get, set, and clear the SecurityContext. The SecurityContextHolder is thread-bound, meaning that each thread has its own SecurityContext</p>
<p>SecurityContext: This interface represents the security information associated with the current thread. It holds the Authentication object, which contains the principal (authenticated user) and their granted authorities.</p>

<img src="https://docs.spring.io/spring-security/reference/_images/servlet/authentication/architecture/securitycontextholder.png">

<p>The simplest way to indicate a user is authenticated is to set the SecurityContextHolder directly:</p>

```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
Authentication authentication =
    new TestingAuthenticationToken("username", "password", "ROLE_USER");
context.setAuthentication(authentication);

SecurityContextHolder.setContext(context);
```

<p>To obtain information about the authenticated principal, access the SecurityContextHolder.</p>

```java
SecurityContext context = SecurityContextHolder.getContext();
Authentication authentication = context.getAuthentication();
String username = authentication.getName();
Object principal = authentication.getPrincipal();
Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
```

<h4>Authentication Manager</h4>
<p>In Spring Security, the AuthenticationManager is a core interface responsible for authenticating a user. </p>
<p>It is responsible for:</p>
<ol>
	<li>Verifying Credentials: An AuthenticationManager takes an Authentication object (holding a user's provided credentials) and validates them.</li>
	<li>Returning Results: If authentication is successful, it updates and returns the Authentication object with the verified principal, granted authorities, and sets isAuthenticated() to true. If the credentials are invalid, it throws an exception.</li>
</ol>
<p>
In Spring Security, the AuthenticationManager is a core interface responsible for authenticating a user. It plays a central role in the authentication process by coordinating the authentication process and delegating the actual authentication to one or more AuthenticationProvider instances.
</p>
<p>The AuthenticationManager interface has a single method:</p>

```java
public interface AuthenticationManager {
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
}
```
<p>The authenticate method takes an Authentication object (typically a specific implementation like UsernamePasswordAuthenticationToken) as its argument and returns an Authentication object if the authentication is successful. If the authentication fails, an exception of type AuthenticationException is thrown.</p>

<p>The AuthenticationManager itself doesn't directly know how to authenticate different types of credentials. Instead, it delegates the job to a list of configured AuthenticationProviders.</p>
<p>The AuthenticationManager iterates through the configured providers until it finds one that supports the provided credentials. That provider then takes a shot at authentication</p>
<p>The ProviderManager is the most common implementation of AuthenticationManager. It lets you configure a list of AuthenticationProviders.</p>
<p> You can build custom AuthenticationManager implementations if needed for unique scenarios.</p>

```java
public class ProviderManager implements AuthenticationManager {
    List<AuthenticationProvider> providers;
    
    // Other configurations...

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Loop through the list of providers and delegate authentication
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(authentication.getClass())) {
                return provider.authenticate(authentication);
            }
        }
        throw new ProviderNotFoundException("No AuthenticationProvider found for " + authentication.getClass());
    }
}
```

<h4>Authentication Provider</h4>
<p>An AuthenticationProvider is a specialist. Each provider is designed with the knowledge of how to authenticate a specific type of authentication request.</p>
<p>Here is an overview of the key methods in the AuthenticationProvider interface:</p>

```java
public interface AuthenticationProvider {
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
    boolean supports(Class<?> authenticationType);
}
```

<p>authenticate(Authentication authentication): This method is called to perform the authentication. It takes an Authentication object as an argument, representing the user's authentication request. The method should return a fully populated Authentication object if the authentication is successful, or throw an exception (typically an instance of AuthenticationException) if the authentication fails.</p>

<p>supports(Class<?> authenticationType): This method indicates whether the AuthenticationProvider supports the provided type of authentication token. It returns true if the provider can process the given authentication token, and false otherwise. This method is crucial when you have multiple authentication providers, and the AuthenticationManager needs to decide which provider to use based on the type of the authentication token.</p>

<p>Implementing a custom AuthenticationProvider involves providing the logic for authentication and specifying the supported authentication token type. Spring Security comes with various built-in implementations of AuthenticationProvider, and developers can also create their own based on the specific requirements of their application.</p>

```java
public class MyAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Custom authentication logic, such as checking credentials

        // If authentication is successful, create a new Authentication object
        // and set the authorities for the authenticated user
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
    }

    @Override
    public boolean supports(Class<?> authenticationType) {
        return authenticationType.equals(UsernamePasswordAuthenticationToken.class);
    }
}
```

<p>How It All Works with the AuthenticationManager</p>
<ol>
	<li>The AuthenticationManager receives an Authentication object (holding a user's credentials).</li>
	<li>It iterates through its configured list of AuthenticationProviders.</li>
	<li>Each provider announces whether it supports the type of credentials (supports() method)</li>
	<li>If a match is found, that provider's authenticate() method is called to perform the verification.</li>
	<li>Either authentication succeeds, or an exception is thrown.</li>
</ol>

<h4>UserDetailsService And UserDetails</h4>
<p>In Spring Security, UserDetailsService and UserDetails are key components used for loading user details from different data sources like inmemory or database and representing a user's information during the authentication process.</p>
<h5>UserDetailsService</h5>
<p>The UserDetailsService interface is responsible for retrieving user-related data during the authentication process. It typically loads user details based on a username and returns an object that implements the UserDetails interface.</p>
<p>The primary method in UserDetailsService is loadUserByUsername(String username), which takes a username as an argument and returns a fully populated UserDetails object.</p>
<p>Developers need to implement this interface to provide their own logic for loading user details. This could involve retrieving user information from a database, an LDAP directory, an external service, or any other authentication source.</p>

```java
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class MyUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Implement logic to load user details from a data source (e.g., database)
        // Return a UserDetails object with the user's information
        // If user not found, throw UsernameNotFoundException
    }
}
```

<h5>UserDetails:</h5>
<p>The UserDetails interface represents the core user information required by Spring Security during the authentication process. It includes methods to retrieve the username, password, authorities (roles), account status, and additional user-related details.</p>
<p>he primary methods in UserDetails include getUsername(), getPassword(), getAuthorities(), isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired(), and isEnabled().</p>
<p>Developers can create their own implementations of UserDetails or use the provided User class in Spring Security, which is a pre-built implementation of UserDetails.</p>

```java
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    // Constructors, getters, and setters

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // Other UserDetails methods...
}
```

<h4>AuthenticationEntryPoint</h4>
<p>The AuthenticationEntryPoint interface in Spring Security plays a crucial role in managing unauthenticated requests. It dictates how the application responds when a user attempts to access a protected resource without providing valid credentials.</p>
<p>Triggered by ExceptionTranslationFilter: This filter intercepts security exceptions and checks if they indicate an authentication failure. If it detects an unauthenticated request, it triggers the configured AuthenticationEntryPoint.</p>
<p>Commence Authentication: The AuthenticationEntryPoint is responsible for initiating the authentication process. How it does this depends on the chosen implementation:</p>
<ul>
	<li>Redirect: Commonly, it redirects the user to a login page where they can enter their credentials and attempt authentication.</li>
	<li>Challenge Response: It might send a challenge (e.g., HTTP header) prompting the user's browser to display a login dialog.</li>
	<li>Custom Logic: You can even implement custom behavior, such as returning a specific error message or throwing an exception.</li>
</ul>

```java
public interface AuthenticationEntryPoint {
    void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException;
}
```

<p>The commence method is called when an unauthenticated user attempts to access a secured resource, and their authentication attempt fails. The method receives the following parameters:</p>

<p>Developers can implement the AuthenticationEntryPoint interface to customize the behavior when authentication fails. Common use cases include redirecting users to a login page, sending a JSON response with an authentication error message, or performing other actions based on the specific requirements of the application.</p>

```java
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // Customize the behavior when authentication fails
        // For example, redirect to a login page or send a JSON response with an error message

        // In this example, redirecting to a login page
        response.sendRedirect("/login");
    }
}
```

<p>To use a custom AuthenticationEntryPoint, it needs to be configured in the Spring Security configuration. For example:</p>

```java
 http
    .authorizeRequests()
        .antMatchers("/secured/**").authenticated()
        .anyRequest().permitAll()
        .and()
    .formLogin()
        .loginPage("/login")
        .and()
    .exceptionHandling()
        .authenticationEntryPoint(authenticationEntryPoint);
```


<h4>AbstractAuthenticationProcessingFilter</h4>
<p>In Spring Security, AbstractAuthenticationProcessingFilter is an abstract class that serves as a base class for implementing custom authentication filters</p>
<p>It provides a template and common logic, leaving the specifics of credential extraction and verification to subclasses.</p>
<p>Before the credentials can be authenticated, Spring Security typically requests the credentials by using AuthenticationEntryPoint.</p>
<p>Next, the AbstractAuthenticationProcessingFilter can authenticate any authentication requests that are submitted to it.</p>
<img src="https://docs.spring.io/spring-security/reference/_images/servlet/authentication/architecture/abstractauthenticationprocessingfilter.png">

<ol>
	<li>The user submits their credentials through an HTTP request.</li>
	<li>The AbstractAuthenticationProcessingFilter (e.g., UsernamePasswordAuthenticationFilter for username/password) creates an Authentication object based on the provided credentials.</li>
	<li>The created Authentication object is passed into the AuthenticationManager for authentication.</li>
	<li>The AuthenticationManager attempts to authenticate the user using the configured authentication providers.</li>
	<li>If authentication fails, the following steps are performed:</li>
	<ul>
		<li>SecurityContextHolder is cleared out.</li>
		<li>RememberMeServices.loginFail is invoked (if remember me is configured).</li>
		<li>AuthenticationFailureHandler is invoked.</li>
	</ul>
	<li>If authentication is successful, the following steps are performed:</li>
	<ul>
		<li>SessionAuthenticationStrategy is notified of a new login.</li>
		<li>The authenticated Authentication object is set on the SecurityContextHolder.</li>
		<li>RememberMeServices.loginSuccess is invoked (if remember me is configured).</li>
		<li>An InteractiveAuthenticationSuccessEvent is published through ApplicationEventPublisher.</li>
		<li>AuthenticationSuccessHandler is invoked.</li>
	</ul>
</ol>