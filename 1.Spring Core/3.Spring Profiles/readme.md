# Spring Profiles and Logging Guide

## Spring Profile

Spring Profiles helps to classify the classes and properties file for the environment

You can create multiple profiles and set one or more profiles as the active profile. Based on the active profile spring framework chooses beans and properties file to run.

Let us see how to configure different profiles using spring profiles in our projects.

### Steps to be followed:

1. Identify the beans which has to be part of a particular profile [Not mandatory]
2. Create environment-based properties file
3. Set active profiles.

You can identify the Spring beans which have to be part of a profile using the following ways

1. Annotation @Profile
2. Bean declaration in XML

@Profile helps spring to identify the beans that belong to a particular environment.

1. Any class which is annotated with stereotype annotations such as @Component,@Service,@Repository and @Configuration can be annotated with @Profile .
2. @Profile is applied at class level except for the classes annotated with @Configuration where @Profile is applied at the method level.

### @Profile-Class level:

```java
@Profile("dev")
@Component
@Aspect
public class LoggingAspect { 
}
```

This LoggingAspect class will run only if "dev" environment is active.

### @Profile- Method level:

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

In the above code snippet CustomerService bean is configured differently in the "dev" and "prod" environments. Depending on the currently active profile Spring fetches CustomerService accordingly.

@Profile annotation in class level should not be overridden in the method level. It will cause NoSuchBeanDefinitionException.

### @Profile value can be prefixed with !(Not) operator

```java
@Profile("!test")
@Configuration
@ComponentScan(basePackages="com.infy.service")
public class SpringConfiguration { 
}
```

This SpringConfiguration class will run in all environments other than test.

**Note:** if you do not apply @profile annotation on a class, means that the particular bean is available in all environment(s).

## Environment-based Properties Files

Spring Boot relies on application.properties file for configuration. To write configuration based on environment, you need to create different application.properties files.

The file name has a naming convention as application-<user created profile name>.properties

### application-dev.properties

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

### application-test.properties

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

### application-prod.properties

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

The above shows how application.properties file with different configuration is created for 3 different environments(dev,test and prod).

## Setting Active Profiles

By now you are aware of how to map a bean or configure properties to a certain profile(eg :dev/prod/test), next we need to set the one which is active.

The different way to do is as follows:

1. application.properties
2. JVM System Parameter
3. Maven Profile

You can make more than 1 profile as active at a time.

### Using application.properties

To set an active profile you need to write the below property in the main application.properties file.

```properties
spring.profiles.active=dev
```

```properties
spring.profiles.active=dev,prod
```

### Using JVM System Parameter

You can set profile using JVM system arguments in two ways , either set the VM arguments in Run configuration or programmatic configuration.

To set through Run configuration give the below command in VM arguments.

```java
-Dspring.profiles.active=dev
```

To set through programmatic approach set system property as follows,

```java
public static void main( String[] args )    {     
    System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "prod");
}
```

### Through the command line:

```
--spring.profiles.active=dev
```

## Spring Logging

During the execution of an application, several events are triggered at different points while the user interacts with the application and a record of all those events need to be kept.

This can help the administrator and the programmer to diagnose, debug and maintain the application.

The process of writing log messages to a central location during the execution of the program, thus tracking every step in the execution of a program including any event or exception along with their relevant details, is called Logging.

There are multiple reasons to log the application activity. We can record unusual circumstances or errors that may be happening in the program as well as information about the application's execution. Having these records helps in quick problem diagnosis, debugging, and maintenance of the application.

We need logging frameworks to log the events and messages in any application.

- It allows you to configure logging level. Logging levels are basically used to differentiate the logs according to their severity. Some examples of logging level are Info, Warn and Error.
- It also allows you to set the destination of the logs according to your requirement. Suppose you want to store it in a file or a database. It allows you to do that.

### Popular Logging Frameworks

There are several logging frameworks to make logging easier. Some of the popular ones are:

- JDK Logging API
- Apache Log4j
- Commons Logging API

Spring Boot uses Apache Commons Logging for logging. It provides default configurations for using Java Util Logging, Log4j2, and Logback as logging implementation. If Spring Boot starters are used then Logback is used for logging by default.

- We can customize the default logging configurations of Spring Boot or use other logging frameworks of our choice as well.

## Logging Levels

**ALL:** For all the levels (required for user defined levels)

**TRACE:** The most detailed level. Used for fine-grained debugging information.

**DEBUG:** Used for debugging information.

**INFO:** Used to provide informational messages.

**WARN:** Used for potentially harmful situations.

**ERROR:** Used for error messages.

**FATAL:** Rarely used, signifies a very severe error that will lead the application to abort.

**OFF:** To disable all the levels

## Logback Configuration

As we have seen before, Spring Boot uses Logback as its default logging framework.

Generally, all 'spring-boot-starter' dependencies make use of Apache Commons Logging library to implicitly add some logging framework jars such as Java Util Logging, Logback , Log4J in the applications.

let us learn about Logback and some of its configurations.

Logging levels, Logging format, and Log file location can be easily configured according to the requirement in "application.properties" file.

### Configuring logging levels

Spring Boot by default, logs messages at ERROR, WARN, and INFO levels. Logging levels can easily be configured just by setting up the following property in application.properties file.

```properties
logging.level.com.infy=INFO
```

Here logging level is set as Info (highlights the progress of an application) for classes of com.infy package.

So, Up to INFO, It Will log

### Configuring logging pattern

You can change the logging pattern for console by setting logging.pattern.console property in application.properties file.

```properties
logging.pattern.console=%d{yyyy-MMM-dd HH:mm:ss a} [%t] %-5level %logger{36} - %msg%n
```

To change the logging pattern for file, set the logging.pattern.file property in application.properties file.

Suppose you want to log a message in error log file in this format.

Then you can set the property like this

```properties
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Configuring logging file

By default, Spring Boot logs only to the console and does not log to files. If you want to log messages in files in addition to the console, you need to set logging.file property in application.properties file. The value for this property can be exact location of file or relative to the current directory. For example, to log messages in error.log file you can set logging.file.name property as follows:

```properties
logging.file.name = Error.log
```

You can also set the location of the log file by setting logging.file.path property in application.properties file.

```properties
logging.file.path=logs/Error.log
```

To have more more control over logging, you can use the logging provider specific configuration file in their default locations, which Spring Boot will automatically use.

We have that Logback is used as the default logging framework by spring boot.

## Using Log4J

In order to use Log4J, first you have to exclude the default logging dependency which is 'spring-boot-starter-logging' and then include 'spring-boot-starter-log4j2' starter in pom.xml file.

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

Log4j also allows you to log the information at different places which can be either file, console, database etc.

For configuration of Log4j, add log4j.properties file to the root class path of the application so that spring boot automatically recognize it and can use it for logging.

Once configuration is done you can use the Log4J logger in the application. To log information, you first need to get a logger object.

This logger object is obtained by invoking getLogger() method of the org.apache.logging.log4j.LogManager class.

This method takes the details of the class in which you want to log as a parameter and gives an object of org.apache.logging.log4j.Logger interface.
