# Spring Framework

Spring Framework is an open-source, lightweight Java framework that simplifies the development process for enterprise applications.

It provides a flexible infrastructure that enables developers to easily create scalable, modular, and testable applications.

Spring makes Java programing easy, focuses on speed, simplicity and productivity.

## Advantages Of Spring Framework

- It is light weight due to POJO implementation
- Simplifies the development process with its modular architecture
- Facilitates easy integration with other Java frameworks and libraries
- Provides built-in support for database access and transactions
- Enhances application security through various mechanisms
- Easy testing, Exception handling, transaction management etc.

The Spring Framework is divided into modules. Applications can choose which modules they need for development.The Spring framework has several key modules as follows

## Key Modules Of Spring Framework

- Spring Core
- Spring Data Access/Integration
- Spring Web
- Spring AOP
- Spring Messaging
- Spring Test

![Spring Framework Overview](https://docs.spring.io/spring-framework/docs/4.3.x/spring-framework-reference/html/images/spring-overview.png)

### Spring Core

Spring Core, often referred to as the core container of the Spring Framework, is the foundational module that provides essential features for the framework. It includes the core components and concepts that form the backbone of the Spring framework, enabling developers to build and manage Java objects efficiently

The key components of Spring Core are:

- Inversion Of Control (IOC)
- Dependecy Injection (DI)
- IOC Container
- Beans
- Bean Scope
- LifeCycle Callbacks
- Spring Configuration
- Spring AOP
- Spring Testing
- Spring Profiles

## Inversion Of Control (IOC)

Inversion of control is a programming design principle that shifts the control flow of a program from application code to a framework or external entity.

In traditional programming, the application code is responsible for creating and managing the objects it needs. With IoC, the control over the object creation and management is inverted or "inverted" to an external entity(Spring Framework)

**Example:**

Suppose we have three classes Airtel,Jio and Phone

Phone Class Must Have to Use Either Airtel or Jio To Make Calls

When one class want to use another class, we have to create objects and use it as follows

```java
public class Airtel{
	void makeCall(){
		System.out.println("Calling Using Airtel Sim Card !");
	}
}

public class Jio{
	void makeCall(){
		System.out.println("Calling Using Jim Sim Card !");
	}
}

public class Phone{
	void call(){
		Airtel airtel=new Airtel();
		airtel.makeCall();
	}
}

Phone phone=new Phone();
phone.call();
```

In the above example Phone call using airtel call to make call

Phone class has to create airtel object to make call

So, here Phone and Airtel classes are tightly coupled

This tight coupling also creates issues when we try to unit test the Phone class

Additionally, as Phone class is tightly coupled with the Airtel class object, any error in the Airtel class will cause error in the Phone class as well.

When we want to change sim from airtel to Jio at run time, it impossible because we have to change code before runs. That is bad coding practice

This issue can be resolved if our application is more loosely coupled. One such way to make our application more loosely coupled can be achieved by using the concept of Abstraction

Suppose we have Sim Interface having method makeCall and Airtel and Sim classes implementing Sim Interface

```java
public interface Sim{
	void makeCall();
}

public class Airtel implements Sim{
	void makeCall(){
		System.out.println("Calling Using Airtel Sim Card !");
	}
}

public class Jio implements Sim{
	void makeCall(){
		System.out.println("Calling Using Jim Sim Card !");
	}
}

public class Phone{
	Sim sim;
	void call(){
		sim=new Airtel();
		sim.makeCall();
	}
}

Phone phone=new Phone();
phone.call();
```

Above abtraction solves changing sim at run time, since sim interface can manage both airtel and jio objects

However, we still need to change the code for object initialization hence we still have a tightly coupled code.

A better design would be if we could pass Airtel/Jio object to the Phone class such that it could remove tight coupling. This can be done using Inversion of Control which is one of the key features of Spring framework.

We can set the Sim Property of Phone either by setter or construtor injection

**Contructor Injection**

```java
public class Phone{
	Sim sim;
	public Phone(Sim currentSim){
		this.sim=currentSim;
	}
	void call(){
		sim.makeCall();
	}
}

Airtel airtel=new Airtel()
Phone phone=new Phone(airtel);
phone.call();
```

**Setter Injection**

```java
public class Phone{
	Sim sim;
	void call(){
		sim.makeCall();
	}
	void setSim(Sim currentSim){
		this.sim=currentSim;
	}
}

Airtel airtel=new Airtel()
Phone phone=new Phone();
phone.setSim(airtel)
phone.call()
```

Now, we can pass sim objects dynamically at run time to phone class and both classes are loosely coupled now

In Previous to Previous Example, we can see that the Phone object gets created first and later the Sim object gets created inside the Phone class. Thus, the control of the program flows from the Phone class to the Sim class.

But in above example,rather than creating the object of Phone first, let us create the object of Sim and then create the object of Phone. The Sim object can then be passed as a constructor/Setter argument inside the Phone class

The control of the program has now changed from the Sim class to the Phone class. This principle by which we invert the control of the program is called as Inversion of Control.

Now, any Sim class object is not being created inside the Phone class. Thus, inverting the program control removes the dependency of the Phone class on Sim class.

Hence, IoC principle helps in designing loosely coupled applications which are easy to test and maintain.

## Dependency Injection (DI)

In previous example, we implemented IoC by providing the Sim object externally while instantiating Phone object, thus providing the dependency externally rather than creating it inside the Phone class.

Although this solution solves tight coupling, consider a case in which a class has multiple dependencies. It becomes tedious to manually provide all the dependencies and manage them. To tackle this, we need some external framework that can provide all the required dependencies to the class and manage them at the same time. This paves the way for Dependency Injection.

Dependency injection is a technique in which the responsibility of creating, assembling, and wiring the dependencies of a dependent class is given to an external framework.

These external frameworks are known as dependency injection (DI) frameworks. There are many third-party frameworks which are available for dependency injection such as Spring Framework, Google Guice, Play Framework, etc.

In this course, we will use Spring Framework for dependency injection.

Spring Framework Achieves Dependency Injection using IOC Container

### IOC Container

The Inversion of Control (IoC) Container is a core component of the Spring Framework that manages the lifecycle of Java objects (beans) and their dependencies.

The IoC container is responsible for creating, configuring, and assembling objects in a Spring application. It promotes the principle of Inversion of Control by taking over the control of object creation and management from the application code.

There are two main types of IoC containers in the Spring Framework:

1. BeanFactory:
2. ApplicationContext:

**1.Beanfactory:**

- The BeanFactory is the simplest IoC container provided by Spring.
- It is a basic container that lazily initializes beans when requested.
- It is suitable for resource-constrained environments or applications with a small number of beans.

**2.ApplicationContext:**

- The ApplicationContext is a more feature-rich and commonly used IoC container.
- It extends the functionality of the BeanFactory and provides additional enterprise-specific features.
- It eagerly initializes beans, supports internationalization, event propagation, and more.

The IoC container is responsible for the following tasks:

- Bean Instantiation -> Creates Objects
- Dependency Injection -> Injects Dependent Objects to required class
- Lifecycle Management -> destroying objects
- Configuration Metadata -> The IoC container reads and interprets the configuration metadata that describes how beans should be created, wired together, and managed.

#### Spring Configuration

In the Spring Framework, configuration is the process of specifying how the application's components (beans) should be created, wired together, and managed.

There are several ways to configure a Spring application

- XML Configuration
- Java Configuration
- Annotation Based Configuration

##### XML-Based Configuration:

**Create a Bean Class:**

```java
public class MyBean {
    public void displayMessage() {
        System.out.println("Hello from MyBean!");
    }
}
```

**Create XML Configuration File (applicationContext.xml):**

```xml
<!-- applicationContext.xml -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Define a bean named "myBean" of type MyBean -->
    <bean id="myBean" class="com.example.MyBean" />
</beans>
```

**Use the Bean in Main Application:**

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
## Java Configuration

In Java-based Configuration, the configuration metadata is provided using a Java class and two annotations: @Configuration and @Bean.

A Java-Based Configuration class looks something like this: Here you can see @configuration annotation is provided at the top of config class and @Bean on top of method returning bean object. Here Name of the Bean will be same as that of Method containing Bean definition

Alternatively, you can mention bean name as part of @Bean annotation.

The @Configuration annotation identifies the Java class as the Configuration class. And this class is expected to contain details on beans that are to be created in the Spring application context.

And @Bean annotation is used for bean declaration. The methods of configuration class that create instances of the desired bean get annotated with this annotation. These methods are invoked by Spring container during bootstrap and the values returned by these methods are treated as Spring beans.

Next, we use the ApplicationContext interface and its AnnotationConfigApplicationContext implementation to instantiate the bean like this:

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

//Configuring Bean 

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // Define a bean named "myBean" of type MyBean
    @Bean
    public MyBean myBean() {
        return new MyBean();
    }

    @Bean(name="anand")
    public MyBean myBean() {
        return new MyBean();
    }
}

