package com.nike.agwy.utils;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nike.agwy.lambda.LambdaHandlerTests;
import io.vavr.control.Try;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class TestUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    TestUtils() {
    }

    public static AwsProxyResponse deserialize(ByteArrayOutputStream bout) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(bout.toByteArray());
        return objectMapper.readValue(in, AwsProxyResponse.class);
    }

    public static InputStream getFileIO(String fileName) throws FileNotFoundException {
        ClassLoader classLoader = new LambdaHandlerTests().getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return new FileInputStream(file);
    }

    public static InputStream setupGatewayRequest(String resource, String path, String httpMethod, String body,
                                                  JsonNode pathParameters, JsonNode queryStringParameters, JsonNode multiValueQueryStringParameters) {
        return setupGatewayRequest(null, resource, path, httpMethod, body, pathParameters, queryStringParameters, multiValueQueryStringParameters);
    }

    public static InputStream setupGatewayRequest(String issuer, String resource, String path, String httpMethod, String body,
                                                  JsonNode pathParameters, JsonNode queryStringParameters, JsonNode multiValueQueryStringParameters) {
        ObjectNode objectNode = Try.of(() -> (ObjectNode) objectMapper.readTree(getFileIO("mock/agwy-base.json")))
                .getOrElseThrow(e -> new Error(e));

        ObjectNode requestContextNode = (ObjectNode) objectNode.get("requestContext");

        // issuer
        if (issuer != null) {
            ((ObjectNode) requestContextNode.get("authorizer")).put("principalId", issuer);
            ((ObjectNode) requestContextNode.get("authorizer")).put("issuer", issuer);
        }
        // resource
        objectNode.put("resource", resource);
        requestContextNode.put("resourcePath", "/" + resource);
        // requestContext.path
        // path & requestContext.resourcePath
        objectNode.put("path", "/" + path);
        requestContextNode.put("path", "/controller/" + path);
        // httpMethod & requestContext.httpMethod
        objectNode.put("httpMethod", httpMethod);
        requestContextNode.put("httpMethod", httpMethod);
        // body
        objectNode.put("body", body);

        // maybe pathParameters
        objectNode.putPOJO("pathParameters", pathParameters);
        // maybe queryStringParameters
        objectNode.putPOJO("queryStringParameters", queryStringParameters);
        // maybe multiValueQueryStringParameters
        objectNode.putPOJO("multiValueQueryStringParameters", multiValueQueryStringParameters);

        objectNode.putPOJO("requestContext", requestContextNode);

        return new ByteArrayInputStream(objectNode.toString().getBytes());

    }

}
