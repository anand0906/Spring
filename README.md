<h1>Spring Framework</h1>
<p>Spring Framework is an open-source, lightweight Java framework that simplifies the development process for enterprise applications</p>
<p>It provides a flexible infrastructure that enables developers to easily create scalable, modular, and testable applications.</p>
<p>Spring makes Java programing easy, focuses on speed, simplicity and productivity.</p>
<h6>Advantages Of Spring Framework</h6>
<ul>
	<li>It is light weight due to POJO implementation</li>
	<li>Simplifies the development process with its modular architecture</li>
	<li>Facilitates easy integration with other Java frameworks and libraries</li>
	<li>Provides built-in support for database access and transactions</li>
	<li>Enhances application security through various mechanisms</li>
	<li>Easy testing, Exception handling, transaction management etc.</li>
</ul>

<p>The Spring Framework is divided into modules. Applications can choose which modules they need for development.The Spring framework has several key modules as follows</p>
<h3>Key Modules Of Spring Framework</h3>
<ul>
	<li>Spring Core</li>
	<li>Spring Data Access/Integration</li>
	<li>Spring Web</li>
	<li>Spring AOP</li>
	<li>Spring Messaging</li>
	<li>Spring Test</li>
</ul>
<img src="https://docs.spring.io/spring-framework/docs/4.3.x/spring-framework-reference/html/images/spring-overview.png">

<h4>Spring Core</h4>
<p>Spring Core, often referred to as the core container of the Spring Framework, is the foundational module that provides essential features for the framework. It includes the core components and concepts that form the backbone of the Spring framework, enabling developers to build and manage Java objects efficiently</p>
<p>The key components of Spring Core are:</p>
<ul>
	<li>Inversion Of Control (IOC)</li>
	<li>Dependecy Injection (DI)</li>
	<li>IOC Container</li>
	<li>Beans</li>
	<li>Bean Scope</li>
	<li>LifeCycle Callbacks</li>
	<li>Spring Configuration</li>
	<li>Spring AOP</li>
	<li>Spring Testing</li>
	<li>Spring Profiles</li>
</ul>
<h3>Inversion Of Control (IOC)</h3>
<p>Inversion of control is a programming design principle that shifts the control flow of a program from application code to a framework or external entity.</p>
<p> In traditional programming, the application code is responsible for creating and managing the objects it needs. With IoC, the control over the object creation and management is inverted or "inverted" to an external entity(Spring Framework)</p>
<p>Example : </p>
<p>Suppose we have three classes Airtel,Jio and Phone</p>
<p>Phone Class Must Have to Use Either Airtel or Jio To Make Calls</p>
<p>When one class want to use another class, we have to create objects and use it as follows</p>

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
<p>In the above example Phone call using airtel call to make call</p>
<p>Phone class has to create airtel object to make call</p>
<p>So, here Phone and Airtel classes are tightly coupled</p>
<p>This tight coupling also creates issues when we try to unit test the Phone class</p>
<p>Additionally, as Phone class is tightly coupled with the Airtel class object, any error in the Airtel class will cause error in the Phone class as well.</p>
<p>When we want to change sim from airtel to Jio at run time, it impossible because we have to change code before runs. That is bad coding practice</p>
<p>This issue can be resolved if our application is more loosely coupled. One such way to make our application more loosely coupled can be achieved by using the concept of Abstraction</p>
<p>Suppose we have Sim Interface having method makeCall and Airtel and Sim classes implementing Sim Interface</p>

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
<p>Above abtraction solves changing sim at run time, since sim interface can manage both airtel and jio objects</p>
<p>However, we still need to change the code for object initialization hence we still have a tightly coupled code.</p>
<p>A better design would be if we could pass Airtel/Jio object to the Phone class such that it could remove tight coupling. This can be done using Inversion of Control which is one of the key features of Spring framework.</p>
<p>We can set the Sim Property of Phone either by setter or construtor injection</p>

<p>Contructor Injection</p>

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

<p>Setter Injection</p>

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
<p>Now, we can pass sim objects dynamically at run time to phone class and both classes are loosely coupled now</p>
<p>In Previous to Previous Example, we can see that the Phone object gets created first and later the Sim object gets created inside the Phone class. Thus, the control of the program flows from the Phone class to the Sim class.</p>
<p>But in above example,rather than creating the object of Phone first, let us create the object of Sim and then create the object of Phone. The Sim object can then be passed as a constructor/Setter argument inside the Phone class</p>
<p>The control of the program has now changed from the Sim class to the Phone class. This principle by which we invert the control of the program is called as Inversion of Control.</p>
<p>Now, any Sim class object is not being created inside the Phone class. Thus, inverting the program control removes the dependency of the Phone class on Sim class.</p>
<p>Hence, IoC principle helps in designing loosely coupled applications which are easy to test and maintain. </p>

<h3>Dependency Injection (DI)</h3>
<p>In previous example, we implemented IoC by providing the Sim object externally while instantiating Phone object, thus providing the dependency externally rather than creating it inside the Phone class. </p>
<p>Although this solution solves tight coupling, consider a case in which a class has multiple dependencies. It becomes tedious to manually provide all the dependencies and manage them. To tackle this, we need some external framework that can provide all the required dependencies to the class and manage them at the same time. This paves the way for Dependency Injection.</p>
<p>Dependency injection is a technique in which the responsibility of creating, assembling, and wiring the dependencies of a dependent class is given to an external framework.</p>
<p>These external frameworks are known as dependency injection (DI) frameworks. There are many third-party frameworks which are available for dependency injection such as Spring Framework, Google Guice, Play Framework, etc.</p>
<p>In this course, we will use Spring Framework for dependency injection.</p>
<p>Spring Framework Achieves Dependency Injection using IOC Container</p>

<h4>IOC Container</h4>
<p>The Inversion of Control (IoC) Container is a core component of the Spring Framework that manages the lifecycle of Java objects (beans) and their dependencies. </p>
<p>The IoC container is responsible for creating, configuring, and assembling objects in a Spring application. It promotes the principle of Inversion of Control by taking over the control of object creation and management from the application code.</p>
<p>There are two main types of IoC containers in the Spring Framework:</p>
<ol>
	<li>BeanFactory:</li>
	<li>ApplicationContext:</li>
