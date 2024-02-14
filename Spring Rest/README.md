<h1>Spring Rest</h1>
<h2>Web Service</h2>
<p>Web services are a standardized way for different software applications to communicate with each other over the internet.</p>
<p>They enable the exchange of data and functionality between systems, regardless of the underlying technologies or programming languages used to build them.</p>
<p>Web services follow a set of protocols and standards, allowing seamless integration between diverse applications and platforms.</p>
<p>Example : </p>
<p>Suppose you have a mobile weather app that allows users to check the current weather conditions in any location. The app itself doesn't have the capability to generate weather data; instead, it relies on a web service to fetch the latest weather information.</p>
<p>Consider a weather web service that offers a simple API endpoint to retrieve current weather information. An application, such as a weather app on your smartphone, can send a request to this web service, and the service responds with the latest weather details for a specified location. The app then displays this information to the user. In this case, the web service acts as a bridge, allowing the weather app to access and use the weather data without needing to store it locally</p>

<p>Web services can be categorized into two main types:</p>
<ul>
	<li>SOAP-based Web Services</li>
	<li>RESTful Web Services</li>
</ul>
<h2>REST (Representational State Transfer):</h2>
<p>REST, which stands for Representational State Transfer, is an architectural style for designing web services which uses HTTP for communication.</p>
<p>The term, REST was coined by Roy Fielding in his doctoral dissertation in 2000.</p>
<p>In REST, everything is a Resource (data/functionality/web page) that is uniquely identified by URI (Uniform Resource Identifier).</p>
<p>The resources of REST can be represented in many ways that include JSON, XML, HTML, etc.,</p>
<h3>Key Features</h3>
<h4>Stateless Communication</h4>
<p>The interaction that takes place between the service provider and consumer is stateless.</p>
<p>Being stateless, the server will never store the state information of its clients. Rather, every request from a service consumer should carry all the necessary information to make the server understand and process the request.</p>
<p>Statelessness ensures that the requests from a consumer should get treated independently. This helps to scale the APIs (making them available in multiple servers) to support a large number of clients concurrently. Any server is free to serve any client since there is no session related dependency. Also, REST APIs stay less complex as server-side state synchronization logic is removed. On top, the performance of APIs is improvised because of less space occupation (no space is required for state-related info).</p>
<p>Simplifies scalability and enhances reliability.</p>
<h4>Uniform Interface</h4>
<p>RESTful systems have a uniform and consistent interface. Key constraints include identification of resources using URIs, manipulation of resources through representations, self-descriptive messages</p>
<p>The client knows that it can use standard HTTP methods and understands the structure of resource URIs. The server returns self-descriptive messages, and the client can navigate through the application state using hyperlinks provided by the server.</p>
<p>In REST, data and functionality are considered as resources. Every resource has a representation (XML/JSON) and a URI to identify the same.</p>
<p>Resources are manipulated using HTTP's GET, POST, PUT and DELETE methods.</p>
<h4>Client Server Architecture</h4>
<p>There is a clear separation of concerns between the client and the server. Clients handle the user interface and user experience, while servers handle processing requests and managing resources.</p>
<p>Supports the independent evolution of the client and server side logic.</p>
<h4>Cacheble</h4>
<p>Service consumers can cache the response data and reuse the same. For example, HTTP GET and HEAD operations are cacheable.</p>
<p>RESTful Web Services use the HTTP protocol as a carrier for the data. And, they can make use of the metadata that is available in the HTTP headers to enable caching. For example, Cache-Control headers are used to optimize caching to enhance performance.</p>
<h4>Code-On-Demand</h4>
<p>Logic that is present in the client end (such as Web browsers) can be modified/maintained independently without affecting the logic at the server side.</p>

