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