</ol>
<p>1.Beanfactory:</p>
<ul>
	<li>The BeanFactory is the simplest IoC container provided by Spring.</li>
	<li>It is a basic container that lazily initializes beans when requested.</li>
	<li>It is suitable for resource-constrained environments or applications with a small number of beans.</li>
</ul>
<p>2.ApplicationContext:</p>
<ul>
	<li>The ApplicationContext is a more feature-rich and commonly used IoC container.</li>
	<li>It extends the functionality of the BeanFactory and provides additional enterprise-specific features.</li>
	<li>It eagerly initializes beans, supports internationalization, event propagation, and more.</li>
</ul>
<p>The IoC container is responsible for the following tasks:</p>
<ul>
	<li>Bean Instantiation -> Creates Objects</li>
	<li>Dependency Injection -> Injects Dependent Objects to required class</li>
	<li>Lifecycle Management -> destroying objects</li>
	<li>Configuration Metadata -> The IoC container reads and interprets the configuration metadata that describes how beans should be created, wired together, and managed.</li>
</ul>

<h5>Spring Configuration</h5>
<p>In the Spring Framework, configuration is the process of specifying how the application's components (beans) should be created, wired together, and managed. </p>
<p>There are several ways to configure a Spring application</p>
<ul>
	<li>XML Configuration</li>
	<li>Java Configuration</li>
	<li>Annotation Based Configuration</li>
</ul>
<h6>XML-Based Configuration:</h6>
<p>Create a Bean Class:</p>

```java
public class MyBean {
    public void displayMessage() {
        System.out.println("Hello from MyBean!");
    }
}
```
<p>Create XML Configuration File (applicationContext.xml):</p>

```xml
<!-- applicationContext.xml -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Define a bean named "myBean" of type MyBean -->
    <bean id="myBean" class="com.example.MyBean" />
</beans>
```
<p>Use the Bean in Main Application:</p>

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

<h6>Java Configuration</h6>
<p>In  Java-based Configuration, the configuration metadata is provided using a Java class and two annotations: @Configuration and @Bean.</p>
<p>A Java-Based Configuration class looks something like this: Here you can see @configuration annotation is provided at the top of config class and @Bean on top of method returning bean object. Here Name of the Bean will be same as that of Method containing Bean definition</p>
<p>Alternatively, you can mention bean name as part of @Bean annotation.</p>
<p>The @Configuration annotation identifies the Java class as the Configuration class. And this class is  expected to contain details on beans that are to be created in the Spring application context. </p>
<p>And @Bean annotation is used for bean declaration. The methods of configuration class that create instances of the desired bean get annotated with this annotation. These methods are invoked by Spring container during bootstrap and the values returned by these methods are treated as Spring beans.</p>
<p>Next, we use the ApplicationContext interface and its AnnotationConfigApplicationContext implementation to instantiate the bean like this:</p>

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

<h6>Java Annotation Based Configuration</h6>
<p>In Java Annotation-Based Configuration, the need for explicit configuration of bean gets eliminated. Instead, Spring automatically scans, detects and instantiates the beans from the specified package through component scanning. Thus, it looks for classes annotated with @Component, @Controller, @Service and @Repository annotations.</p>
<p>@Component is a general purpose annotation used to mark a class as Spring bean.</p>

```java
@Component
public class CustomerLoginController{	
	//rest of the code
}
```
<p>@Service is a specialization of @Component annotation, which is used to define a Service layer Spring bean.</p>

```java
@Service
public class CustomerLoginServiceImpl implements CustomerLoginService{	
	//rest of the code
}
```
<p>@Repository is a specialization of @Component annotation, which is used to define a Persistence layer Spring bean.</p>

```java
@Repository
public class CustomerLoginRepositoryImpl implements CustomerLoginRepository{	
	//rest of the code
}
```
<p>@Controller is a specialization of @Component annotation, which is used to define a Presentation layer Web component.</p>

```java
@Controller
public class CustomerLoginController{	
	//rest of the code
}

```
<p>Next, to enable auto-scan of the components we use the @ComponentScan annotation in the configuration class. This annotation scans only the package containing the configuration class and its sub-packages for beans.</p>

```java
@Configuration
@ComponentScan
public class SpringConfig {
}

```
<p>so, we can also specify different or multiple packages for scanning using basePackages parameter:</p>
<p>Finally, we use the ApplicationContext interface and its AnnotationConfigApplicationContext implementation to instantiate the bean:</p>

```java
@Configuration
@ComponentScan(basePackages = "com.infy.service com.infy.repository")
public class SpringConfig {
}

```

<p>Here the ApplicationContext container is loaded with the configuration metadata from SpringConfig.class. And, the "customerBean" is retrieved and typecast from Object type to Customer type. Hence, we successfully instantiated beans using Java Annotation-based configuration.</p>

```java
public static void main(String[] args) {
    ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    Customer customer = (Customer) context.getBean("customerBean");
}

```

<p>Here the ApplicationContext container is loaded with the configuration metadata from SpringConfig.class. And, the "customerBean" is retrieved and typecast from Object type to Customer type. Hence, we successfully instantiated beans using Java Annotation-based configuration.</p>

<h6>Lazy Initialization of Spring Beans</h6>
<p>Let us first understand the concept of Lazy and Eager loading with respect to Spring Beans. </p>
<ul>
	<li>By default, the spring beans are initialized during application startup, regardless of whether they are needed or not at that time, such loading of beans is known as Eager Loading</li>
	<li>Instead, if a spring bean is initialized only “on-demand” that is it loaded only when it requested by another method or class, such loading of bean is known as Lazy Loading.</li>
	<li>As we know, Spring IoC Container can be represented by either Bean Factory or Application Context interfaces, by default Bean Factory supports Lazy Loading while Application Context supports Eager Loading.</li>
	<li>So In Bean Factory, beans are initialized only when getBean() method is invoked. Whereas in Application Context, all beans initialized when the Application Context is loaded.</li>