//Using Bean

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class JavaConfigExample {
    public static void main(String[] args) {
        // Load the Java configuration class
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Retrieve the bean from the container
        MyBean myBean =(MyBean) context.getBean(MyBean.class);

        MyBean2 myBean =(MyBean2) context.getBean("anand");

        // Use the bean
        myBean.displayMessage();
    }
}
```

## Java Annotation Based Configuration

In Java Annotation-Based Configuration, the need for explicit configuration of bean gets eliminated. Instead, Spring automatically scans, detects and instantiates the beans from the specified package through component scanning. Thus, it looks for classes annotated with @Component, @Controller, @Service and @Repository annotations.

@Component is a general purpose annotation used to mark a class as Spring bean.

```java
@Component
public class CustomerLoginController{	
	//rest of the code
}
```

@Service is a specialization of @Component annotation, which is used to define a Service layer Spring bean.

```java
@Service
public class CustomerLoginServiceImpl implements CustomerLoginService{	
	//rest of the code
}
```

@Repository is a specialization of @Component annotation, which is used to define a Persistence layer Spring bean.

```java
@Repository
public class CustomerLoginRepositoryImpl implements CustomerLoginRepository{	
	//rest of the code
}
```

@Controller is a specialization of @Component annotation, which is used to define a Presentation layer Web component.

```java
@Controller
public class CustomerLoginController{	
	//rest of the code
}
```

Next, to enable auto-scan of the components we use the @ComponentScan annotation in the configuration class. This annotation scans only the package containing the configuration class and its sub-packages for beans.

```java
@Configuration
@ComponentScan
public class SpringConfig {
}
```

so, we can also specify different or multiple packages for scanning using basePackages parameter:

Finally, we use the ApplicationContext interface and its AnnotationConfigApplicationContext implementation to instantiate the bean:

```java
@Configuration
@ComponentScan(basePackages = "com.infy.service com.infy.repository")
public class SpringConfig {
}
```

Here the ApplicationContext container is loaded with the configuration metadata from SpringConfig.class. And, the "customerBean" is retrieved and typecast from Object type to Customer type. Hence, we successfully instantiated beans using Java Annotation-based configuration.

```java
public static void main(String[] args) {
    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    Customer customer = (Customer) context.getBean("customerBean");
}
```

Here the ApplicationContext container is loaded with the configuration metadata from SpringConfig.class. And, the "customerBean" is retrieved and typecast from Object type to Customer type. Hence, we successfully instantiated beans using Java Annotation-based configuration.

## Lazy Initialization of Spring Beans

Let us first understand the concept of Lazy and Eager loading with respect to Spring Beans.

- By default, the spring beans are initialized during application startup, regardless of whether they are needed or not at that time, such loading of beans is known as Eager Loading
- Instead, if a spring bean is initialized only "on-demand" that is it loaded only when it requested by another method or class, such loading of bean is known as Lazy Loading.
- As we know, Spring IoC Container can be represented by either Bean Factory or Application Context interfaces, by default Bean Factory supports Lazy Loading while Application Context supports Eager Loading.
- So In Bean Factory, beans are initialized only when getBean() method is invoked. Whereas in Application Context, all beans initialized when the Application Context is loaded.

Now, you might ask, "If we are using Application Context primarily for loading Spring IoC Containers and we also want Lazy Initialization of beans, how can we achieve both ? ".To answer that question, we can actually instruct Application Context to lazily load all or any spring beans either through either XML-based Configuration or through Java-based Configuration.

In a XML-based configuration, we can lazy load a particular bean by setting "lazy-init" attribute as "true" inside the bean tag for the particular spring bean:

```xml
<bean id="customerBean" class="com.infosys.demo.Customer" lazy-init="true">
    </bean>