<h3>Spring REST</h3>
<p>Spring is an end to end framework which has a lot of modules in a highly organized way.</p>
<p>One among such modules is, Spring MVC that provides the support for REST in addition to the standard MVC support.</p>
<p>The Spring MVC (Model-View-Controller) module is at the core of building RESTful web services in Spring. It provides a powerful and flexible way to handle HTTP requests and responses.</p>
<p>Spring MVC Architecture</p>
<img src="https://terasolunaorg.github.io/guideline/1.0.1.RELEASE/en/_images/RequestLifecycle.png">
<ol>
	<li>The DispatcherServlet intercepts the incoming HTTP request.</li>
	<li>An application can have multiple controllers. So, the DispatcherServlet consults the handler mapping to decide the controller that should work on the request.</li>
	<li>The handler mapping uses request URL to choose the controller. Once done, this decision should be sent to the DispatcherServlet back.</li>
	<li>After receiving appropriate controller name, DispatcherServlet sends the request to the controller.</li>
	<li>The controller receives the request from the DispatcherServlet and executes the business logic that is available in the service layer.</li>
	<li>Once done with the business logic, controller generates some information that needs to be sent back to the client and displayed in the web browser. This information is called as model. Now, the controller sends the model and logical name of the view to the DispatcherServlet.</li>
	<li>The DispatcherServlet passes the logical View name to the ViewResolver, which determines the actual view.</li>
	<li>The actual view is sent back to the DispatcherServlet, now.</li>
	<li>The DispatcherServlet then passes the model to the View, which generates the response.</li>
	<li>The generated respose is sent back to the DispatcherServlet.</li>
	<li>The DispatcherServlet returns the generated response over to the client.</li>
</ol>

<p>Spring provides support for creating RESTful web services using Spring MVC. Availing this support requires, Spring version 3.0 and above.</p>
<p>The REST controllers are different from MVC controllers because REST controllers' methods return results which can be mapped to a representation(data) rather than a view(html pages).</p>
<p>For this, Spring 3.0 introduced @ResponseBody annotation. So, the methods of REST controllers that are annotated with @ResponseBody tells the DispatcherServlet that the result of execution need not to be mapped with a view.</p>
<p>@ResponseBody annotation automatically converts the response to a JSON string literal by applying serialization on the return value of the method.</p>
<img src="https://media.geeksforgeeks.org/wp-content/uploads/20220305144809/SpringResponseBodyAnnotation.JPG">
<p>In Spring 4.0, the @RestController annotation was introduced.</p>
<p>This annotation is a combination of @Controller and @ResponseBody.</p>
<p>This annotation when used on a REST controller class bounds all the values returned by controller methods to the response body</p>
<p>Developing Spring REST as a Boot project still simplifies the developers' job as a lot of things are auto-configured..</p>
<ul>
	<li>Spring MVC : Spring MVC response is a View/Page by default</li>
	<li>Spring REST : In Spring REST data is returned directly back to the client</li>
</ul>

<h4>Steps For Development</h4>
<p>Spring Web MVC module is the source of Spring REST as well.</p>
<p>Spring REST requests are delegated to the DispatcherServlet that identifies the specific controller with the help of handler mapper. Then, the identified controller processes the request and renders the response. This response, in turn, reaches the dispatcher servlet and finally gets rendered to the client.</p>
<p>Here, ViewResolver has no role to play.</p>
<img src="https://media.geeksforgeeks.org/wp-content/uploads/20220305144809/SpringResponseBodyAnnotation.JPG">
<p>The steps involved in exposing business functionality as RESTful web service is:</p>
<ul>
	<li>Create a REST Resource</li>
	<li>Add the service methods that are mapped against the standard HTTP methods</li>
	<li>Configure and deploy the REST application</li>
</ul>
<p>To develop REST applications using Spring Boot, lot of configurations are needed in Step-3 wshich can be avoided as Spring Boot takes care of the same.</p>
<h5>Creating Rest Resource</h5>
<p>Any class that needs to be exposed as a RESTful resource has to be annotated with @RestController</p>
<h6>@RestController</h6>
<p>This annotation is used to create REST controllers.</p>
<p>It is applied on a class in order to mark it as a request handler/REST resource.</p>
<p>This annotation is a combination of @Controller and @ResponseBody annotations. </p>
<p>@ResponseBody is responsible for the automatic conversion of the response to a JSON string literal. If @Restcontroller is in place, there is no need to use @ResponseBody annotation to denote that the Service method simply returns data, not a view. </p>
<p>@RestController is an annotation that takes care of instantiating the bean and marking the same as REST controller.</p>

```java
@RestController
@RequestMapping("/api")
public class MyRestController {
    @GetMapping("/resource")
    public String getResource() {
        return "This is a REST resource.";
    }
}
```

