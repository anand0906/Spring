# Spring Aspect Oriented Programming (AOP)

Spring AOP (Aspect-Oriented Programming) is a framework provided by the Spring framework that enables developers to seperate and modularize cross-cutting concerns in their applications code.

## Cross Cutting Concerns

Imagine you're building a big software project, and there are certain things that need to happen across different parts of your code. These things are not directly related to what each part of your code , they're more like common background tasks that multiple parts of your code need to handle. Examples could be logging, security checks, or error handling.

These are called cross cutting concerns

Now, in regular programming, you would have to write this code throughout your code, making it a bit messy and harder to understand.

To Handle this issue, AOP helps you keep these background tasks separate from your main code, making everything cleaner and more organized.

Simply, when we are writing code for an application, some code will common for some classes, which is nt directly depedent on individual class logics, but those are tightly coupled with that classes. It looks bit messy, so we will try to separate those common code and make it to run automatically with resepct to that classes.

These way of programming is called aspect oriented programming

## Key concepts of AOP

### Aspect

Aspect is a class that implements the cross-cutting concerns

To declare a class as an aspect it should be annotated with the @Aspect annotation

It should be applied to the class which is annotated with @Component annotation or with derivatives of it.

### Joint Point

Join point is a specific point in the application such as method execution, exception handling, changing object variable values, etc during its execution.

It is basically defines when the common code should be executed

In Spring AOP a join point is always the execution of a method.

### Advice

Advice is a method of the aspect class that provides the implementation for the cross-cutting concern.

It gets executed at the selected join point(s)

There are different types of advices in spring aop

- **Before :** The advice gets executed before the join-point.
- **After Returning :** The advice gets executed after the execution of the join-point finishes.
- **After Throwing :** The advice gets executed if any exception is thrown from the join-point.
- **After :** The advice gets executed after the execution of the join-point whether it throws an exception or not.
- **Around :** The advice gets executed around the join-point, which means that it is invoked before the join-point and after the execution of the join-point.

### PointCut

Pointcut represents an expression used to identify in which places advices should be associated

It is used to determine exactly for which methods of Spring beans advice needs to be applied.

It has the following syntax:

```java
execution(<modifiers> <return-type> <fully qualified class name>.<method-name>(parameters))
```

execution : It is called as pointcut designator, It tells spring that joint point is execution of matching method

<modifiers> : It determines the access specifier of matching method.It could either be public, protected, or private. It is not mandatory.

<return-type> : It determines the return type of the method in order for a join point to be matched. It is mandatory. If the return type doesn't matter wildcard * is used.

<fully qualified class name> :  specifies the fully qualified name of the class which has methods on the execution of which advice gets executed. It is optional. You can also use * wildcard as name or part of a name.

<method-name> specifies the name of the method on the execution of which advice gets executed. It is mandatory. You can also use * wildcard as name or part of a name.

parameters are used for matching parameters. To skip parameter filtering, use two dots .. as parameters.

| Pointcut | Description |
|----------|-------------|
| execution(public * *(..)) | execution of any public method |
| execution(* service*(..)) | execution of any method with a name beginning with "service" |
| execution(* com.infy.service.*.*(..)) | execution of any method defined in the com.infy.service package |
| execution(* com.infy.service.CustomerServiceImpl.*(..)) | execution of any method defined in CustomerServiceImpl of com.infy.service package |
| execution(public * com.infy.repository.CustomerRepository.*(..)) | execution of any public method in CustomerRepository of com.infy.repository package |
| execution(public String com.infy.repository.CustomerRepository.*(..)) | execution of all public method in CustomerRepository of com.infy.repository package that returns a String |

## Setup

To use Spring AOP and AspectJ in Spring Boot project you have to add spring-boot-starter-aop starter in pom.xml file as follows:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

This starter adds following key dependencies:

- Spring AOP which provides basic AOP capabilities
- AspectJ which provides a complete AOP framework

## Around Advice

It executed around the join point, i.e before and after the execution of the target method

It is declared using @Around annotation

You can perform custom logic before and after the method invocation, and you can even decide whether to proceed with the method execution or skip it altogether.

This is powerful because it gives you the ability to modify the method's behavior.

```java
public class MathService {

    public double divide(int numerator, int denominator) {
        return numerator / denominator;
    }
}


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class LoggingAspect {

    @Around("execution(* com.example.service.MathService.divide(..))")
    public Object logAndHandleDivision(ProceedingJoinPoint joinPoint) throws Throwable {
        // Before the method execution
        System.out.println("Before method execution: Logging...");

        // Accessing method arguments
        Object[] methodArgs = joinPoint.getArgs();
        int numerator = (int) methodArgs[0];
        int denominator = (int) methodArgs[1];

        // Handling division by zero
        if (denominator == 0) {
            System.out.println("Denominator is zero. Cannot divide.");
            return Double.NaN; // Returning a special value for division by zero
        }

        // Proceeding with the method execution
        Object result = joinPoint.proceed();

        // After the method execution
        System.out.println("After method execution: Logging...");

        return result;
    }
}
```

In The Above Example, The @Around annotation indicates that this is an "around advice."

The advice method logAndHandleDivision takes a ProceedingJoinPoint parameter, which allows you to control the method execution.

Before the actual method (divide) is executed, you can perform custom logic (logging in this case).

You can access the method arguments using joinPoint.getArgs() and modify them if needed.

You can choose to proceed with the method execution using joinPoint.proceed() or skip it based on some conditions.

After the method execution, you can perform additional actions.

This is a powerful way to wrap custom logic around a method, providing a centralized way to handle common concerns.

## Before Advice

"before advice" is a type of advice that allows you to execute custom logic before a method is invoked. It provides a way to perform actions, such as logging or validation, prior to the actual execution of the target method.

