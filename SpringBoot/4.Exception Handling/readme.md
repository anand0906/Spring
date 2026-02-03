# Spring Boot Exception Handling & Validation

> **Clean Error Handling**: Professional error handling and validation patterns

---

## Table of Contents
1. [Introduction](#introduction)
2. [Global Exception Handling](#global-exception-handling)
3. [@ControllerAdvice + @ExceptionHandler](#controlleradvice--exceptionhandler)
4. [Custom Error Responses](#custom-error-responses)
5. [Validation with javax.validation](#validation-with-javaxvalidation)
6. [Custom Validators](#custom-validators)
7. [Handling Async Errors](#handling-async-errors)
8. [Handling Reactive Errors](#handling-reactive-errors)
9. [Best Practices](#best-practices)

---

## Introduction

Clean error handling is a hallmark of professional applications. Poor error handling leads to:
- ✗ Confusing error messages
- ✗ Security vulnerabilities (stack traces exposed)
- ✗ Poor user experience
- ✗ Difficult debugging

**Good error handling provides**:
- ✓ Clear, consistent error responses
- ✓ Proper HTTP status codes
- ✓ Secure error messages
- ✓ Easy troubleshooting
- ✓ Better user experience

---

## Global Exception Handling

### The Problem Without Global Handling

```java
// ✗ BAD: Error handling in every controller
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
}
```

**Problems**:
- Repetitive code
- Inconsistent error responses
- Hard to maintain
- No centralized logging

### The Solution: Global Exception Handler

```java
// ✓ GOOD: Centralized exception handling
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
        // No try-catch needed!
    }
}

// Global handler catches all exceptions
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

---

## @ControllerAdvice + @ExceptionHandler

### Basic Structure

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // Handle specific exception
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        
        return ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    // Handle multiple exceptions
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(Exception ex) {
        log.error("Bad request: {}", ex.getMessage());
        
        return ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    // Catch-all handler
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        
        return ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

### Custom Exceptions

```java
// Base exception
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    
    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
    
    // Getters
    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}

// Specific exceptions
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Long id) {
        super(
            String.format("%s not found with id: %d", resource, id),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }
}

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String resource, String field, String value) {
        super(
            String.format("%s already exists with %s: %s", resource, field, value),
            HttpStatus.CONFLICT,
            "DUPLICATE_RESOURCE"
        );
    }
}

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
```

### Exception Handler for Custom Exceptions

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.error("Business exception: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .status(ex.getStatus().value())
            .error(ex.getStatus().getReasonPhrase())
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(ex.getStatus()).body(error);
    }
}
```

### Using Custom Exceptions

```java
@Service
public class UserService {
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
    
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }
        return userRepository.save(user);
    }
}
```

### Handling HTTP Method Not Supported

```java
@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public ErrorResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    
    String message = String.format(
        "Method '%s' is not supported for this endpoint. Supported methods: %s",
        ex.getMethod(),
        String.join(", ", ex.getSupportedMethods())
    );
    
    return ErrorResponse.builder()
        .status(HttpStatus.METHOD_NOT_ALLOWED.value())
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
}
```

### Handling Missing Parameters

```java
@ExceptionHandler(MissingServletRequestParameterException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ErrorResponse handleMissingParams(MissingServletRequestParameterException ex) {
    
    String message = String.format(
        "Required parameter '%s' of type '%s' is missing",
        ex.getParameterName(),
        ex.getParameterType()
    );
    
    return ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
}
```

### Handling Type Mismatch

```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    
    String message = String.format(
        "Parameter '%s' should be of type '%s'",
        ex.getName(),
        ex.getRequiredType().getSimpleName()
    );
    
    return ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
}
```

---

## Custom Error Responses

### Basic Error Response

```java
@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
```

**Example Response**:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 123",
  "timestamp": "2024-01-15T10:30:45"
}
```

### Detailed Error Response

```java
@Data
@Builder
public class DetailedErrorResponse {
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String path;
    private LocalDateTime timestamp;
    private List<FieldError> fieldErrors;
    
    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
```

**Example Response**:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "path": "/api/users",
  "timestamp": "2024-01-15T10:30:45",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Email must be valid",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "age",
      "message": "Age must be at least 18",
      "rejectedValue": 15
    }
  ]
}
```

### Building Detailed Error Response

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public DetailedErrorResponse handleValidationErrors(
    MethodArgumentNotValidException ex,
    WebRequest request
) {
    List<DetailedErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> new DetailedErrorResponse.FieldError(
            error.getField(),
            error.getDefaultMessage(),
            error.getRejectedValue()
        ))
        .collect(Collectors.toList());
    
    return DetailedErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Input validation failed")
        .errorCode("VALIDATION_ERROR")
        .path(((ServletWebRequest) request).getRequest().getRequestURI())
        .timestamp(LocalDateTime.now())
        .fieldErrors(fieldErrors)
        .build();
}
```

### Error Response with Metadata

```java
@Data
@Builder
public class ApiErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}

// Usage
@ExceptionHandler(InsufficientBalanceException.class)
public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(
    InsufficientBalanceException ex
) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("currentBalance", ex.getCurrentBalance());
    metadata.put("requiredAmount", ex.getRequiredAmount());
    metadata.put("shortfall", ex.getShortfall());
    
    ApiErrorResponse error = ApiErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Insufficient Balance")
        .message(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .metadata(metadata)
        .build();
    
    return ResponseEntity.badRequest().body(error);
}
```

### Localized Error Messages

```java
@ControllerAdvice
public class LocalizedExceptionHandler {
    
    @Autowired
    private MessageSource messageSource;
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException ex,
        Locale locale
    ) {
        String localizedMessage = messageSource.getMessage(
            "error.resource.notfound",
            new Object[]{ex.getResourceName(), ex.getResourceId()},
            locale
        );
        
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(localizedMessage)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

```properties
# messages.properties (English)
error.resource.notfound={0} not found with id: {1}

# messages_es.properties (Spanish)
error.resource.notfound={0} no encontrado con id: {1}

# messages_fr.properties (French)
error.resource.notfound={0} introuvable avec l'id: {1}
```

---

## Validation with javax.validation

### Basic Validation Annotations

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    
    @NotNull(message = "Name is required")
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be less than 120")
    private Integer age;
    
    @Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be valid"
    )
    private String phoneNumber;
    
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    @Future(message = "Appointment date must be in the future")
    private LocalDateTime appointmentDate;
}
```

### Using @Valid in Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        // If validation fails, MethodArgumentNotValidException is thrown
        User user = userService.createUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserDTO userDTO
    ) {
        User user = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(user);
    }
}
```

### Handling Validation Errors

```java
@ControllerAdvice
public class ValidationExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        List<ValidationErrorResponse.FieldError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());
        
        return ValidationErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();
    }
}

@Data
@Builder
class ValidationErrorResponse {
    private int status;
    private String message;
    private List<FieldError> errors;
    private LocalDateTime timestamp;
    
    @Data
    @AllArgsConstructor
    static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
```

### Nested Object Validation

```java
@Data
public class OrderDTO {
    
    @NotNull
    @Valid  // ← Important: validate nested object
    private CustomerDTO customer;
    
    @NotEmpty(message = "Order must have at least one item")
    @Valid  // ← Validate each item in list
    private List<OrderItemDTO> items;
}

@Data
class CustomerDTO {
    @NotBlank
    private String name;
    
    @Email
    private String email;
    
    @Valid  // ← Nested validation
    private AddressDTO address;
}

@Data
class AddressDTO {
    @NotBlank
    private String street;
    
    @NotBlank
    private String city;
    
    @Pattern(regexp = "\\d{5}")
    private String zipCode;
}
```

### Validation Groups

```java
public interface CreateValidation {}
public interface UpdateValidation {}

@Data
public class UserDTO {
    
    @Null(groups = CreateValidation.class)  // Must be null on create
    @NotNull(groups = UpdateValidation.class)  // Must be present on update
    private Long id;
    
    @NotBlank(groups = {CreateValidation.class, UpdateValidation.class})
    private String name;
    
    @NotBlank(groups = CreateValidation.class)  // Required only on create
    private String password;
}

// In controller
@PostMapping
public ResponseEntity<User> create(
    @Validated(CreateValidation.class) @RequestBody UserDTO dto
) {
    // ...
}

@PutMapping("/{id}")
public ResponseEntity<User> update(
    @PathVariable Long id,
    @Validated(UpdateValidation.class) @RequestBody UserDTO dto
) {
    // ...
}
```

### Conditional Validation

```java
@Data
@ConditionalValidation  // Custom annotation
public class PaymentDTO {
    
    @NotNull
    private PaymentMethod paymentMethod;
    
    // Only required if paymentMethod is CREDIT_CARD
    private String cardNumber;
    
    // Only required if paymentMethod is BANK_TRANSFER
    private String accountNumber;
}

enum PaymentMethod {
    CREDIT_CARD, BANK_TRANSFER, CASH
}
```

---

## Custom Validators

### Creating a Custom Validator

**Step 1: Create the Annotation**
```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Step 2: Implement the Validator**
```java
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        // Initialization if needed
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;  // Use @NotNull for null check
        }
        return PHONE_PATTERN.matcher(value).matches();
    }
}
```

**Step 3: Use the Validator**
```java
@Data
public class ContactDTO {
    