<h6>@RequestMapping</h6>
<p>This annotation is used for mapping web requests onto methods that are available in the resource classes. It is capable of getting applied at both class and method levels. At method level, we use this annotation mostly to specify the HTTP method.
</p>
<p>@RequestMapping is used to map HTTP requests to specific methods in the controller.</p>

```java
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
@RestController
@RequestMapping("/customers")
public class CustomerController 
{
   @RequestMapping(method=RequestMethod.POST)
   public String createCustomer()
   {
     //Functionality goes here
   }
}
```

<p>REST resources have handler methods with appropriate HTTP method mappings to handle the incoming HTTP requests. And, this method mapping usually happens with annotations.</p>
<p>Spring provides annotations for different HTTP methods like @GetMapping, @PostMapping, @PutMapping, and @DeleteMapping. These simplify the mapping of methods to specific HTTP requests.</p>

```java
@RestController
@RequestMapping("/customers")
public class CustomerController 
{
	
	//Fetching the customer details
	@GetMapping
	public String fetchCustomer()
	{
		//This method will fetch the customers of Infytel and return the same. 
		return "customers fetched successfully";
	}
		
	//Adding a new customer
	@PostMapping
	public String createCustomer() 
	{
		//This method will persist the details of a customer
		return "Customer added successfully";
	}
	
	//Updating an existing customer
	@PutMapping
	public String updateCustomer() 
	{
		//This method will update the details of an existing customer 
		return "customer details updated successfully";
	}
	
		
	//Deleting a customer
	@DeleteMapping
	public String deleteCustomer() 
	{
		//This method will delete a customer 
		return "customer details deleted successfully";
	}
}
```

<p>Since a Spring Boot application is to be created with spring-boot-starter-web dependency, much focus is not required on the configurations.</p>
<p>spring-boot-starter-web dependency in the pom.xml will provide the dependencies that are required to build a Spring MVC application including the support for REST.  Also, it will ensure that our application is deployed on an embedded Tomcat server and this option can be replaced with the one that we prefer based on the requirement.</p>

```xml
      <dependency>
           <groupId>org.springframework.boot</groupId>
           <artifactId>spring-boot-starter</artifactId>
      </dependency>
      <dependency>
      <groupId>org.springframework.boot</groupId>
           <artifactId>spring-boot-starter-web</artifactId>
      </dependency>
```

<h6>@RequestBody</h6>
<p>@RequestBody is the annotation that helps map the HTTP request body to a Java Data Transfer Object(DTO). And, this annotation has to be applied on the local parameter (Java DTO) of the request method.</p>
<p>Whenever Spring encounters @RequestBody, it takes the help of the registered HttpMessageConverters which will help convert the HTTP request body to Java Object depending on the MIME type of the Request body.</p>
<p>Example: In the below code snippet, the incoming HTTP request body is deserialized to CustomerDTO. If the MIME type of the incoming data is not mentioned, it will be considered as JSON by default and Spring will use the JSON message converter to deserialize the incoming data.</p>

```java
@PostMapping
public String  createCustomer( @RequestBody CustomerDTO customerDTO)
{
		// logic goes here
}
```

<p>We can specify the expected Mime type using the consumes attribute of the HTTP method matching annotation</p>

```java
@PostMapping(consumes="application/json")
public ResponseEntity<String> createCustomer( @RequestBody CustomerDTO customerDTO)
{
	// logic goes here
}
```

<p>consumes attribute can be supplied with a single value or an array of media types as shown below</p>

```properties
consumes = "text/plain"
  or      
consumes = {"text/plain", "application/json"}
Some more valid values:
consumes = "application/json"
consumes = {"application/xml", "application/json"}
consumes = {"text/plain", "application/*"}
```


<h6>ResponseEntity</h6>
<p>How to send a Java Object in the response?</p>
<p>Java objects can be returned by the handler method like how the normal Java methods can return an object.</p>
<p>Example: In the below example, the fetchCustomer() returns a list of Customer Objects. This list will be converted by Spring's message converter to JSON data.</p>

```java
@GetMapping
public  List<CustomerDTO> fetchCustomer()
{
	//business logic goes here
	return customerService.fetchCustomer();
}
```