Keep in mind that while "before advice" is useful for tasks like logging, it doesn't allow you to modify the method's input or output.

```java 
@component
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }
}

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@component
@Aspect
public class LoggingAspect {

    @Before("execution(* com.example.Calculator.*(..))")
    public void logBeforeMethodExecution(JoinPoint joinPoint) {
        System.out.println("Before method execution: Logging..."+joinPoint.getSignature());
    }
}
```

The @Before annotation indicates that this is a "before advice."

The advice method logBeforeMethodExecution is executed before any method in the Calculator class (execution(* com.example.Calculator.*(..)).

When you run your application with Spring AOP configured, every time a method in the Calculator class is invoked, the "Before method execution: Logging..." message will be printed to the console.

## After Advice

This advice is declared using @After annotation. It is executed after the execution of the actual method(fetchCustomer), even if it throws an exception during execution. It is commonly used for resource cleanup such as temporary files or closing database connections. The following is an example of this advice:

```java
public class MessagingService {

    public void sendMessage(String message) {
        // Code to send the message
        System.out.println("Message sent: " + message);
    }
}


import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class LoggingAspect {

    @After("execution(* com.example.MessagingService.sendMessage(String)) && args(message)")
    public void logAfterMessageSent(String message) {
        System.out.println("After sending message: Log - Message sent: " + message);
    }
}
```

The @After annotation indicates that this is an "after advice."

The advice method logAfterMessageSent is executed after the sendMessage method in the MessagingService class.

This can be helpful when you want to perform actions based on the specific method that was intercepted or inspect the arguments passed to that method.

## After Returning Advice

This advice is declared using @AfterReturning annotation. It gets executed after joinpoint finishes its execution.

f the target method throws an exception the advice is not executed

```java
public class Calculator {

    public int add(int a, int b) {
        int result = a + b;
        return result;
    }

    public int divide(int numerator, int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return numerator / denominator;
    }
}

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class LoggingAspect {

    @AfterReturning(
            pointcut = "execution(* com.example.Calculator.*(..))",
            returning = "result")
    public void logAfterMethodExecution(Object result) {
        System.out.println("After method execution: Logging...");

        // Accessing the result returned by the intercepted method
        System.out.println("Method result: " + result);
    }
}
```

The @AfterReturning annotation indicates that this is an "after returning advice."

The pointcut expression targets all methods in the Calculator class (execution(* com.example.Calculator.*(..)).

The returning attribute specifies the name of the parameter in the advice method (result) that will receive the value returned by the intercepted method.

When you run your application with Spring AOP configured, every time a method in the Calculator class is invoked, and it successfully returns a result, the "After method execution: Logging..." message will be printed to the console, along with the result returned by the method.

```java
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class LoggingAspect {

    @AfterReturning("execution(* com.example.Calculator.*(..))")
    public void logAfterMethodExecution(JoinPoint joinPoint) {
        System.out.println("After method execution: Logging..."+joinPoint.getSignature());
    }
}
```

## AfterThrowing Advice

This advice is defined using @AfterThrowing annotation. It gets executed after an exception is thrown from the target method.

This allows you to handle or log exceptions in a centralized way without modifying the original method.

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


import org.aspectj.lang.AfterThrowing;
import org.aspectj.lang.JoinPoint;

public class ExceptionHandlingAspect {

    @AfterThrowing(
            pointcut = "execution(* com.example.BankAccount.withdraw(double))",
            throwing = "exception")
    public void handleInsufficientFunds(JoinPoint joinPoint, InsufficientFundsException exception) {
        System.out.println("Exception occurred: " + exception.getMessage());

        // Additional handling logic can be added here, e.g., sending an email, logging, etc.
    }
}
```

The @AfterThrowing annotation indicates that this is an "after throwing advice."

The pointcut expression targets the withdraw method in the BankAccount class with a double parameter.

The throwing attribute specifies the name of the parameter in the advice method (exception) that will receive the thrown exception.

Now, when you run your application and attempt to withdraw an amount greater than the balance, the aspect will catch the InsufficientFundsException and execute the handleInsufficientFunds advice, logging the exception message.

We can also use any advice without argument

```java
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ExceptionHandlingAspect {

    @AfterThrowing("execution(* *(..))")
    public void handleException(JoinPoint joinPoint) {
        System.out.println("Exception occurred in method: " + joinPoint.getSignature().getName());

        // Additional handling logic can be added here, e.g., sending an email, logging, etc.
    }
}
```

## @Pointcut Annotation

The @Pointcut annotation in Spring AOP is used to define a reusable pointcut expression, which is a set of join points where advice should be applied.

It allows you to name and reuse a specific pointcut expression across multiple advice methods. This helps in keeping your code modular and avoids redundancy by centralizing the pointcut definitions.

```java
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class LoggingAspect {

    // Define a reusable pointcut expression
    @Pointcut("execution(* com.example.MyService.*(..))")
    public void serviceMethods() {}

    // Advice applied to methods matching the pointcut expression
    @Before("serviceMethods()")
    public void logBeforeServiceMethods() {
        System.out.println("Before service method execution: Logging...");
    }

    // Another advice using the same pointcut expression
    @Before("serviceMethods()")
    public void additionalActionBeforeServiceMethods() {
        System.out.println("Before service method execution: Additional action...");
    }
}
```

The @Pointcut annotation is used to define a pointcut expression named serviceMethods().

The pointcut expression is specified as execution(* com.example.MyService.*(..)), which captures all methods in the MyService class.

Two @Before advices use the serviceMethods() pointcut expression. This means that the advice logic will be applied to all methods matching the pointcut expression.