    @ValidPhoneNumber(message = "Phone number must be in E.164 format")
    private String phoneNumber;
}
```

### Cross-Field Validation

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchesValidator.class)
public @interface PasswordMatches {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PasswordMatchesValidator 
    implements ConstraintValidator<PasswordMatches, RegistrationDTO> {
    
    @Override
    public boolean isValid(RegistrationDTO dto, ConstraintValidatorContext context) {
        if (dto.getPassword() == null || dto.getConfirmPassword() == null) {
            return true;
        }
        
        boolean valid = dto.getPassword().equals(dto.getConfirmPassword());
        
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Password and confirm password do not match"
            )
            .addPropertyNode("confirmPassword")
            .addConstraintViolation();
        }
        
        return valid;
    }
}

@Data
@PasswordMatches  // ← Class-level validation
public class RegistrationDTO {
    @NotBlank
    private String username;
    
    @NotBlank
    @Size(min = 8)
    private String password;
    
    @NotBlank
    private String confirmPassword;
}
```

### Date Range Validator

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    String startDate();
    String endDate();
}

public class DateRangeValidator 
    implements ConstraintValidator<ValidDateRange, Object> {
    
    private String startDateField;
    private String endDateField;
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateField = constraintAnnotation.startDate();
        this.endDateField = constraintAnnotation.endDate();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field startField = value.getClass().getDeclaredField(startDateField);
            Field endField = value.getClass().getDeclaredField(endDateField);
            
            startField.setAccessible(true);
            endField.setAccessible(true);
            
            LocalDate startDate = (LocalDate) startField.get(value);
            LocalDate endDate = (LocalDate) endField.get(value);
            
            if (startDate == null || endDate == null) {
                return true;
            }
            
            return endDate.isAfter(startDate);
            
        } catch (Exception e) {
            return false;
        }
    }
}