```

And we can lazy load all the spring beans, globally, by setting "default-lazy-init" attribute as "true" inside the beans tag of the XML configuration metadata:

```xml
<beans default-lazy-init="true">
<bean id="customerBean" class="com.infosys.demo.Customer">
</bean>
</beans>
```

On the other hand, In a Java-based configuration, we can lazy load a particular bean by using @Lazy annotation along with @Bean in the Spring Configuration class:

```java
@Configuration
public class SpringConfig {
	@Lazy
              @Bean(name="customerBean")
	public Customer customer(){
		return new Customer();
	}
}
```

And we can lazy load all the spring beans, globally, by using @Lazy annotation along with @Configuration in the Spring Configuration class:

```java
@Lazy
@Configuration
public class SpringConfig {
	@Bean(name="customerBean")     //Customizing Name of Bean using "name" parameter
	public Customer customer(){
		return new Customer();
	}
}
```

Lastly, we can also Autowire lazy beans. And to achieve this, we have to use @Lazy annotation mandatorily at:

Bean definition which is to be lazy loaded, and

```java
@Lazy
@Service
public class CustomerLoginServiceImpl implements CustomerLoginService {
  //
}
```

The place where the bean is injected using @Autowired

```java
@Controller
public class EmployeeController {
 
@Lazy
@Autowired
 EmployeeManager employeeManager;
```

## Bean Wiring and Autowiring

Dependency Injection, as we discussed previously, helps us solve tight coupling. And this is done a process of combining the two beans, called wiring.

But, what is Wiring precisely?

The process of combining beans within the Spring IoC Container is called Wiring or Bean Wiring. When we use wiring, we inform the Spring IoC Container which beans will be required and how the container should combine them using dependency injection.

In our Example, our Phone class is dependent on Sim. Hence, we need to create an object of either Jio class or airtel class when the Phone Calls. To achieve this, first we need to create a bean of Sim interface, which returns an object of Airtel class and a bean of Phone class in the configuration class:

```java
@configuration
public class config{

