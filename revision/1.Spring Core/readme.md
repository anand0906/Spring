# Spring Framework - Comprehensive Guide

## Table of Contents

1. [Spring Framework Overview](#1-spring-framework-overview)
2. [POJO (Plain Old Java Object)](#2-pojo-plain-old-java-object)
3. [Spring Modules](#3-spring-modules)
4. [Spring Core](#4-spring-core)
5. [IOC, Dependency Injection & IOC Container](#5-ioc-dependency-injection--ioc-container)
   - [Inversion of Control (IOC)](#inversion-of-control-ioc)
   - [Dependency Injection](#dependency-injection)
   - [IOC Container](#ioc-container)
6. [Spring Configuration](#6-spring-configuration)
   - [XML-Based Configuration](#xml-based-configuration)
   - [Java-Based Configuration](#java-based-configuration)
   - [Annotation-Based Configuration](#annotation-based-configuration)
7. [Bean Initialization & @Lazy Annotation](#7-bean-initialization--lazy-annotation)
8. [Bean Scopes](#8-bean-scopes)
9. [Lifecycle Callbacks](#9-lifecycle-callbacks)

---

## 1. Spring Framework Overview

**What is Spring Framework?**

Spring Framework is an **open-source, lightweight Java framework** designed for developing enterprise-level applications. It provides flexible infrastructure to develop scalable, modular, and testable applications with ease.

### Advantages of Spring Framework

- **Lightweight** - Uses POJO (Plain Old Java Object) implementations
- **Database Support** - Built-in support for database access and transactions
- **Security** - Built-in security handling mechanisms
- **Integration** - Easy integration with other Java frameworks and libraries
- **Testing & Exception Handling** - Provides easy testing, exception handling, and transaction management

---

## 2. POJO (Plain Old Java Object)

**POJO** stands for **Plain Old Java Object**.

A POJO is a simple Java class used to hold data, which:
- Doesn't depend on any framework
- Doesn't implement or extend any external classes/interfaces

**Simply:** `POJO = data + getters/setters`

### How Spring Uses POJOs

Spring takes POJO classes and adds "superpowers" using:
- Configuration
- Dependency Injection
- Cross-cutting concerns

**Important:** Spring doesn't force you to extend Spring classes or implement Spring interfaces. Instead, it wraps your plain Java objects with behavior at runtime.

---

## 3. Spring Modules

The Spring Framework is divided into modules, allowing applications to choose only the modules needed for development.

### Key Modules

1. **Spring Core**
2. **Spring Data Access**
3. **Spring Web**
4. **Spring AOP**
5. **Spring Test**
6. **Spring Messaging**

---

## 4. Spring Core

Spring Core is the **foundational module** of the Spring Framework. It provides all essential features to manage Java objects efficiently.

### Key Components of Spring Core

1. IOC (Inversion of Control)
2. Dependency Injection
3. IOC Container
4. Beans
5. Bean Scopes
6. Lifecycle Callbacks
7. Spring Configuration
8. Spring AOP
9. Spring Testing
10. Spring Profiles

---

## 5. IOC, Dependency Injection & IOC Container

### Inversion of Control (IOC)

**IOC** is a programming design principle that shifts the control flow of a program from application code to an external framework or entity.

- **Traditional Programming:** Application code creates and manages objects
- **With IOC:** The framework (Spring) creates and manages objects

#### Example: Without IOC (Tight Coupling)

```java
public class Airtel {
    void makeCall() {
        System.out.println("Calling Using Airtel Sim Card!");
    }
}

public class Jio {
    void makeCall() {
        System.out.println("Calling Using Jio Sim Card!");
    }
}

public class Phone {
    void call() {
        Airtel airtel = new Airtel();  // Tightly coupled
        airtel.makeCall();
    }
}

Phone phone = new Phone();
phone.call();
```

**Problem:** To switch from Airtel to Jio, we need to modify the Phone class code.

#### Example: With IOC (Loose Coupling)

```java
public interface Sim {
    void makeCall();
}

public class Airtel implements Sim {
    void makeCall() {
        System.out.println("Calling Using Airtel Sim Card!");
    }
}

public class Jio implements Sim {
    void makeCall() {
        System.out.println("Calling Using Jio Sim Card!");
    }
}

public class Phone {
    Sim sim;
    
    void call() {
        sim.makeCall();
    }
    
    void setSim(Sim currentSim) {
        this.sim = currentSim;
    }
}

Airtel airtel = new Airtel();
Phone phone = new Phone();
phone.setSim(airtel);
phone.call();
```

**Benefit:** Control flow is shifted from Phone class to external code, achieving loose coupling.

---

### Dependency Injection

**Dependency Injection (DI)** is a technique/design pattern where the responsibility of creating, assembling, and wiring dependencies is given to external frameworks.

These external frameworks are called **Dependency Injection Frameworks**.

**Examples:** Play Framework, Spring Framework, Google Guice

Spring achieves Dependency Injection using the **IOC Container**.

---

### IOC Container

The **IOC Container** is the core component of the Spring Framework that handles the lifecycle of Java objects and their dependencies.

#### Responsibilities

1. **Bean Instantiation** - Creates objects
2. **Dependency Injection** - Injects dependent objects into required classes
3. **Lifecycle Management** - Destroys objects when needed
4. **Configuration Metadata** - Reads and interprets configuration describing how beans should be created and managed

#### Types of IOC Containers

##### 1. Bean Factory

- Simplest container provided by Spring
- **Lazy initialization** of objects
- Suitable for small applications with fewer beans or resource-constrained environments

##### 2. Application Context

- Extends BeanFactory with more features for enterprise applications
- Supports both **eager and lazy initialization**
- Recommended for most applications

---

## 6. Spring Configuration

Configuration is the process of specifying:
- Which objects to create and when?
- What depends on what?
- When to destroy objects?
- How many objects to create?

### Types of Configuration

1. XML-Based Configuration
2. Java-Based Configuration
3. Annotation-Based Configuration

---

### XML-Based Configuration

#### Step 1: Create a Bean Class

```java
public class MyBean {
    public void displayMessage() {
        System.out.println("Hello from MyBean!");
    }
}
```

#### Step 2: Create XML Configuration File (applicationContext.xml)

```xml
<!-- applicationContext.xml -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans 
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Define a bean named "myBean" of type MyBean -->
    <bean id="myBean" class="com.example.MyBean" />
</beans>
```

#### Step 3: Use the Bean in Main Application

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XmlConfigExample {
    public static void main(String[] args) {
        // Load the XML configuration file
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        // Retrieve the bean from the container
        MyBean myBean = context.getBean("myBean", MyBean.class);

        // Use the bean
        myBean.displayMessage();
    }
}
```

---

### Java-Based Configuration

#### Step 1: Create Bean Classes

```java
public class MyBean {
    public void displayMessage() {
        System.out.println("Hello from MyBean!");
    }
}

public class MyBean2 {
    public void displayMessage() {
        System.out.println("Hello from MyBean2!");
    }
}
```

#### Step 2: Configure Beans using @Configuration

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // Define a bean named "myBean" of type MyBean
    @Bean
    public MyBean myBean() {
        return new MyBean();
    }

    // Define a bean with custom name
    @Bean(name="anand")
    public MyBean2 myBean2() {
        return new MyBean2();
    }
}
```

#### Step 3: Use the Beans

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class JavaConfigExample {
    public static void main(String[] args) {
        // Load the Java configuration class
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Retrieve beans from the container
        MyBean myBean = (MyBean) context.getBean(MyBean.class);
        MyBean2 myBean2 = (MyBean2) context.getBean("anand");

        // Use the beans
        myBean.displayMessage();
        myBean2.displayMessage();
    }
}
```

---

### Annotation-Based Configuration

In Annotation-Based Configuration, Spring automatically scans, detects, and instantiates beans from specified packages through **component scanning**.

#### Common Annotations

- **@Component** - General-purpose annotation for Spring beans
- **@Service** - Specialization of @Component for Service layer
- **@Repository** - Specialization of @Component for Persistence layer
- **@Controller** - Specialization of @Component for Presentation layer

#### Examples

```java
@Component
public class CustomerLoginController {	
    // rest of the code
}

@Service
public class CustomerLoginServiceImpl implements CustomerLoginService {	
    // rest of the code
}

@Repository
public class CustomerLoginRepositoryImpl implements CustomerLoginRepository {	
    // rest of the code
}

@Controller
public class CustomerLoginController {	
    // rest of the code
}
```

#### Enable Component Scanning

```java
@Configuration
@ComponentScan
public class SpringConfig {
}
```

To scan specific or multiple packages:

```java
@Configuration
@ComponentScan(basePackages = "com.infy.service com.infy.repository")
public class SpringConfig {
}
```

#### Instantiate Beans

```java
public static void main(String[] args) {
    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    Customer customer = (Customer) context.getBean("customerBean");
}
```

---

## 7. Bean Initialization & @Lazy Annotation

### Eager vs Lazy Initialization

- **Eager Initialization (Default):** All Spring beans are initialized when the application starts
- **Lazy Initialization:** Beans are created only when called/needed

### Using @Lazy Annotation

#### XML Configuration

```xml
<!-- Single bean lazy initialization -->
<bean id="customerBean" class="com.infosys.demo.Customer" lazy-init="true">
</bean>

<!-- All beans lazy initialization -->
<beans default-lazy-init="true">
    <bean id="customerBean" class="com.infosys.demo.Customer">
    </bean>
</beans>
```

#### Java Configuration

```java
@Configuration
public class SpringConfig {
    @Lazy
    @Bean(name="customerBean")
    public Customer customer() {
        return new Customer();
    }
}
```

Or apply to entire configuration class:

```java
@Lazy
@Configuration
public class SpringConfig {
    @Bean(name="customerBean")
    public Customer customer() {
        return new Customer();
    }
}
```

#### On Service Classes

```java
@Lazy
@Service
public class CustomerLoginServiceImpl implements CustomerLoginService {
    // Implementation
}
```

#### On Autowired Dependencies

```java
@Controller
public class EmployeeController {
    @Lazy
    @Autowired
    EmployeeManager employeeManager;
}
```

---

## 8. Bean Scopes

Bean scope defines how long a bean should live and how the IOC container should manage it.

### Available Bean Scopes

1. **Singleton (Default)** - One instance per Spring container
2. **Prototype** - New instance every time the bean is requested
3. **Request** - One instance per HTTP request (Web applications)
4. **Session** - One instance per HTTP session (Web applications)
5. **Application** - Singleton per Servlet Context
6. **Custom Scopes** - User-defined scopes

---

## 9. Lifecycle Callbacks

Lifecycle callbacks provide a way to perform certain actions during the lifecycle of a bean, allowing customized initialization and destruction.

### Methods to Implement Lifecycle Callbacks

#### 1. Using @PostConstruct and @PreDestroy Annotations

```java
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class MyBean {

    @PostConstruct
    public void init() {
        // Initialization code
        System.out.println("Bean initialized");
    }

    @PreDestroy
    public void cleanup() {
        // Cleanup code
        System.out.println("Bean about to be destroyed");
    }
}
```

#### 2. Implementing InitializingBean and DisposableBean Interfaces

```java
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class MyBean implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // Initialization code
        System.out.println("Bean initialized");
    }

    @Override
    public void destroy() throws Exception {
        // Cleanup code
        System.out.println("Bean about to be destroyed");
    }
}
```

#### 3. Custom Init and Destroy Methods

```java
public class MyBean {

    public void customInit() {
        // Initialization code
        System.out.println("Custom initialization method called");
    }

    public void customDestroy() {
        // Cleanup code
        System.out.println("Custom destruction method called");
    }
}
```

Configuration:

```java
@Bean(initMethod = "customInit", destroyMethod = "customDestroy")
public MyBean myBean() {
    return new MyBean();
}
```

#### Example Usage

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class LifecycleCallbacksExample {
    public static void main(String[] args) {
        // Create the application context
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Retrieve the bean from the container
        MyBean myBean = context.getBean(MyBean.class);

        // @PostConstruct method called during bean initialization
        // @PreDestroy method called before bean is destroyed

        // Use the bean
        myBean.displayMessage();
    }
}
```

---