</ul>
<p>Now, you might ask, “If we are using Application Context primarily for loading Spring IoC Containers and we also want Lazy Initialization of beans, how can we achieve both ? ”.To answer that question, we can actually instruct Application Context to lazily load all or any spring beans either through either XML-based Configuration or through Java-based Configuration. </p>
<p>In a XML-based configuration, we can lazy load a particular bean by setting “lazy-init” attribute as “true” inside the bean tag for the particular spring bean:</p>

```xml
<bean id="customerBean" class="com.infosys.demo.Customer" lazy-init=”true”>
    </bean>
```
<p>And we can lazy load all the spring beans, globally, by setting “default-lazy-init” attribute as “true” inside the beans tag of the XML configuration metadata:</p>

```xml
<beans default-lazy-init=”true”>
<bean id="customerBean" class="com.infosys.demo.Customer">
</bean>
</beans>
```

<p>On the other hand, In a Java-based configuration, we can lazy load a particular bean by using @Lazy annotation along with @Bean in the Spring Configuration class:</p>

```java
@Configuration
public class SpringConfig {
	@Lazy
              @Bean(name=”customerBean”)
	public Customer customer(){
		return new Customer();
	}
}
```
<p>And we can lazy load all the spring beans, globally, by using @Lazy annotation along with @Configuration in the Spring Configuration class:</p>

```java
@Lazy
@Configuration
public class SpringConfig {
	@Bean(name=”customerBean”)     //Customizing Name of Bean using “name” parameter
	public Customer customer(){
		return new Customer();
	}
}
```

<p>Lastly, we can also Autowire lazy beans. And to achieve this, we have to use @Lazy annotation mandatorily at:</p>
<p>Bean definition which is to be lazy loaded, and</p>

```java
@Lazy
@Service
public class CustomerLoginServiceImpl implements CustomerLoginService {
  //
}
```

<p>The place where the bean is injected using @Autowired</p>

```java
@Controller
public class EmployeeController {
 
@Lazy
@Autowired
 EmployeeManager employeeManager;

```

<p>Dependency Injection, as we discussed previously, helps us solve tight coupling. And this is done a process of combining the two beans, called wiring.</p>
<p>But, what is Wiring precisely?</p>
<p>The process of combining beans within the Spring IoC Container is called Wiring or Bean Wiring. When we use wiring, we inform the Spring IoC Container which beans will be required and how the container should combine them using dependency injection.</p>
<p>In our Example, our Phone class is dependent on Sim. Hence, we need to create an object of either Jio class or airtel class when the Phone Calls. To achieve this, first we need to create a bean of Sim interface, which returns an object of Airtel class and a bean of Phone class in the configuration class:</p>

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

<p>To Inject Sim Object into phone object, we use autowire annotation</p>

```java
public class Phone{

	@Autowired
	Sim sim;

	void call(){
		sim.makeCall();
	}
}
```

<p>This annotation allows Spring IoC container to resolve and inject dependencies into your bean. It is responsible for injecting the plan dependency into the customer bean.</p>
<p>When @Autowired is encountered by the application context, it tells the container to wire the Sim bean with a bean of Phone. In other words, the Sim bean is injected into the phone bean, without manual intervention.</p>
<p>Autowiring can be achieved in three different ways: </p>
<p>Using @Autowired on properties: Like our previous implementation of autowiring, @Autowired annotation can be used directly on instance variables or properties.</p>

```java
public class Phone{

	@Autowired
	Sim sim;

	void call(){
		sim.makeCall();
	}
}
```

<p>Using @Autowired on setter methods: @Autowired annotation can also be used on setter methods. And this is known as Setter injection</p>

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

<p>Using @Autowired on constructors: @Autowired annotation can also be used on constructor. And this is known as Constructor injection.</p>

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


<p>By default, @Autowired annotation resolves bean dependencies based on type.</p>
<p>But what if we have two beans with the same data type? How will we resolve the bean dependencies in such a situation?</p>
<p>Let’s consider two different beans of Sim, that are planPrepaid and planPostpaid:</p>

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
<p>The Above example will return error because sim property will gets two objects to inject, that will throw exception</p>
<p>To Overcome this issue,@Qualifier annotation can be used along with @Autowired annotation to achieve this.</p>

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

<p>When there are more than one beans of the same type in the Spring container, @Qualifier annotation can be used to specify which bean has to be wired, based on the bean name.</p>
<p>Use of @Qualifier will make @Autowired resolve dependency based on the name of bean specified with @Qualifier.</p>

<h4>Bean Scope</h4>
<p>In the Spring Framework, the scope of a bean defines the lifecycle and visibility of instances managed by the Spring IoC container. It specifies how long a bean should live and how the container should manage instances of that bean. Spring provides several bean scopes, each serving different purposes. The main bean scopes in Spring are:</p>
<ul>
	<li>Singleton (Default)</li>
	<li>Prototype</li>
	<li>Request</li>
	<li>Session</li>
	<li>Application (Singleton per Servlet Context)</li>
	<li>Custom Scopes</li>
</ul>
<h6>1.Singleton (Default)</h6>
<ul>
	<li>The singleton scope is the default scope in Spring.</li>
	<li>With singleton scope, the container creates and manages only one instance of the bean throughout the entire lifecycle of the application.</li>
	<li>All requests for the bean result in the same shared instance.</li>
</ul>

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

<h6>2.Prototype:</h6>
<ul>
	<li>The prototype scope instructs the container to create a new instance of the bean each time it is requested.</li>
	<li>Multiple requests for the bean result in different instances.</li>
	<li>Useful when beans have state that should not be shared among clients.</li>
</ul>

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
<h6>3.Request:</h6>
<ul>
	<li>The request scope is specific to web applications.</li>
	<li>A new instance of the bean is created for each HTTP request.</li>
	<li>Useful when you want a separate instance of the bean for each user request.</li>