	@Bean
	public Sim sim(){
		return new Airtel();
	}

	@Bean
	public Phone phone(){
		return new Phone();
	}
}
```

To Inject Sim Object into phone object, we use autowire annotation

```java
public class Phone{

	@Autowired
	Sim sim;

	void call(){
		sim.makeCall();
	}
}
```

This annotation allows Spring IoC container to resolve and inject dependencies into your bean. It is responsible for injecting the plan dependency into the customer bean.

When @Autowired is encountered by the application context, it tells the container to wire the Sim bean with a bean of Phone. In other words, the Sim bean is injected into the phone bean, without manual intervention.

Autowiring can be achieved in three different ways:

Using @Autowired on properties: Like our previous implementation of autowiring, @Autowired annotation can be used directly on instance variables or properties.

```java
public class Phone{

	@Autowired
	Sim sim;

	void call(){
		sim.makeCall();
	}
}
```

Using @Autowired on setter methods: @Autowired annotation can also be used on setter methods. And this is known as Setter injection

```java
public class Phone{
	Sim sim;
	void call(){
		sim.makeCall();
	}
	@Autowired
	void setSim(Sim currentSim){
		this.sim=currentSim;
	}
}
```

Using @Autowired on constructors: @Autowired annotation can also be used on constructor. And this is known as Constructor injection.

```java
public class Phone{
	Sim sim;

	@Autowired // it is optional
	public Phone(Sim currentSim){
		this.sim=currentSim;
	}
	void call(){
		sim.makeCall();
	}
}
```

By default, @Autowired annotation resolves bean dependencies based on type.

But what if we have two beans with the same data type? How will we resolve the bean dependencies in such a situation?

Let's consider two different beans of Sim, that are planPrepaid and planPostpaid:

```java
@configuration
public class config{

	@Bean
	public Sim airtel(){
		return new Airtel();
	}

	@Bean
	public Sim jio(){
		return new Jio();
	}

	@Bean
	public Phone phone(){
		return new Phone();
	}
}

public class Phone{

	@Autowired
	Sim sim;

	void call(){
		sim.makeCall();
	}
}
```

The Above example will return error because sim property will gets two objects to inject, that will throw exception

To Overcome this issue,@Qualifier annotation can be used along with @Autowired annotation to achieve this.

```java
public class Phone{

	@Autowired
	@Qualifier("airtel")
	Sim sim;