@Data
@ValidDateRange(startDate = "startDate", endDate = "endDate")
public class EventDTO {
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
}
```

### Database Unique Validator

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {
    String message() default "Email already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) {
            return true;
        }
        return !userRepository.existsByEmail(email);
    }
}

@Data
public class UserRegistrationDTO {
    
    @NotBlank
    @Email
    @UniqueEmail  // ← Custom database validation
    private String email;
}
```

### Enum Validator

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
public @interface ValidEnum {
    Class<? extends Enum<?>> enumClass();
    String message() default "Invalid value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    
    private Class<? extends Enum<?>> enumClass;
    
    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        return Arrays.stream(enumClass.getEnumConstants())
            .anyMatch(e -> e.name().equals(value));
    }
}

// Usage
@Data
public class OrderDTO {
    
    @ValidEnum(enumClass = OrderStatus.class, message = "Invalid order status")
    private String status;
}

enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
```

---

## Handling Async Errors

### Async Controller Methods

```java
@RestController
@RequestMapping("/api/async")
public class AsyncController {
    
    @Autowired
    private AsyncService asyncService;
    
    @GetMapping("/data")
    public CompletableFuture<String> getData() {
        return asyncService.fetchDataAsync()
            .exceptionally(ex -> {
                // Handle exception in async chain
                throw new AsyncProcessingException("Failed to fetch data", ex);
            });
    }
}
```

### Exception Handler for Async

```java
@ControllerAdvice
public class AsyncExceptionHandler {
    