</ul>

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MyRequestScopedBean {
    // Class definition
}
```
<h6>4.Session:</h6>
<ul>
	<li>Similar to the request scope, the session scope is specific to web applications.</li>
	<li>A new instance of the bean is created for each user session.</li>
	<li>Useful for maintaining state across multiple requests from the same user.</li>
</ul>

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MySessionScopedBean {
    // Class definition
}

```
<h6>5.Application (Singleton per Servlet Context):</h6>
<ul>
	<li>Similar to the singleton scope, but limited to a specific Servlet context.</li>
	<li>A single instance of the bean is created for each Servlet context.</li>
</ul>

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MyApplicationScopedBean {
    // Class definition
}
```
<h6>6.Custom Scopes:</h6>
<ul>
	<li>Spring allows you to define custom scopes to meet specific requirements.</li>
	<li>You can implement the org.springframework.beans.factory.config.Scope interface or use custom annotations.</li>
</ul>

```java
@Component
@CustomScopeAnnotation
public class MyCustomScopedBean {
    // Class definition
}
```
<p>The choice of bean scope depends on the requirements of your application. For stateless and widely shared components, the singleton scope is typically suitable. For stateful components that should not be shared, prototype or session scope may be more appropriate. Web applications often use request or session scope to manage the lifecycle of beans tied to HTTP requests or user sessions.</p>


<h4>LifeCycle CallBacks</h4>
<p>In the Spring Framework, lifecycle callbacks provide a way to perform certain actions or tasks at specific points in the lifecycle of a bean. These callbacks allow you to customize the initialization and destruction processes of your beans.</p>
<p>Spring supports various lifecycle callback mechanisms, including methods annotated with @PostConstruct and @PreDestroy, as well as implementing specific interfaces.</p>
<h6>@PostConstruct and @PreDestroy Annotations:</h6>
<p>The @PostConstruct annotation is used on a method that should be invoked after a bean has been constructed and its dependencies have been injected. It serves as an initialization callback.</p>
<p>The @PreDestroy annotation is used on a method that should be invoked just before the bean is removed from the Spring IoC container. It serves as a cleanup or destruction callback.</p>

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

<h6>InitializingBean and DisposableBean Interfaces:</h6>
<p>The InitializingBean interface provides a single method afterPropertiesSet() that a bean can implement to perform initialization tasks.</p>
<p>The DisposableBean interface provides a single method destroy() that a bean can implement to perform cleanup tasks before being removed from the container.</p>

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

<h6>Custom Initialization and Destruction Methods:</h6>

<p>You can define custom initialization and destruction methods in your bean class. These methods can have any name, but you need to specify them in the bean configuration.</p>
<p>Use the init-method attribute in XML-based configuration or the @Bean(initMethod = "init") annotation attribute in Java-based configuration.</p>

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

<p>Lifecycle callbacks are useful for performing tasks such as resource allocation, connection establishment, or cleanup operations during the lifecycle of Spring beans. Choose the approach that best fits your needs and coding style when working with Spring beans.</p>



<h4>Spring Aspect Oriented Programming (AOP) </h4>

<p>Spring AOP (Aspect-Oriented Programming) is a framework provided by the Spring framework that enables developers to seperate and modularize cross-cutting concerns in their applications code.</p>

<h6>Cross Cutting Concerns : </h6>
<p>Imagine you're building a big software project, and there are certain things that need to happen across different parts of your code. These things are not directly related to what each part of your code , they're more like common background tasks that multiple parts of your code need to handle. Examples could be logging, security checks, or error handling.</p>
<p>These are called cross cutting concerns</p>
<p>Now, in regular programming, you would have to write this code throughout your code, making it a bit messy and harder to understand.</p>
<p>To Handle this issue, AOP helps you keep these background tasks separate from your main code, making everything cleaner and more organized.</p>
<p>Simply, when we are writing code for an application, some code will common for some classes, which is nt directly depedent on individual class logics, but those are tightly coupled with that classes. It looks bit messy, so we will try to separate those common code and make it to run automatically with resepct to that classes.</p>	
<p>These way of programming is called aspect oriented programming</p>

<h5>Key concepts of AOP</h5>
<h6>Aspect</h6>
<p>Aspect is a class that implements the cross-cutting concerns</p>
<p>To declare a class as an aspect it should be annotated with the @Aspect annotation</p>
<p>It should be applied to the class which is annotated with @Component annotation or with derivatives of it.</p>
<h6>Joint Point</h6>
<p>Join point is a specific point in the application such as method execution, exception handling, changing object variable values, etc during its execution. </p>
<p>It is basically defines when the common code should be executed</p>
<p>In Spring AOP a join point is always the execution of a method.</p>
<h6>Advice</h6>
<p>Advice is a method of the aspect class that provides the implementation for the cross-cutting concern.</p>
<p>It gets executed at the selected join point(s)</p>
<p>There are different types of advices in spring aop</p>
<ul>
	<li><strong>Before : </strong> The advice gets executed before the join-point.</li>
	<li><strong>After Returning : </strong> The advice gets executed after the execution of the join-point finishes.</li>
	<li><strong>After Throwing : </strong> The advice gets executed if any exception is thrown from the join-point.</li>
	<li><strong>After : </strong> The advice gets executed after the execution of the join-point whether it throws an exception or not.</li>
	<li><strong>Around : </strong> The advice gets executed around the join-point, which means that it is invoked before the join-point and after the execution of the join-point.</li>
</ul>

<h6>PointCut</h6>
<p>Pointcut represents an expression used to identify in which places advices should be associated</p>
<p>It is used to determine exactly for which methods of Spring beans advice needs to be applied.</p>
<p> It has the following syntax:</p>

```java
execution(<modifiers> <return-type> <fully qualified class name>.<method-name>(parameters))
```

<p>execution : It is called as pointcut designator, It tells spring that joint point is execution of matching method</p>
<p><modifiers> : It determines the access specifier of matching method.It could either be public, protected, or private. It is not mandatory. </p>
<p><return-type> : It determines the return type of the method in order for a join point to be matched. It is mandatory. If the return type doesn't matter wildcard * is used.</p>
<p><fully qualified class name> :  specifies the fully qualified name of the class which has methods on the execution of which advice gets executed. It is optional. You can also use * wildcard as name or part of a name.</p>
<p><method-name> specifies the name of the method on the execution of which advice gets executed. It is mandatory. You can also use * wildcard as name or part of a name.</p>
<p>parameters are used for matching parameters. To skip parameter filtering, use two dots .. as parameters.</p>
<table summary="This table shows different pointcuts and their corresponding description">
	<thead>
		<tr>
			<th style="null">
				<span style="null">Pointcut</span>
			</th>
			<th style="null"><
				<span style="null">Description</span>
			</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>execution(public * *(..))</td>
			<td>execution of any public method</td>
		</tr>
		<tr>
			<td>execution(* service*(..))</td>
			<td>execution of any method with a name beginning with “service”</td>
		</tr>
		<tr>
			<td>execution(* com.infy.service.*.*(..))</td>
			<td>execution of any method defined in the com.infy.service package</td>
		</tr>
		<tr>
			<td>execution(* com.infy.service.CustomerServiceImpl.*(..))</td>
			<td>execution of any method defined in CustomerServiceImpl of com.infy.service package</td>
		</tr>
		<tr>
			<td>execution(public * com.infy.repository.CustomerRepository.*(..)) &nbsp;&nbsp;</td>
			<td>execution of any public method in CustomerRepository of com.infy.repository package</td>
		</tr>
		<tr>
			<td>execution(public String com.infy.repository.CustomerRepository.*(..))</td>
			<td>execution of all public method in CustomerRepository of com.infy.repository package that returns a String</td>
		</tr>
	</tbody>
</table>

<p>To use Spring AOP and AspectJ in Spring Boot project you have to add spring-boot-starter-aop starter in pom.xml file as follows:</p>

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

<p>This starter adds following key dependencies:</p>
<ul>
	<li>Spring AOP which provides basic AOP capabilities</li>
	<li>AspectJ which provides a complete AOP framework</li>
</ul>

<h5>Around Advice :</h5>
<p>It executed around the join point, i.e before and after the execution of the target method</p>
<p>It is declared using @Around annotation</p>
<p>You can perform custom logic before and after the method invocation, and you can even decide whether to proceed with the method execution or skip it altogether.</p>
<p>This is powerful because it gives you the ability to modify the method's behavior.</p>

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

<p>In The Above Example, The @Around annotation indicates that this is an "around advice."</p>
<p>The advice method logAndHandleDivision takes a ProceedingJoinPoint parameter, which allows you to control the method execution.</p>
<p>Before the actual method (divide) is executed, you can perform custom logic (logging in this case).</p>
<p>You can access the method arguments using joinPoint.getArgs() and modify them if needed.</p>
<p>You can choose to proceed with the method execution using joinPoint.proceed() or skip it based on some conditions.</p>
<p>After the method execution, you can perform additional actions.</p>
<p>This is a powerful way to wrap custom logic around a method, providing a centralized way to handle common concerns.</p>

<h5>Before Advice</h5>
<p>"before advice" is a type of advice that allows you to execute custom logic before a method is invoked. It provides a way to perform actions, such as logging or validation, prior to the actual execution of the target method.</p>
<p>Keep in mind that while "before advice" is useful for tasks like logging, it doesn't allow you to modify the method's input or output.</p>

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

<p>The @Before annotation indicates that this is a "before advice."</p>
<p>The advice method logBeforeMethodExecution is executed before any method in the Calculator class (execution(* com.example.Calculator.*(..))).</p>
<p>When you run your application with Spring AOP configured, every time a method in the Calculator class is invoked, the "Before method execution: Logging..." message will be printed to the console.</p>

<h5>After Advice</h5>
<p>This advice is declared using @After annotation. It is executed after the execution of the actual method(fetchCustomer), even if it throws an exception during execution. It is commonly used for resource cleanup such as temporary files or closing database connections. The following is an example of this advice:</p>

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
<p>The @After annotation indicates that this is an "after advice."</p>
<p>The advice method logAfterMessageSent is executed after the sendMessage method in the MessagingService class.</p>
<p>This can be helpful when you want to perform actions based on the specific method that was intercepted or inspect the arguments passed to that method.</p>

<h5>After Returning Advice : </h5>
<p>This advice is declared using @AfterReturning annotation. It gets executed after joinpoint finishes its execution.</p>
<p>f the target method throws an exception the advice is not executed</p>

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

<p>The @AfterReturning annotation indicates that this is an "after returning advice."</p>
<p>The pointcut expression targets all methods in the Calculator class (execution(* com.example.Calculator.*(..))).</p>
<p>The returning attribute specifies the name of the parameter in the advice method (result) that will receive the value returned by the intercepted method.</p>
<p>When you run your application with Spring AOP configured, every time a method in the Calculator class is invoked, and it successfully returns a result, the "After method execution: Logging..." message will be printed to the console, along with the result returned by the method.</p>

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

<h5>AfterThrowing Advice</h5>
<p>This advice is defined using @AfterThrowing annotation. It gets executed after an exception is thrown from the target method.</p>
<p>This allows you to handle or log exceptions in a centralized way without modifying the original method.</p>

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

<p>The @AfterThrowing annotation indicates that this is an "after throwing advice."</p>
<p>The pointcut expression targets the withdraw method in the BankAccount class with a double parameter.</p>
<p>The throwing attribute specifies the name of the parameter in the advice method (exception) that will receive the thrown exception.</p>
<p>Now, when you run your application and attempt to withdraw an amount greater than the balance, the aspect will catch the InsufficientFundsException and execute the handleInsufficientFunds advice, logging the exception message.</p>

<p>We can also use any advice without argument</p>

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

<h5>@Pointcut Annotation</h5>
<p>The @Pointcut annotation in Spring AOP is used to define a reusable pointcut expression, which is a set of join points where advice should be applied.</p>
<p>It allows you to name and reuse a specific pointcut expression across multiple advice methods. This helps in keeping your code modular and avoids redundancy by centralizing the pointcut definitions.</p>

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

<p>The @Pointcut annotation is used to define a pointcut expression named serviceMethods().</p>
<p>The pointcut expression is specified as execution(* com.example.MyService.*(..)), which captures all methods in the MyService class.</p>
<p>Two @Before advices use the serviceMethods() pointcut expression. This means that the advice logic will be applied to all methods matching the pointcut expression.</p>

<h4>Spring Profile</h4>
<p>Spring Profiles helps to classify  the classes and  properties file for the environment</p>
<p>You can create multiple profiles and set one or more profiles as the  active profile. Based on the active profile spring framework chooses beans and properties file to run.</p>
<p>Let us see how to configure different profiles using spring profiles in our projects.</p>
<p>Steps to be followed:</p>
<ol>
	<li>Identify the beans which has to be part of a particular profile [Not mandatory]</li>
	<li>Create environment-based properties file</li>
	<li>Set active profiles.</li>
</ol>
<p>You can identify the Spring beans which have to be part of a profile using the following ways</p>
<ol>
	<li>Annotation @Profile</li>
	<li>Bean declaration in XML</li>
</ol>
<p>@Profile helps spring to identify the beans that belong to a particular environment.</p>
<ol>
	<li>Any class which is annotated with stereotype annotations such as @Component,@Service,@Repository and @Configuration can be annotated with @Profile .</li>
	<li>@Profile is applied at class level except for the classes annotated with @Configuration where @Profile is applied at the method level.</li>
</ol>
<p>@Profile-Class level:</p>

```java
@Profile("dev")
@Component
@Aspect
public class LoggingAspect { 
}
```

<p>This LoggingAspect class will run only if “dev” environment is active.</p>

<p>@Profile- Method level:</p>

```java
@Configuration
public class SpringConfiguration {
	@Bean("customerService")
	@Profile("dev")
	public CustomerService customerServiceDev() {
		CustomerService customerServiceDev= new CustomerService();
		customerServiceDev.setName("Developement-Customer");
		return customerServiceDev;
	}
	@Bean("customerService")
	@Profile("prod")
	public CustomerService customerServiceProd() {
		CustomerService customerServiceProd=new 	CustomerService();
		customerServiceProd.setName("Production-Customer");
		return customerServiceProd;
	}
}

