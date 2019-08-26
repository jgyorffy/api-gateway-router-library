# RISE API GATEWAY ROUTER LIBRARY

## Features
- Intented to work with AWS's API Gateway Proxy requests
- For Lambdas that do not use Spring but would like the same controller pattern
- Minimum reliance on 3rd party libraries and small code-base
- Uses Jackson JSON parser to convert return objects to the resonse
- Works with the Golang Rise-authorizer to extract the issuer, audience, and other claims
- Built-in lambda warmer logic
- Traps and maps exceptions to your custom exception output handler
- Uses JSR-380 for JSON to-object validation on fields


## Getting Started
### Gradle
[Check for the latest library](https://github.com/jgyorffy/api-gateway-router-library)
```groovy
implementation 'com.datapark.agwy:api-gateway-router-library:0.1.+'
```
### Imports
```java
import com.datapark.agwy.lambda.Controller;
import com.datapark.agwy.lambda.LambdaRequest;
import com.datapark.agwy.lambda.LambdaResponse;
import com.datapark.agwy.lambda.RouteHandler;
import com.datapark.agwy.lambda.ValidationException;
```
### AWS Lambda Setup
The entry function for the lambda is:
```java
api-gateway-router-library.LambdaHandler::handleRequest
```


### Lambda Request and Security Context from the JWT Authorizer
The request object passed to the function that is mapped in the controller has several convenience functions to extract path parameters, query parameters, and JST claims.
```java
//Gets the claims from the api gateway authorizer
public SecurityContext getSecurityContext();
//get the param such as path/{id}  or the param such as path?id=foo
public String getResourceParamAsString(String key) ;
//gets the multivalue param such as path?id=foo&id=yuk
public Map<String, List<String>> getResourceMultiValueMap();
public Map<String, String[]> getResourceMultiValueArray();
public List<String> getResourceParamAsList(String key);
public List<String> getResourceParamAsListOrDefault(String key, List<String> defaultVal);
public String getResourceParamAsStringOrDefault(String key, String defaultVal);
public long getResourceParamAsLongOrDefault(String key, long defaultVal);
public int getResourceParamAsIntOrDefault(String key, int defaultVal);
public double getResourceParamAsDoubleOrDefault(String key, double defaultVal);
public Optional<String> getHeaderValue(String key)
//gets the body and converts it with Jackson and validates JSR-380 annotations
public <T> T getBodyAsObject(Class<T> clazz);
//gets the resource path in the URL
public String getResource();
public String getHttpMethod();
```
#### Security context
Uses the [api-gateway-jwt-authorizer](https://github.com/jgyorffy/api-gateway-jwt-authorizer) for extracting claims
```java
//get all custom claims such as scp, etc (if it has commans you will have to split)
public String getClaim(String key);
//get the issuer of the JWT
public String getIssuer();
//get a list of audiences this JWT targets
public List<String> getAudiences();
```

### Setting up a controller with routes

#### Override the lambda handler class
``` java
public class ControllerHandler extends LambdaHandler {
	@Override
	protected void loadControllers(final Router router) {
	 router.addController(routeHandler -> routeHandler.setRouteHandler("/pathvar/{param1}/{param2}", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    return LambdaResponse.builder()
                            .returnCode(200)
                            .headers(new HashMap<>())
                            .responseObject(new TestModel("1234", "USA")).build();
                }, getErrorHandlers()));
	};
	router.addController(routeHandler -> routeHandler.setRouteHandler("/pathvar/{param1}", RouteHandler.HTTPMethod.PUT, objectMapper, (request) -> {
                    return LambdaResponse.builder()
                            .returnCode(202)
                            .headers(new HashMap<>())
                            .responseObject(new TestModel("1234", "USA")).build();
                }, getErrorHandlers()));
	}
}
```
*** *Note*: objectMapper is your customized Jackson ObjectMapper object if you have one, otherwise, null.
*** *Note*: getErrorHandlers() is your own function to load the error handlers as described next.

#### Define error handlers and error return object
 
```java
public class MyBigExceptionHandler {

    private static RouteHandler.ExceptionHandlerInfo getErrorHandler(Class clazz, RouteHandler.ExceptionHandler handler) {
        return new RouteHandler.ExceptionHandlerInfo() {
            @Override
            public LambdaResponse apply(Throwable ex) {
                return handler.apply(ex);
            }

            @Override
            public Class getExceptionClass() {
                return clazz;
            }
        };
    }

    static Set<RouteHandler.ExceptionHandlerInfo> getErrorHandlers() {
        Set<RouteHandler.ExceptionHandlerInfo> setOfExceptionHandlers = new HashSet<>();
        
        //example of returning based on a generic runtime exception
        setOfExceptionHandlers.add(getErrorHandler(Exception.class, (ex) -> {
            log.error("Unexpected error occurred", ex);
            return LambdaResponse.builder()
                    .returnCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .responseObject(ControllerErrorResponse.builder().message("An unexpected error occurred").build()).build();
        }));

        //Example of catching and returning based on a specfic exception
        setOfExceptionHandlers.add(getErrorHandler(ElasticsearchIndexNotFoundException.class, (ex) -> {
            log.error("Index not found exception {}", elasticsearchUrl(), ex);
            return LambdaResponse.builder()
                    .returnCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .responseObject(ControllerErrorResponse.builder().message("An unexpected error occurred").build()).build();
        }));
        
        //Example of failing JSR-380 validation when submitted with a POST or PUT
        setOfExceptionHandlers.add(getErrorHandler(ValidationException.class, (ex) -> {
            ValidationException validationException = (ValidationException) ex;
            List<SingleControllerError> violations = null;
            if (!validationException.getViolations().isEmpty()) {
                violations = validationException.getViolations()
                        .stream()
                        .map(violation -> SingleControllerError.builder()
                                .code((violation.getConstraintDescriptor().getAnnotation().getClass().equals(NotNull.class) ||
                                        violation.getConstraintDescriptor().getAnnotation().getClass().equals(NotBlank.class)) ?
                                        ValidationErrorCode.MISSING_REQUIRED.getCode()
                                        : ValidationErrorCode.INVALID_VALUE.getCode())
                                .field(violation.getPropertyPath().toString())
                                .message(violation.getMessage())
                                .build()
                        )
                        .collect(Collectors.toList());
            }

            log.info("Request was invalid ", ex);
            return LambdaResponse.builder()
                    .returnCode(HttpStatus.BAD_REQUEST.value())
                    .responseObject(ControllerErrorResponse.builder()
                            .message("Request validation failed")
                            .errors(violations)
                            .build()
                    ).build();
        }));
   ...
```

*** *Note*: You must have at least one handler for the `Exception.class` for general internal server errors

#### Log format using slf4j
```
<PatternLayout pattern="%-5p %d [%t] %c lid=%X{lid} app=myapp traceId=%X{traceId}:
```
Where 
 - lid is the unique ID of the lambda container (useful for monitoring lambda warming)
 - traceId is the `X-B3-TraceId` or the `X-Amzn-Trace-Id` sent from AWS's API Gateway
 - 
#### Using Lambda warming functionality
This library will handle lambda warming for you. Lambda warming requires that you request for a new lambda container that is not already active (in memory). To have approximately x number of lambda containers ready to service requests without a `cold start`, all lambdas *must* be busy servicing a request before a new container will start. To do warming this approach is used:
- Use cloudwatch to send an empty proxy JSON payload (format described below) every `5` minutes (no more or less!)
- Have the lambda using this gateway library sleep x amount of time to stay busy while the next lambda is called in parallel.
- *Never* warm all lambdas that you have allocated for the max concurrency of lambdas. It should just be 1/3 to 1/2 warm dependining on the average concurrent lambdas you have over time.

```json
{"body":null,"resource":null,"requestContext":{"resourceId":null,"apiId":null,"resourcePath":null,"httpMethod":null,"requestId":null,"extendedRequestId":null,"accountId":null,"identity":{"apiKey":null,"apiKeyId":null,"userArn":null,"cognitoAuthenticationType":null,"caller":"_lambda_warmer_","userAgent":null,"user":null,"cognitoIdentityPoolId":null,"cognitoIdentityId":null,"cognitoAuthenticationProvider":null,"sourceIp":null,"accountId":null,"accessKey":null},"authorizer":null,"stage":null,"path":"*","protocol":null,"requestTime":null,"requestTimeEpoch":0,"elb":null},"multiValueQueryStringParameters":{},"multiValueHeaders":{},"pathParameters":{"CALL_NUMBER":"100","SLEEP_MS":"500"},"httpMethod":null,"stageVariables":{},"path":"*","base64Encoded":false,"requestSource":"API_GATEWAY"}
```
`"pathParameters":{"CALL_NUMBER":"100","SLEEP_MS":"500"}`
Where pathParameters have two values that can change depending on your tests:
- CALL_NUMBER: how many lambda to warm (could be slightly lower than planned due to execution probability).
- SLEEP_MS: how many milliseconds to make the lambda execution sleep so another container would be forced to start.