	void call(){
		sim.makeCall();
	}
}
```

When there are more than one beans of the same type in the Spring container, @Qualifier annotation can be used to specify which bean has to be wired, based on the bean name.

Use of @Qualifier will make @Autowired resolve dependency based on the name of bean specified with @Qualifier.

## Bean Scope

In the Spring Framework, the scope of a bean defines the lifecycle and visibility of instances managed by the Spring IoC container. It specifies how long a bean should live and how the container should manage instances of that bean. Spring provides several bean scopes, each serving different purposes. The main bean scopes in Spring are:

- Singleton (Default)
- Prototype
- Request
- Session
- Application (Singleton per Servlet Context)
- Custom Scopes

### 1. Singleton (Default)

- The singleton scope is the default scope in Spring.
- With singleton scope, the container creates and manages only one instance of the bean throughout the entire lifecycle of the application.
- All requests for the bean result in the same shared instance.

```java
@Component
@Qualifier("customerBean")
@scope("singleton")
public class customer{

}


public static void main(String[] args) {
    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    Customer customer = (Customer) context.getBean("customerBean");
    Customer customer2 = (Customer) context.getBean("customerBean");
    assertEquals(customer==customer2,True)
}
```

### 2. Prototype:

- The prototype scope instructs the container to create a new instance of the bean each time it is requested.
- Multiple requests for the bean result in different instances.
- Useful when beans have state that should not be shared among clients.

```java
@Component
@Qualifier("customerBean")
@scope("prototype")
public class customer{

}


public static void main(String[] args) {
    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    Customer customer = (Customer) context.getBean("customerBean");
    Customer customer2 = (Customer) context.getBean("customerBean");
    assertEquals(customer==customer2,False)
}
```

### 3. Request:

- The request scope is specific to web applications.
- A new instance of the bean is created for each HTTP request.
- Useful when you want a separate instance of the bean for each user request.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MyRequestScopedBean {
    // Class definition
}
```

### 4. Session:

- Similar to the request scope, the session scope is specific to web applications.
- A new instance of the bean is created for each user session.
- Useful for maintaining state across multiple requests from the same user.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MySessionScopedBean {
    // Class definition
}
```

### 5. Application (Singleton per Servlet Context):

- Similar to the singleton scope, but limited to a specific Servlet context.
- A single instance of the bean is created for each Servlet context.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MyApplicationScopedBean {
    // Class definition
}
```

### 6. Custom Scopes:

- Spring allows you to define custom scopes to meet specific requirements.
- You can implement the org.springframework.beans.factory.config.Scope interface or use custom annotations.

```java
@Component
@CustomScopeAnnotation
public class MyCustomScopedBean {
    // Class definition
}
```

The choice of bean scope depends on the requirements of your application. For stateless and widely shared components, the singleton scope is typically suitable. For stateful components that should not be shared, prototype or session scope may be more appropriate. Web applications often use request or session scope to manage the lifecycle of beans tied to HTTP requests or user sessions.

## LifeCycle CallBacks

In the Spring Framework, lifecycle callbacks provide a way to perform certain actions or tasks at specific points in the lifecycle of a bean. These callbacks allow you to customize the initialization and destruction processes of your beans.

Spring supports various lifecycle callback mechanisms, including methods annotated with @PostConstruct and @PreDestroy, as well as implementing specific interfaces.

### @PostConstruct and @PreDestroy Annotations:

The @PostConstruct annotation is used on a method that should be invoked after a bean has been constructed and its dependencies have been injected. It serves as an initialization callback.

The @PreDestroy annotation is used on a method that should be invoked just before the bean is removed from the Spring IoC container. It serves as a cleanup or destruction callback.

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

### InitializingBean and DisposableBean Interfaces:

The InitializingBean interface provides a single method afterPropertiesSet() that a bean can implement to perform initialization tasks.

The DisposableBean interface provides a single method destroy() that a bean can implement to perform cleanup tasks before being removed from the container.

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

### Custom Initialization and Destruction Methods:

You can define custom initialization and destruction methods in your bean class. These methods can have any name, but you need to specify them in the bean configuration.

Use the init-method attribute in XML-based configuration or the @Bean(initMethod = "init") annotation attribute in Java-based configuration.

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

@Bean(initMethod = "customInit", destroyMethod = "customDestroy")
public MyBean myBean() {
    return new MyBean();
}

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class LifecycleCallbacksExample {
    public static void main(String[] args) {
        // Create the application context using annotation-based configuration class
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Retrieve the bean from the container
        MyBean myBean = context.getBean(MyBean.class);

        // The @PostConstruct method will be called during bean initialization
        // The @PreDestroy method will be called before the bean is destroyed

        // Use the bean
        myBean.displayMessage();
    }
}
```

Lifecycle callbacks are useful for performing tasks such as resource allocation, connection establishment, or cleanup operations during the lifecycle of Spring beans. Choose the approach that best fits your needs and coding style when working with Spring beans.