<p>The MIME type can be specified, to which the data to be serialized, using the produces attribute of HTTP method matching annotations</p>

```java
@GetMapping(produces="application/json")
public  List<CustomerDTO> fetchCustomer()
{
	//This method will return the customers of Infytel
	return customerService.fetchCustomer();
}
```

<p>Just like consumes, the attribute, produces can also take a single value or an array of MIME types</p>

```properties
Valid values for produces attribute:
produces = "text/plain"
produces = {"text/plain", "application/json"}
produces = {"application/xml", "application/json"} 
```

<p>While sending a response, set the HTTP status code and headers .To help achieving this, we can use ResponseEntity class.</p>
<p>ResponseEntity<T> Will help us add a HttpStatus status code and headers to the response.</p>
<p>Example: In the below code snippet, createCustomer() method is returning a String value and setting the status code as 200.</p>

```java
@PostMapping(consumes="application/json")
public ResponseEntity<String> createCustomer(@RequestBody CustomerDTO customerDTO)
{
	//This method will create a customer
	String response = customerService.createCustomer(customerDTO);
	return ResponseEntity.ok(response);
}
```

<p>Below is the list of constructors available to create ResponseEntity</p>

<ul>
	<li>ResponseEntity(HttpStatus status)</li>
	<li>ResponseEntity(MultiValueMap<String,String> headers, HttpStatus status)</li>
	<li>ResponseEntity(T body, HttpStatus status)</li>
	<li>ResponseEntity(T body, MultiValueMap<String,String> headers, HttpStatus status)</li>
</ul>

<p>here are some ResponseEntity methods available as well.</p>

<ul>
	<li>ok(T body)</li>
	<li>ResponseBuilder badRequest() : ResponseEntity.badRequest().body(message).build();</li>
	<li>ResponseBuilder notFound() : ResponseEntity.notFound().build();</li>
</ul>

<h4>Handling URI Data</h4>
<p>It is not always that the client prefers sending the data as request body. The client can even choose to send the data as part of the request URI. For example, if the client feels that the data is not sensitive and doesn't need a separate channel to get transferred.  So, how to receive this kind of data that appears in the URI.</p>
<p>There are three types</p>
<ol>
	<li>Query Parameter</li>
	<li>Path Variables</li>
	<li>Matrix Variables</li>
</ol>
<img src="https://www.baeldung.com/wp-content/uploads/sites/4/2023/05/API-GET-Request-with-Matrix-Parameter-1.png">

<h5>Query Parameter</h5>
<p>Query parameters or request parameters usually travel with the URI and are delimited by question mark.</p>
<p>The query parameters in turn are delimited by ampersand from one another.</p>
<p>The annotation @RequestParam helps map query/request parameters to the method arguments.</p>
<p>@RequestParam annotation expects the name that it holds to be similar to the one that is present in the request URI.This makes the code tightly coupled with the request URI.</p>

```java
@RestController
@RequestMapping("/calldetails")
public class CallDetailsController 
{
	//Fetching call details based on the request parameters being passed along with the URI
	@GetMapping(produces = "application/json")
	public List<CallDetailsDTO> callDetails(
			@RequestParam("calledBy") long calledBy, @RequestParam("calledOn") String calledOn)
	{
		//code goes here
	}
}
```

<h5>Path Variables</h5>
<p>Path variables are usually available at the end of the request URIs delimited by slash (/).</p>
<p>@Pathvariable annotation is applied on the argument of the controller method whose value needs to be extracted out of the request URI.</p>
<p>A request URI can have any number of path variables.</p>
<p>Multiple path variables require the usage of multiple @PathVariable annotations.</p>
<p>@Pathvariable can be used with any type of request method. For example, GET, POST, DELETE, etc.,</p>
<p>Make sure that the name of the local parameter (int id) and the placeholder ({id}) are same. Name of the PathVariable annotation's argument (@PathVarible("id")) and the placeholder ({id}) should be equal.</p>

```java
@GetMapping("/{id}")
public String controllerMethod(@PathVariable int id){}

@GetMapping("/{id}")
public String controllerMethod(@PathVariable("id") int empId){}

```