```

<p>In the above code snippet CustomerService bean is configured differently in the "dev" and "prod" environments. Depending on the currently active profile Spring fetches CustomerService accordingly.</p>
<p>@Profile annotation in class level should not be overridden in the method level. It will cause NoSuchBeanDefinitionException.</p>
<p>@Profile value can be prefixed with !(Not) operator</p>

```java
@Profile("!test")
@Configuration
@ComponentScan(basePackages="com.infy.service")
public class SpringConfiguration { 
}
```

<p>This SpringConfiguration class will run in all environments other than test.</p>

<p>Note : if you do not apply @profile annotation on a  class, means that the particular bean is available in all environment(s). </p>

<p>Spring Boot relies on application.properties file for configuration. To write configuration based on environment, you need to create different application.properties files. </p>

<p>The file name has a naming convention as application-<user created profile name>.properties</p>
<p>application-dev.properties</p>

```properties
# Oracle settings
spring.datasource.url=jdbc:oracle:thin:@localhost:1522:devDB
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver.class=oracle.jdbc.driver.OracleDriver
# logging
logging.pattern.file= %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%
logging.level.org.springframework.web: ERROR
```

<p>application-test.properties</p>

```properties
 # Oracle settings
spring.datasource.url=jdbc:oracle:thin:@localhost:1522:testDB
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver.class=oracle.jdbc.driver.OracleDriver
# logging
logging.pattern.file= %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.level.org.springframework.web: DEBUG
```

<p>application-prod.properties</p>

```properties
# mysql settings
spring.datasource.url=jdbc:mysql://localhost:3306/prodDB?useSSL=false
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver.class=com.mysql.jdbc.Driver
# logging
logging.pattern.file= %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%
logging.level.org.springframework.web: INFO
```

<p>The above shows how application.properties file with different configuration is created for 3 different environments(dev,test and prod).</p>

<p>By now you are aware of how to map a bean or configure properties to a certain profile(eg :dev/prod/test), next we need to set the one which is active.</p>
<p>The different way to do is as follows:</p>
<ol>
	<li>application.properties</li>
	<li>JVM System Parameter</li>
	<li>Maven Profile</li>
</ol>
<p>You can make more than 1 profile as active at a time.</p>

<p>To set an active profile you need to write  the below property in the main application.properties file.</p>

```properties
spring.profiles.active=dev
```

```properties
spring.profiles.active=dev,prod
```

<p>You can set profile using JVM system arguments in two ways , either set the VM arguments in Run configuration or programmatic configuration.</p>
<p>To set through Run configuration give  the below command in VM arguments.</p>

```java
-Dspring.profiles.active=dev
```

<p>To set through programmatic approach set system property as follows,</p>

```java
public static void main( String[] args )    {     
    System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "prod");
}