    @ExceptionHandler(AsyncProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAsyncException(AsyncProcessingException ex) {
        
        Throwable cause = ex.getCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();
        
        return ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("Async operation failed: " + message)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

### Async with @Async

```java
@Service
public class EmailService {
    
    @Async
    public CompletableFuture<Void> sendEmail(String to, String subject, String body) {
        try {
            // Send email logic
            mailSender.send(to, subject, body);
            return CompletableFuture.completedFuture(null);
        } catch (MailException ex) {
            // Return failed future
            return CompletableFuture.failedFuture(
                new EmailSendException("Failed to send email", ex)
            );
        }
    }
}
```

### Async Exception Handler Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}

@Slf4j
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Async exception in method: {}", method.getName(), ex);
        log.error("Parameters: {}", Arrays.toString(params));
        
        // Send alert, log to monitoring system, etc.
    }
}
```

### Combining Multiple Async Operations

```java
@Service
public class OrderService {
    
    public CompletableFuture<OrderResult> processOrder(OrderDTO order) {
        
        CompletableFuture<Void> payment = processPaymentAsync(order)
            .exceptionally(ex -> {
                throw new PaymentException("Payment failed", ex);
            });
        
        CompletableFuture<Void> inventory = updateInventoryAsync(order)
            .exceptionally(ex -> {
                throw new InventoryException("Inventory update failed", ex);
            });
        
        CompletableFuture<Void> notification = sendNotificationAsync(order)
            .exceptionally(ex -> {
                // Just log, don't fail the entire operation
                log.error("Notification failed", ex);
                return null;
            });
        
        return CompletableFuture.allOf(payment, inventory, notification)
            .thenApply(v -> new OrderResult("Order processed successfully"));
    }
}
```

---

## Handling Reactive Errors

### Reactive Error Handling with onErrorResume

```java
@RestController
@RequestMapping("/api/reactive")
public class ReactiveController {
    
    @Autowired
    private ReactiveUserService userService;
    
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .onErrorResume(UserNotFoundException.class, ex -> {
                // Return default value or alternative
                return Mono.just(User.getDefaultUser());
            })
            .onErrorResume(DatabaseException.class, ex -> {
                // Convert to different exception
                return Mono.error(new ServiceUnavailableException("Database is down"));
            });
    }
}
```

### Global Reactive Exception Handler

```java
@ControllerAdvice
public class ReactiveExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Mono<ErrorResponse>> handleNotFound(
        ResourceNotFoundException ex
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Mono.just(error));
    }
}
```

### Using onErrorMap

```java
@Service
public class ReactiveOrderService {
    
    public Mono<Order> createOrder(OrderDTO orderDTO) {
        return orderRepository.save(orderDTO)
            .onErrorMap(DataIntegrityViolationException.class, ex -> 
                new DuplicateOrderException("Order already exists")
            )
            .onErrorMap(TimeoutException.class, ex ->
                new ServiceUnavailableException("Database timeout")
            );
    }
}
```

### Reactive Validation

```java
@RestController
@RequestMapping("/api/reactive/users")
public class ReactiveUserController {
    
    @PostMapping
    public Mono<User> createUser(@Valid @RequestBody Mono<UserDTO> userDtoMono) {
        return userDtoMono
            .flatMap(userDTO -> {
                // Additional reactive validation
                return validateUserAsync(userDTO)
                    .flatMap(valid -> {
                        if (!valid) {
                            return Mono.error(
                                new ValidationException("User validation failed")
                            );
                        }
                        return userService.createUser(userDTO);
                    });
            })
            .onErrorResume(ValidationException.class, ex -> {
                // Handle validation error
                return Mono.error(ex);
            });
    }
    