<h5>Matrix Variables</h5>
<p>Matrix variables are a block/segment of values that travel along with the URI. For example, /localRate=1,2,3/</p>
<p>These variables may appear in the middle of the path unlike query parameters which appear only towards the end of the URI.</p>
<p>Matrix variables follow name=value format and use semicolon to get delimited from one other matrix variable. </p>
<p>A matrix variable can carry any number of values, delimited by commas.</p>
<p>@MatrixVariable is used to extract the matrix variables.</p>
<p>Below is the URI path. Observe that a variable localRate with two values separated by "," appear in the middle of the path.</p>

```uri
http://<<hostname>>:<<port>>/<<contextpath>>/plans/localRate=1,4 /plan
```

```java
@RestController
@RequestMapping("/plans")
public class PlanController 
{
   //{query} here is a place holder for the matrix variables that travel in the URI, 
   //it is not mandatory that the client URI should hold a string literal called query
   @GetMapping(value = "/{query}/plan", produces = {"application/json","application/xml"})
   public EntityList<PlanDTO> plansLocalRate(
		   @MatrixVariable(pathVar="query") Map<String, List<Integer>> map ) {
   //code goes here
   }
}

```

<p>@MatrixVariable(pathVar="query") Map<String, List> map :The code snippet mentions that all the matrix variables that appear in the path segment of name query should be stored in a Map instance called map. Here, the map's key is nothing but the name of the matrix variable and that is nothing but localRate. And, the value of the map is a collection of localRates (1,4) of type Integer.</p>
<p>Note:If the matrix variable appears towards the end of the URI as in the below example,</p>
<p>URI:http://localhost:8081/infytel-1/customers/calldetails/phoneNo=9123456789</p>

```java
@GetMapping("/customers/{query}")
public ResponseEntity<CallDetails> getCallDetails(@MatrixVariable String phoneNo)
{
//code goes here
}
```

<h4>Exception Handling</h4>
<p>Handling exceptions will make sure that the entire stack trace is not thrown at the end-user which is very hard to read and, possesses a lot of security risks as well. With proper exception handling routine, it is possible for an application to send customized messages during failures and continue without being terminated abruptly.</p>
<p>Exception handling makes any application robust.</p>
<p>@ExceptionHandler is a Spring annotation that plays a vital role in handling exceptions thrown out of handler methods(Controller operations). This annotation can be used on the </p>
<ul>
	<li>Methods of a controller class. Doing so will help handle exceptions thrown out of the methods of that specific controller alone.</li>
	<li>Methods of classes that are annotated with @RestControllerAdvice. Doing so will make exception handling global.</li>
</ul>
<p>The most common way is applying @ExceptionHandler on methods of the class that is annotated with @RestControllerAdvice. This will make the exceptions that are thrown from the controllers of the application get handled in a centralized way. As well, there is no need for repeating the exception handling code in all the controllers, keeping the code more manageable.</p>

```java
@RestController
@RequestMapping("/customers")
public class CustomerController 
{
	// Deleting a customer
	@DeleteMapping(value = "/{phoneNumber}", produces = "text/html")
	public String deleteCustomer(
			@PathVariable("phoneNumber") long phoneNumber) 
				throws NoSuchCustomerException {
		// code goes here
	}
}


{
    "timestamp": "2019-05-02T16:10:45.805+0000",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Customer does not exist :121",
    "path": "/infytel-7/customers/121"
}

```

<p>In case, a customized error message should be provided that is easy to understand , you need to</p>
<ul>
	<li>Create a class annotated with @RestControllerAdvice</li>
	<li>Have methods annotated with @ExceptionHandler(value=NameoftheException) which takes the exception class as the value for which the method is the handler</li>
</ul>
<p>There should be multiple methods in the Advice class to handle exceptions of different types and return the custom messages accordingly.</p>

```java
@RestControllerAdvice
public class ExceptionControllerAdvice {
	@ExceptionHandler(NoSuchCustomerException.class)
	public ResponseEntity<String> exceptionHandler2(NoSuchCustomerException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}
}

```

<p>It is important to map the exceptions to objects that can provide some specific information which allows the API clients to know, what exactly must have happened. So, instead of returning a String, an object that holds the error message and error code must be sent back to the client.</p>
<p>The object which is going to hold a custom error message is ErrorMessage with two variables, errorcode and message.</p>