```

<p>Through the command line: --spring.profiles.active=dev</p>

<h4>Spring Logging</h4>
<p>During the execution of an application, several events are triggered at different points while the user interacts with the application and a record of all those events need to be kept.</p>
<p>This can help the administrator and the programmer to diagnose, debug and maintain the application.</p>
<p>The process of writing log messages to a central location during the execution of the program, thus tracking every step in the execution of a program including any event or exception along with their relevant details, is called Logging.</p>
<p>There are multiple reasons to log the application activity. We can record unusual circumstances or errors that may be happening in the program as well as information about the application’s execution. Having these records helps in quick problem diagnosis, debugging, and maintenance of the application.</p>
<p>We need logging frameworks to log the events and messages in any application.</p>
<ul>
	<li>It allows you to configure logging level. Logging levels are basically used to differentiate the logs according to their severity. Some examples of logging level are Info, Warn and Error. </li>
	<li>It also allows you to set the destination of the logs according to your requirement. Suppose you want to store it in a file or a database. It allows you to do that.</li>
</ul>
<p>There are several logging frameworks to make logging easier. Some of the popular ones are:</p>
<ul>
	<li>JDK Logging API</li>
	<li>Apache Log4j</li>
	<li>Commons Logging API</li>
</ul>
<p>Spring Boot uses Apache Commons Logging for logging. It provides default configurations for using Java Util Logging, Log4j2, and Logback as logging implementation. If Spring Boot starters are used then Logback is used for logging by default.</p>
<li>We can  customize the default logging configurations of Spring Boot or use other logging frameworks of our choice as well.</li>
<h1>Logging Levels in HTML Terms</h1>
<div class="log-level trace">
	<strong>ALL:</strong> For all the levels (required for user defined levels)
</div>
<div class="log-level trace">
	<strong>TRACE:</strong> The most detailed level. Used for fine-grained debugging information.
</div>
<div class="log-level debug">
    <strong>DEBUG:</strong> Used for debugging information.
</div>
<div class="log-level info">
    <strong>INFO:</strong> Used to provide informational messages.
</div>
<div class="log-level warn">
    <strong>WARN:</strong> Used for potentially harmful situations.
</div>
<div class="log-level error">
    <strong>ERROR:</strong> Used for error messages.
</div>
<div class="log-level fatal">
    <strong>FATAL:</strong> Rarely used, signifies a very severe error that will lead the application to abort.
</div>
<div class="log-level fatal">
    <strong>OFF:</strong> To disable all the levels
</div>

<p>As we have seen before, Spring Boot uses Logback as its default logging framework.</p>
<p>Generally, all ‘spring-boot-starter’ dependencies make use of Apache Commons Logging library to implicitly add some logging framework jars such as Java Util Logging, Logback , Log4J in the applications.</p>
<p>let us learn about Logback and some of its configurations.</p>
<p>Logging levels, Logging format, and Log file location can be easily configured according to the requirement in “application.properties” file. </p>

<h6>Configuring logging levels</h6>
<p>Spring Boot by default, logs messages at ERROR, WARN, and INFO levels. Logging levels can easily be configured just by setting up the following property in application.properties file.</p>
<p>logging.level.com.infy=INFO </p>
<p>Here logging level is set as Info (highlights the progress of an application) for classes of com.infy package.</p>
<p>So, Up to INFO, It Will log</p>

<h6>Configuring logging pattern</h6>
<p>You can change the logging pattern for console by setting logging.pattern.console property in application.properties file.</p>

```properties
logging.pattern.console=%d{yyyy-MMM-dd HH:mm:ss a} [%t] %-5level %logger{36} - %msg%n
```
<p>To change the logging pattern for file, set the logging.pattern.file property in application.properties file.</p>

<p>Suppose you want to log a message in error log file in this format.</p>

<p>Then you can set the property like this</p>

```properties
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