    private Mono<Boolean> validateUserAsync(UserDTO userDTO) {
        // Async validation logic
        return userRepository.existsByEmail(userDTO.getEmail())
            .map(exists -> !exists);
    }
}
```

### Error Handling with Flux

```java
@GetMapping("/users")
public Flux<User> getAllUsers() {
    return userService.findAll()
        .onErrorResume(ex -> {
            log.error("Error fetching users", ex);
            return Flux.empty();  // Return empty stream on error
        })
        .onErrorContinue((ex, item) -> {
            // Continue processing other items
            log.error("Error processing user: {}", item, ex);
        });
}
```

### Timeout Handling

```java
@GetMapping("/users/{id}")
public Mono<User> getUser(@PathVariable Long id) {
    return userService.findById(id)
        .timeout(Duration.ofSeconds(5))
        .onErrorResume(TimeoutException.class, ex -> {
            return Mono.error(new ServiceUnavailableException(
                "User service timeout"
            ));
        });
}
```

### Retry Logic

```java
@Service
public class ReactiveExternalService {
    
    public Mono<String> fetchDataWithRetry() {
        return webClient.get()
            .uri("/external-api/data")
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof WebClientResponseException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    throw new ServiceUnavailableException(
                        "External service failed after retries"
                    );
                })
            );
    }
}
```

### Complete Reactive Error Handling Example

```java
@RestController
@RequestMapping("/api/reactive/orders")
public class ReactiveOrderController {
    
    @Autowired
    private ReactiveOrderService orderService;
    
    @PostMapping
    public Mono<OrderResponse> createOrder(@Valid @RequestBody Mono<OrderDTO> orderMono) {
        return orderMono
            .flatMap(order -> orderService.createOrder(order))
            .map(order -> new OrderResponse(order.getId(), "Created"))
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(ValidationException.class, ex -> 
                Mono.error(new BadRequestException(ex.getMessage()))
            )
            .onErrorResume(TimeoutException.class, ex ->
                Mono.error(new ServiceUnavailableException("Request timeout"))
            )
            .onErrorResume(Exception.class, ex -> {
                log.error("Unexpected error creating order", ex);
                return Mono.error(new InternalServerException("Order creation failed"));
            });
    }
}
```

---

## Best Practices

### 1. Use Specific Exceptions

```java
// ✓ GOOD: Specific exceptions
throw new UserNotFoundException(userId);
throw new InsufficientBalanceException(currentBalance, requiredAmount);

// ✗ BAD: Generic exceptions
throw new RuntimeException("User not found");
throw new Exception("Error occurred");
```

### 2. Don't Expose Internal Details

```java
// ✓ GOOD: User-friendly message
return ErrorResponse.builder()
    .message("Unable to process payment")
    .build();

// ✗ BAD: Exposing internals
return ErrorResponse.builder()
    .message("SQLException: Connection to MySQL failed at 192.168.1.100:3306")
    .build();
```

### 3. Use Proper HTTP Status Codes

```java
// ✓ GOOD: Correct status codes
404 NOT_FOUND       - Resource doesn't exist
400 BAD_REQUEST     - Invalid input
401 UNAUTHORIZED    - Not authenticated
403 FORBIDDEN       - Not authorized
409 CONFLICT        - Duplicate resource
422 UNPROCESSABLE   - Validation failed
500 INTERNAL_ERROR  - Server error

// ✗ BAD: Everything is 500 or 200
```

### 4. Log Exceptions Appropriately

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    // ✓ GOOD: Log with context
    log.error("Unexpected error processing request", ex);
    
    // ✗ BAD: No logging
    // ✗ BAD: log.error(ex.getMessage()) - loses stack trace
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.builder()
            .message("An error occurred")
            .build());
}
```

### 5. Consistent Error Response Format

```java
// ✓ GOOD: Consistent structure across all endpoints
{
  "status": 404,
  "message": "User not found",
  "timestamp": "2024-01-15T10:30:45",
  "errorCode": "USER_NOT_FOUND"
}

// ✗ BAD: Different formats
// Endpoint 1: {"error": "Not found"}
// Endpoint 2: {"message": "Not found", "code": 404}
```