```java
public class ErrorMessage {
	 private int errorCode;
	 private String message;
     //getters and setters go here
}

@RestControllerAdvice
public class ExceptionControllerAdvice {
	@ExceptionHandler(NoSuchCustomerException.class)
	public ResponseEntity<ErrorMessage> exceptionHandler2(NoSuchCustomerException ex) {
		ErrorMessage error = new ErrorMessage();
		error.setErrorCode(HttpStatus.BAD_GATEWAY.value());
		error.setMessage(ex.getMessage());
		return new ResponseEntity<>(error, HttpStatus.OK);
	}
}

```

<p>Here, the handler method returns the instance of ErrorMessage that holds the error code and message, rather returning a String as the body of ResponseEntity.</p>



<h4>Data Validation</h4>
<p>Some times, the RESTful web service might need data in certain standard that should pass through specific constraints.</p>
<p>Spring supports Java Bean Validation (JSR 380) annotations that can be applied to model attributes or method parameters.</p>
<p>Common annotations include @NotNull, @NotEmpty, @Size, @Pattern, etc.</p>

```java
public class Book {
    @NotNull
    private String title;

    @NotNull
    @Size(min = 1, max = 50)
    private String author;

    // Getters and setters
}

```

<p>Annotations like @Valid or @Validated can be used in controller methods to trigger validation on request parameters or request bodies.</p>

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    @PostMapping("/create")
    public ResponseEntity<String> createBook(@Valid @RequestBody Book book) {
        // Logic to create a new book
        return new ResponseEntity<>("Book created successfully", HttpStatus.CREATED);
    }
}
```

<p>When validation fails, Spring automatically generates a MethodArgumentNotValidException. This exception can be handled globally or per-controller basis using @ExceptionHandler.</p>

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                              .getFieldErrors()
                              .stream()
                              .map(error -> error.getDefaultMessage())
                              .collect(Collectors.toList());

        return new ResponseEntity<>(String.join(", ", errors), HttpStatus.BAD_REQUEST);
    }
}

```

<p>Developers can create custom validation annotations by implementing custom constraints and validators.</p>

```java
@Constraint(validatedBy = UniqueUsernameValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueUsername {
    String message() default "Username must be unique";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

<p>When validation errors occur, they can be accessed through the BindingResult object in controller methods.</p>

```java
@PostMapping("/create")
public ResponseEntity<String> createBook(@Valid @RequestBody Book book, BindingResult result) {
    if (result.hasErrors()) {
        // Handle validation errors
        return new ResponseEntity<>("Validation failed: " + result.getAllErrors(), HttpStatus.BAD_REQUEST);
    }
    // Logic to create a new book
    return new ResponseEntity<>("Book created successfully", HttpStatus.CREATED);
}
```

<h4>Versioning</h4>
<p>Versioning in Spring REST involves managing different versions of your API to ensure backward compatibility while introducing new features or changes. There are various strategies for versioning APIs, and Spring provides support for multiple approaches. Here are some common ways to implement versioning in Spring REST:</p>
<p>1. URI Versioning:</p>
<p>In URI versioning, the API version is included in the URI path. It's one of the simplest approaches but may lead to longer and less readable URLs.</p>

```java
// Version 1
@RequestMapping("/api/v1/books")

// Version 2
@RequestMapping("/api/v2/books")

```

<p>2. Request Parameter Versioning:</p>
<p>In this approach, the API version is specified as a request parameter.</p>

```java
// Version 1
@RequestMapping(value = "/api/books", params = "version=1")

// Version 2
@RequestMapping(value = "/api/books", params = "version=2")

```

<p>3. Header Versioning:</p>
<p>The API version is specified in the request header. This approach helps keep the URL clean.</p>

```java
// Version 1
@RequestMapping(value = "/api/books", headers = "X-API-Version=1")

// Version 2
@RequestMapping(value = "/api/books", headers = "X-API-Version=2")

```

<p>4. Accept Header or Content Negotiation Versioning:</p>
<p>The API version is determined based on the Accept header in the HTTP request, typically using custom media types.</p>

```java
// Version 1
@RequestMapping(value = "/api/books", produces = "application/vnd.company.api.v1+json")