<p>Configuring logging file</p>
<p>By default, Spring Boot logs only to the console and does not log to files. If you want to log messages in files in addition to the console, you need to set logging.file property in application.properties file. The value for this property can be exact location of file or relative to the current directory. For example, to log messages in error.log file you can set logging.file.name property as follows:</p>

```properties
logging.file.name = Error.log
```

<p>You can also set the location of the log file by setting logging.file.path property in application.properties file.</p>

```properties
logging.file.path=logs/Error.log
```

<p>To have more more control over logging, you can use the logging provider specific configuration file in their default locations, which Spring Boot will automatically use.</p>

<p>We have that Logback is used as the default logging framework by spring boot.</p>

<p>In order to use Log4J, first you have to exclude the default logging dependency which is  ‘spring-boot-starter-logging’  and then include ‘spring-boot-starter-log4j2’ starter in pom.xml file.</p>

```xml
	<!-- Exclude Spring Boot's Default Logging -->
	<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter</artifactId>
			<exclusions>
				<exclusion>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-starter-logging</artifactId>
				</exclusion>
			</exclusions>
	</dependency>
	<!-- Add Log4j2 Dependency -->
	<dependency>

```

<p>Log4j also allows you to log the information at different places which can be either file, console, database etc.</p>

<p>For configuration of Log4j, add log4j.properties file to the root class path of the application so that spring boot automatically recognize it and can use it for logging.</p>	

<p>Once configuration is done you can use the Log4J logger in the application. To log information, you first need to get a logger object.</p>
<p>his logger object is obtained by invoking getLogger() method of the org.apache.logging.log4j.LogManager class.</p>
<p>This method takes the details of the class in which you want to log as a parameter and gives an object of org.apache.logging.log4j.Logger interface.</p>


<h4>Spring Data Access/Integration</h4>
<p>In Software Development, the process of storing data permanently into storage system is called is data persistance</p>
<p>Implementing this data persistance through programming is one of the most critcal challenge for developers.</p>
<p>The process of storing data involve three major steps</p>
<ol>
	<li>Data : What to store ?</li>
	<li>Medium : How to store ?</li>
	<li>Storage : Where to store ?</li>
</ol>
<h6>Data :</h6>
<p>The data which needs to be stored can be</p>
<p>Raw Data : Collected from file or other souces in the form of bytes</p>
<p>Objects : It can be also in the form of objects in field of programming</p>
<h6>Storage :</h6>
<p>The Data which is collected can be stored to</p>
<p>RAM or Secondary Storage Devices like hard drive</p>
<p>Logical devices like Databases or files</p>
<h6>Medium : </h6>
<p>To Store data into storage, java provides various ways like</p>
<ul>
	<li>I/O Streams and Serialization</li>
	<li>JDBC</li>
	<li>ORM Frameworks like Hibernate</li>