### 6. Validate Early

```java
// ✓ GOOD: Validate at controller level
@PostMapping
public User createUser(@Valid @RequestBody UserDTO dto) {
    return userService.createUser(dto);
}

// ✗ BAD: Validate deep in service layer
public User createUser(UserDTO dto) {
    // ... lots of processing ...
    if (dto.getName() == null) {
        throw new ValidationException("Name required");
    }
}
```

### 7. Handle Specific Before General

```java
@ControllerAdvice
public class ExceptionHandler {
    
    // ✓ GOOD: Order matters - specific first
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(...) { }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(...) { }
    
    @ExceptionHandler(Exception.class)  // Last - catch-all
    public ResponseEntity<?> handleGeneral(...) { }
}
```

### 8. Don't Swallow Exceptions

```java
// ✓ GOOD: Handle or propagate
try {
    userService.deleteUser(id);
} catch (UserNotFoundException ex) {
    throw new ResourceNotFoundException("User", id);
}

// ✗ BAD: Silent failure
try {
    userService.deleteUser(id);
} catch (Exception ex) {
    // Silent - no logging, no throwing
}
```

### 9. Use Error Codes for Client Logic

```java
@Data
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private String errorCode;  // ← For programmatic handling
    private LocalDateTime timestamp;
}

// Client can check errorCode
if (error.errorCode === "INSUFFICIENT_BALANCE") {
    // Show add funds dialog
}
```

### 10. Test Exception Handling

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.findById(1L))
            .thenThrow(new UserNotFoundException(1L));
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found with id: 1"))
            .andExpect(jsonPath("$.status").value(404));
    }
}
```

---

## Complete Example

### Domain Exception

```java
@Getter
public class OrderException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;
    private final Map<String, Object> details;
    
    public OrderException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    public OrderException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
```

### Service Layer

```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Transactional
    public Order createOrder(OrderDTO orderDTO) {
        // Validation
        if (orderDTO.getItems().isEmpty()) {
            throw new OrderException(
                "Order must contain at least one item",
                HttpStatus.BAD_REQUEST,
                "EMPTY_ORDER"
            );
        }
        
        // Check inventory
        for (OrderItemDTO item : orderDTO.getItems()) {
            if (!inventoryService.isAvailable(item.getProductId(), item.getQuantity())) {
                throw new OrderException(
                    "Insufficient inventory",
                    HttpStatus.CONFLICT,
                    "INSUFFICIENT_INVENTORY"
                )
                .addDetail("productId", item.getProductId())
                .addDetail("requested", item.getQuantity())
                .addDetail("available", inventoryService.getAvailable(item.getProductId()));
            }
        }
        
        // Create order
        try {
            return orderRepository.save(orderDTO.toEntity());
        } catch (DataIntegrityViolationException ex) {
            throw new OrderException(
                "Duplicate order",
                HttpStatus.CONFLICT,
                "DUPLICATE_ORDER"
            );
        }
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody OrderDTO orderDTO
    ) {
        Order order = orderService.createOrder(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrderResponse(order));
    }
}
```

### Global Exception Handler

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(
        OrderException ex,
        WebRequest request
    ) {
        log.error("Order exception: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .status(ex.getStatus().value())
            .error(ex.getStatus().getReasonPhrase())
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .details(ex.getDetails())
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(ex.getStatus()).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        List<FieldError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());
        
        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## Summary

**Key Takeaways**:

1. **Global Handling**: Use `@ControllerAdvice` for centralized exception handling
2. **Custom Exceptions**: Create meaningful, specific exception classes
3. **Error Responses**: Maintain consistent error response format
4. **Validation**: Use `javax.validation` annotations for input validation
5. **Custom Validators**: Create validators for complex business rules
6. **HTTP Status**: Use correct HTTP status codes
7. **Async Errors**: Handle `CompletableFuture` exceptions properly
8. **Reactive Errors**: Use `onErrorResume`, `onErrorMap` for reactive streams
9. **Logging**: Always log exceptions with context
10. **Security**: Never expose sensitive internal details

**Remember**: Clean error handling improves user experience, security, and maintainability! 🚀