// Version 2
@RequestMapping(value = "/api/books", produces = "application/vnd.company.api.v2+json")
```

<p>5. Custom Header Versioning:</p>
<p>Define a custom header for versioning.</p>

```java
// Version 1
@RequestMapping(value = "/api/books", headers = "API-Version=1")

// Version 2
@RequestMapping(value = "/api/books", headers = "API-Version=2")

@RestController
public class BookController {

    // Version 1
    @RequestMapping(value = "/api/v1/books", method = RequestMethod.GET)
    public ResponseEntity<String> getVersion1() {
        return new ResponseEntity<>("Version 1", HttpStatus.OK);
    }

    // Version 2
    @RequestMapping(value = "/api/v2/books", method = RequestMethod.GET)
    public ResponseEntity<String> getVersion2() {
        return new ResponseEntity<>("Version 2", HttpStatus.OK);
    }
}

```

<h4>Cross Origin Resource Sharing (CORS)</h4>
<p>Cross-Origin Resource Sharing (CORS) is a security feature implemented by web browsers to control how web pages in one domain can request and interact with resources from another domains</p>
<p>In the context of a Spring REST API, CORS is relevant when a web application served from one domain makes requests to a Spring REST API hosted on a different domain. By default, web browsers restrict cross-origin requests for security reasons, but CORS headers can be used to control and allow these requests.</p>
<p>Spring provides multiple ways to configure CORS in a REST API:</p>
<h5>Using @CrossOrigin Annotation:</h5>
<p>You can use the @CrossOrigin annotation at the controller level or at the method level to specify which origins are allowed to access the RESTful API.</p>

```java
@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://allowed-domain.com")
public class BookController {
    // Controller methods
}

//You can also allow multiple origins:

@CrossOrigin(origins = {"http://allowed-domain-1.com", "http://allowed-domain-2.com"})

```

<h5>Global CORS Configuration:</h5>
<p>Create a global configuration for CORS by extending the WebMvcConfigurer interface and overriding the addCorsMappings method.</p>

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://allowed-domain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```
<p>This example allows cross-origin requests to all endpoints under /api/, specifies allowed methods and headers, allows credentials, and sets the maximum age of the pre-flight request.</p>

<h6>Configuration Options:</h6>
<p>allowedOrigins: Specifies the list of origins allowed to access the resource.</p>
<p>allowedMethods: Specifies the HTTP methods allowed for cross-origin requests.</p>
<p>allowedHeaders: Specifies the HTTP headers allowed for cross-origin requests.</p>
<p>allowCredentials: Indicates whether the browser should include credentials such as cookies or HTTP authentication in the request.</p>
<p>maxAge: Specifies the maximum age (in seconds) of the results of a pre-flight request.</p>

<h5>Handling Preflight Requests:</h5>
<p>Browsers may send a preflight OPTIONS request to check if the actual request is safe to send.</p>
<p>Spring automatically handles preflight requests if the CORS configuration is properly set.</p>
<p>The @CrossOrigin annotation and CorsRegistry provide options for configuring preflight requests.</p>

```java
@CrossOrigin(origins = "http://allowed-domain.com", maxAge = 3600)
@GetMapping("/resource")
public ResponseEntity<String> getResource() {
    // Logic to get a resource
    return new ResponseEntity<>("Resource data", HttpStatus.OK);
}
```

<h1>Swagger with Spring REST</h1>

<p>
    Swagger is a powerful tool for documenting and testing APIs. When integrated with Spring REST, it simplifies API documentation and provides an interactive interface for developers to explore and understand the API.
</p>

<h2>Integration Steps</h2>

<ol>
    <li><strong>Add Swagger Dependencies:</strong></li>
</ol>

<p>
    Include the necessary Swagger dependencies in your Spring Boot project. For Maven, add the following dependency to your <code>pom.xml</code>:
</p>

<pre>
    <dependency>;
        <groupId>io.springfox</groupId>
        <artifactId>springfox-boot-starter</artifactId>
        <version>3.0.0</version>
    </dependency>;
</pre>

<ol start="2">
    <li><strong>Configure Swagger:</strong></li>
</ol>
<p>Enable Swagger in our REST Application through @EnableSwagger2 annotation as shown below</p>
```java
@SpringBootApplication
@EnableSwagger2
public class InfytelDemo10Application {
	public static void main(String[] args) {
		SpringApplication.run(InfytelDemo10Application.class, args);
	}
}
```