</ul>
<p>The Java Input-Output(I/O) API provides classes for performing input and output operations on raw data.</p>
<p>These classes are available in the java.io package.</p>
<p>Java I/O API is built on four abstract classes. This depends upon the type of data it can handle (byte/character). </p>
<p>InputStream and OutputStream: deals with bytes. </p>
<p>Reader and Writer: deals with character.</p>
<p>Serialization helps to covert raw data to java objects and vice-versa</p>
<p>An object can be marked serializable by implementing the java.io.Serializable interface.</p>
<p>Serializable objects can be converted into a stream of bytes.</p>
<p>This stream of bytes can be written into a file.</p>
<p>These bytes can be read back to re-create the object.</p>
<p>Deserialization is the process of retrieving an object from the byte streams.</p>
<p>Java I/O APIs  covers all the functionalities as a data persistence medium.</p>
<p>Working with the file system is very difficult and inefficient in handling large and complex data.</p>
<p>Using Java I/O also need lower-level details of the data to be retrieved, stored, or manipulated. </p>
<p>Serialization  has certain disadvantages such as : </p>
<p>Since storing and retrieval of the entire object graph is done at once, it is not a suitable approach while working with a large amount of data.</p>
<p>Concurrent access is not possible.</p>
<p>It provides no query capabilities.</p>
<p>The data cannot be retrieved without de-serialization.</p>
<p>JDBC or 'Java Database Connectivity' is a Java Core API for performing database interaction.</p>
<p>Using JDBC API, a Java application can access a variety of databases such as MySQL, Oracle, etc.</p>
<p>JDBC follows a relational database-oriented approach to work with the data using SQL queries. </p>
<p>The problem with Serialization is solved by JDBC, but it does not store the Java objects directly. The data from the objects need to be converted into a SQL query and then executed, for persistence. </p>
<p>SQL code has to be embedded within Java Programs which makes it non-portable.</p>
<p>JDBC API allows the developer to fire the SQL queries from the Java code. That means the developer needs to know the specific SQL constructs for the Relational Database Management System (RDBMS) used.</p>
<p>Also, it is the responsibility of the programmer to make sure that the data model and the object model are synchronized properly.</p>
<p>Due to this JDBC API is not a maintainable solution for enterprise applications.</p>
<p>JDBC, I/O, Serialization do not solve the problem of data persistence effectively.</p>
<p>For a medium to be effective, it needs to take care of the fundamental difference in the way Object-Oriented Programs(OOP) and RDBMS deals with the data.</p>
<ul>
	<li>In Programming languages like Java, the related information or the data is persisted in the form of hierarchical and interrelated objects.</li>
	<li>In the relational database, the data is persisted as table format or relations.</li>
</ul>
<p>To Solve these issues, we combine both OOP and RDBMS by mapping tables to Objects</p>
<p>The greatest challenge in integrating the concepts of RDBMS and OOP is  mapping of the Java objects to databases.</p>
<p>When object and relational paradigms work with each other, there arises technical and conceptual difficulties arise, as mapping of an object to a table may not be possible in all the contexts.</p>
<p>Storing and retrieving Java objects using a Relational database exposes a paradigm mismatch called "Object-Relational Impedance Mismatch"</p>
<p>These differences are because of perception, style, and patterns involved in both the paradigms that leads to the following paradigm mismatches:</p>
<ul>
	<li>Granularity: Mismatch between the number of classes in the object model and the number of tables in the relational model.</li>
	<li>Inheritance or Subtype: Inheritance is an object-oriented paradigm that is not available in RDBMS.</li>
	<li>Associations: In object-oriented programming, the association is represented using reference variables, whereas, in the relational model foreign keys are used for associating two tables.</li>
	<li>Identity: In Java, object equality is determined by the "==" operator or "equals()" method, whereas in RDBMS, uses the primary key to uniquely identify the records.</li>
	<li>Data Navigation: In Java, the dot(.) operator is used to travel through the object network, whereas, in RDBMS join operation is used to move between related records. </li>
</ul>

<p>Resolving Object-Relational Impedance Mismatch is one of the key challenges in data persistence. Object Relational Mapping helps to achieve the same.</p>
<p>Object-Relational Mapping (ORM) is a programming paradigm that allows developers to interact with relational databases using an object-oriented approach. In simpler terms, ORM helps bridge the gap between the object-oriented programming language (like Java or Python) and relational databases (like MySQL or PostgreSQL).</p>
<p>Many third-party ORM persistence frameworks like Hibernate, EclipseLink, are available in the market.</p>
<p>These frameworks helped the developers to achieve Object Relational Mapping and perform database operations in the object-oriented approach</p>
<p>But it became challenging to the port application from one ORM framework to another, as every framework addressed the Object-Relational Impedance mismatch in its own way.</p>
<p>In 2006, Java Persistence API (JPA) was released by Java Community Process, to standardize the persistence process. </p>
<p>JPA incorporated many features from the existing frameworks like Hibernate and TopLink Essentials.</p>
<p>JPA became the standard specification for ORM in Java. As the name indicates, JPA is a specification (with a set of interfaces), which provides the standards and specifications to be followed while mapping the Java objects to the database tables.</p>
<p>Each vendor, who provides an ORM framework, implements JPA. A few of the ORM frameworks are EclipseLink, OpenJPA, and Hibernate. Any of these ORM frameworks can be used to connect a Java application to the database. </p>
<img src="https://i.stack.imgur.com/4sVPQ.png">
<h5>Hibernate</h5>
<p>Hibernate is a pure Java Persistence Framework that supports Object Relational Mapping. The main goal of this framework is to release the programmers from the common data persistence related works. It is an open-source framework.</p>
<p>Hibernate provides an implementation for JPA Specification.</p>
<p>Hibernate is a powerful ORM solution that maps user-defined Java classes to DB tables.</p>
<p>Hibernate has a strong query language which is called HQL. It supports native SQL as well.</p>
<p>Hibernate reduces the number of lines in the code by keeping object-table mapping itself and gives the result to an application as Java objects. It ensures the programmer doesn't have to manually handle persistent data, this way reducing the time of development and cost of maintenance.</p>
<p>Hibernate uses SQL based schema for mapping object model to the relational model.</p>