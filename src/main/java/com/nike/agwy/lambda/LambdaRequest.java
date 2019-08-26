package com.nike.agwy.lambda;


import com.amazonaws.serverless.proxy.model.ApiGatewayAuthorizerContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;


public class LambdaRequest {
    private final AwsProxyRequest awsProxyRequest;
    private final ObjectMapper objectMapper;
    private final SecurityContext securityContext;

    public LambdaRequest(AwsProxyRequest awsProxyRequest, ObjectMapper objectMapper) {
        Optional.ofNullable(awsProxyRequest).orElseThrow(() -> new IllegalArgumentException("AwsProxyRequest can't be null"));
        Optional.ofNullable(objectMapper).orElseThrow(() -> new IllegalArgumentException("ObjectMapper can't be null"));

        ApiGatewayAuthorizerContext apiGatewayAuthorizerContext =
                Optional.of(awsProxyRequest).map(AwsProxyRequest::getRequestContext)
                        .map(AwsProxyRequestContext::getAuthorizer).orElse(null);

        this.awsProxyRequest = awsProxyRequest;
        this.objectMapper = objectMapper;
        this.securityContext = new SecurityContext(apiGatewayAuthorizerContext);
    }

    public SecurityContext getSecurityContext() {
        return this.securityContext;
    }

    public String getResourceParamAsString(String key) {
        return Optional.ofNullable(awsProxyRequest.getPathParameters()).map(m -> m.get(key)).orElse(Optional.ofNullable(awsProxyRequest.getMultiValueQueryStringParameters()).map(n -> n.getFirst(key)).orElse(""));
    }

    public Map<String, List<String>> getResourceMultiValueMap() {
        Map<String, List<String>> retMapOfLists = new HashMap<>();
        MultiValuedTreeMap<String, String> queryParams = Optional.ofNullable(awsProxyRequest.getMultiValueQueryStringParameters()).orElse(new MultiValuedTreeMap<>());
        queryParams.entrySet().stream().forEach(e -> retMapOfLists.put(e.getKey(), e.getValue()));
        return retMapOfLists;
    }

    public Map<String, String[]> getResourceMultiValueArray() {
        Map<String, String[]> retMapOfLists = new HashMap<>();
        MultiValuedTreeMap<String, String> queryParams = Optional.ofNullable(awsProxyRequest.getMultiValueQueryStringParameters()).orElse(new MultiValuedTreeMap<>());
        queryParams.entrySet().stream().forEach(e -> retMapOfLists.put(e.getKey(), e.getValue().stream().toArray(String[]::new)));
        return retMapOfLists;
    }

    public List<String> getResourceParamAsList(String key) {
        return Stream.of(getResourceParamAsString(key).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public List<String> getResourceParamAsListOrDefault(String key, List<String> defaultVal) {
        return Optional.of(getResourceParamAsList(key)).filter(s -> !s.isEmpty()).orElse(defaultVal);
    }

    public String getResourceParamAsStringOrDefault(String key, String defaultVal) {
        return Optional.of(getResourceParamAsString(key)).filter(s -> !s.isEmpty()).orElse(defaultVal);
    }

    public long getResourceParamAsLongOrDefault(String key, long defaultVal) {
        return Try.of(() -> Long.parseLong(getResourceParamAsString(key)))
                .onFailure(ex -> ex.toString())
                .getOrElse(defaultVal);
    }

    public int getResourceParamAsIntOrDefault(String key, int defaultVal) {
        return Try.of(() -> Integer.parseInt(getResourceParamAsString(key)))
                .onFailure(ex -> ex.toString())
                .getOrElse(defaultVal);
    }

    public double getResourceParamAsDoubleOrDefault(String key, double defaultVal) {
        return Try.of(() -> Double.parseDouble(getResourceParamAsString(key)))
                .onFailure(ex -> ex.toString())
                .getOrElse(defaultVal);
    }

    public Optional<String> getHeaderValue(String key) {
        return Optional.ofNullable(awsProxyRequest.getMultiValueHeaders()).map(h -> h.get(key)).filter(l -> !l.isEmpty()).map(l -> l.get(0));
    }

    public <T> T getBodyAsObject(Class<T> clazz) {

        T obj = Try.of(() -> objectMapper.readValue(awsProxyRequest.getBody(), clazz))
                .getOrElseThrow((ex) -> new RuntimeException("Invalid object type in request body", ex));

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator JSONvalidator = factory.getValidator();
        Set<ConstraintViolation<T>> violations = JSONvalidator.validate(obj);
        if (!violations.isEmpty()) {
            throw new ValidationException("Failed validation", violations.stream().collect(toSet()));
        }

        return obj;
    }

    public String getResource() {
        return awsProxyRequest.getResource();
    }

    public String getHttpMethod() {
        return awsProxyRequest.getRequestContext().getHttpMethod();
    }
}