<ol start="3">
    <li><strong>Access Swagger UI:</strong></li>
</ol>

<p>
    Once your Spring Boot application is running, access the Swagger UI by navigating to:
</p>

<pre>
    <strong>Swagger UI URL:</strong> http://localhost:8080/swagger-ui.html
</pre>

<p>
    The Swagger UI provides an interactive documentation interface where you can explore your API and test endpoints.
</p>

<h2>Common Swagger Annotations</h2>

<ol>
    <li><strong>@Api Annotation:</strong></li>
</ol>

<p>
    The <code>@Api</code> annotation is used at the class level to provide metadata about the API.
</p>

<pre>
    <code>
        @RestController
        @RequestMapping("/api/books")
        @Api(tags = "Book Controller", description = "Operations related to books")
        public class BookController {
            // ...
        }
    </code>
</pre>

<p>
    The <code>tags</code> attribute describes the tags associated with the API, and the <code>description</code> attribute provides a short description of the API.
</p>

<ol start="2">
    <li><strong>@ApiOperation Annotation:</strong></li>
</ol>

<p>
    The <code>@ApiOperation</code> annotation is used at the method level to provide metadata about a specific operation.
</p>

<pre>
    <code>
        @GetMapping("/{id}")
        @ApiOperation(value = "Get a book by ID", notes = "Retrieves a book based on its unique ID")
        public Book getBookById(@PathVariable Long id) {
            // ...
        }
    </code>
</pre>

<p>
    The <code>value</code> attribute provides a short description of the operation, and the <code>notes</code> attribute gives additional notes about the operation.
</p>

<ol start="3">
    <li><strong>@ApiParam Annotation:</strong></li>
</ol>

<p>
    The <code>@ApiParam</code> annotation is used to describe a parameter in an API operation.
</p>

<pre>
    <code>
        @GetMapping("/{id}")
        @ApiOperation(value = "Get a book by ID", notes = "Retrieves a book based on its unique ID")
        public Book getBookById(@ApiParam(value = "ID of the book", required = true) @PathVariable Long id) {
            // ...
        }
    </code>
</pre>

<p>
    The <code>value</code> attribute provides a brief description of the parameter, and the <code>required</code> attribute indicates whether the parameter is required.
</p>

<ol start="4">
    <li><strong>@ApiResponse and @ApiResponses Annotations:</strong></li>
</ol>

<p>
    The <code>@ApiResponse</code> and <code>@ApiResponses</code> annotations are used to provide multiple possible responses for an API operation.
</p>

<pre>
    <code>
        @GetMapping("/{id}")
        @ApiOperation(value = "Get a book by ID", notes = "Retrieves a book based on its unique ID")
        @ApiResponses({
            @ApiResponse(code = 200, message = "Successfully retrieved the book"),
            @ApiResponse(code = 404, message = "Book not found"),
            @ApiResponse(code = 500, message = "Internal server error")
        })
        public ResponseEntity<Book> getBookById(@PathVariable Long id) {
            // ...
        }
    </code>
</pre>

<p>
    The <code>@ApiResponse</code> annotation is applied at the method level, and <code>@ApiResponses</code> can be used to encapsulate multiple <code>@ApiResponse</code> annotations.
</p>

<ol start="5">
    <li><strong>@ApiModel and @ApiModelProperty Annotations:</strong></li>
</ol>

<p>
    The <code>@ApiModel</code> and <code>@ApiModelProperty</code> annotations are used to describe the model of an API response or request.
</p>

<pre>
    <code>
        @ApiModel(description = "Details about the book")
        public class Book {
            @ApiModelProperty(notes = "The unique ID of the book")
            private Long id;

            @ApiModelProperty(notes = "Title of the book")
            private String title;

            // Getters and setters
        }
    </code>
</pre>

<p>
    The <code>@ApiModel</code> annotation is applied at the class level, and <code>@ApiModelProperty</code> is applied at the field level to describe specific properties of the model.
</p>

<p>
    These annotations, when used appropriately, help Swagger generate comprehensive and accurate documentation for your Spring REST API. They enhance the readability and understanding of your API by providing clear information about operations, parameters, models, and possible responses.
</p>