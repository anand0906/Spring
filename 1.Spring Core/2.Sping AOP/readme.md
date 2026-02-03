# Spring Aspect Oriented Programming (AOP)

Spring AOP (Aspect-Oriented Programming) is a framework provided by the Spring framework that enables developers to separate and modularize cross-cutting concerns in their applications.

## Table of Contents

- [What are Cross-Cutting Concerns?](#what-are-cross-cutting-concerns)
- [Key Concepts of AOP](#key-concepts-of-aop)
  - [Aspect](#aspect)
  - [Join Point](#join-point)
  - [Advice](#advice)
  - [Pointcut](#pointcut)
- [Setup](#setup)
- [Types of Advice](#types-of-advice)
  - [Around Advice](#around-advice)
  - [Before Advice](#before-advice)
  - [After Advice](#after-advice)
  - [After Returning Advice](#after-returning-advice)
  - [After Throwing Advice](#after-throwing-advice)
- [Reusable Pointcuts](#reusable-pointcuts)

---

## What are Cross-Cutting Concerns?

Cross-cutting concerns are functionalities that span multiple parts of an application but are not directly related to the core business logic. Common examples include:

- Logging
- Security checks
- Error handling
- Transaction management
- Performance monitoring

**The Problem:** In traditional programming, these concerns are scattered throughout the codebase, making it messy and harder to maintain.

**The Solution:** AOP helps you keep these background tasks separate from your main code, making everything cleaner and more organized. The common code runs automatically with respect to the classes that need it.

---

## Key Concepts of AOP

### Aspect

An **Aspect** is a class that implements cross-cutting concerns.

**How to declare:**
- Annotate the class with `@Aspect`
- The class must also be annotated with `@Component` or its derivatives

```java
@Component
@Aspect
public class LoggingAspect {
    // Advice methods go here
}
```

### Join Point

A **Join Point** is a specific point in the application execution where cross-cutting logic can be applied, such as:

- Method execution
- Exception handling
- Changing object variable values

> **Note:** In Spring AOP, a join point is always the execution of a method.

### Advice

**Advice** is a method in the aspect class that contains the implementation of the cross-cutting concern. It gets executed at selected join points.

**Types of Advice:**

| Type | Annotation | Description |
|------|------------|-------------|
| **Before** | `@Before` | Executes before the join point |
| **After Returning** | `@AfterReturning` | Executes after the join point finishes successfully |
| **After Throwing** | `@AfterThrowing` | Executes if an exception is thrown from the join point |
| **After** | `@After` | Executes after the join point (whether it throws an exception or not) |
| **Around** | `@Around` | Executes around the join point (before and after) |

### Pointcut

A **Pointcut** is an expression used to identify where advices should be applied. It determines exactly which methods of Spring beans need the advice.

**Syntax:**

```java
execution(<modifiers> <return-type> <fully-qualified-class-name>.<method-name>(parameters))
```

**Components:**

- `execution` - Pointcut designator (tells Spring the join point is method execution)
- `<modifiers>` - Access specifier (public, protected, private) - *Optional*
- `<return-type>` - Return type of the method - *Mandatory* (use `*` for any)
- `<fully-qualified-class-name>` - Full class name - *Optional* (use `*` as wildcard)
- `<method-name>` - Method name - *Mandatory* (use `*` as wildcard)
- `parameters` - Method parameters (use `..` to match any parameters)

**Common Pointcut Examples:**

| Pointcut Expression | Description |
|---------------------|-------------|
| `execution(public * *(..))` | Any public method |
| `execution(* service*(..))` | Any method with name beginning with "service" |
| `execution(* com.infy.service.*.*(..))` | Any method in the `com.infy.service` package |
| `execution(* com.infy.service.CustomerServiceImpl.*(..))` | Any method in `CustomerServiceImpl` class |
| `execution(public * com.infy.repository.CustomerRepository.*(..))` | Any public method in `CustomerRepository` |
| `execution(public String com.infy.repository.CustomerRepository.*(..))` | Any public method in `CustomerRepository` that returns a String |

---

## Setup

To use Spring AOP and AspectJ in a Spring Boot project, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

This starter includes:
- **Spring AOP** - Provides basic AOP capabilities
- **AspectJ** - Provides a complete AOP framework

---

## Types of Advice

### Around Advice

**Around advice** executes before and after the join point, giving you complete control over method execution.

**Key Features:**
- Can perform custom logic before and after method invocation
- Can decide whether to proceed with method execution or skip it
- Can modify method behavior

**Annotation:** `@Around`

**Example:**

```java
public class MathService {
    public double divide(int numerator, int denominator) {
        return numerator / denominator;
    }
}

@Component
@Aspect
public class LoggingAspect {

    @Around("execution(* com.example.service.MathService.divide(..))")
    public Object logAndHandleDivision(ProceedingJoinPoint joinPoint) throws Throwable {
        // Before method execution
        System.out.println("Before method execution: Logging...");

        // Access method arguments
        Object[] methodArgs = joinPoint.getArgs();
        int numerator = (int) methodArgs[0];
        int denominator = (int) methodArgs[1];

        // Custom validation logic
        if (denominator == 0) {
            System.out.println("Denominator is zero. Cannot divide.");
            return Double.NaN; // Return special value for division by zero
        }

        // Proceed with method execution
        Object result = joinPoint.proceed();

        // After method execution
        System.out.println("After method execution: Logging...");

        return result;
    }
}
```

---

### Before Advice

**Before advice** executes custom logic before a method is invoked. Commonly used for logging or validation.

**Key Features:**
- Executes before the target method
- Cannot modify method input or output
- Cannot prevent method execution

**Annotation:** `@Before`

**Example:**

```java
@Component
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }
}

@Component
@Aspect
public class LoggingAspect {

    @Before("execution(* com.example.Calculator.*(..))")
    public void logBeforeMethodExecution(JoinPoint joinPoint) {
        System.out.println("Before method execution: Logging..." + joinPoint.getSignature());
    }
}
```

**Output:** Every time a method in the `Calculator` class is invoked, the logging message will be printed before the method executes.

---

### After Advice

**After advice** executes after the join point completes, regardless of whether it throws an exception or not. Commonly used for resource cleanup.

**Key Features:**
- Executes after method completion (success or failure)
- Useful for cleanup tasks (closing connections, deleting temp files)
- Cannot access the return value

**Annotation:** `@After`

**Example:**

```java
public class MessagingService {
    public void sendMessage(String message) {
        System.out.println("Message sent: " + message);
    }
}

@Component
@Aspect
public class LoggingAspect {

    @After("execution(* com.example.MessagingService.sendMessage(String)) && args(message)")
    public void logAfterMessageSent(String message) {
        System.out.println("After sending message: Log - Message sent: " + message);
    }
}
```

---

### After Returning Advice

**After returning advice** executes after a join point completes successfully (without throwing an exception).

**Key Features:**
- Executes only on successful method completion
- Can access the return value
- Does not execute if an exception is thrown

**Annotation:** `@AfterReturning`

**Example:**

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int divide(int numerator, int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return numerator / denominator;
    }
}

@Component
@Aspect
public class LoggingAspect {

    @AfterReturning(
            pointcut = "execution(* com.example.Calculator.*(..))",
            returning = "result")
    public void logAfterMethodExecution(Object result) {
        System.out.println("After method execution: Logging...");
        System.out.println("Method result: " + result);
    }
}
```

**Alternative (without accessing result):**

```java
@AfterReturning("execution(* com.example.Calculator.*(..))")
public void logAfterMethodExecution(JoinPoint joinPoint) {
    System.out.println("After method execution: Logging..." + joinPoint.getSignature());
}
```

---

### After Throwing Advice

**After throwing advice** executes when a method throws an exception. Allows centralized exception handling without modifying the original method.

**Key Features:**
- Executes only when an exception is thrown
- Can access the thrown exception
- Useful for logging, notifications, or custom error handling

**Annotation:** `@AfterThrowing`

**Example:**

```java
public class BankAccount {
    private double balance;

    public BankAccount(double balance) {
        this.balance = balance;
    }

    public void withdraw(double amount) {
        if (amount > balance) {
            throw new InsufficientFundsException("Insufficient funds to withdraw: " + amount);
        }
        balance -= amount;
        System.out.println("Withdrawal successful. Remaining balance: " + balance);
    }
}

@Component
@Aspect
public class ExceptionHandlingAspect {

    @AfterThrowing(
            pointcut = "execution(* com.example.BankAccount.withdraw(double))",
            throwing = "exception")
    public void handleInsufficientFunds(JoinPoint joinPoint, InsufficientFundsException exception) {
        System.out.println("Exception occurred: " + exception.getMessage());
        // Additional handling: send email, log to database, etc.
    }
}
```

**Generic Exception Handling (all methods):**

```java
@AfterThrowing("execution(* *(..))")
public void handleException(JoinPoint joinPoint) {
    System.out.println("Exception occurred in method: " + joinPoint.getSignature().getName());
    // Additional handling logic
}
```

---

## Reusable Pointcuts

The `@Pointcut` annotation allows you to define reusable pointcut expressions, avoiding redundancy and centralizing pointcut definitions.

**Benefits:**
- Define once, use multiple times
- Keeps code modular and maintainable
- Easier to update pointcut logic

**Example:**

```java
@Component
@Aspect
public class LoggingAspect {

    // Define a reusable pointcut expression
    @Pointcut("execution(* com.example.MyService.*(..))")
    public void serviceMethods() {}

    // Advice using the reusable pointcut
    @Before("serviceMethods()")
    public void logBeforeServiceMethods() {
        System.out.println("Before service method execution: Logging...");
    }

    // Another advice using the same pointcut
    @Before("serviceMethods()")
    public void additionalActionBeforeServiceMethods() {
        System.out.println("Before service method execution: Additional action...");
    }
}
```

**How it works:**
1. The `@Pointcut` annotation defines a named pointcut expression (`serviceMethods()`)
2. Multiple advice methods reference this pointcut by name
3. Changes to the pointcut expression only need to be made in one place

---

## Summary

Spring AOP provides a powerful way to modularize cross-cutting concerns in your application:

- **Aspects** encapsulate cross-cutting logic
- **Join Points** define where the logic can be applied
- **Advice** contains the actual implementation
- **Pointcuts** specify which join points should trigger the advice

By using AOP, you can keep your business logic clean and separate common concerns like logging, security, and error handling into dedicated aspects.
